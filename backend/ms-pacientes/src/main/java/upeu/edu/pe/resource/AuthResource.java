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
import io.quarkus.panache.common.Page;
import org.mindrot.jbcrypt.BCrypt;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import upeu.edu.pe.Paciente;
import upeu.edu.pe.dto.AuthRequest;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.dto.ChangePasswordRequest;
import upeu.edu.pe.dto.RegisterRequest;
import upeu.edu.pe.dto.UsuarioResponse;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.SistemaConfig;
import upeu.edu.pe.entity.Usuario;
import upeu.edu.pe.service.AuthService;
import upeu.edu.pe.service.CodigoService;
import upeu.edu.pe.service.NotificacionesClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
                .entity("{\"error\":\"Credenciales inválidas\"}")
                .build();
        }
    }

    @POST
    @Path("/register")
    @Transactional
    public Response register(@Valid RegisterRequest request, @Context SecurityContext securityContext) {
        if (Usuario.count() == 0) {
            request.roles = List.of(Rol.ADMIN);
            return registrarUsuario(request);
        }

        if (securityContext == null || !securityContext.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"Token ADMIN requerido\"}")
                .build();
        }

        return registrarUsuario(request);
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
        return registrarUsuario(request);
    }

    private Response registrarUsuario(RegisterRequest request) {
        if (Usuario.findByUsername(request.username) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"Usuario ya existe\"}")
                .build();
        }
        if (Usuario.findByEmail(request.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"Email ya registrado\"}")
                .build();
        }

        boolean esPaciente = request.roles != null && request.roles.stream()
            .anyMatch(r -> r == Rol.PACIENTE);

        Paciente paciente = null;

        if (esPaciente && request.dni != null && !request.dni.isEmpty()) {
            paciente = Paciente.find("dni = ?1", request.dni).firstResult();

            if (paciente == null) {
                paciente = new Paciente();
                paciente.nombres = request.nombres;
                paciente.apellidoPaterno = request.apellidos;
                paciente.dni = request.dni;
                paciente.telefono = request.telefono;
                paciente.email = request.email;
                if (request.fechaNacimiento != null) {
                    paciente.fechaNacimiento = LocalDate.parse(request.fechaNacimiento);
                }
                if (request.direccion != null) {
                    paciente.direccion = request.direccion;
                }
                paciente.activo = true;
                paciente.persist();
            }
        }

        Usuario usuario = new Usuario();
        usuario.username = request.username;
        usuario.password = BCrypt.hashpw(request.password, BCrypt.gensalt());
        usuario.email = request.email;
        usuario.roles = request.roles != null ? request.roles : List.of();
        usuario.activo = true;
        usuario.nombres = request.nombres;
        usuario.apellidos = request.apellidos;
        usuario.especialidad = request.especialidad;
        usuario.dni = request.dni;
        usuario.telefono = request.telefono;

        if (paciente != null) {
            usuario.paciente = paciente;
        }

        usuario.persist();

        UsuarioResponse response = toResponse(usuario);
        if (paciente != null) {
            response.pacienteId = paciente.id;
        }
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/usuarios")
    @RolesAllowed("ADMIN")
    public Response listarUsuarios(@QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("size") @DefaultValue("10") int size) {
        List<UsuarioResponse> content = Usuario.findAll()
            .page(Page.of(page, size))
            .list().stream()
            .map(u -> toResponse((Usuario) u))
            .collect(Collectors.toList());
        long total = Usuario.count();
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return Response.ok(result).build();
    }

    @GET
    @Path("/usuarios/{id}")
    @RolesAllowed("ADMIN")
    public Response buscarUsuario(@PathParam("id") Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }
        return Response.ok(toResponse(usuario)).build();
    }

    @GET
    @Path("/usuarios/rol/{rol}")
    @RolesAllowed("ADMIN")
    public Response listarPorRol(@PathParam("rol") String rol) {
        try {
            List<Usuario> usuarios = Usuario.findByRole(rol);
            return Response.ok(
                usuarios.stream()
                    .map(u -> toResponse(u))
                    .collect(Collectors.toList())
            ).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Rol inválido\"}")
                .build();
        }
    }

    @PUT
    @Path("/usuarios/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response actualizar(@PathParam("id") Long id, RegisterRequest request) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }

        if (request.nombres != null) usuario.nombres = request.nombres;
        if (request.apellidos != null) usuario.apellidos = request.apellidos;
        if (request.email != null) usuario.email = request.email;
        if (request.especialidad != null) usuario.especialidad = request.especialidad;
        if (request.dni != null) usuario.dni = request.dni;
        if (request.telefono != null) usuario.telefono = request.telefono;
        if (request.roles != null) {
            usuario.roles = request.roles;
        }

        return Response.ok(toResponse(usuario)).build();
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
        Usuario usuario = Usuario.findByUsername(username);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }

        if (request.nombres != null) usuario.nombres = request.nombres;
        if (request.apellidos != null) usuario.apellidos = request.apellidos;
        if (request.email != null) usuario.email = request.email;
        if (request.dni != null) usuario.dni = request.dni;
        if (request.telefono != null) usuario.telefono = request.telefono;
        if (request.especialidad != null) usuario.especialidad = request.especialidad;

        return Response.ok(toResponse(usuario)).build();
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
        Usuario usuario = Usuario.findByUsername(username);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }

        if (request.oldPassword == null || request.newPassword == null || request.newPassword.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Contraseña actual y nueva son requeridas\"}")
                .build();
        }

        if (!BCrypt.checkpw(request.oldPassword, usuario.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"Contraseña actual incorrecta\"}")
                .build();
        }

        if (request.newPassword.length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"La nueva contraseña debe tener al menos 6 caracteres\"}")
                .build();
        }

        usuario.password = BCrypt.hashpw(request.newPassword, BCrypt.gensalt());
        return Response.ok("{\"mensaje\":\"Contraseña actualizada exitosamente\"}").build();
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
            // Don't reveal if email exists
            return Response.ok("{\"mensaje\":\"Si el email está registrado, recibirás un código de verificación\"}").build();
        }

        String codigo = codigoService.generarCodigo();
        LocalDateTime expiracion = codigoService.calcularExpiracion();
        forgotCodes.put(email, new ForgotPasswordData(codigo, expiracion));

        try {
            String emailBody = mapper.writeValueAsString(Map.of("to", email, "codigo", codigo,
                "subject", "Recuperación de contraseña - MedTrack",
                "mensaje", "Tu código para restablecer contraseña es: " + codigo));
            notificacionesClient.enviarCorreo(emailBody);
        } catch (Exception e) {
            // Email service error
        }

        return Response.ok("{\"mensaje\":\"Si el email está registrado, recibirás un código de verificación\"}").build();
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
                .entity("{\"error\":\"Email, código y nueva contraseña son requeridos\"}")
                .build();
        }

        if (newPassword.length() < 6) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"La contraseña debe tener al menos 6 caracteres\"}")
                .build();
        }

        ForgotPasswordData data = forgotCodes.get(email);
        if (data == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"No se solicitó recuperación de contraseña para este email\"}")
                .build();
        }

        if (!codigoService.codigoValido(codigo, data.codigo, data.expiracion)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Código inválido o expirado\"}")
                .build();
        }

        Usuario usuario = Usuario.findByEmail(email);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }

        usuario.password = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        forgotCodes.remove(email);

        return Response.ok("{\"mensaje\":\"Contraseña restablecida exitosamente\"}").build();
    }

    @POST
    @Path("/self-register")
    @Transactional
    public Response selfRegister(@Valid RegisterRequest request) {
        if (request.username == null || request.password == null || request.email == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Username, password y email son requeridos\"}")
                .build();
        }

        if (Usuario.findByUsername(request.username) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"El nombre de usuario ya existe\"}")
                .build();
        }
        if (Usuario.findByEmail(request.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"El email ya está registrado\"}")
                .build();
        }

        request.roles = List.of(Rol.PACIENTE);

        if (request.dni != null && !request.dni.isEmpty()) {
            Paciente paciente = Paciente.find("dni = ?1", request.dni).firstResult();
            if (paciente == null) {
                paciente = new Paciente();
                paciente.nombres = request.nombres;
                paciente.apellidoPaterno = request.apellidos;
                paciente.dni = request.dni;
                paciente.telefono = request.telefono;
                paciente.email = request.email;
                paciente.activo = true;
                paciente.persist();
            }
        }

        Usuario usuario = new Usuario();
        usuario.username = request.username;
        usuario.password = BCrypt.hashpw(request.password, BCrypt.gensalt());
        usuario.email = request.email;
        usuario.roles = List.of(Rol.PACIENTE);
        usuario.activo = true;
        usuario.nombres = request.nombres;
        usuario.apellidos = request.apellidos;
        usuario.dni = request.dni;
        usuario.telefono = request.telefono;
        usuario.persist();

        Map<String, String> resp = new HashMap<>();
        resp.put("mensaje", "Registro exitoso. Puedes iniciar sesión.");
        return Response.status(Response.Status.CREATED).entity(resp).build();
    }

    @DELETE
    @Path("/usuarios/{id}")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response eliminar(@PathParam("id") Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }
        usuario.activo = false;
        return Response.ok("{\"mensaje\":\"Usuario desactivado\"}").build();
    }

    @GET
    @Path("/pendientes")
    @RolesAllowed("ADMIN")
    public Response listarPendientes(@QueryParam("page") @DefaultValue("0") int page,
                                     @QueryParam("size") @DefaultValue("10") int size) {
        List<UsuarioResponse> content = Usuario.find("activo = false")
            .page(Page.of(page, size))
            .list().stream()
            .map(u -> toResponse((Usuario) u))
            .collect(Collectors.toList());
        long total = Usuario.count("activo = false");
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return Response.ok(result).build();
    }

    @PUT
    @Path("/pendientes/{id}/aprobar")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response aprobarUsuario(@PathParam("id") Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Usuario no encontrado\"}")
                .build();
        }
        if (usuario.activo) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"El usuario ya está activo\"}")
                .build();
        }
        usuario.activo = true;
        return Response.ok("{\"mensaje\":\"Usuario aprobado exitosamente\"}").build();
    }

    @GET
    @Path("/config")
    @Transactional
    public Response getConfig() {
        SistemaConfig config = SistemaConfig.ensureExists();
        Map<String, Object> map = new HashMap<>();
        map.put("hospitalName", config.hospitalName != null ? config.hospitalName : "");
        map.put("hospitalLogo", config.hospitalLogo != null ? config.hospitalLogo : "");
        return Response.ok(map).build();
    }

    @PUT
    @Path("/config")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response updateConfig(Map<String, String> body) {
        SistemaConfig config = SistemaConfig.ensureExists();
        if (body.containsKey("hospitalName")) config.hospitalName = body.get("hospitalName");
        if (body.containsKey("hospitalLogo")) config.hospitalLogo = body.get("hospitalLogo");
        Map<String, Object> map = new HashMap<>();
        map.put("hospitalName", config.hospitalName != null ? config.hospitalName : "");
        map.put("hospitalLogo", config.hospitalLogo != null ? config.hospitalLogo : "");
        return Response.ok(map).build();
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.id = usuario.id;
        r.username = usuario.username;
        r.email = usuario.email;
        r.roles = usuario.roles != null
            ? usuario.roles.stream().map(Rol::name).toArray(String[]::new)
            : new String[0];
        r.nombres = usuario.nombres;
        r.apellidos = usuario.apellidos;
        r.especialidad = usuario.especialidad;
        r.dni = usuario.dni;
        r.telefono = usuario.telefono;
        r.activo = usuario.activo;
        if (usuario.paciente != null) {
            r.pacienteId = usuario.paciente.id;
        }
        return r;
    }
}
