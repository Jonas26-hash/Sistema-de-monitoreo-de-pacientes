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

    @GET
    @Path("/auth/pendientes")
    public Response getAuthPendientes(@HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("auth")
                .path("pendientes")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.get());
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/auth/usuarios")
    public Response getAuthUsuarios(@HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("auth")
                .path("usuarios")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.get());
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/auth/usuarios/{id}")
    public Response getAuthUsuariosId(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("auth", "usuarios/" + id, authHeader);
    }

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.WILDCARD)
    public Response proxyLogin(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("login", body, authHeader);
    }

    @POST
    @Path("/auth/pre-registro")
    @Consumes(MediaType.WILDCARD)
    public Response proxyPreRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("pre-registro", body, authHeader);
    }

    @POST
    @Path("/auth/verificar-codigo")
    @Consumes(MediaType.WILDCARD)
    public Response proxyVerificarCodigo(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("verificar-codigo", body, authHeader);
    }

    @POST
    @Path("/auth/completar-registro")
    @Consumes(MediaType.WILDCARD)
    public Response proxyCompletarRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("completar-registro", body, authHeader);
    }

    @POST
    @Path("/auth/register")
    @Consumes(MediaType.WILDCARD)
    public Response proxyRegister(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("register", body, authHeader);
    }

    @POST
    @Path("/auth/pre-registro-personal")
    @Consumes(MediaType.WILDCARD)
    public Response proxyPreRegistroPersonal(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostServiceAuth("pre-registro-personal", body, authHeader);
    }

    @PUT
    @Path("/auth/pendientes/{id}/aprobar")
    @Consumes(MediaType.WILDCARD)
    public Response proxyAprobarPendiente(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("auth")
                .path("pendientes")
                .path(String.valueOf(id))
                .path("aprobar")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.put(Entity.entity("", MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
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
            return handleResponse(request.get());
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
            return handleResponse(request.get());
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
    @Consumes(MediaType.WILDCARD)
    @CircuitBreaker(name = "pacientesService", fallbackMethod = "fallbackPostPacientes")
    public Response postPacientes(String body, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(pacientesUrl)
                .path("pacientes")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
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
            return handleResponse(request.get());
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
    @Consumes(MediaType.WILDCARD)
    @CircuitBreaker(name = "citasService", fallbackMethod = "fallbackPostCitas")
    public Response postCitas(String body, @HeaderParam("Authorization") String authHeader,
            @HeaderParam("Idempotency-Key") String idempotencyKey) {
        try {
            var request = client.target(atencionUrl)
                .path("citas")
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                request = request.header("Idempotency-Key", idempotencyKey);
            }
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return fallbackPostCitas(body, authHeader, e);
        }
    }

    public Response fallbackPostCitas(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear la cita\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @GET
    @Path("/citas/{path:.*}")
    @CircuitBreaker(name = "citasService", fallbackMethod = "fallbackCitasPath")
    @Retry(name = "citasService")
    public Response getCitasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        try {
            var request = client.target(atencionUrl)
                .path("citas")
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.get());
        } catch (Exception e) {
            return fallbackCitasPath(path, authHeader, e);
        }
    }

    public Response fallbackCitasPath(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de citas no disponible\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    // ============ CONSULTAS ============
    @GET
    @Path("/consultas")
    public Response getConsultas(@HeaderParam("Authorization") String authHeader) {
        return proxyGetService("consultas", "", authHeader);
    }

    @GET
    @Path("/consultas/{path:.*}")
    public Response getConsultasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("consultas", path, authHeader);
    }

    @POST
    @Path("/consultas")
    @Consumes(MediaType.WILDCARD)
    public Response postConsultas(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostService("consultas", body, authHeader);
    }

    // ============ RECETAS ============
    @GET
    @Path("/recetas")
    public Response getRecetas(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("recetas", authHeader);
    }

    @GET
    @Path("/recetas/{path:.*}")
    public Response getRecetasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("recetas", path, authHeader);
    }

    @POST
    @Path("/recetas")
    @Consumes(MediaType.WILDCARD)
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
    @Consumes(MediaType.WILDCARD)
    public Response postMedicamentos(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("medicamentos", body, authHeader);
    }

    @GET
    @Path("/medicamentos/{path:.*}")
    public Response getMedicamentosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("medicamentos", path, authHeader);
    }

    // ============ DISPENSACIONES ============
    @GET
    @Path("/dispensaciones")
    public Response getDispensaciones(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("dispensaciones", authHeader);
    }

    @POST
    @Path("/dispensaciones")
    @Consumes(MediaType.WILDCARD)
    public Response postDispensaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("dispensaciones", body, authHeader);
    }

    @GET
    @Path("/dispensaciones/{path:.*}")
    public Response getDispensacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("dispensaciones", path, authHeader);
    }

    // ============ COBROS ============
    @GET
    @Path("/cobros")
    public Response getCobros(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("cobros", authHeader);
    }

    @GET
    @Path("/cobros/{path:.*}")
    public Response getCobrosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("cobros", path, authHeader);
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.WILDCARD)
    public Response postCobros(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("cobros", body, authHeader);
    }

    // ============ NOTIFICACIONES ============
    @GET
    @Path("/notificaciones")
    public Response getNotificaciones(@HeaderParam("Authorization") String authHeader) {
        return proxyGet("notificaciones", authHeader);
    }

    @GET
    @Path("/notificaciones/{path:.*}")
    public Response getNotificacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return proxyGetService("notificaciones", path, authHeader);
    }

    @POST
    @Path("/notificaciones")
    @Consumes(MediaType.WILDCARD)
    public Response postNotificaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPost("notificaciones", body, authHeader);
    }

    @POST
    @Path("/notificaciones/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    public Response postNotificacionesPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        return proxyPostService("notificaciones", path, body, authHeader);
    }

// Métodos auxiliares
    private String getServiceUrl(String service) {
        return switch (service) {
            case "recetas" -> recetasUrl;
            case "medicamentos" -> farmaciaUrl;
            case "dispensaciones" -> farmaciaUrl;
            case "cobros" -> cobrosUrl;
            case "notificaciones" -> notificacionesUrl;
            case "consultas" -> atencionUrl;
            case "citas" -> atencionUrl;
            case "auth" -> pacientesUrl;
            default -> recetasUrl;
        };
    }

    private Response handleResponse(Response response) {
        if (response.getStatus() == 403) {
            response.close();
            return Response.status(403)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\":\"Acceso denegado\",\"mensaje\":\"No tienes permisos para acceder a este recurso. Verifica que tu rol tenga acceso.\",\"code\":403}")
                .build();
        }
        return response;
    }

    private Response proxyGetService(String service, String path, String authHeader) {
        try {
            String baseUrl = getServiceUrl(service);
            var request = client.target(baseUrl)
                .path(service)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.get());
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private Response proxyPostServiceAuth(String path, String body, String authHeader) {
        try {
            String baseUrl = getServiceUrl("auth");
            var request = client.target(baseUrl)
                .path("auth")
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private Response proxyPostService(String service, String body, String authHeader) {
        try {
            String baseUrl = getServiceUrl(service);
            var request = client.target(baseUrl)
                .path(service)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    private Response proxyPostService(String service, String path, String body, String authHeader) {
        try {
            String baseUrl = getServiceUrl(service);
            var request = client.target(baseUrl)
                .path(service)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
                .build();
        }
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
            return handleResponse(request.get());
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
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
            return handleResponse(request.post(Entity.entity(body, MediaType.APPLICATION_JSON)));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"Error del servidor\",\"mensaje\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
