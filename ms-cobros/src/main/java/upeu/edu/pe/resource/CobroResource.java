package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    @Transactional
    @RolesAllowed({"ADMIN", "CAJERO", "ATENCION_CLIENTE"})
    public List<Cobro> listar() {
        return Cobro.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @Transactional
    @RolesAllowed({"ADMIN", "CAJERO", "ATENCION_CLIENTE"})
    public List<Cobro> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cobro.list("pacienteId = ?1", pacienteId);
    }

    @GET
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "CAJERO", "ATENCION_CLIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Cobro cobro = Cobro.findById(id);
        if (cobro == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Cobro no encontrado\"}").build();
        }
        return Response.ok(cobro).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "CAJERO", "ATENCION_CLIENTE"})
    public Response crear(@Valid Cobro cobro) {
        cobro.persist();
        return Response.status(Response.Status.CREATED).entity(cobro).build();
    }
}
