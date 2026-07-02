package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "servicios")
public class Servicio extends PanacheEntity {

    @Column(nullable = false, unique = true)
    @NotBlank
    public String codigo;

    @Column(nullable = false)
    @NotBlank
    public String nombre;

    @Column(nullable = false)
    @NotBlank
    public String tipo;

    @NotNull
    @Column(nullable = false)
    @DecimalMin("0.00")
    public Double precio;

    @Column
    public Boolean activo = true;
}
