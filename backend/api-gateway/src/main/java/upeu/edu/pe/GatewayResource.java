package upeu.edu.pe;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import upeu.edu.pe.discovery.ServiceDiscovery;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {

    private Client client = ClientBuilder.newClient();

    @Inject
    ServiceDiscovery discovery;

    @Context
    HttpHeaders incomingHeaders;

    @Context
    UriInfo uriInfo;

    @Context
    ContainerRequestContext requestContext;

    private String url(String service) { return discovery.getUrl(service); }

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
        return handleResponse(buildGet(url("ms-pacientes"), "auth/pendientes", authHeader));
    }

    @GET
    @Path("/auth/usuarios")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuthUsuarios(@QueryParam("page") @DefaultValue("0") int page,
                                    @QueryParam("size") @DefaultValue("10") int size,
                                    @QueryParam("search") String search,
                                    @HeaderParam("Authorization") String authHeader) {
        WebTarget target = client.target(url("ms-pacientes")).path("auth/usuarios")
            .queryParam("page", page)
            .queryParam("size", size);
        if (search != null && !search.isBlank()) {
            target = target.queryParam("search", search);
        }
        var request = target.request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
        return handleResponse(request.get());
    }

    @GET
    @Path("/auth/usuarios/{id}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response getAuthUsuariosId(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "auth/usuarios/" + id, authHeader));
    }

    @GET
    @Path("/auth/usuarios/rol/{rol}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuthUsuariosRol(@PathParam("rol") String rol, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "auth/usuarios/rol/" + rol, authHeader));
    }

    @POST
    @Path("/auth/login")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response proxyLogin(byte[] body, @HeaderParam("Authorization") String authHeader) {
        gwLog.info("PROXY_LOGIN body {} bytes: {}", body.length,
            new String(body, 0, Math.min(body.length, 80), StandardCharsets.UTF_8));
        return handleResponse(buildPost(url("ms-pacientes"), "auth/login", body, authHeader));
    }

    @POST
    @Path("/auth/debug/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public Response debugEcho() {
        Object prop = requestContext.getProperty("rawBody");
        byte[] body = prop instanceof byte[] ? (byte[]) prop : new byte[0];
        return Response.ok(new String(body, StandardCharsets.UTF_8)).build();
    }

    @POST
    @Path("/auth/pre-registro")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistro(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/pre-registro", body, authHeader));
    }

    @POST
    @Path("/auth/verificar-codigo")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyVerificarCodigo(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/verificar-codigo", body, authHeader));
    }

    @POST
    @Path("/auth/completar-registro")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyCompletarRegistro(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/completar-registro", body, authHeader));
    }

    @POST
    @Path("/auth/register")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyRegister(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/register", body, authHeader));
    }

    @GET
    @Path("/auth/profile")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuthProfile")
    public Response getAuthProfile(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "auth/profile", authHeader));
    }

    @PUT
    @Path("/auth/profile")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyUpdateProfile(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/profile", body, authHeader));
    }

    @PUT
    @Path("/auth/change-password")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyChangePassword(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/change-password", body, authHeader));
    }

    @GET
    @Path("/auth/config")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuthConfig(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "auth/config", authHeader));
    }

    @PUT
    @Path("/auth/config")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response putAuthConfig(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/config", body, authHeader));
    }

    @POST
    @Path("/auth/forgot-password")
    @Consumes(MediaType.WILDCARD)
    public Response proxyForgotPassword(byte[] body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/forgot-password", body, null));
    }

    @POST
    @Path("/auth/reset-password")
    @Consumes(MediaType.WILDCARD)
    public Response proxyResetPassword(byte[] body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/reset-password", body, null));
    }

    @POST
    @Path("/auth/self-register")
    @Consumes(MediaType.WILDCARD)
    public Response proxySelfRegister(byte[] body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/self-register", body, null));
    }

    @POST
    @Path("/auth/register-init")
    @Consumes(MediaType.WILDCARD)
    public Response proxyRegisterInit(byte[] body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/register-init", body, null));
    }

    @POST
    @Path("/auth/registro-paciente-desde-lista")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyRegistroPacienteLista(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/registro-paciente-desde-lista", body, authHeader));
    }

    @POST
    @Path("/auth/pre-registro-personal")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistroPersonal(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/pre-registro-personal", body, authHeader));
    }

    @PUT
    @Path("/auth/pendientes/{id}/aprobar")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyAprobarPendiente(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/pendientes/" + id + "/aprobar", authHeader));
    }

    // ============ PACIENTES ============
    @GET
    @Path("/pacientes")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackPacientes")
    public Response getPacientes(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "pacientes", authHeader));
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
        return handleResponse(buildGet(url("ms-pacientes"), "pacientes/" + path, authHeader));
    }

    public Response fallbackPacientesPath(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de pacientes no disponible\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/pacientes")
    @Consumes(MediaType.WILDCARD)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostPacientes")
    public Response postPacientes(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "pacientes", body, authHeader));
    }

    public Response fallbackPostPacientes(byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @PUT
    @Path("/pacientes/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackPutPacientesPath")
    public Response putPacientesPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "pacientes/" + path, body, authHeader));
    }

    public Response fallbackPutPacientesPath(String path, byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo actualizar el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @DELETE
    @Path("/pacientes/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response deletePacientesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-pacientes"), "pacientes/" + path, authHeader));
    }

    // ============ CITAS ============
    @GET
    @Path("/citas")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackCitas")
    public Response getCitas(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "citas", authHeader));
    }

    public Response fallbackCitas(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de citas no disponible\",\"mensaje\":\"Por favor intente más tarde\",\"type\":\"CIRCUIT_BREAKER\"}")
            .build();
    }

    @POST
    @Path("/citas")
    @Consumes(MediaType.WILDCARD)
    public Response postCitas(byte[] body, @HeaderParam("Authorization") String authHeader,
            @HeaderParam("Idempotency-Key") String idempotencyKey) {
        return handleResponse(buildPost(url("ms-atencion"), "citas", body, authHeader, idempotencyKey));
    }

    @POST
    @Path("/citas/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    public Response postCitasPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader,
            @HeaderParam("Idempotency-Key") String idempotencyKey) {
        return handleResponse(buildPost(url("ms-atencion"), "citas/" + path, body, authHeader, idempotencyKey));
    }

    @GET
    @Path("/citas/{path:.*}")
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 3, delay = 500)
    @Fallback(fallbackMethod = "fallbackCitasPath")
    public Response getCitasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "citas/" + path, authHeader));
    }

    @PUT
    @Path("/citas/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    public Response putCitasPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-atencion"), "citas/" + path, body, authHeader));
    }

    @DELETE
    @Path("/citas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response deleteCitasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-atencion"), "citas/" + path, authHeader));
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
        return handleResponse(buildGet(url("ms-atencion"), "consultas", authHeader));
    }

    @GET
    @Path("/consultas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getConsultasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "consultas/" + path, authHeader));
    }

    @POST
    @Path("/consultas")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postConsultas(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-atencion"), "consultas", body, authHeader));
    }

    // ============ TRIAJES ============
    @GET
    @Path("/triajes")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getTriajes(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "triajes", authHeader));
    }

    @GET
    @Path("/triajes/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getTriajesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "triajes/" + path, authHeader));
    }

    @POST
    @Path("/triajes")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postTriajes(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-atencion"), "triajes", body, authHeader));
    }

    @PUT
    @Path("/triajes/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putTriajesPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-atencion"), "triajes/" + path, body, authHeader));
    }

    // ============ HISTORIAL ============
    @GET
    @Path("/historial/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getHistorialPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "historial/" + path, authHeader));
    }

    // ============ ORDENES EXAMEN ============
    @GET
    @Path("/ordenes-examen")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getOrdenesExamen(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "ordenes-examen", authHeader));
    }

    @GET
    @Path("/ordenes-examen/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getOrdenesExamenPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-atencion"), "ordenes-examen/" + path, authHeader));
    }

    @POST
    @Path("/ordenes-examen")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postOrdenesExamen(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-atencion"), "ordenes-examen", body, authHeader));
    }

    @PUT
    @Path("/ordenes-examen/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putOrdenesExamenPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-atencion"), "ordenes-examen/" + path, body, authHeader));
    }

    @DELETE
    @Path("/ordenes-examen/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response deleteOrdenesExamenPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-atencion"), "ordenes-examen/" + path, authHeader));
    }

    // ============ RECETAS ============
    @GET
    @Path("/recetas")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getRecetas(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-recetas"), "recetas", authHeader));
    }

    @GET
    @Path("/recetas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getRecetasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-recetas"), "recetas/" + path, authHeader));
    }

    @POST
    @Path("/recetas")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postRecetas(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-recetas"), "recetas", body, authHeader));
    }

    @PUT
    @Path("/recetas/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putRecetasPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-recetas"), "recetas/" + path, body, authHeader));
    }

    @DELETE
    @Path("/recetas/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response deleteRecetasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-recetas"), "recetas/" + path, authHeader));
    }

    // ============ MEDICAMENTOS ============
    @GET
    @Path("/medicamentos")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getMedicamentos(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-farmacia"), "medicamentos", authHeader));
    }

    @POST
    @Path("/medicamentos")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postMedicamentos(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-farmacia"), "medicamentos", body, authHeader));
    }

    @GET
    @Path("/medicamentos/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getMedicamentosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-farmacia"), "medicamentos/" + path, authHeader));
    }

    // ============ DISPENSACIONES ============
    @GET
    @Path("/dispensaciones")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getDispensaciones(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-farmacia"), "dispensaciones", authHeader));
    }

    @POST
    @Path("/dispensaciones")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postDispensaciones(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-farmacia"), "dispensaciones", body, authHeader));
    }

    @GET
    @Path("/dispensaciones/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getDispensacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-farmacia"), "dispensaciones/" + path, authHeader));
    }

    // ============ COBROS ============
    @GET
    @Path("/cobros")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobros(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "cobros", authHeader));
    }

    @GET
    @Path("/cobros/deudas/{pacienteId}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobrosDeudas(@PathParam("pacienteId") Long pacienteId, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "cobros/deudas/" + pacienteId, authHeader));
    }

    @GET
    @Path("/cobros/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobrosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "cobros/" + path, authHeader));
    }

    @POST
    @Path("/cobros/pago-unico")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCobrosPagoUnico(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "cobros/pago-unico", body, authHeader));
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCobros(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "cobros", body, authHeader));
    }

    @PUT
    @Path("/cobros/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putCobros(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-cobros"), "cobros/" + path, body, authHeader));
    }

    @GET
    @Path("/cobros/pendientes-verificacion")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCobrosPendientesVerificacion(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "cobros/pendientes-verificacion", authHeader));
    }

    // ============ SERVICIOS ============
    @GET
    @Path("/servicios")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getServicios(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "servicios", authHeader));
    }

    @GET
    @Path("/servicios/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getServiciosPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "servicios/" + path, authHeader));
    }

    @POST
    @Path("/servicios")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postServicios(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "servicios", body, authHeader));
    }

    // ============ CAMPANIAS ============
    @GET
    @Path("/campanias")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCampanias(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "campanias", authHeader));
    }

    @GET
    @Path("/campanias/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getCampaniasPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-cobros"), "campanias/" + path, authHeader));
    }

    @POST
    @Path("/campanias")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCampanias(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "campanias", body, authHeader));
    }

    // ============ NOTIFICACIONES ============
    @GET
    @Path("/notificaciones")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getNotificaciones(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-notificaciones"), "notificaciones", authHeader));
    }

    @GET
    @Path("/notificaciones/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getNotificacionesPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-notificaciones"), "notificaciones/" + path, authHeader));
    }

    @POST
    @Path("/notificaciones")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificaciones(byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-notificaciones"), "notificaciones", body, authHeader));
    }

    @POST
    @Path("/notificaciones/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificacionesPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-notificaciones"), "notificaciones/" + path, body, authHeader));
    }

    @PUT
    @Path("/notificaciones/{path:.*}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putNotificacionesPath(@PathParam("path") String path, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-notificaciones"), "notificaciones/" + path, body, authHeader));
    }

    // ============ AUDIT ============
    @GET
    @Path("/audit")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAudit(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "audit", authHeader));
    }

    @GET
    @Path("/audit/{path:.*}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response getAuditPath(@PathParam("path") String path, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "audit/" + path, authHeader));
    }

    @POST
    @Path("/audit")
    @Consumes(MediaType.WILDCARD)
    public Response postAudit(byte[] body) {
        return handleResponse(buildPost(url("ms-pacientes"), "audit", body, null));
    }

    public Response fallbackAuth(byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackAuth(Long id, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackAuthProfile(String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de perfil no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String path, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @PUT
    @Path("/auth/usuarios/{id}")
    @Consumes(MediaType.WILDCARD)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response putAuthUsuariosId(@PathParam("id") Long id, byte[] body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/usuarios/" + id, body, authHeader));
    }

    @DELETE
    @Path("/auth/usuarios/{id}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response deleteAuthUsuariosId(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-pacientes"), "auth/usuarios/" + id, authHeader));
    }

    public Response fallbackAuth(Long id, byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(Long pacienteId, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String path, byte[] body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(int page, int size, String search, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    // Métodos auxiliares — propagan X-Request-Id para correlación
    private String correlationId() {
        String rid = incomingHeaders.getHeaderString("X-Request-Id");
        return (rid != null && !rid.isEmpty()) ? rid : null;
    }

    private void addCorrelationId(jakarta.ws.rs.client.Invocation.Builder request) {
        String cid = correlationId();
        if (cid != null) request.header("X-Request-Id", cid);
    }

    private Response buildGet(String baseUrl, String path, String authHeader) {
        try {
            WebTarget target = client.target(baseUrl).path(path);
            MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                for (String value : entry.getValue()) {
                    target = target.queryParam(entry.getKey(), value);
                }
            }
            var request = target.request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            addCorrelationId(request);
            return request.get();
        } catch (jakarta.ws.rs.WebApplicationException e) {
            return e.getResponse();
        }
    }

    private static final org.slf4j.Logger gwLog = org.slf4j.LoggerFactory.getLogger("GatewayResource");

    private Response buildPost(String baseUrl, String path, byte[] bodyBytes, String authHeader) {
        return buildPost(baseUrl, path, bodyBytes, authHeader, null);
    }

    private Response buildPost(String baseUrl, String path, byte[] bodyBytes, String authHeader, String idempotencyKey) {
        try {
            var url = new java.net.URL(baseUrl + "/" + path);
            gwLog.info("POST {} -> {} ({} bytes)", url.toString(),
                bodyBytes != null ? new String(bodyBytes, 0, Math.min(bodyBytes.length, 60), StandardCharsets.UTF_8) : "null",
                bodyBytes != null ? bodyBytes.length : 0);
            var conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (authHeader != null && !authHeader.isEmpty()) {
                conn.setRequestProperty("Authorization", authHeader);
            }
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                conn.setRequestProperty("Idempotency-Key", idempotencyKey);
            }
            String cid = correlationId();
            if (cid != null) conn.setRequestProperty("X-Request-Id", cid);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            if (bodyBytes != null && bodyBytes.length > 0) {
                conn.setFixedLengthStreamingMode(bodyBytes.length);
                try (var os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                    os.flush();
                }
            }
            int status = conn.getResponseCode();
            String responseBody;
            try (var stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                if (stream == null) {
                    responseBody = "[null stream]";
                } else {
                    responseBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            gwLog.info("GW_POST_RESULT {} -> status={} body={}", url.toString(), status, responseBody);
            return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(responseBody)
                .build();
        } catch (Exception e) {
            gwLog.error("POST {} failed: {} {} {}", baseUrl + "/" + path, e.getClass().getName(), e.getMessage(), e);
            jakarta.ws.rs.WebApplicationException wae;
            if (e instanceof jakarta.ws.rs.WebApplicationException) {
                wae = (jakarta.ws.rs.WebApplicationException) e;
            } else {
                wae = new jakarta.ws.rs.WebApplicationException(e.getMessage(), e, Response.Status.INTERNAL_SERVER_ERROR);
            }
            return wae.getResponse();
        }
    }

    private Response buildPut(String baseUrl, String path, String authHeader) {
        try {
            var request = client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            addCorrelationId(request);
            return request.put(Entity.entity("", MediaType.APPLICATION_JSON));
        } catch (jakarta.ws.rs.WebApplicationException e) {
            return e.getResponse();
        }
    }

    private Response buildPut(String baseUrl, String path, byte[] bodyBytes, String authHeader) {
        try {
            var body = bodyBytes != null ? new String(bodyBytes, StandardCharsets.UTF_8) : "";
            var request = client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            addCorrelationId(request);
            return request.put(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (jakarta.ws.rs.WebApplicationException e) {
            return e.getResponse();
        }
    }

    private Response buildDelete(String baseUrl, String path, String authHeader) {
        try {
            var request = client.target(baseUrl)
                .path(path)
                .request(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isEmpty()) {
                request = request.header("Authorization", authHeader);
            }
            addCorrelationId(request);
            return request.delete();
        } catch (jakarta.ws.rs.WebApplicationException e) {
            return e.getResponse();
        }
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
