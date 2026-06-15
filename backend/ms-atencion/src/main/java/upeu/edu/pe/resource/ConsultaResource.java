package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Consulta> listar() {
        return Consulta.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Consulta> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Consulta.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Consulta consulta = Consulta.findById(id);
        if (consulta == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Consulta no encontrada\"}").build();
        }
        return Response.ok(consulta).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response crear(@Valid Consulta consulta) {
        consulta.persist();
        return Response.status(Response.Status.CREATED).entity(consulta).build();
    }
}