package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import upeu.edu.pe.entity.Cita;
import upeu.edu.pe.entity.EventOutbox;
import upeu.edu.pe.entity.IdempotencyRecord;
import upeu.edu.pe.service.IdempotencyService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/citas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CitaResource {

    @Inject
    ObjectMapper mapper;

    @Inject
    IdempotencyService idempotencyService;

    @ConfigProperty(name = "saga.cobros.url", defaultValue = "http://ms-cobros:8080/eventos/saga")
    String cobrosSagaUrl;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Cita> listar() {
        return Cita.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Cita> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cita.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Cita cita = Cita.findById(id);
        if (cita == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Cita no encontrada\"}").build();
        }
        return Response.ok(cita).build();
    }

    @GET
    @Path("/doctores-ocupados")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response doctoresOcupados(@QueryParam("fechaHora") String fechaHoraStr) {
        try {
            LocalDateTime fechaHora = LocalDateTime.parse(fechaHoraStr);
            LocalDateTime start = fechaHora.minusMinutes(30);
            LocalDateTime end = fechaHora.plusMinutes(30);
            List<Cita> conflictos = Cita.list(
                "fechaHora >= ?1 AND fechaHora <= ?2 AND doctorId IS NOT NULL AND estado != 'CANCELADA'",
                start, end);
            List<Long> ocupados = conflictos.stream()
                .map(c -> c.doctorId).distinct().toList();
            return Response.ok(ocupados).build();
        } catch (Exception e) {
            return Response.ok(java.util.Collections.emptyList()).build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    @Transactional
    public Response crear(@Valid Cita cita, @HeaderParam("Idempotency-Key") String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyService.findExisting(idempotencyKey);
            if (existing != null) {
                return Response.status(existing.responseStatus)
                    .entity(existing.responseBody)
                    .type(MediaType.APPLICATION_JSON)
                    .header("X-Idempotent", "replayed")
                    .build();
            }
        }

        if (cita.estado == null || cita.estado.isBlank()) {
            cita.estado = "PENDIENTE";
        }
        if (cita.precio == null) {
            cita.precio = 0.0;
        }
        cita.persist();

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

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String fullJson = mapper.writeValueAsString(cita);
                idempotencyService.saveRecord(idempotencyKey, fullJson, 201);
            } catch (Exception e) {
                idempotencyService.saveRecord(idempotencyKey, "{\"id\":" + cita.id + "}", 201);
            }
        }

        return Response.status(Response.Status.CREATED)
            .entity(cita)
            .header("X-Idempotent", "new")
            .build();
    }

    @POST
    @Path("/por-dni")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    @Transactional
    public Response crearPorDni(Map<String, Object> body, @HeaderParam("Idempotency-Key") String idempotencyKey) {
        String dni = (String) body.get("dni");
        if (dni == null || dni.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"DNI es obligatorio\"}").build();
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
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Paciente no encontrado con DNI " + dni + "\"}").build();
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyService.findExisting(idempotencyKey);
            if (existing != null) {
                return Response.status(existing.responseStatus)
                    .entity(existing.responseBody)
                    .type(MediaType.APPLICATION_JSON)
                    .header("X-Idempotent", "replayed")
                    .build();
            }
        }

        Cita cita = new Cita();
        cita.pacienteId = pacienteId;
        Object doctorIdObj = body.get("doctorId");
        cita.doctorId = doctorIdObj != null ? ((Number) doctorIdObj).longValue() : null;
        cita.fechaHora = LocalDateTime.parse((String) body.get("fechaHora"));
        cita.motivo = (String) body.get("motivo");
        cita.observaciones = (String) body.get("observaciones");
        cita.estado = "PROGRAMADA";
        cita.precio = 0.0;
        cita.persist();

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

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String fullJson = mapper.writeValueAsString(cita);
                idempotencyService.saveRecord(idempotencyKey, fullJson, 201);
            } catch (Exception e) {
                idempotencyService.saveRecord(idempotencyKey, "{\"id\":" + cita.id + "}", 201);
            }
        }

        return Response.status(Response.Status.CREATED)
            .entity(cita)
            .header("X-Idempotent", "new")
            .build();
    }
}
