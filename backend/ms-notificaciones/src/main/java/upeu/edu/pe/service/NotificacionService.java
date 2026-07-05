package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificacionService {

    @Inject
    EmailService emailService;

    public List<Notificacion> listar(String search) {
        if (search == null || search.isBlank()) {
            return Notificacion.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return Notificacion.list("LOWER(mensaje) LIKE ?1 OR LOWER(destinatario) LIKE ?1", pattern);
    }

    public List<Notificacion> findByPaciente(Long pacienteId) {
        return Notificacion.list("pacienteId = ?1", pacienteId);
    }

    @Transactional
    public Notificacion crear(Notificacion notificacion) {
        notificacion.fechaEnvio = LocalDateTime.now();
        notificacion.leida = false;
        notificacion.persist();

        if ("EMAIL".equals(notificacion.tipo) && notificacion.destinatario != null && !notificacion.destinatario.isBlank()) {
            try {
                emailService.enviarNotificacion(notificacion.destinatario, "Notificación - Sistema de Monitoreo de Pacientes", notificacion.mensaje);
                notificacion.enviada = true;
            } catch (Exception e) {
                notificacion.enviada = false;
            }
        }

        return notificacion;
    }

    @Transactional
    public void marcarLeida(Long id) {
        Notificacion n = Notificacion.findById(id);
        if (n != null) {
            n.leida = true;
            n.persist();
        }
    }
}
