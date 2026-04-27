package upeu.edu.pe.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import upeu.edu.pe.entity.Rol;

public class RegisterRequest {

    @NotBlank
    public String username;

    @NotBlank
    public String password;

    @NotBlank
    public String email;

    public List<Rol> roles;

    public Long pacienteId;
}