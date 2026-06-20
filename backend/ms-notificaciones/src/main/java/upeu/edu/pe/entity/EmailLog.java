package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
public class EmailLog extends PanacheEntity {

    @Column(nullable = false)
    public String destinatario;

    @Column(nullable = false)
    public String asunto;

    @Column(columnDefinition = "TEXT")
    public String mensaje;

    @Column(nullable = false)
    public LocalDateTime fechaEnvio;

    @Column(nullable = false)
    public String tipo;

    public boolean exitoso = true;
}
