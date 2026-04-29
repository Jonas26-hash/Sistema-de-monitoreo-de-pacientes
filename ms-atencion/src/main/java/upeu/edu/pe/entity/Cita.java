package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "citas")
public class Cita extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    @NotNull(message = "El ID del paciente es obligatorio")
    public Long pacienteId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(name = "fecha_hora", nullable = false)
    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La cita debe programarse en una fecha futura")
    public LocalDateTime fechaHora;

    @Column(nullable = false)
    @NotBlank(message = "El estado es obligatorio")
    public String estado;

    @Column(columnDefinition = "TEXT")
    public String observaciones;

    @Column
    @Size(max = 200, message = "El motivo no debe exceder 200 caracteres")
    public String motivo;

    public static List<Cita> findByPaciente(Long pacienteId) {
        return list("pacienteId = ?1", pacienteId);
    }
}