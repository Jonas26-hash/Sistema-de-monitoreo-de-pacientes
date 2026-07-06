package upeu.edu.pe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PeruApiResponse {
    public String dni;
    public String cliente;
    public String nombres;
    public String apellido_paterno;
    public String apellido_materno;
}
