package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Cita;
import java.util.List;

@Path("/citas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CitaResource {

    @GET
    public List<Cita> listar() {
        return Cita.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    public List<Cita> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cita.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    public Cita buscar(@PathParam("id") Long id) {
        return Cita.findById(id);
    }

    @POST
    public Response crear(Cita cita) {
        cita.persist();
        return Response.status(Response.Status.CREATED).entity(cita).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, Cita citaActualizado) {
        Cita cita = Cita.findById(id);
        if (cita == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        cita.fechaHora = citaActualizado.fechaHora;
        cita.estado = citaActualizado.estado;
        cita.observaciones = citaActualizado.observaciones;
        return Response.ok(cita).build();
    }
}