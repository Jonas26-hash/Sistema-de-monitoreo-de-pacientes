package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.dto.CorreoRequest;
import upeu.edu.pe.entity.Notificacion;
import upeu.edu.pe.service.EmailService;
import java.time.LocalDateTime;
import java.util.List;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    @Inject
    EmailService emailService;

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
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(@Valid Notificacion notificacion) {
        notificacion.fechaEnvio = LocalDateTime.now();
        notificacion.persist();
        return Response.status(Response.Status.CREATED).entity(notificacion).build();
    }

    @POST
    @Path("/enviar-correo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enviarCorreo(@Valid CorreoRequest request) {
        try {
            emailService.enviarCodigoVerificacion(request.to, request.codigo);
            return Response.ok("{\"mensaje\":\"Correo enviado exitosamente\"}").build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error al enviar correo\"}")
                .build();
        }
    }
}