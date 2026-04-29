package upeu.edu.pe.resource;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Cita> listar() {
        return Cita.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Cita> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cita.findByPaciente(pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Cita buscar(@PathParam("id") Long id) {
        return Cita.findById(id);
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response crear(@Valid Cita cita) {
        cita.persist();
        return Response.status(Response.Status.CREATED).entity(cita).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response actualizar(@PathParam("id") Long id, @Valid Cita citaActualizado) {
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