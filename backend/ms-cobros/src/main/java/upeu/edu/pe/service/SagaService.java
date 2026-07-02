package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import upeu.edu.pe.entity.Cobro;
import upeu.edu.pe.entity.EventOutbox;
import upeu.edu.pe.entity.Servicio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SagaService {

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "saga.notificaciones.url", defaultValue = "http://ms-notificaciones:8080/eventos/saga")
    String notificacionesSagaUrl;

    @ConfigProperty(name = "saga.atencion.url", defaultValue = "http://ms-atencion:8080/eventos/saga")
    String atencionSagaUrl;

    @Transactional
    public String procesarCitaCreada(String payload) {
        try {
            JsonNode cita = mapper.readTree(payload);

            Cobro cobro = new Cobro();
            cobro.pacienteId = cita.has("pacienteId") ? cita.get("pacienteId").asLong() : null;
            cobro.tipo = "CONSULTA";
            cobro.referenciaId = cita.has("citaId") ? cita.get("citaId").asLong() : null;
            cobro.monto = cita.has("precio") ? cita.get("precio").asDouble() : 0.0;
            cobro.estado = "PENDIENTE";
            cobro.fechaCobro = LocalDate.now();
            cobro.descripcion = "Factura por consulta médica";
            cobro.persist();

            EventOutbox eventoSalida = new EventOutbox();
            eventoSalida.eventId = UUID.randomUUID().toString();
            eventoSalida.eventType = "FACTURA_CREADA";
            eventoSalida.source = "ms-cobros";
            eventoSalida.targetUrl = notificacionesSagaUrl;
            eventoSalida.payload = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("cobroId", cobro.id)
                    .put("pacienteId", cobro.pacienteId)
                    .put("monto", cobro.monto)
                    .put("email", cita.has("email") ? cita.get("email").asText() : "")
                    .put("pacienteNombre", cita.has("pacienteNombre") ? cita.get("pacienteNombre").asText() : "Paciente")
            );
            eventoSalida.status = "PENDING";
            eventoSalida.createdAt = LocalDateTime.now();
            eventoSalida.retryCount = 0;
            eventoSalida.persist();

            return "OK";

        } catch (Exception e) {
            throw new RuntimeException("Error processing CITA_CREADA payload", e);
        }
    }

    @Transactional
    public String procesarConsultaCreada(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            long citaId = node.has("citaId") ? node.get("citaId").asLong() : 0;

            if (citaId > 0) {
                long existing = Cobro.count("referenciaId = ?1 AND tipo = 'CONSULTA'", citaId);
                if (existing > 0) return "OK";
            }

            double precio = 50.0;
            List<Servicio> servicios = Servicio.list("tipo = ?1 AND activo = true", "CONSULTA");
            if (!servicios.isEmpty()) {
                precio = servicios.get(0).precio;
            }

            Cobro cobro = new Cobro();
            cobro.pacienteId = node.has("pacienteId") ? node.get("pacienteId").asLong() : null;
            cobro.tipo = "CONSULTA";
            cobro.referenciaId = citaId > 0 ? citaId : (node.has("consultaId") ? node.get("consultaId").asLong() : null);
            cobro.monto = precio;
            cobro.estado = "PENDIENTE";
            cobro.fechaCobro = LocalDate.now();
            cobro.descripcion = "Consulta m\u00e9dica";
            cobro.persist();
            return "OK";
        } catch (Exception e) {
            throw new RuntimeException("Error processing CONSULTA_CREADA payload", e);
        }
    }

    @Transactional
    public String crearEventoFacturaFallida(String payload) {
        try {
            JsonNode cita = mapper.readTree(payload);

            EventOutbox eventoSalida = new EventOutbox();
            eventoSalida.eventId = UUID.randomUUID().toString();
            eventoSalida.eventType = "FACTURA_FALLIDA";
            eventoSalida.source = "ms-cobros";
            eventoSalida.targetUrl = atencionSagaUrl;
            eventoSalida.payload = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("citaId", cita.has("citaId") ? cita.get("citaId").asLong() : 0)
                    .put("pacienteId", cita.has("pacienteId") ? cita.get("pacienteId").asLong() : 0)
                    .put("motivo", "Error al crear factura")
            );
            eventoSalida.status = "PENDING";
            eventoSalida.createdAt = LocalDateTime.now();
            eventoSalida.retryCount = 0;
            eventoSalida.persist();

            return "COMPENSATED";
        } catch (Exception e) {
            throw new RuntimeException("Error creating compensation event", e);
        }
    }
}
