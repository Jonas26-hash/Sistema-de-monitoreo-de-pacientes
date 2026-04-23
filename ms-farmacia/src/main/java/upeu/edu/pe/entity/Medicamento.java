package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "medicamentos")
public class Medicamento extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String codigo;

    @Column(nullable = false)
    public String nombre;

    @Column(columnDefinition = "TEXT")
    public String descripcion;

    @Column
    public String presentacion;

    @Column
    public Integer stock = 0;

    @Column(name = "stock_minimo")
    public Integer stockMinimo = 10;

    @Column(columnDefinition = "TEXT")
    public String contraindicaciones;
}