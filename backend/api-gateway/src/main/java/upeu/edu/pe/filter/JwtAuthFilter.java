package upeu.edu.pe.filter;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;
import io.smallrye.jwt.auth.principal.JWTParser;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(JwtAuthFilter.class.getName());

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/",
        "/auth/login",
        "/auth/debug/echo",
        "/auth/pre-registro",
        "/auth/verificar-codigo",
        "/auth/completar-registro",
        "/auth/register",
        "/auth/register-init",
        "/auth/pre-registro-personal",
        "/auth/self-register",
        "/auth/forgot-password",
        "/auth/reset-password"
    );

    @Inject
    JWTParser jwtParser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        // Capture raw body for all requests before RESTEasy Reactive consumes it
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            try {
                var is = requestContext.getEntityStream();
                if (is != null) {
                    byte[] raw = is.readAllBytes();
                    requestContext.setProperty("rawBody", raw);
                    requestContext.setEntityStream(new ByteArrayInputStream(raw));
                }
            } catch (Exception e) {
                // body not available yet, skip
            }
        }

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
            var jwt = jwtParser.parse(token);
            SecurityContext original = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> jwt.getSubject() != null ? jwt.getSubject() : "unknown";
                }
                @Override
                public boolean isUserInRole(String role) {
                    return jwt.getGroups().contains(role);
                }
                @Override
                public boolean isSecure() {
                    return original != null && original.isSecure();
                }
                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });
        } catch (Exception e) {
            log.warning("JWT validation failed for path: " + path);
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
