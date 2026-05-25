package upeu.edu.pe.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    public void enviarCodigoVerificacion(String to, String codigo) {
        mailer.send(
            Mail.withText(to,
                "Código de verificación - Sistema de Monitoreo de Pacientes",
                "Tu código de verificación es: " + codigo + "\n\n"
                + "Este código expira en 10 minutos.\n\n"
                + "Ingresa a: http://localhost:8080/auth/verificar-codigo\n"
                + "y completa tu registro eligiendo tu usuario y contraseña.\n\n"
                + "Si no solicitaste este registro, ignora este mensaje.")
        );
    }

    public void enviarNotificacion(String to, String asunto, String mensaje) {
        mailer.send(
            Mail.withText(to, asunto, mensaje)
        );
    }
}
