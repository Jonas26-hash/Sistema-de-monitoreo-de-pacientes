package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompletarRegistroRequest {

    @NotBlank @Email
    public String email;

    @Size(min = 6)
    public String password;

    @Size(min = 6)
    public String confirmarPassword;

    public String username;

    public String codigo;
}
