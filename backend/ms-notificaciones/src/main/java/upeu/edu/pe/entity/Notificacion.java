package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion extends PanacheEntity {

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @NotBlank
    @Column(nullable = false)
    public String tipo;

    @NotBlank
    @Column(nullable = false)
    public String mensaje;

    @Column(name = "fecha_envio")
    public LocalDateTime fechaEnvio;

    @Column
    public Boolean enviada = false;

    @Column
    public String canal;

    @Column
    public Boolean leida = false;

    @Column(name = "remitente_id")
    public Long remitenteId;

    public String remitenteTipo;

    public String remitenteNombre;
}
