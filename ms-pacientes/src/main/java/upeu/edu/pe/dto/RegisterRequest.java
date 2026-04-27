package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import upeu.edu.pe.entity.Rol;

public class RegisterRequest {

    @NotBlank
    public String username;

    @NotBlank
    public String password;

    @Email
    @NotBlank
    public String email;

    public List<Rol> roles;

    public Long pacienteId;

    public String nombres;

    public String apellidos;

    public String especialidad;

    public String dni;

    public String telefono;

    public String fechaNacimiento;

    public String direccion;
}