package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CorreoRequest {
    @NotBlank @Email
    public String to;
    @NotBlank
    public String codigo;
}
