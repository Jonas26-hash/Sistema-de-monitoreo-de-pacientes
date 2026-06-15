package upeu.edu.pe.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
public class AuditLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(AuditLogFilter.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3)).build();

    @ConfigProperty(name = "ms.pacientes.url", defaultValue = "http://localhost:8081")
    String pacientesUrl;

    @Context
    SecurityContext securityContext;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String method = ctx.getMethod();
        String path = ctx.getUriInfo().getPath();

        String ip = ctx.getHeaderString("X-Forwarded-For");
        if (ip == null) ip = ctx.getHeaderString("X-Real-IP");
        if (ip == null) ip = "unknown";

        String userAgent = ctx.getHeaderString("User-Agent");
        if (userAgent == null) userAgent = "unknown";

        String requestId = ctx.getHeaderString("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        ctx.setProperty("X-Request-Id", requestId);
        ctx.setProperty("audit_ip", ip);
        ctx.setProperty("audit_ua", userAgent);
        ctx.setProperty("audit_start", System.currentTimeMillis());

        log.info(String.format("[AUDIT] [%s] %s %s | IP=%s | UA=%s",
            requestId, method, path, ip, userAgent));
    }

    @Override
    public void filter(ContainerRequestContext ctx, ContainerResponseContext resp) {
        String method = ctx.getMethod();
        String path = ctx.getUriInfo().getPath();
        int status = resp.getStatus();
        String requestId = (String) ctx.getProperty("X-Request-Id");
        if (requestId == null) requestId = "-";

        String username = null;
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            username = securityContext.getUserPrincipal().getName();
        }

        long elapsed = 0;
        Object startObj = ctx.getProperty("audit_start");
        if (startObj instanceof Long) {
            elapsed = System.currentTimeMillis() - (Long) startObj;
        }

        log.info(String.format("[AUDIT] [%s] %s %s → %d (%dms) user=%s",
            requestId, method, path, status, elapsed,
            username != null ? username : "-"));

        registrarAuditoriaAsync(username, method, path, status, elapsed,
            (String) ctx.getProperty("audit_ip"),
            (String) ctx.getProperty("audit_ua"),
            requestId);
    }

    private void registrarAuditoriaAsync(String username, String accion, String recurso,
                                          int statusCode, long tiempoMs, String ip,
                                          String userAgent, String requestId) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("username", username);
            body.put("accion", accion);
            body.put("recurso", recurso);
            body.put("statusCode", statusCode);
            body.put("tiempoMs", tiempoMs);
            body.put("ip", ip);
            body.put("userAgent", userAgent);
            body.put("requestId", requestId);

            String json = MAPPER.writeValueAsString(body);

            CompletableFuture.runAsync(() -> {
                try {
                    var req = HttpRequest.newBuilder()
                        .uri(URI.create(pacientesUrl + "/audit"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(3))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                    HTTP.send(req, HttpResponse.BodyHandlers.discarding());
                } catch (Exception e) {
                    log.fine("Error enviando auditoría: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            log.fine("Error serializando auditoría: " + e.getMessage());
        }
    }
}
