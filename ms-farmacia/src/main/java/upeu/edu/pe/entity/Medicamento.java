package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "medicamentos")
public class Medicamento extends PanacheEntity {

    @Column(nullable = false, unique = true)
    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "El código solo debe contener letras mayúsculas y números")
    public String codigo;

    @Column(nullable = false)
    @NotBlank(message = "El nombre es obligatorio")
    public String nombre;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column
    public String presentacion;

    @Column
    @Min(value = 0, message = "El stock no puede ser negativo")
    public Integer stock = 0;

    @Column(name = "stock_minimo")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    public Integer stockMinimo = 10;

    @Column(columnDefinition = "TEXT")
    public String contraindicaciones;
}