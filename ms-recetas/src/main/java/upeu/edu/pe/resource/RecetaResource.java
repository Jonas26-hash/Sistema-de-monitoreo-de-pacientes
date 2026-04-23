package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Receta;
import java.util.List;

@Path("/recetas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecetaResource {

    @GET
    public List<Receta> listar() {
        return Receta.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    public List<Receta> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Receta.list("pacienteId = ?1", pacienteId);
    }

    @GET
    @Path("/{id}")
    public Receta buscar(@PathParam("id") Long id) {
        return Receta.findById(id);
    }

    @POST
    public Response crear(Receta receta) {
        receta.persist();
        return Response.status(Response.Status.CREATED).entity(receta).build();
    }
}