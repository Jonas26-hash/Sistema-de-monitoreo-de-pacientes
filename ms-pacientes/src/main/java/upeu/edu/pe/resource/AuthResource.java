package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.Paciente;
import upeu.edu.pe.dto.AuthRequest;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.dto.RegisterRequest;
import upeu.edu.pe.dto.UsuarioResponse;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;
import upeu.edu.pe.service.AuthService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(AuthRequest request) {
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
    @RolesAllowed("ADMIN")
    public Response register(RegisterRequest request) {
        // Si es el primer usuario del sistema, permitir sin token
        if (Usuario.count() == 0) {
            return registrarUsuario(request);
        }

        // Si ya hay usuarios, se requiere token ADMIN (manejado por @RolesAllowed)
        return registrarUsuario(request);
    }

    @POST
    @Path("/register-init")
    @Transactional
    public Response registerBootstrap(RegisterRequest request) {
        // Solo permitir si es el primer usuario
        if (Usuario.count() > 0) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"Solo para inicializar sistema\"}")
                .build();
        }
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
        usuario.password = request.password;
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
    public List<UsuarioResponse> listarUsuarios() {
        return Usuario.listAll().stream()
            .map(u -> toResponse((Usuario) u))
            .collect(Collectors.toList());
    }

    @GET
    @Path("/usuarios/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "CAJERO"})
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
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "CAJERO"})
    public List<UsuarioResponse> listarPorRol(@PathParam("rol") String rol) {
        try {
            return Usuario.list("roles like ?1 and activo = true", "%" + rol + "%").stream()
                .map(u -> toResponse((Usuario) u))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
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

        usuario.nombres = request.nombres;
        usuario.apellidos = request.apellidos;
        usuario.especialidad = request.especialidad;
        usuario.dni = request.dni;
        usuario.telefono = request.telefono;
        if (request.roles != null) {
            usuario.roles = request.roles;
        }

        return Response.ok(toResponse(usuario)).build();
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
        return Response.ok("{\"message\":\"Usuario desactivado\"}").build();
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse r = new UsuarioResponse();
        r.id = usuario.id;
        r.username = usuario.username;
        r.email = usuario.email;
        r.roles = usuario.roles.stream().map(Rol::name).toArray(String[]::new);
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