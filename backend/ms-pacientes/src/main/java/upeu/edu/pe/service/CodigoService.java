package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.LocalDateTime;
import java.util.Random;

@ApplicationScoped
public class CodigoService {

    private static final int DIGITOS = 6;
    private final Random random = new Random();

    @ConfigProperty(name = "codigo.expiracion.minutos", defaultValue = "15")
    int expiracionMinutos;

    public String generarCodigo() {
        return String.format("%0" + DIGITOS + "d", random.nextInt((int) Math.pow(10, DIGITOS)));
    }

    public LocalDateTime calcularExpiracion() {
        return LocalDateTime.now().plusMinutes(expiracionMinutos);
    }

    public boolean codigoValido(String codigoIngresado, String codigoGuardado, LocalDateTime expiracion) {
        return codigoIngresado != null
            && codigoIngresado.equals(codigoGuardado)
            && LocalDateTime.now().isBefore(expiracion);
    }
}
