package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Random;

@ApplicationScoped
public class CodigoService {

    private static final int DIGITOS = 6;
    private static final int EXPIRACION_MINUTOS = 10;
    private final Random random = new Random();

    public String generarCodigo() {
        return String.format("%0" + DIGITOS + "d", random.nextInt((int) Math.pow(10, DIGITOS)));
    }

    public LocalDateTime calcularExpiracion() {
        return LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS);
    }

    public boolean codigoValido(String codigoIngresado, String codigoGuardado, LocalDateTime expiracion) {
        return codigoIngresado != null
            && codigoIngresado.equals(codigoGuardado)
            && LocalDateTime.now().isBefore(expiracion);
    }
}
