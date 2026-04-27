package upeu.edu.pe.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuthService {

    private static final String ISSUER = "hospital-system";

    public AuthResponse authenticate(String username, String password) {
        Usuario usuario = Usuario.findByUsername(username);

        if (usuario == null || !usuario.activo) {
            throw new SecurityException("Usuario inválido");
        }

        if (!password.equals(usuario.password)) {
            throw new SecurityException("Credenciales inválidas");
        }

        Set<String> rolesSet = usuario.roles.stream()
            .map(Rol::name)
            .collect(Collectors.toSet());

        String rolesString = String.join(" ", rolesSet);

        String token = Jwt.issuer(ISSUER)
            .subject(username)
            .claim("userId", usuario.id)
            .claim("roles", rolesString)
            .claim("email", usuario.email)
            .claim("nombres", usuario.nombres != null ? usuario.nombres : "")
            .claim("apellidos", usuario.apellidos != null ? usuario.apellidos : "")
            .groups(rolesSet)
            .expiresIn(Duration.ofDays(1))
            .sign();

        AuthResponse response = new AuthResponse();
        response.token = token;
        response.username = usuario.username;
        response.email = usuario.email;
        response.roles = rolesSet.toArray(new String[0]);

        return response;
    }
}