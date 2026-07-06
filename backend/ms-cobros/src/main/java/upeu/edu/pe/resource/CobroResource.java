package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Cobro;
import upeu.edu.pe.service.CobroService;
import java.util.List;

@Path("/cobros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CobroResource {

    @Inject
    CobroService service;

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Cobro> listar(@QueryParam("search") String search) {
        return service.listar(search);
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Cobro> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.buscar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(@Valid Cobro cobro) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(cobro)).build();
    }

    @GET
    @Path("/deudas/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public Response deudasPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Response.ok(service.deudasPaciente(pacienteId)).build();
    }

    @GET
    @Path("/pendientes-agrupados")
    public List<Cobro> pendientesAgrupados() {
        return Cobro.list("estado = 'PENDIENTE' ORDER BY pacienteId");
    }

    @POST
    @Path("/pago-unico")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public Response pagoUnico(byte[] bodyBytes) {
        try {
            String bodyStr = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            return Response.status(Response.Status.CREATED)
                .entity(service.pagoUnico(bodyStr)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/pendientes-verificacion")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Cobro> pendientesVerificacion() {
        return service.pendientesVerificacion();
    }

    @PUT
    @Path("/{id}/verificar")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response verificar(@PathParam("id") Long id, String body) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(body);
            String codigo = json.has("codigoVerificacion") ? json.get("codigoVerificacion").asText() : "";
            return Response.ok(service.verificar(id, codigo)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Error al verificar: " + e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }
}
