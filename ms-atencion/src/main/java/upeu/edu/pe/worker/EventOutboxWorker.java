package upeu.edu.pe.worker;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import upeu.edu.pe.entity.EventOutbox;

import java.net.http.HttpClient;
import java.util.List;

@ApplicationScoped
public class EventOutboxWorker {

    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    EventOutboxProcessor processor;

    @Scheduled(every = "10s")
    public void processPending() {
        List<EventOutbox> pending = EventOutbox.findPending();
        for (EventOutbox event : pending) {
            processor.processEvent(event, client);
        }
    }
}
