package upeu.edu.pe.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.USER)
public class AuditLogFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(AuditLogFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");

        requestContext.setProperty("audit_start", System.currentTimeMillis());

        log.info(String.format("[AUDIT] %s %s | autenticado=%s | %s",
            method, path, hasToken, LocalDateTime.now()));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        int status = responseContext.getStatus();
        Object startObj = requestContext.getProperty("audit_start");
        if (startObj instanceof Long) {
            long elapsed = System.currentTimeMillis() - (Long) startObj;
            log.info(String.format("[AUDIT] %s %s → %d (%dms)", method, path, status, elapsed));
        } else {
            log.info(String.format("[AUDIT] %s %s → %d", method, path, status));
        }
    }
}
