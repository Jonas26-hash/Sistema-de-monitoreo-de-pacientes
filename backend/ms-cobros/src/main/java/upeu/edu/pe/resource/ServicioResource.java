package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Servicio;
import upeu.edu.pe.service.ServicioService;
import java.util.List;

@Path("/servicios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicioResource {

    @Inject
    ServicioService service;

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Servicio> listar() {
        return service.listar();
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

    @GET
    @Path("/tipo/{tipo}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Servicio> porTipo(@PathParam("tipo") String tipo) {
        return service.porTipo(tipo);
    }

    @POST
    @RolesAllowed({"ADMIN"})
    public Response crear(@Valid Servicio s) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(s)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response actualizar(@PathParam("id") Long id, @Valid Servicio data) {
        try {
            return Response.ok(service.actualizar(id, data)).build();
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
