package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Usuario;
import java.util.Map;

@Path("/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalResource {

    @GET
    @Path("/usuario/{username}")
    public Response buscarUsuarioPorUsername(@PathParam("username") String username) {
        Usuario usuario = Usuario.findByUsername(username);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Usuario no encontrado"))
                .build();
        }
        return Response.ok(Map.of(
            "id", usuario.id,
            "username", usuario.username,
            "nombres", usuario.nombres != null ? usuario.nombres : "",
            "apellidos", usuario.apellidos != null ? usuario.apellidos : ""
        )).build();
    }
}
