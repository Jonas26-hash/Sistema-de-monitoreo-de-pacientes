package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificacionService {

    public List<Notificacion> listar() {
        return Notificacion.listAll();
    }

    public List<Notificacion> findByPaciente(Long pacienteId) {
        return Notificacion.list("pacienteId = ?1", pacienteId);
    }

    @Transactional
    public Notificacion crear(Notificacion notificacion) {
        notificacion.fechaEnvio = LocalDateTime.now();
        notificacion.persist();
        return notificacion;
    }
}
