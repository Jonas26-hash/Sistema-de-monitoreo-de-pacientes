package upeu.edu.pe.filter;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final long CACHE_TTL_MS = 30_000;
    private static final ConcurrentHashMap<Long, CachedStatus> statusCache = new ConcurrentHashMap<>();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    private static class CachedStatus {
        boolean activo;
        long timestamp;
        CachedStatus(boolean activo) {
            this.activo = activo;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @Inject
    JWTParser jwtParser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            try {
                var is = requestContext.getEntityStream();
                if (is != null) {
                    byte[] raw = is.readAllBytes();
                    requestContext.setProperty("rawBody", raw);
                    requestContext.setEntityStream(new ByteArrayInputStream(raw));
                }
            } catch (Exception e) {
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

            Object userIdClaim = jwt.getClaim("userId");
            Long userId = userIdClaim != null ? Long.valueOf(userIdClaim.toString()) : null;

            if (userId != null) {
                CachedStatus cached = statusCache.get(userId);
                if (cached == null || cached.isExpired()) {
                    boolean activo = checkUserActive(userId);
                    statusCache.put(userId, new CachedStatus(activo));
                    if (!activo) {
                        requestContext.abortWith(
                            Response.status(Response.Status.FORBIDDEN)
                                .type("application/json")
                                .entity("{\"error\":\"cuenta desactivada\",\"mensaje\":\"Tu cuenta ha sido desactivada. Contacta al administrador.\"}")
                                .build()
                        );
                    }
                } else if (!cached.activo) {
                    requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                            .type("application/json")
                            .entity("{\"error\":\"cuenta desactivada\",\"mensaje\":\"Tu cuenta ha sido desactivada. Contacta al administrador.\"}")
                            .build()
                    );
                }
            }
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

    private boolean checkUserActive(Long userId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://ms-pacientes:8080/auth/check-status/" + userId))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 && resp.body().contains("\"activo\":true");
        } catch (Exception e) {
            log.warning("Failed to check user status for " + userId + ": " + e.getMessage());
            return true;
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
