package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Dispensacion;
import upeu.edu.pe.service.DispensacionService;
import java.util.List;

@Path("/dispensaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DispensacionResource {

    @Inject
    DispensacionService service;

    @GET
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> listar(@QueryParam("search") String search) {
        return service.listar(search);
    }

    @GET
    @Path("/receta/{recetaId}")
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> findByReceta(@PathParam("recetaId") Long recetaId) {
        return service.findByReceta(recetaId);
    }

    @POST
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response crear(@Valid Dispensacion dispensacion) {
        try {
            return Response.status(Response.Status.CREATED)
                .entity(service.crear(dispensacion)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
