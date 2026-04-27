package upeu.edu.pe;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente extends PanacheEntity {

    @Column(nullable = false)
    public String nombres;

    @Column(nullable = false)
    public String apellidoPaterno;

    @Column
    public String apellidoMaterno;

    @Column(nullable = false, unique = true)
    public String dni;

    @Column
    public LocalDate fechaNacimiento;

    @Column
    public String genero;

    @Column
    public String direccion;

    @Column
    public String telefono;

    @Column
    public String email;

    @Column(name = "antecedentes_familiares")
    public String antecedentesFamiliares;

    @Column
    public String alergias;

    @Column
    public String condiciones;

    @Column
    public String medicamentosActual;

    @Column
    public String nombreSeguro;

    @Column
    public String numeroPoliza;

    @Column
    public LocalDate vigenciaSeguro;

    @Column
    public Boolean activo = true;
}