package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Entity
@Table(name = "cobros")
public class Cobro extends PanacheEntity {

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column
    public String tipo;

    @Column(name = "referencia_id")
    public Long referenciaId;

    @NotNull @DecimalMin("0.01")
    @Column(nullable = false)
    public Double monto;

    @Column
    public String estado;

    @PastOrPresent
    @Column(name = "fecha_cobro")
    public LocalDate fechaCobro;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column(name = "tipo_comprobante")
    public String tipoComprobante;

    @Column(name = "num_documento")
    public String numDocumento;

    @Column(name = "codigo_verificacion", length = 3)
    public String codigoVerificacion;
}
