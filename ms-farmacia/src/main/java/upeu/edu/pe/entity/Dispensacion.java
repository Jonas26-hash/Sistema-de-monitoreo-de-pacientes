package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Entity
@Table(name = "dispensaciones")
public class Dispensacion extends PanacheEntity {

    @NotNull
    @Column(name = "receta_id", nullable = false)
    public Long recetaId;

    @NotNull
    @Column(name = "medicamento_id", nullable = false)
    public Long medicamentoId;

    @NotNull @Min(1)
    @Column(name = "cantidad", nullable = false)
    public Integer cantidad;

    @NotNull @PastOrPresent
    @Column(name = "fecha_dispensacion", nullable = false)
    public LocalDate fechaDispensacion;

    @Column(name = "farmaceutico_id")
    public Long farmaceuticoId;

    @Column(columnDefinition = "TEXT")
    public String observaciones;
}
