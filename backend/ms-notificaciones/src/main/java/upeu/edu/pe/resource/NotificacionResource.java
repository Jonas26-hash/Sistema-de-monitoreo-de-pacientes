package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import upeu.edu.pe.dto.CorreoPersonalizadoRequest;
import upeu.edu.pe.dto.CorreoRequest;
import upeu.edu.pe.entity.Notificacion;
import upeu.edu.pe.service.EmailService;
import upeu.edu.pe.service.NotificacionService;
import java.util.List;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacionResource {

    private static final Logger log = LoggerFactory.getLogger(NotificacionResource.class);

    @Inject
    NotificacionService notificacionService;

    @Inject
    EmailService emailService;

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Notificacion> listar(@QueryParam("search") String search) {
        return notificacionService.listar(search);
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Notificacion> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return notificacionService.findByPaciente(pacienteId);
    }

    @POST
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(@Valid Notificacion notificacion) {
        return Response.status(Response.Status.CREATED)
            .entity(notificacionService.crear(notificacion)).build();
    }

    @PUT
    @Path("/{id}/leer")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE", "PACIENTE"})
    public Response marcarLeida(@PathParam("id") Long id) {
        notificacionService.marcarLeida(id);
        return Response.ok().build();
    }

    @POST
    @Path("/enviar-correo")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enviarCorreo(@Valid CorreoRequest request) {
        try {
            emailService.enviarCodigoVerificacion(request.to, request.codigo, request.username, request.nombres, request.apellidos, request.esStaff, request.link);
            return Response.ok("{\"mensaje\":\"Correo enviado exitosamente\"}").build();
        } catch (Exception e) {
            log.error("Error enviando correo de verificacion", e);
            return Response.serverError()
                .entity("{\"error\":\"Error al enviar correo\"}")
                .build();
        }
    }

    @POST
    @Path("/enviar-correo-personalizado")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enviarCorreoPersonalizado(@Valid CorreoPersonalizadoRequest request) {
        try {
            emailService.enviarNotificacion(request.to, request.asunto, request.mensaje);
            return Response.ok("{\"mensaje\":\"Correo enviado exitosamente\"}").build();
        } catch (Exception e) {
            log.error("Error enviando correo personalizado", e);
            return Response.serverError()
                .entity("{\"error\":\"Error al enviar correo\"}")
                .build();
        }
    }
}
