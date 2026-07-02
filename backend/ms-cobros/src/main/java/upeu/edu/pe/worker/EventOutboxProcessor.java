package upeu.edu.pe.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import upeu.edu.pe.dto.SagaEvent;
import upeu.edu.pe.entity.EventOutbox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@ApplicationScoped
public class EventOutboxProcessor {

    @Inject
    ObjectMapper mapper;

    @Inject
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void processEvent(EventOutbox event, HttpClient client) {
        try {
            event = em.merge(event);

            SagaEvent sagaEvent = new SagaEvent();
            sagaEvent.eventId = event.eventId;
            sagaEvent.eventType = event.eventType;
            sagaEvent.source = event.source;
            sagaEvent.payload = event.payload;

            String json = mapper.writeValueAsString(sagaEvent);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(event.targetUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                event.status = "SENT";
                event.processedAt = LocalDateTime.now();
            } else {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            event.retryCount++;
            String msg = e.getMessage();
            event.lastError = msg != null ? msg.substring(0, Math.min(msg.length(), 500)) : "Unknown error";
            if (event.retryCount >= 5) {
                event.status = "FAILED";
            }
        }
    }
}
