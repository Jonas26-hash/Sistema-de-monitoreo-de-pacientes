package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ordenes_examen")
public class OrdenExamen extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "cita_id")
    public Long citaId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(nullable = false)
    public String tipo;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column(columnDefinition = "TEXT")
    public String resultado;

    @Column
    @DecimalMin("0.00")
    public Double costo;

    @Column(nullable = false)
    public String estado;

    @Column(name = "fecha_orden", nullable = false)
    public LocalDateTime fechaOrden;

    @Column(name = "fecha_resultado")
    public LocalDateTime fechaResultado;

    @Column(name = "pagado")
    public Boolean pagado;

    public static List<OrdenExamen> findByPaciente(Long pacienteId) {
        return list("pacienteId = ?1 ORDER BY fechaOrden DESC", pacienteId);
    }

    public static List<OrdenExamen> findByCita(Long citaId) {
        return list("citaId = ?1", citaId);
    }

    public static List<OrdenExamen> findPendientesByPaciente(Long pacienteId) {
        return list("pacienteId = ?1 AND estado = 'PENDIENTE' AND (pagado IS NULL OR pagado = false)", pacienteId);
    }
}
