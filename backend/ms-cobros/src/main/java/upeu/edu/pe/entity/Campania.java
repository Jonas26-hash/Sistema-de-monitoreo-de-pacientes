package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "campanias")
public class Campania extends PanacheEntity {

    @Column(nullable = false, unique = true)
    @NotBlank
    public String codigo;

    @Column(nullable = false)
    @NotBlank
    public String nombre;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @NotNull
    @Column(name = "descuento_porcentaje", nullable = false)
    @Min(0)
    @Max(100)
    public Integer descuentoPorcentaje;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    public LocalDate fechaInicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    public LocalDate fechaFin;

    @Column
    public Boolean activo = true;
}