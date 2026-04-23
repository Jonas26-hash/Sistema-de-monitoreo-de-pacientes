package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "citas")
public class Cita extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(name = "fecha_hora", nullable = false)
    public LocalDateTime fechaHora;

    @Column(nullable = false)
    public String estado;

    @Column(columnDefinition = "TEXT")
    public String observaciones;

    @Column
    public String motivo;

    public static List<Cita> findByPaciente(Long pacienteId) {
        return list("pacienteId = ?1", pacienteId);
    }
}