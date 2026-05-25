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

    @NotNull
    public Rol rolSolicitado;
}
