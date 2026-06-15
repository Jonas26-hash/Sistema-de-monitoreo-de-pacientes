package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord extends PanacheEntity {

    @Column(unique = true, nullable = false, length = 100)
    public String idempotencyKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String responseBody;

    public int responseStatus;

    public LocalDateTime createdAt;

    public static IdempotencyRecord findByKey(String key) {
        return find("idempotencyKey", key).firstResult();
    }
}
