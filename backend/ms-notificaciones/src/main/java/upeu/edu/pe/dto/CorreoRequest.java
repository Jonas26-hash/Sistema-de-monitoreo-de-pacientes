package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CorreoRequest {
    @NotBlank @Email
    public String to;
    @NotBlank
    public String codigo;
    public String username;
    public String nombres;
    public String apellidos;
    public boolean esStaff;
    public String link;
}
