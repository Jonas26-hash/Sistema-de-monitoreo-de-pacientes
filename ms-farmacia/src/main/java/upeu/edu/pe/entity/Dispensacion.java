package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dispensaciones")
public class Dispensacion extends PanacheEntity {

    @Column(name = "receta_id", nullable = false)
    public Long recetaId;

    @Column(name = "medicamento_id", nullable = false)
    public Long medicamentoId;

    @Column(name = "cantidad", nullable = false)
    public Integer cantidad;

    @Column(name = "fecha_dispensacion", nullable = false)
    public LocalDate fechaDispensacion;

    @Column(name = "farmaceutico_id")
    public Long farmaceuticoId;

    @Column(columnDefinition = "TEXT")
    public String observaciones;
}