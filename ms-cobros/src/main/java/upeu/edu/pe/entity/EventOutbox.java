package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "event_outbox")
public class EventOutbox extends PanacheEntity {

    public String eventId;

    public String eventType;

    public String source;

    @Column(length = 500)
    public String targetUrl;

    @Column(columnDefinition = "TEXT")
    public String payload;

    @Column(name = "outbox_status")
    public String status;

    public LocalDateTime createdAt;

    public LocalDateTime processedAt;

    public int retryCount;

    @Column(columnDefinition = "TEXT")
    public String lastError;

    public static List<EventOutbox> findPending() {
        return list("status", "PENDING");
    }
}
