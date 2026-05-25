package upeu.edu.pe.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import upeu.edu.pe.dto.SagaEvent;
import upeu.edu.pe.entity.EventOutbox;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class EventOutboxWorker {

    @Inject
    ObjectMapper mapper;

    @Scheduled(every = "10s")
    @Transactional
    public void processPending() {
        List<EventOutbox> pending = EventOutbox.findPending();
        HttpClient client = HttpClient.newHttpClient();

        for (EventOutbox event : pending) {
            try {
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
            event.persist();
        }
    }
}
