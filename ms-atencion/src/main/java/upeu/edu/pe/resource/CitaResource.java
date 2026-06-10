package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
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
