package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.OrdenExamen;
import java.time.LocalDateTime;
import java.util.List;

@Path("/ordenes-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenExamenResource {

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "ENFERMERO"})
    public List<OrdenExamen> listar() {
        return OrdenExamen.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<OrdenExamen> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return OrdenExamen.findByPaciente(pacienteId);
    }

    @GET
    @Path("/cita/{citaId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<OrdenExamen> findByCita(@PathParam("citaId") Long citaId) {
        return OrdenExamen.findByCita(citaId);
    }

    @GET
    @Path("/pendientes/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<OrdenExamen> pendientesByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return OrdenExamen.findPendientesByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        OrdenExamen examen = OrdenExamen.findById(id);
        if (examen == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Orden de examen no encontrada\"}").build();
        }
        return Response.ok(examen).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response crear(@Valid OrdenExamen examen) {
        if (examen.fechaOrden == null) {
            examen.fechaOrden = LocalDateTime.now();
        }
        if (examen.estado == null || examen.estado.isBlank()) {
            examen.estado = "PENDIENTE";
        }
        if (examen.pagado == null) {
            examen.pagado = false;
        }
        examen.persist();
        return Response.status(Response.Status.CREATED).entity(examen).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response actualizar(@PathParam("id") Long id, @Valid OrdenExamen examen) {
        OrdenExamen existing = OrdenExamen.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Orden de examen no encontrada\"}").build();
        }
        existing.tipo = examen.tipo;
        existing.descripcion = examen.descripcion;
        existing.costo = examen.costo;
        existing.persist();
        return Response.ok(existing).build();
    }

    @PUT
    @Path("/{id}/resultado")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response ingresarResultado(@PathParam("id") Long id, String body) {
        OrdenExamen existing = OrdenExamen.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Orden de examen no encontrada\"}").build();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode json = mapper.readTree(body);
            if (json.has("resultado")) {
                existing.resultado = json.get("resultado").asText();
            }
            existing.estado = "COMPLETADO";
            existing.fechaResultado = LocalDateTime.now();
            existing.persist();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"JSON inválido\"}").build();
        }
        return Response.ok(existing).build();
    }

    @PUT
    @Path("/{id}/pagar")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response pagar(@PathParam("id") Long id) {
        OrdenExamen existing = OrdenExamen.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Orden de examen no encontrada\"}").build();
        }
        existing.pagado = true;
        existing.persist();
        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        OrdenExamen existing = OrdenExamen.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Orden de examen no encontrada\"}").build();
        }
        existing.delete();
        return Response.ok("{\"mensaje\":\"Orden de examen eliminada\"}").build();
    }
}
