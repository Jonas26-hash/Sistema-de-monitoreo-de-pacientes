package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cobros")
public class Cobro extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column
    public String tipo;

    @Column(name = "referencia_id")
    public Long referenciaId;

    @Column(nullable = false)
    public Double monto;

    @Column
    public String estado;

    @Column(name = "fecha_cobro")
    public LocalDate fechaCobro;

    @Column(columnDefinition = "TEXT")
    public String descripcion;
}