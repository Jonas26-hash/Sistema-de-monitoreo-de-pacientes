package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerificarCodigoRequest {

    @NotBlank @Email
    public String email;

    @NotBlank
    public String codigo;
}
