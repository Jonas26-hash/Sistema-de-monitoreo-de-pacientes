package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.OrdenExamen;
import upeu.edu.pe.service.OrdenExamenService;
import java.util.List;

@Path("/ordenes-examen")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenExamenResource {

    @Inject
    OrdenExamenService service;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "ENFERMERO"})
    public List<OrdenExamen> listar() {
        return service.listar();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<OrdenExamen> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.findByPaciente(pacienteId);
    }

    @GET
    @Path("/cita/{citaId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<OrdenExamen> findByCita(@PathParam("citaId") Long citaId) {
        return service.findByCita(citaId);
    }

    @GET
    @Path("/pendientes/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<OrdenExamen> pendientesByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.pendientesByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.buscar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response crear(@Valid OrdenExamen examen) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(examen)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response actualizar(@PathParam("id") Long id, @Valid OrdenExamen examen) {
        try {
            return Response.ok(service.actualizar(id, examen)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/resultado")
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response ingresarResultado(@PathParam("id") Long id, String body) {
        try {
            return Response.ok(service.ingresarResultado(id, body)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/pagar")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response pagar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.pagar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        try {
            service.eliminar(id);
            return Response.ok("{\"mensaje\":\"Orden de examen eliminada\"}").build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
