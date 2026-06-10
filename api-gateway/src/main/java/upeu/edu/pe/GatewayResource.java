package upeu.edu.pe;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

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

    @PreDestroy
    void cleanup() {
        if (client != null) client.close();
    }

    @GET
    public Response status() {
        return Response.ok("{\"status\":\"API Gateway running\",\"version\":\"1.0.0\",\"resilience\":\"enabled\"}").build();
    }

    @GET
    @Path("/auth/pendientes")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuthPendientes(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(pacientesUrl, "auth/pendientes", authHeader));
    }

    @GET
    @Path("/auth/usuarios")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuthUsuarios(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(pacientesUrl, "auth/usuarios", authHeader));
    }

    @GET
    @Path("/auth/usuarios/{id}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response getAuthUsuariosId(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(pacientesUrl, "auth/usuarios/" + id, authHeader));
    }

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyLogin(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/login", body, authHeader));
    }

    @POST
    @Path("/auth/pre-registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/pre-registro", body, authHeader));
    }

    @POST
    @Path("/auth/verificar-codigo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyVerificarCodigo(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/verificar-codigo", body, authHeader));
    }

    @POST
    @Path("/auth/completar-registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyCompletarRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/completar-registro", body, authHeader));
    }

    @POST
    @Path("/auth/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyRegister(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/register", body, authHeader));
    }

    @POST
    @Path("/auth/pre-registro-personal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistroPersonal(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "auth/pre-registro-personal", body, authHeader));
    }

    @PUT
    @Path("/auth/pendientes/{id}/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyAprobarPendiente(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(pacientesUrl, "auth/pendientes/" + id + "/aprobar", authHeader));
    }

    // ============ PACIENTES ============
    @GET
    @Path("/pacientes")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackPacientes")
    public Response getPacientes(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(pacientesUrl, "pacientes", authHeader));
    }

    public Response fallbackPacientes(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de pacientes no disponible\",\"mensaje\":\"Por favor intente más tarde\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @GET
    @Path("/pacientes/{path:.*}")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackPacientesPath")
    public Response getPacientesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(pacientesUrl, "pacientes/" + path, authHeader));
    }

    public Response fallbackPacientesPath(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de pacientes no disponible\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/pacientes")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostPacientes")
    public Response postPacientes(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(pacientesUrl, "pacientes", body, authHeader));
    }

    public Response fallbackPostPacientes(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    // ============ CITAS ============
    @GET
    @Path("/citas")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackCitas")
    public Response getCitas(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(atencionUrl, "citas", authHeader));
    }

    public Response fallbackCitas(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de citas no disponible\",\"mensaje\":\"Por favor intente más tarde\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/citas")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostCitas")
    public Response postCitas(String body, @HeaderParam("Authorization") String authHeader,
            @HeaderParam("Idempotency-Key") String idempotencyKey) {
        return handleResponse(buildPost(atencionUrl, "citas", body, authHeader, idempotencyKey));
    }

    public Response fallbackPostCitas(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear la cita\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @GET
    @Path("/citas/{path:.*}")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackCitasPath")
    public Response getCitasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(atencionUrl, "citas/" + path, authHeader));
    }

    public Response fallbackCitasPath(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de citas no disponible\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    // ============ CONSULTAS ============
    @GET
    @Path("/consultas")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getConsultas(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(atencionUrl, "consultas", authHeader));
    }

    @GET
    @Path("/consultas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getConsultasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(atencionUrl, "consultas/" + path, authHeader));
    }

    @POST
    @Path("/consultas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postConsultas(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(atencionUrl, "consultas", body, authHeader));
    }

    // ============ RECETAS ============
    @GET
    @Path("/recetas")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getRecetas(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(recetasUrl, "recetas", authHeader));
    }

    @GET
    @Path("/recetas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getRecetasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(recetasUrl, "recetas/" + path, authHeader));
    }

    @POST
    @Path("/recetas")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postRecetas(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(recetasUrl, "recetas", body, authHeader));
    }

    // ============ MEDICAMENTOS ============
    @GET
    @Path("/medicamentos")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getMedicamentos(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(farmaciaUrl, "medicamentos", authHeader));
    }

    @POST
    @Path("/medicamentos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postMedicamentos(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(farmaciaUrl, "medicamentos", body, authHeader));
    }

    @GET
    @Path("/medicamentos/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getMedicamentosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(farmaciaUrl, "medicamentos/" + path, authHeader));
    }

    // ============ DISPENSACIONES ============
    @GET
    @Path("/dispensaciones")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getDispensaciones(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(farmaciaUrl, "dispensaciones", authHeader));
    }

    @POST
    @Path("/dispensaciones")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postDispensaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(farmaciaUrl, "dispensaciones", body, authHeader));
    }

    @GET
    @Path("/dispensaciones/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getDispensacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(farmaciaUrl, "dispensaciones/" + path, authHeader));
    }

    // ============ COBROS ============
    @GET
    @Path("/cobros")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobros(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(cobrosUrl, "cobros", authHeader));
    }

    @GET
    @Path("/cobros/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobrosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(cobrosUrl, "cobros/" + path, authHeader));
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCobros(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(cobrosUrl, "cobros", body, authHeader));
    }

    // ============ NOTIFICACIONES ============
    @GET
    @Path("/notificaciones")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getNotificaciones(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(notificacionesUrl, "notificaciones", authHeader));
    }

    @GET
    @Path("/notificaciones/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getNotificacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(notificacionesUrl, "notificaciones/" + path, authHeader));
    }

    @POST
    @Path("/notificaciones")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(notificacionesUrl, "notificaciones", body, authHeader));
    }

    @POST
    @Path("/notificaciones/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificacionesPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(notificacionesUrl, "notificaciones/" + path, body, authHeader));
    }

    public Response fallbackAuth(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackAuth(Long id, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String path, String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    // Métodos auxiliares sin try-catch para que FT annotations funcionen
    private Response buildGet(String baseUrl, String path, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        return request.get();
    }

    private Response buildPost(String baseUrl, String path, String body, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
    }

    private Response buildPost(String baseUrl, String path, String body, String authHeader, String idempotencyKey) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
    }

    private Response buildPut(String baseUrl, String path, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        return request.put(Entity.entity("", MediaType.APPLICATION_JSON));
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
}
