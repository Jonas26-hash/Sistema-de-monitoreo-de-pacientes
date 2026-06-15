package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import upeu.edu.pe.service.AuditService;

@Path("/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditResource {

    @Inject
    AuditService auditService;

    @POST
    public Response recibir(Map<String, Object> body) {
        String username = (String) body.getOrDefault("username", null);
        String accion = (String) body.get("accion");
        String recurso = (String) body.get("recurso");
        Integer statusCode = body.get("statusCode") != null
            ? ((Number) body.get("statusCode")).intValue() : null;
        Long tiempoMs = body.get("tiempoMs") != null
            ? ((Number) body.get("tiempoMs")).longValue() : null;
        String ip = (String) body.getOrDefault("ip", null);
        String userAgent = (String) body.getOrDefault("userAgent", null);
        String requestId = (String) body.getOrDefault("requestId", null);

        if (accion == null || recurso == null || statusCode == null || tiempoMs == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Faltan campos obligatorios: accion, recurso, statusCode, tiempoMs\"}")
                .build();
        }

        auditService.guardar(username, accion, recurso, statusCode, tiempoMs, ip, userAgent, requestId);
        return Response.ok("{\"mensaje\":\"Audit log registrado\"}").build();
    }

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "FARMACEUTICO", "ATENCION_CLIENTE", "ENFERMERO", "PACIENTE"})
    public Response listar(
            @QueryParam("username") String username,
            @QueryParam("accion") String accion,
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {

        if (size > 200) size = 200;
        if (page < 0) page = 0;
        if (size < 1) size = 50;

        LocalDateTime desdeDt = null;
        if (desde != null && !desde.isBlank()) {
            try { desdeDt = LocalDate.parse(desde).atStartOfDay(); }
            catch (Exception e) { /* ignore */ }
        }
        LocalDateTime hastaDt = null;
        if (hasta != null && !hasta.isBlank()) {
            try { hastaDt = LocalDate.parse(hasta).atTime(LocalTime.MAX); }
            catch (Exception e) { /* ignore */ }
        }

        Map<String, Object> result = auditService.listar(username, accion, desdeDt, hastaDt, page, size);
        return Response.ok(result).build();
    }
}
