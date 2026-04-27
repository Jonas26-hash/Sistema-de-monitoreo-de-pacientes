package upeu.edu.pe.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.dto.AuthRequest;
import upeu.edu.pe.dto.AuthResponse;
import upeu.edu.pe.dto.RegisterRequest;
import upeu.edu.pe.entity.Usuario;
import upeu.edu.pe.service.AuthService;
import java.util.List;

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
    public Response register(RegisterRequest request) {
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

        Usuario usuario = new Usuario();
        usuario.username = request.username;
        usuario.password = request.password;
        usuario.email = request.email;
        usuario.roles = request.roles != null ? request.roles : List.of();
        usuario.activo = true;
        usuario.persist();

        return Response.ok("{\"message\":\"Usuario creado\"}").build();
    }
}