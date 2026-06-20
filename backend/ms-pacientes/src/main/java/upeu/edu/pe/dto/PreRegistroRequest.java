package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import upeu.edu.pe.entity.Rol;

public class PreRegistroRequest {

    @NotBlank @Email
    public String email;

    @NotBlank
    public String nombres;

    @NotBlank
    public String apellidos;

    public String dni;

    public String telefono;

    public String fechaNacimiento;

    public String direccion;

    public Rol rolSolicitado;

    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "Username: solo letras, números y guión bajo (3-50 caracteres)")
    public String username;

    public String password;
}
