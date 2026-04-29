package upeu.edu.pe;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {

    private Client client = ClientBuilder.newClient();

    @ConfigProperty(name = "ms.pacientes.url", defaultValue = "http://localhost:8081")
    String pacientesUrl;

    @ConfigProperty(name = "ms.atencion.url", defaultValue = "http://localhost:8082")
    String atencionUrl;

    @ConfigProperty(name = "ms.recetas.url", defaultValue = "http://localhost:8083")
    String recetasUrl;

    @ConfigProperty(name = "ms.farmacia.url", defaultValue = "http://localhost:8084")
    String farmaciaUrl;

    @ConfigProperty(name = "ms.cobros.url", defaultValue = "http://localhost:8085")
    String cobrosUrl;

    @ConfigProperty(name = "ms.notificaciones.url", defaultValue = "http://localhost:8086")
    String notificacionesUrl;

    @GET
    public Response status() {
        return Response.ok("{\"status\":\"API Gateway running\",\"version\":\"1.0.0\",\"resilience\":\"enabled\"}").build();
    }

    @POST
    @Path("/auth/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postAuth(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("auth")
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ PACIENTES ============
    @GET
    @Path("/pacientes")
    @CircuitBreaker(name = "pacientesService", fallbackMethod = "fallbackPacientes")
    @Retry(name = "pacientesService")
    @TimeLimiter(name = "pacientesService")
    public Response getPacientes(@HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("pacientes")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.get();
        } catch (Exception e) {
            return fallbackPacientes(authHeader, e);
        }
    }

    public Response fallbackPacientes(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de pacientes no disponible\",\"mensaje\":\"Por favor intente más tarde\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @GET
    @Path("/pacientes/{path:.*}")
    @CircuitBreaker(name = "pacientesService", fallbackMethod = "fallbackPacientesPath")
    @Retry(name = "pacientesService")
    public Response getPacientesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("pacientes")
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.get();
        } catch (Exception e) {
            return fallbackPacientesPath(path, authHeader, e);
        }
    }

    public Response fallbackPacientesPath(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de pacientes no disponible\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/pacientes")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(name = "pacientesService", fallbackMethod = "fallbackPostPacientes")
    public Response postPacientes(String body, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("pacientes")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return fallbackPostPacientes(body, authHeader, e);
        }
    }

    public Response fallbackPostPacientes(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    // ============ CITAS ============
    @GET
    @Path("/citas")
    @CircuitBreaker(name = "citasService", fallbackMethod = "fallbackCitas")
    @Retry(name = "citasService")
    @TimeLimiter(name = "citasService")
    public Response getCitas(@HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(atencionUrl)
                .path("citas")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.get();
        } catch (Exception e) {
            return fallbackCitas(authHeader, e);
        }
    }

    public Response fallbackCitas(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de citas no disponible\",\"mensaje\":\"Por favor intente más tarde\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/citas")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(name = "citasService", fallbackMethod = "fallbackPostCitas")
    public Response postCitas(String body, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(atencionUrl)
                .path("citas")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return fallbackPostCitas(body, authHeader, e);
        }
    }

    public Response fallbackPostCitas(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear la cita\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    // ============ RECETAS ============
    @GET
    @Path("/recetas")
    public Response getRecetas(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("recetas", authHeader);
    }

    @POST
    @Path("/recetas")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postRecetas(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("recetas", body, authHeader);
    }

    // ============ MEDICAMENTOS ============
    @GET
    @Path("/medicamentos")
    public Response getMedicamentos(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("medicamentos", authHeader);
    }

    @POST
    @Path("/medicamentos")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postMedicamentos(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("medicamentos", body, authHeader);
    }

    // ============ COBROS ============
    @GET
    @Path("/cobros")
    public Response getCobros(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("cobros", authHeader);
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCobros(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("cobros", body, authHeader);
    }

    // ============ NOTIFICACIONES ============
    @GET
    @Path("/notificaciones")
    public Response getNotificaciones(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("notificaciones", authHeader);
    }

    @POST
    @Path("/notificaciones")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postNotificaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("notificaciones", body, authHeader);
    }

// Métodos auxiliares
    private String getServiceUrl(String service) {
        return switch (service) {
            case "recetas" -> recetasUrl;
            case "medicamentos" -> farmaciaUrl;
            case "cobros" -> cobrosUrl;
            case "notificaciones" -> notificacionesUrl;
            default -> recetasUrl;
        };
    }

    private Response proxyGet(String path, String authHeader) {
        try {
            String baseUrl = getServiceUrl(path);
            var request = client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private Response proxyPost(String path, String body, String authHeader) {
        try {
            String baseUrl = getServiceUrl(path);
            var request = client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
