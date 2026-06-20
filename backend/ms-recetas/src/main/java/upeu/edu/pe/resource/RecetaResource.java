package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Receta;
import upeu.edu.pe.service.RecetaService;
import java.util.List;

@Path("/recetas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecetaResource {

    @Inject
    RecetaService service;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Receta> listar() {
        return service.listar();
    }

    @GET
    @Path("/pendientes")
    @RolesAllowed({"ADMIN", "FARMACEUTICO", "ATENCION_CLIENTE"})
    public List<Receta> pendientes() {
        return service.pendientes();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Receta> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.findByPaciente(pacienteId);
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
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response crear(@Valid Receta receta) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(receta)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response actualizar(@PathParam("id") Long id, @Valid Receta receta) {
        try {
            return Response.ok(service.actualizar(id, receta)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/dispensar")
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response dispensar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.dispensar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
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

    @GET
    @Path("/pendientes-pago/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Receta> pendientesPagoByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return service.pendientesPagoByPaciente(pacienteId);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        try {
            service.eliminar(id);
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
