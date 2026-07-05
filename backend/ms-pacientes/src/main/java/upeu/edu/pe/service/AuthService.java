package upeu.edu.pe.service;

import io.quarkus.panache.common.Page;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.dto.RegisterRequest;
import upeu.edu.pe.dto.UsuarioResponse;
import upeu.edu.pe.entity.Paciente;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.SistemaConfig;
import upeu.edu.pe.entity.Usuario;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {

    private static final String ISSUER = "clinica-system";

    public String generateToken(Usuario usuario) {
        Set<String> rolesSet = usuario.roles != null
            ? usuario.roles.stream().map(Rol::name).collect(Collectors.toSet())
            : Set.of();
        String rolesString = String.join(" ", rolesSet);
        return Jwt.issuer(ISSUER)
            .subject(usuario.username)
            .claim("userId", usuario.id)
            .claim("roles", rolesString)
            .claim("email", usuario.email)
            .claim("nombres", usuario.nombres != null ? usuario.nombres : "")
            .claim("apellidos", usuario.apellidos != null ? usuario.apellidos : "")
            .groups(rolesSet)
            .expiresIn(Duration.ofDays(1))
            .sign();
    }

    @Transactional
    public AuthResponse authenticate(String username, String password) {
        Usuario usuario = Usuario.findByUsername(username);

        if (usuario == null || !usuario.activo) {
            throw new SecurityException("Credenciales inv\u00e1lidas");
        }

        boolean bcryptMatch = false;
        try {
            bcryptMatch = BCrypt.checkpw(password, usuario.password);
        } catch (IllegalArgumentException e) {
            // password is not a BCrypt hash (plain text from legacy system)
        }
        if (!bcryptMatch) {
            if (!password.equals(usuario.password)) {
                throw new SecurityException("Credenciales inv\u00e1lidas");
            }
            usuario.password = BCrypt.hashpw(password, BCrypt.gensalt());
            usuario.persist();
        }

        String token = generateToken(usuario);

        AuthResponse response = new AuthResponse();
        response.token = token;
        response.username = usuario.username;
        response.email = usuario.email;
        response.roles = usuario.roles != null
            ? usuario.roles.stream().map(Rol::name).toArray(String[]::new)
            : new String[0];
        response.avatar = usuario.avatar;

        return response;
    }

    public Usuario findByUsername(String username) {
        return Usuario.findByUsername(username);
    }

    public Usuario findById(Long id) {
        return Usuario.findById(id);
    }

    @Transactional
    public UsuarioResponse registrarUsuario(RegisterRequest request) {
        if (Usuario.findByUsername(request.username) != null) {
            throw new IllegalArgumentException("Usuario ya existe");
        }
        if (Usuario.findByEmail(request.email) != null) {
            throw new IllegalArgumentException("Email ya registrado");
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
        return response;
    }

    @Transactional
    public UsuarioResponse selfRegister(RegisterRequest request) {
        if (request.username == null || request.password == null || request.email == null) {
            throw new IllegalArgumentException("Username, password y email son requeridos");
        }

        if (Usuario.findByUsername(request.username) != null) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }
        if (Usuario.findByEmail(request.email) != null) {
            throw new IllegalArgumentException("El email ya est\u00e1 registrado");
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

        // Return minimal response for self-registration
        UsuarioResponse r = new UsuarioResponse();
        r.username = usuario.username;
        r.email = usuario.email;
        return r;
    }

    public Map<String, Object> listarUsuarios(int page, int size, String search) {
        List<Usuario> usuarios;
        long total;
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            usuarios = Usuario.find("LOWER(nombres) LIKE ?1 OR LOWER(apellidos) LIKE ?1 OR dni LIKE ?1", pattern)
                .page(Page.of(page, size))
                .list();
            total = Usuario.count("LOWER(nombres) LIKE ?1 OR LOWER(apellidos) LIKE ?1 OR dni LIKE ?1", pattern);
        } else {
            usuarios = Usuario.findAll()
                .page(Page.of(page, size))
                .list();
            total = Usuario.count();
        }
        List<UsuarioResponse> content = usuarios.stream()
            .map(u -> toResponse((Usuario) u))
            .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    public Map<String, Object> listarPendientes(int page, int size) {
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
        return result;
    }

    public List<UsuarioResponse> listarPorRol(String rol) {
        List<Usuario> usuarios = Usuario.findByRole(rol);
        return usuarios.stream()
            .map(u -> toResponse(u))
            .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, RegisterRequest request) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) throw new IllegalArgumentException("Usuario no encontrado");

        if (request.nombres != null) usuario.nombres = request.nombres;
        if (request.apellidos != null) usuario.apellidos = request.apellidos;
        if (request.email != null) usuario.email = request.email;
        if (request.especialidad != null) usuario.especialidad = request.especialidad;
        if (request.dni != null) usuario.dni = request.dni;
        if (request.telefono != null) usuario.telefono = request.telefono;
        if (request.roles != null) {
            usuario.roles = request.roles;
        }
        if (request.avatar != null) {
            usuario.avatar = request.avatar.isEmpty() ? null : request.avatar;
        }

        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(RegisterRequest request, String username) {
        Usuario usuario = Usuario.findByUsername(username);
        if (usuario == null) throw new IllegalArgumentException("Usuario no encontrado");

        if (request.nombres != null) usuario.nombres = request.nombres;
        if (request.apellidos != null) usuario.apellidos = request.apellidos;
        if (request.email != null) usuario.email = request.email;
        if (request.dni != null) usuario.dni = request.dni;
        if (request.telefono != null) usuario.telefono = request.telefono;
        if (request.especialidad != null) usuario.especialidad = request.especialidad;
        if (request.avatar != null) {
            usuario.avatar = request.avatar.isEmpty() ? null : request.avatar;
        }

        if (usuario.paciente != null) {
            if (request.email != null) {
                usuario.paciente.email = request.email;
            }
            if (request.fechaNacimiento != null) {
                usuario.paciente.fechaNacimiento = LocalDate.parse(request.fechaNacimiento);
            }
            if (request.genero != null) {
                usuario.paciente.genero = request.genero;
            }
            if (request.direccion != null) {
                usuario.paciente.direccion = request.direccion;
            }
        }

        return toResponse(usuario);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Usuario usuario = Usuario.findByUsername(username);
        if (usuario == null) throw new IllegalArgumentException("Usuario no encontrado");

        if (oldPassword == null || newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Contrase\u00f1a actual y nueva son requeridas");
        }

        if (!BCrypt.checkpw(oldPassword, usuario.password)) {
            throw new SecurityException("Contrase\u00f1a actual incorrecta");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("La nueva contrase\u00f1a debe tener al menos 6 caracteres");
        }

        usuario.password = BCrypt.hashpw(newPassword, BCrypt.gensalt());
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) throw new IllegalArgumentException("Usuario no encontrado");
        usuario.activo = false;
    }

    @Transactional
    public void aprobarUsuario(Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null) throw new IllegalArgumentException("Usuario no encontrado");
        if (usuario.activo) throw new IllegalArgumentException("El usuario ya est\u00e1 activo");
        usuario.activo = true;
    }

    @Transactional
    public SistemaConfig getConfig() {
        return SistemaConfig.ensureExists();
    }

    @Transactional
    public Map<String, Object> updateConfig(Map<String, String> body) {
        SistemaConfig config = SistemaConfig.ensureExists();
        if (body.containsKey("hospitalName")) config.hospitalName = body.get("hospitalName");
        if (body.containsKey("hospitalLogo")) config.hospitalLogo = body.get("hospitalLogo");
        Map<String, Object> map = new HashMap<>();
        map.put("hospitalName", config.hospitalName != null ? config.hospitalName : "");
        map.put("hospitalLogo", config.hospitalLogo != null ? config.hospitalLogo : "");
        return map;
    }

    public UsuarioResponse toResponse(Usuario usuario) {
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
        r.avatar = usuario.avatar;
        if (usuario.paciente != null) {
            r.pacienteId = usuario.paciente.id;
            if (usuario.paciente.fechaNacimiento != null) {
                r.fechaNacimiento = usuario.paciente.fechaNacimiento.toString();
            }
            r.genero = usuario.paciente.genero;
            r.direccion = usuario.paciente.direccion;
        }
        return r;
    }
}
