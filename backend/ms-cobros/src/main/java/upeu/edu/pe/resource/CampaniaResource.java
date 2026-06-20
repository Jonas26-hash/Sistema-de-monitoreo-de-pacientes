package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Campania;
import upeu.edu.pe.service.CampaniaService;
import java.util.List;

@Path("/campanias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampaniaResource {

    @Inject
    CampaniaService service;

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Campania> listar() {
        return service.listar();
    }

    @GET
    @Path("/activas")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Campania> activas() {
        return service.activas();
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
    @RolesAllowed({"ADMIN"})
    public Response crear(@Valid Campania c) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(c)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response actualizar(@PathParam("id") Long id, @Valid Campania data) {
        try {
            return Response.ok(service.actualizar(id, data)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/toggle")
    @RolesAllowed({"ADMIN"})
    public Response toggle(@PathParam("id") Long id) {
        try {
            return Response.ok(service.toggle(id)).build();
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
            return Response.noContent().build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
