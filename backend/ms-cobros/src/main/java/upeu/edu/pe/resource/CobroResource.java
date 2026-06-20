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
    public List<Cobro> listar() {
        return service.listar();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
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
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response deudasPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Response.ok(service.deudasPaciente(pacienteId)).build();
    }

    @POST
    @Path("/pago-unico")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response pagoUnico(String body) {
        try {
            return Response.status(Response.Status.CREATED)
                .entity(service.pagoUnico(body)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
