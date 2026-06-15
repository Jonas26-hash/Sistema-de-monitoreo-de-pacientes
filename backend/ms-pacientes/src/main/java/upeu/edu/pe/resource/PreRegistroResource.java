package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mindrot.jbcrypt.BCrypt;
import upeu.edu.pe.Paciente;
import upeu.edu.pe.dto.CompletarRegistroRequest;
import upeu.edu.pe.dto.PreRegistroRequest;
import upeu.edu.pe.dto.VerificarCodigoRequest;
import upeu.edu.pe.entity.RegistroPendiente;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;
import upeu.edu.pe.service.CodigoService;
import upeu.edu.pe.service.NotificacionesClient;
import upeu.edu.pe.service.UsernameGenerator;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class PreRegistroResource {

    @Inject
    CodigoService codigoService;

    @Inject @RestClient
    NotificacionesClient notificacionesClient;

    @Inject
    ObjectMapper mapper;

    @Inject
    UsernameGenerator usernameGenerator;

    @POST
    @Path("/pre-registro")
    @Transactional
    public Response preRegistro(@Valid PreRegistroRequest request) {
        if (Usuario.findByEmail(request.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"Email ya registrado\"}")
                .build();
        }

        String username = request.nombres != null && request.apellidos != null
            ? usernameGenerator.generar(request.nombres, request.apellidos)
            : null;

        RegistroPendiente pendiente = RegistroPendiente.find("email", request.email).firstResult();

        String codigo = codigoService.generarCodigo();
        LocalDateTime expiracion = codigoService.calcularExpiracion();

        if (pendiente == null) {
            pendiente = new RegistroPendiente();
            pendiente.email = request.email;
            pendiente.creadoEn = LocalDateTime.now();
        }

        pendiente.nombres = request.nombres;
        pendiente.apellidos = request.apellidos;
        pendiente.dni = request.dni;
        pendiente.telefono = request.telefono;
        if (request.fechaNacimiento != null && !request.fechaNacimiento.isEmpty()) {
            pendiente.fechaNacimiento = LocalDate.parse(request.fechaNacimiento);
        }
        pendiente.direccion = request.direccion;
        pendiente.rolSolicitado = Rol.PACIENTE.name();
        pendiente.codigoVerificacion = codigo;
        pendiente.codigoExpiracion = expiracion;
        pendiente.verificado = false;
        pendiente.usernameSugerido = username;
        pendiente.persist();

        try {
            String body = mapper.writeValueAsString(Map.of(
                "to", request.email,
                "codigo", codigo,
                "username", username != null ? username : "",
                "nombres", request.nombres != null ? request.nombres : "",
                "apellidos", request.apellidos != null ? request.apellidos : ""
            ));
            notificacionesClient.enviarCorreo(body);
        } catch (Exception e) {
            // Email service error logged, user can request resend
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mensaje", "Código de verificación enviado a tu correo");
        if (username != null) {
            response.put("usernameSugerido", username);
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/pre-registro-personal")
    @Transactional
    @RolesAllowed("ADMIN")
    public Response preRegistroPersonal(@Valid PreRegistroRequest request) {
        if (Usuario.findByEmail(request.email) != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity("{\"error\":\"Email ya registrado\"}")
                .build();
        }

        if (request.rolSolicitado == Rol.ADMIN) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"No puedes crear otro administrador\"}")
                .build();
        }

        String username = request.nombres != null && request.apellidos != null
            ? usernameGenerator.generar(request.nombres, request.apellidos)
            : null;

        RegistroPendiente pendiente = RegistroPendiente.find("email", request.email).firstResult();

        String codigo = codigoService.generarCodigo();
        LocalDateTime expiracion = codigoService.calcularExpiracion();

        if (pendiente == null) {
            pendiente = new RegistroPendiente();
            pendiente.email = request.email;
            pendiente.creadoEn = LocalDateTime.now();
        }

        pendiente.nombres = request.nombres;
        pendiente.apellidos = request.apellidos;
        pendiente.dni = request.dni;
        pendiente.telefono = request.telefono;
        if (request.fechaNacimiento != null && !request.fechaNacimiento.isEmpty()) {
            pendiente.fechaNacimiento = LocalDate.parse(request.fechaNacimiento);
        }
        pendiente.direccion = request.direccion;
        pendiente.rolSolicitado = request.rolSolicitado.name();
        pendiente.codigoVerificacion = codigo;
        pendiente.codigoExpiracion = expiracion;
        pendiente.verificado = false;
        pendiente.usernameSugerido = username;
        pendiente.persist();

        try {
            String body = mapper.writeValueAsString(Map.of(
                "to", request.email,
                "codigo", codigo,
                "username", username != null ? username : "",
                "nombres", request.nombres != null ? request.nombres : "",
                "apellidos", request.apellidos != null ? request.apellidos : ""
            ));
            notificacionesClient.enviarCorreo(body);
        } catch (Exception e) {
            // Email service error logged, user can request resend
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("mensaje", "Código de verificación enviado al correo del empleado");
        if (username != null) {
            response.put("usernameSugerido", username);
        }
        return Response.ok(response).build();
    }

    @POST
    @Path("/verificar-codigo")
    @Transactional
    public Response verificarCodigo(@Valid VerificarCodigoRequest request) {
        RegistroPendiente pendiente = RegistroPendiente.find("email", request.email).firstResult();

        if (pendiente == null || pendiente.verificado) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"No hay registro pendiente para este email\"}")
                .build();
        }

        if (!codigoService.codigoValido(request.codigo, pendiente.codigoVerificacion, pendiente.codigoExpiracion)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Código inválido o expirado\"}")
                .build();
        }

        pendiente.verificado = true;
        pendiente.persist();

        return Response.ok("{\"mensaje\":\"Código verificado correctamente\"}").build();
    }

    @POST
    @Path("/completar-registro")
    @Transactional
    public Response completarRegistro(@Valid CompletarRegistroRequest request) {
        RegistroPendiente pendiente = RegistroPendiente.find("email", request.email).firstResult();

        if (pendiente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"No hay registro pendiente para este email\"}")
                .build();
        }

        if (!pendiente.verificado) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Debes verificar tu código primero\"}")
                .build();
        }

        if (!request.password.equals(request.confirmarPassword)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Las contraseñas no coinciden\"}")
                .build();
        }

        Rol rol = Rol.valueOf(pendiente.rolSolicitado);
        boolean autoActivar = rol == Rol.PACIENTE;

        Usuario usuario = new Usuario();
        usuario.username = (request.username != null && !request.username.isEmpty())
            ? request.username : pendiente.email;
        usuario.password = BCrypt.hashpw(request.password, BCrypt.gensalt());
        usuario.email = pendiente.email;
        usuario.nombres = pendiente.nombres;
        usuario.apellidos = pendiente.apellidos;
        usuario.dni = pendiente.dni;
        usuario.telefono = pendiente.telefono;
        usuario.roles = List.of(rol);
        usuario.activo = autoActivar;

        if (rol == Rol.PACIENTE && pendiente.dni != null && !pendiente.dni.isEmpty()) {
            Paciente paciente = Paciente.find("dni", pendiente.dni).firstResult();
            if (paciente == null) {
                paciente = new Paciente();
                paciente.nombres = pendiente.nombres;
                paciente.apellidoPaterno = pendiente.apellidos;
                paciente.dni = pendiente.dni;
                paciente.telefono = pendiente.telefono;
                paciente.email = pendiente.email;
                if (pendiente.fechaNacimiento != null) {
                    paciente.fechaNacimiento = pendiente.fechaNacimiento;
                }
                if (pendiente.direccion != null) {
                    paciente.direccion = pendiente.direccion;
                }
                paciente.activo = true;
                paciente.persist();
            }
            usuario.paciente = paciente;
        }

        usuario.persist();

        pendiente.delete();

        String mensaje = autoActivar
            ? "Registro exitoso"
            : "Registro exitoso. Tu cuenta está pendiente de aprobación por un administrador.";

        return Response.status(Response.Status.CREATED)
            .entity(mapper.createObjectNode().put("mensaje", mensaje))
            .build();
    }
}
