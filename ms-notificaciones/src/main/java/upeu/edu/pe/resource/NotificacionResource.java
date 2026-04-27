package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Notificacion;
import java.time.LocalDateTime;
import java.util.List;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Notificacion> listar() {
        return Notificacion.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Notificacion> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Notificacion.list("pacienteId = ?1", pacienteId);
    }

    @POST
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(Notificacion notificacion) {
        notificacion.fechaEnvio = LocalDateTime.now();
        notificacion.persist();
        return Response.status(Response.Status.CREATED).entity(notificacion).build();
    }
}