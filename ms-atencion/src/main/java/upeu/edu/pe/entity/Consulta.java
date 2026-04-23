package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "consultas")
public class Consulta extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "cita_id")
    public Long citaId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(name = "fecha_consulta", nullable = false)
    public LocalDateTime fechaConsulta;

    @Column(columnDefinition = "TEXT")
    public String sintomas;

    @Column(columnDefinition = "TEXT")
    public String diagnostico;

    @Column(columnDefinition = "TEXT")
    public String tratamiento;

    @Column(columnDefinition = "TEXT")
    public String observaciones;

    public static List<Consulta> findByPaciente(Long pacienteId) {
        return list("pacienteId = ?1", pacienteId);
    }
}