package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "recetas")
public class Receta extends PanacheEntity {

    @NotNull
    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(name = "consulta_id")
    public Long consultaId;

    @NotNull
    @Column(name = "fecha_emision", nullable = false)
    public LocalDate fechaEmision;

    @Column(name = "fecha_vigencia")
    public LocalDate fechaVigencia;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    public String medicamentos;

    @Column(columnDefinition = "TEXT")
    public String indicaciones;

    @Column(nullable = false)
    public boolean dispensada = false;

    @Column
    public Boolean pagado;

    @Column(name = "fecha_dispensacion")
    public LocalDate fechaDispensacion;

    @Column
    public Double costo;
}
