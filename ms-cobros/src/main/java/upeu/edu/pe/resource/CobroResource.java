package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Cobro;
import java.util.List;

@Path("/cobros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CobroResource {

    @GET
    public List<Cobro> listar() {
        return Cobro.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    public List<Cobro> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cobro.list("pacienteId = ?1", pacienteId);
    }

    @GET
    @Path("/{id}")
    public Cobro buscar(@PathParam("id") Long id) {
        return Cobro.findById(id);
    }

    @POST
    public Response crear(Cobro cobro) {
        cobro.persist();
        return Response.status(Response.Status.CREATED).entity(cobro).build();
    }
}