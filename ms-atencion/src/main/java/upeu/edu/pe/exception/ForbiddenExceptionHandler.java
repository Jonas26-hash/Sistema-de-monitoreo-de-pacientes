package upeu.edu.pe.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Provider
public class ForbiddenExceptionHandler {
    @ServerExceptionMapper
    public Response handleForbidden(SecurityException e) {
        return Response.status(Response.Status.FORBIDDEN)
            .type(MediaType.APPLICATION_JSON)
            .entity("{\"error\":\"Acceso denegado\",\"mensaje\":\"No tienes permisos para acceder a este recurso. Verifica que tu rol tenga acceso.\",\"code\":403}")
            .build();
    }
}