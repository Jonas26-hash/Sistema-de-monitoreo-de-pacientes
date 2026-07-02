package upeu.edu.pe.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.EmailLog;
import java.time.LocalDateTime;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    public void enviarCodigoVerificacion(String to, String codigo, String username, String nombres, String apellidos) {
        enviarCodigoVerificacion(to, codigo, username, nombres, apellidos, false, null);
    }

    public void enviarCodigoVerificacion(String to, String codigo, String username, String nombres, String apellidos, boolean esStaff) {
        enviarCodigoVerificacion(to, codigo, username, nombres, apellidos, esStaff, null);
    }

    public void enviarCodigoVerificacion(String to, String codigo, String username, String nombres, String apellidos, boolean esStaff, String link) {
        String nombreCompleto = (nombres != null && apellidos != null) ? nombres + " " + apellidos : "";
        String usuarioLinea = (username != null && !username.isEmpty())
            ? "<p>Tu nombre de usuario sugerido es: <strong>" + username + "</strong></p>"
            + "<p>Puedes cambiarlo si lo deseas al ingresar al sistema.</p>"
            : "";

        String linkLinea = (link != null && !link.isEmpty())
            ? "<div style='text-align:center;margin:20px 0'>"
            + "<a href='" + link + "' style='display:inline-block;background:#059669;color:#fff;padding:14px 32px;border-radius:10px;text-decoration:none;font-weight:600;font-size:16px'>Completar mi registro</a>"
            + "</div>"
            : "";

        String staffLinea = esStaff
            ? "<div style='background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:12px;margin:16px 0'>"
            + "<p style='color:#991b1b;font-weight:600;margin:0'>Tu cuenta será activada por un administrador. Recibirás un correo cuando esté lista.</p>"
            + "</div>"
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
            + linkLinea
            + staffLinea
            + "<p class='footer'>Si no solicitaste este registro, ignora este mensaje.</p>"
            + "</div></body></html>";

        String asunto = "Código de verificación - Sistema de Monitoreo de Pacientes";
        mailer.send(Mail.withHtml(to, asunto, html));
        registrarEnvio(to, asunto, html, "VERIFICACION");
    }

    public void enviarNotificacion(String to, String asunto, String mensaje) {
        mailer.send(Mail.withText(to, asunto, mensaje));
        registrarEnvio(to, asunto, mensaje, "NOTIFICACION");
    }

    @Transactional
    void registrarEnvio(String to, String asunto, String mensaje, String tipo) {
        EmailLog log = new EmailLog();
        log.destinatario = to;
        log.asunto = asunto;
        log.mensaje = mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
        log.fechaEnvio = LocalDateTime.now();
        log.tipo = tipo;
        log.exitoso = true;
        log.persist();
    }
}
