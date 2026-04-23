package upeu.edu.pe.resource;

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
    public List<Dispensacion> listar() {
        return Dispensacion.listAll();
    }

    @GET
    @Path("/receta/{recetaId}")
    public List<Dispensacion> findByReceta(@PathParam("recetaId") Long recetaId) {
        return Dispensacion.list("recetaId = ?1", recetaId);
    }

    @POST
    public Response crear(Dispensacion dispensacion) {
        dispensacion.persist();
        return Response.status(Response.Status.CREATED).entity(dispensacion).build();
    }
}