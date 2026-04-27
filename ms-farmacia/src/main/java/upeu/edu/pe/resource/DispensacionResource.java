package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Dispensacion;
import java.util.List;

@Path("/dispensaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DispensacionResource {

    @GET
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> listar() {
        return Dispensacion.listAll();
    }

    @GET
    @Path("/receta/{recetaId}")
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> findByReceta(@PathParam("recetaId") Long recetaId) {
        return Dispensacion.list("recetaId = ?1", recetaId);
    }

    @POST
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response crear(Dispensacion dispensacion) {
        dispensacion.persist();
        return Response.status(Response.Status.CREATED).entity(dispensacion).build();
    }
}