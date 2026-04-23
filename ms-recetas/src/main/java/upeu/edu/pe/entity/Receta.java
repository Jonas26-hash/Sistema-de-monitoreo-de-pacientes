package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "recetas")
public class Receta extends PanacheEntity {

    @Column(name = "paciente_id", nullable = false)
    public Long pacienteId;

    @Column(name = "doctor_id")
    public Long doctorId;

    @Column(name = "consulta_id")
    public Long consultaId;

    @Column(name = "fecha_emision", nullable = false)
    public LocalDate fechaEmision;

    @Column(name = "fecha_vigencia")
    public LocalDate fechaVigencia;

    @Column(columnDefinition = "TEXT", nullable = false)
    public String medicamentos;

    @Column(columnDefinition = "TEXT")
    public String indicaciones;

    @Column
    public Boolean dispensada = false;

    @Column(name = "fecha_dispensacion")
    public LocalDate fechaDispensacion;
}