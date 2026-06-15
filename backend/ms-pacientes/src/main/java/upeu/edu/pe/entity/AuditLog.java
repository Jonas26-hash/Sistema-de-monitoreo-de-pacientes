package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog extends PanacheEntity {

    @Column(length = 100)
    public String username;

    @Column(nullable = false, length = 10)
    public String accion;

    @Column(nullable = false, length = 512)
    public String recurso;

    @Column(nullable = false)
    public Integer statusCode;

    @Column(nullable = false)
    public Long tiempoMs;

    @Column(length = 50)
    public String ip;

    @Column(length = 512)
    public String userAgent;

    @Column(length = 50)
    public String requestId;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
