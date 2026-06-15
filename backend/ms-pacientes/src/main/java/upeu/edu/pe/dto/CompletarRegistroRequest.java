package upeu.edu.pe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompletarRegistroRequest {

    @NotBlank @Email
    public String email;

    @NotBlank @Size(min = 6)
    public String password;

    @NotBlank @Size(min = 6)
    public String confirmarPassword;

    public String username;
}
