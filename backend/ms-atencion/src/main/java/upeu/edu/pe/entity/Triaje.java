package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "triajes")
public class Triaje extends PanacheEntity {

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "cita_id")
    public Long citaId;

    @Column(name = "enfermero_id")
    public Long enfermeroId;

    @NotNull
    @Column(name = "fecha_triaje", nullable = false)
    public LocalDateTime fechaTriaje;

    @Column
    public Double peso;

    @Column
    public Double talla;

    @Column(name = "presion_sistolica")
    public Integer presionSistolica;

    @Column(name = "presion_diastolica")
    public Integer presionDiastolica;

    @Column
    public Double temperatura;

    @Column(name = "frecuencia_cardiaca")
    public Integer frecuenciaCardiaca;

    @Column(name = "spo2")
    public Double spo2;

    @Column(name = "frecuencia_respiratoria")
    public Integer frecuenciaRespiratoria;

    @Column(name = "motivo_consulta", columnDefinition = "TEXT")
    public String motivoConsulta;

    @Column(columnDefinition = "TEXT")
    public String observaciones;

    public static List<Triaje> findByPaciente(Long pacienteId) {
        return list("pacienteId = ?1 ORDER BY fechaTriaje DESC", pacienteId);
    }

    public static List<Triaje> findByCita(Long citaId) {
        return list("citaId = ?1", citaId);
    }
}
