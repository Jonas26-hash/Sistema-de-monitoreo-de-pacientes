package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Cita;
import upeu.edu.pe.entity.IdempotencyRecord;
import upeu.edu.pe.service.CitaService;
import upeu.edu.pe.service.IdempotencyService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/citas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CitaResource {

    @Inject
    CitaService citaService;

    @Inject
    IdempotencyService idempotencyService;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Cita> listar(@QueryParam("search") String search) {
        return citaService.listar(search);
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Cita> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return citaService.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(citaService.buscar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/doctores-ocupados")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response doctoresOcupados(@QueryParam("fechaHora") String fechaHoraStr) {
        return Response.ok(citaService.doctoresOcupados(fechaHoraStr)).build();
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

        Cita result;
        try {
            result = citaService.crear(cita, idempotencyKey);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                String fullJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
                idempotencyService.saveRecord(idempotencyKey, fullJson, 201);
            } catch (Exception e) {
                idempotencyService.saveRecord(idempotencyKey, "{\"id\":" + result.id + "}", 201);
            }
        }

        return Response.status(Response.Status.CREATED)
            .entity(result)
            .header("X-Idempotent", "new")
            .build();
    }

    @GET
    @Path("/proximas")
    public List<Cita> proximas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime maniana = ahora.plusHours(24);
        return Cita.list("fechaHora >= ?1 AND fechaHora < ?2 AND estado IN ('PROGRAMADA', 'CONFIRMADA')", ahora, maniana);
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    @Transactional
    public Response actualizar(@PathParam("id") Long id, @Valid Cita citaActualizada) {
        try {
            Cita cita = citaService.buscar(id);
            cita.pacienteId = citaActualizada.pacienteId;
            cita.doctorId = citaActualizada.doctorId;
            cita.fechaHora = citaActualizada.fechaHora;
            cita.estado = citaActualizada.estado;
            cita.observaciones = citaActualizada.observaciones;
            cita.motivo = citaActualizada.motivo;
            cita.precio = citaActualizada.precio;
            cita.persist();
            return Response.ok(cita).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/por-dni")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    @Transactional
    public Response crearPorDni(byte[] bodyBytes, @HeaderParam("Idempotency-Key") String idempotencyKey) {
        try {
            String bodyStr = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            ObjectMapper localMapper = new ObjectMapper();
            Map<String, Object> body = localMapper.readValue(bodyStr, Map.class);

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

            Cita result = citaService.crearPorDni(body, idempotencyKey);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                try {
                    String fullJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
                    idempotencyService.saveRecord(idempotencyKey, fullJson, 201);
                } catch (Exception e) {
                    idempotencyService.saveRecord(idempotencyKey, "{\"id\":" + result.id + "}", 201);
                }
            }

            return Response.status(Response.Status.CREATED)
                .entity(result)
                .header("X-Idempotent", "new")
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
