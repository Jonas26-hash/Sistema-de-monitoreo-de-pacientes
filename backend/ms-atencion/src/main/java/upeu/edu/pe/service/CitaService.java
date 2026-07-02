package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import upeu.edu.pe.entity.Cita;
import upeu.edu.pe.entity.EventOutbox;
import upeu.edu.pe.entity.IdempotencyRecord;
import upeu.edu.pe.service.IdempotencyService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CitaService {

    @Inject
    ObjectMapper mapper;

    @Inject
    IdempotencyService idempotencyService;

    @ConfigProperty(name = "saga.cobros.url", defaultValue = "http://ms-cobros:8080/eventos/saga")
    String cobrosSagaUrl;

    public List<Cita> listar(String search) {
        if (search == null || search.isBlank()) {
            return Cita.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return Cita.list("LOWER(motivo) LIKE ?1 OR LOWER(observaciones) LIKE ?1", pattern);
    }

    public List<Cita> findByPaciente(Long pacienteId) {
        return Cita.findByPaciente(pacienteId);
    }

    public Cita buscar(Long id) {
        Cita c = Cita.findById(id);
        if (c == null) throw new NotFoundException("Cita no encontrada");
        return c;
    }

    public List<Long> doctoresOcupados(String fechaHoraStr) {
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraStr);
            LocalDateTime start = fechaHora.minusMinutes(30);
            LocalDateTime end = fechaHora.plusMinutes(30);
            List<Cita> conflictos = Cita.list(
                "fechaHora >= ?1 AND fechaHora <= ?2 AND doctorId IS NOT NULL AND estado != 'CANCELADA'",
                start, end);
            return conflictos.stream().map(c -> c.doctorId).distinct().toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Transactional
    public Cita crear(Cita cita, String idempotencyKey) {
        if (cita.fechaHora != null && cita.fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La cita debe programarse en una fecha futura");
        }
        if (cita.estado == null || cita.estado.isBlank()) {
            cita.estado = "PENDIENTE";
        }
        if (cita.precio == null || cita.precio == 0.0) {
            cita.precio = obtenerPrecioConsulta();
        }
        cita.persist();
        crearOutbox(cita);
        return cita;
    }

    @Transactional
    public Cita crearPorDni(Map<String, Object> body, String idempotencyKey) {
        String dni = (String) body.get("dni");
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("DNI es obligatorio");
        }

        Long pacienteId;
        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-pacientes:8080")
                .path("pacientes/dni/" + dni)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode paciente = mapper.readTree(json);
            pacienteId = paciente.get("id").asLong();
        } catch (Exception e) {
            throw new NotFoundException("Paciente no encontrado con DNI " + dni);
        }

        Cita cita = new Cita();
        cita.pacienteId = pacienteId;
        Object doctorIdObj = body.get("doctorId");
        cita.doctorId = doctorIdObj != null ? ((Number) doctorIdObj).longValue() : null;
        cita.fechaHora = LocalDateTime.parse((String) body.get("fechaHora"));
        if (cita.fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La cita debe programarse en una fecha futura");
        }
        cita.motivo = (String) body.get("motivo");
        cita.observaciones = (String) body.get("observaciones");
        cita.estado = "PROGRAMADA";
        cita.precio = obtenerPrecioConsulta();
        cita.persist();
        crearOutbox(cita);
        return cita;
    }

    private double obtenerPrecioConsulta() {
        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-cobros:8080")
                .path("servicios/tipo/CONSULTA")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode servicios = mapper.readTree(json);
            if (servicios.isArray() && servicios.size() > 0) {
                return servicios.get(0).get("precio").asDouble();
            }
        } catch (Exception ignored) {}
        return 50.0;
    }

    private void crearOutbox(Cita cita) {
        try {
            EventOutbox outbox = new EventOutbox();
            outbox.eventId = UUID.randomUUID().toString();
            outbox.eventType = "CITA_CREADA";
            outbox.source = "ms-atencion";
            outbox.targetUrl = cobrosSagaUrl;
            outbox.payload = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("citaId", cita.id)
                    .put("pacienteId", cita.pacienteId)
                    .put("doctorId", cita.doctorId != null ? cita.doctorId : 0)
                    .put("fechaHora", cita.fechaHora != null ? cita.fechaHora.toString() : "")
                    .put("motivo", cita.motivo != null ? cita.motivo : "")
                    .put("email", "")
                    .put("pacienteNombre", "")
                    .put("precio", cita.precio)
            );
            outbox.status = "PENDING";
            outbox.createdAt = LocalDateTime.now();
            outbox.retryCount = 0;
            outbox.persist();
        } catch (Exception e) {
            throw new RuntimeException("Error creating saga outbox event", e);
        }
    }
}
