package upeu.edu.pe.filter;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import java.util.logging.Logger;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.principal.JWTParser;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(JwtAuthFilter.class.getName());

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/",
        "/auth/login",
        "/auth/pre-registro",
        "/auth/verificar-codigo",
        "/auth/completar-registro"
    );

    @Inject
    JWTParser jwtParser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        if ("OPTIONS".equalsIgnoreCase(method) || isPublicPath(path)) {
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .type("application/json")
                    .entity("{\"error\":\"Token JWT requerido\",\"mensaje\":\"Debes iniciar sesion para acceder a este recurso\"}")
                    .build()
            );
            return;
        }

        String token = authHeader.substring(7);

        try {
            jwtParser.parse(token);
        } catch (Exception e) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .type("application/json")
                    .entity("{\"error\":\"Token invalido o expirado\"}")
                    .build()
            );
        }
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String pp : PUBLIC_PATHS) {
            if (path.startsWith(pp + "/")) {
                return true;
            }
        }
        return false;
    }
}
