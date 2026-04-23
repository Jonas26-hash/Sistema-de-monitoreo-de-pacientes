package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Consulta;
import java.util.List;

@Path("/consultas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConsultaResource {

    @GET
    public List<Consulta> listar() {
        return Consulta.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    public List<Consulta> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Consulta.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    public Consulta buscar(@PathParam("id") Long id) {
        return Consulta.findById(id);
    }

    @POST
    public Response crear(Consulta consulta) {
        consulta.persist();
        return Response.status(Response.Status.CREATED).entity(consulta).build();
    }
}