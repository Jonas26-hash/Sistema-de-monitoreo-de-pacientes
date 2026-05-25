package upeu.edu.pe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.Notificacion;
import java.time.LocalDateTime;

@ApplicationScoped
public class SagaService {

    @Inject
    ObjectMapper mapper;

    @Inject
    EmailService emailService;

    @Transactional
    public void procesarFacturaCreada(String payload) {
        try {
            JsonNode factura = mapper.readTree(payload);
            String email = factura.has("email") ? factura.get("email").asText() : "";
            String nombre = factura.has("pacienteNombre") ? factura.get("pacienteNombre").asText() : "Paciente";
            Long pacienteId = factura.has("pacienteId") ? factura.get("pacienteId").asLong() : null;
            Double monto = factura.has("monto") ? factura.get("monto").asDouble() : 0;

            Notificacion notif = new Notificacion();
            notif.pacienteId = pacienteId;
            notif.tipo = "FACTURA_CREADA";
            notif.mensaje = "Se ha generado una factura por consulta médica por S/ " + monto;
            notif.fechaEnvio = LocalDateTime.now();
            notif.enviada = true;
            notif.canal = "EMAIL";
            notif.persist();

            if (!email.isEmpty()) {
                emailService.enviarNotificacion(email,
                    "Factura generada - Sistema de Monitoreo de Pacientes",
                    "Estimado " + nombre + ",\n\n"
                    + "Se ha generado una factura por su consulta médica.\n"
                    + "Monto: S/ " + monto + "\n"
                    + "Estado: PENDIENTE\n\n"
                    + "Puede realizar el pago a través del sistema.\n\n"
                    + "Saludos,\nSistema de Monitoreo de Pacientes");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing FACTURA_CREADA payload", e);
        }
    }
}
