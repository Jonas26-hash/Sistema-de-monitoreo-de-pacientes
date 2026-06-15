package upeu.edu.pe;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente extends PanacheEntity {

    @Column(nullable = false)
    @NotBlank(message = "Los nombres son obligatorios")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "Los nombres solo deben contener letras")
    public String nombres;

    @Column(nullable = false)
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras")
    public String apellidoPaterno;

    @Column
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El apellido materno solo debe contener letras")
    public String apellidoMaterno;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos numéricos")
    public String dni;

    @Column
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    public LocalDate fechaNacimiento;

    @Column
    public String genero;

    @Column
    public String direccion;

    @Column
    @Pattern(regexp = "^\\d{9}$", message = "El teléfono debe tener 9 dígitos numéricos")
    public String telefono;

    @Column
    @Email(message = "El email debe tener un formato válido")
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