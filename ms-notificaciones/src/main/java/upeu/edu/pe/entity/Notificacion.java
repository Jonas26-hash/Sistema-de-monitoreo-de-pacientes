package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(nullable = false)
    public String tipo;

    @Column(nullable = false)
    public String mensaje;

    @Column(name = "fecha_envio")
    public LocalDateTime fechaEnvio;

    @Column
    public Boolean enviada = false;

    @Column
    public String canal;
}