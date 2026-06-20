package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.validation.Valid;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import upeu.edu.pe.dto.AuthRequest;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.dto.ChangePasswordRequest;
import upeu.edu.pe.dto.RegisterRequest;
import upeu.edu.pe.dto.UsuarioResponse;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;
import upeu.edu.pe.service.AuthService;
import upeu.edu.pe.service.CodigoService;
import upeu.edu.pe.service.NotificacionesClient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    CodigoService codigoService;

    @Inject @RestClient
    NotificacionesClient notificacionesClient;

    @Inject
    ObjectMapper mapper;

    private static final ConcurrentHashMap<String, ForgotPasswordData> forgotCodes = new ConcurrentHashMap<>();

    private static class ForgotPasswordData {
        String codigo;
        LocalDateTime expiracion;
        ForgotPasswordData(String codigo, LocalDateTime expiracion) {
            this.codigo = codigo;
            this.expiracion = expiracion;
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Valid AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request.username, request.password);
            return Response.ok(response).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/register")
    @Transactional
    public Response register(@Valid RegisterRequest request, @Context SecurityContext securityContext) {
        if (Usuario.count() == 0) {
            request.roles = List.of(Rol.ADMIN);
            return ejecutarRegistro(request);
        }

        if (securityContext == null || !securityContext.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"Token ADMIN requerido\"}")
                .build();
        }

        return ejecutarRegistro(request);
    }

    @POST
    @Path("/register-init")
    @Transactional
    public Response registerBootstrap(@Valid RegisterRequest request) {
        if (Usuario.count() > 0) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"Solo para inicializar sistema\"}")
                .build();
        }
        request.roles = List.of(Rol.ADMIN);
        return ejecutarRegistro(request);
    }

    private Response ejecutarRegistro(RegisterRequest request) {
        try {
            UsuarioResponse response = authService.registrarUsuario(request);
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/usuarios")
    @RolesAllowed("ADMIN")
    public Response listarUsuarios(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("10") int size) {
        return Response.ok(authService.listarUsuarios(page, size)).build();
    }

    @GET
    @Path("/usuarios/{id}")
    @RolesAllowed("ADMIN")
    public Response buscarUsuario(@PathParam("id") Long id) {
        Usuario usuario = authService.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }
        return Response.ok(authService.toResponse(usuario)).build();
    }

    @GET
    @Path("/usuarios/rol/{rol}")
    @RolesAllowed("ADMIN")
    public Response listarPorRol(@PathParam("rol") String rol) {
        try {
            return Response.ok(authService.listarPorRol(rol)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Rol inv\u00e1lido\"}")
                .build();
        }
    }

    @PUT
    @Path("/usuarios/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response actualizar(@PathParam("id") Long id, RegisterRequest request) {
        try {
            return Response.ok(authService.actualizarUsuario(id, request)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/profile")
    @RolesAllowed({"ADMIN", "DOCTOR", "FARMACEUTICO", "ATENCION_CLIENTE", "ENFERMERO", "PACIENTE"})
    public Response obtenerPerfil(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"No autenticado\"}")
                .build();
        }
        String username = securityContext.getUserPrincipal().getName();
        Usuario usuario = authService.findByUsername(username);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }
        return Response.ok(authService.toResponse(usuario)).build();
    }

    @PUT
    @Path("/profile")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "FARMACEUTICO", "ATENCION_CLIENTE", "ENFERMERO", "PACIENTE"})
    public Response actualizarPerfil(RegisterRequest request, @Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"No autenticado\"}")
                .build();
        }
        String username = securityContext.getUserPrincipal().getName();
        try {
            return Response.ok(authService.actualizarPerfil(request, username)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @PUT
    @Path("/change-password")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "FARMACEUTICO", "ATENCION_CLIENTE", "ENFERMERO", "PACIENTE"})
    public Response changePassword(ChangePasswordRequest request, @Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"No autenticado\"}")
                .build();
        }
        String username = securityContext.getUserPrincipal().getName();
        try {
            authService.changePassword(username, request.oldPassword, request.newPassword);
            return Response.ok("{\"mensaje\":\"Contrase\u00f1a actualizada exitosamente\"}").build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/forgot-password")
    @Transactional
    public Response forgotPassword(Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Email requerido\"}")
                .build();
        }

        Usuario usuario = Usuario.findByEmail(email);
        if (usuario == null) {
            return Response.ok("{\"mensaje\":\"Si el email est\u00e1 registrado, recibir\u00e1s un c\u00f3digo de verificaci\u00f3n\"}").build();
        }

        String codigo = codigoService.generarCodigo();
        LocalDateTime expiracion = codigoService.calcularExpiracion();
        forgotCodes.put(email, new ForgotPasswordData(codigo, expiracion));

        try {
            String emailBody = mapper.writeValueAsString(Map.of("to", email, "codigo", codigo,
                "subject", "Recuperaci\u00f3n de contrase\u00f1a - MedTrack",
                "mensaje", "Tu c\u00f3digo para restablecer contrase\u00f1a es: " + codigo));
            notificacionesClient.enviarCorreo(emailBody);
        } catch (Exception e) {
            // Email service error - non-blocking
        }

        return Response.ok("{\"mensaje\":\"Si el email est\u00e1 registrado, recibir\u00e1s un c\u00f3digo de verificaci\u00f3n\"}").build();
    }

    @POST
    @Path("/reset-password")
    @Transactional
    public Response resetPassword(Map<String, String> body) {
        String email = body.get("email");
        String codigo = body.get("codigo");
        String newPassword = body.get("newPassword");

        if (email == null || codigo == null || newPassword == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Email, c\u00f3digo y nueva contrase\u00f1a son requeridos\"}")
                .build();
        }

        if (newPassword.length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"La contrase\u00f1a debe tener al menos 6 caracteres\"}")
                .build();
        }

        ForgotPasswordData data = forgotCodes.get(email);
        if (data == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"No se solicit\u00f3 recuperaci\u00f3n de contrase\u00f1a para este email\"}")
                .build();
        }

        if (!codigoService.codigoValido(codigo, data.codigo, data.expiracion)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"C\u00f3digo inv\u00e1lido o expirado\"}")
                .build();
        }

        Usuario usuario = Usuario.findByEmail(email);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }

        try {
            authService.changePassword(usernameFromEmail(email), "", newPassword);
        } catch (Exception e) {
            // BCrypt needs old password, so handle directly
        }

        org.mindrot.jbcrypt.BCrypt.checkpw(newPassword, usuario.password);
        try {
            usuario.password = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        } catch (Exception e) {
            usuario.password = newPassword;
        }
        forgotCodes.remove(email);

        return Response.ok("{\"mensaje\":\"Contrase\u00f1a restablecida exitosamente\"}").build();
    }

    private String usernameFromEmail(String email) {
        Usuario u = Usuario.findByEmail(email);
        return u != null ? u.username : "";
    }

    @POST
    @Path("/self-register")
    @Transactional
    public Response selfRegister(@Valid RegisterRequest request) {
        try {
            authService.selfRegister(request);
            return Response.status(Response.Status.CREATED)
                .entity("{\"mensaje\":\"Registro exitoso. Puedes iniciar sesi\u00f3n.\"}")
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @DELETE
    @Path("/usuarios/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response eliminar(@PathParam("id") Long id) {
        try {
            Usuario usuario = authService.findById(id);
            String userEmail = usuario != null ? usuario.email : "";
            String userNombres = usuario != null ? usuario.nombres : "";

            authService.eliminarUsuario(id);

            // Notify user (non-blocking)
            if (userEmail != null && !userEmail.isEmpty()) {
                try {
                    String emailBody = mapper.writeValueAsString(Map.of(
                        "to", userEmail,
                        "asunto", "Tu cuenta ha sido desactivada",
                        "mensaje", "Hola " + (userNombres != null ? userNombres : "") +
                            ", tu cuenta en el Sistema de Monitoreo de Pacientes ha sido desactivada. Contacta al administrador para m\u00e1s informaci\u00f3n."
                    ));
                    notificacionesClient.enviarCorreoPersonalizado(emailBody);
                } catch (Exception ignored) {}
            }

            return Response.ok("{\"mensaje\":\"Usuario desactivado\"}").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/pendientes")
    @RolesAllowed("ADMIN")
    public Response listarPendientes(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("10") int size) {
        return Response.ok(authService.listarPendientes(page, size)).build();
    }

    @PUT
    @Path("/pendientes/{id}/aprobar")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response aprobarUsuario(@PathParam("id") Long id) {
        try {
            Usuario usuario = authService.findById(id);
            String userEmail = usuario != null ? usuario.email : "";
            String userNombres = usuario != null ? usuario.nombres : "";

            authService.aprobarUsuario(id);

            if (userEmail != null && !userEmail.isEmpty()) {
                try {
                    String emailBody = mapper.writeValueAsString(Map.of(
                        "to", userEmail,
                        "asunto", "Tu cuenta ha sido activada",
                        "mensaje", "Hola " + (userNombres != null ? userNombres : "") +
                            ", tu cuenta en el Sistema de Monitoreo de Pacientes ha sido activada. Ya puedes iniciar sesi\u00f3n con tu usuario."
                    ));
                    notificacionesClient.enviarCorreoPersonalizado(emailBody);
                } catch (Exception ignored) {}
            }

            return Response.ok("{\"mensaje\":\"Usuario aprobado exitosamente\"}").build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/config")
    public Response getConfig() {
        return Response.ok(authService.getConfig()).build();
    }

    @PUT
    @Path("/config")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response updateConfig(Map<String, String> body) {
        return Response.ok(authService.updateConfig(body)).build();
    }
}
