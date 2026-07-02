package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Triaje;
import upeu.edu.pe.service.TriajeService;
import java.util.List;

@Path("/triajes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TriajeResource {

    @Inject
    TriajeService service;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE"})
    public List<Triaje> listar(@QueryParam("search") String search) {
        return service.listar(search);
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Triaje> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.findByPaciente(pacienteId);
    }

    @GET
    @Path("/cita/{citaId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE"})
    public List<Triaje> findByCita(@PathParam("citaId") Long citaId) {
        return service.findByCita(citaId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.buscar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "ENFERMERO"})
    public Response crear(@Valid Triaje triaje) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(triaje)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ENFERMERO"})
    public Response actualizar(@PathParam("id") Long id, @Valid Triaje triaje) {
        try {
            return Response.ok(service.actualizar(id, triaje)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
