package upeu.edu.pe.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    public void enviarCodigoVerificacion(String to, String codigo, String username, String nombres, String apellidos) {
        String nombreCompleto = (nombres != null && apellidos != null) ? nombres + " " + apellidos : "";
        String usuarioLinea = (username != null)
            ? "<p>Tu nombre de usuario sugerido es: <strong>" + username + "</strong></p>"
            + "<p>Puedes cambiarlo si lo deseas al ingresar al link.</p>"
            : "";

        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body{font-family:Arial,sans-serif;background:#f5f7fb;padding:40px 20px}"
            + ".card{max-width:480px;margin:0 auto;background:#fff;border-radius:16px;padding:32px;box-shadow:0 4px 20px rgba(0,0,0,0.08)}"
            + "h2{color:#1e293b;font-size:20px;margin:0 0 16px}"
            + ".code{font-size:40px;font-weight:700;color:#059669;text-align:center;letter-spacing:8px;background:#f0fdf4;border-radius:12px;padding:20px;margin:20px 0}"
            + "p{color:#475569;font-size:14px;line-height:1.6;margin:8px 0}"
            + ".link{color:#059669;font-weight:600;text-decoration:none}"
            + ".footer{color:#94a3b8;font-size:12px;margin-top:24px;text-align:center}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<h2>" + (nombreCompleto.isEmpty() ? "Hola," : "Hola, " + nombreCompleto) + "</h2>"
            + "<p>Tu código de verificación es:</p>"
            + "<div class='code'>" + codigo + "</div>"
            + "<p>Este código expira en 10 minutos.</p>"
            + usuarioLinea
            + "<p>Ingresa a: <a class='link' href='http://localhost:3000/verificacion?email=" + to
            + (username != null ? "&amp;username=" + username : "") + "'>http://localhost:3000/verificacion</a></p>"
            + "<p>y completa tu registro eligiendo tu usuario y contraseña.</p>"
            + "<p class='footer'>Si no solicitaste este registro, ignora este mensaje.</p>"
            + "</div></body></html>";

        mailer.send(
            Mail.withHtml(to,
                "Código de verificación - Sistema de Monitoreo de Pacientes",
                html)
        );
    }

    public void enviarNotificacion(String to, String asunto, String mensaje) {
        mailer.send(
            Mail.withText(to, asunto, mensaje)
        );
    }
}
