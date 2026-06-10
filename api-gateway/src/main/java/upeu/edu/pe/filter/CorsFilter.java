package upeu.edu.pe.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();

        String origin = requestContext.getHeaderString("Origin");
        if (origin != null && !origin.isEmpty()) {
            headers.add("Access-Control-Allow-Origin", origin);
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("Vary", "Origin");
        } else {
            headers.add("Access-Control-Allow-Origin", "*");
        }
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization, Idempotency-Key");
        headers.add("Access-Control-Max-Age", "86400");
    }
}
