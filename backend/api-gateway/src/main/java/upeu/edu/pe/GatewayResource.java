package upeu.edu.pe;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    public Response getAuthUsuarios(@HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildGet(url("ms-pacientes"), "auth/usuarios", authHeader));
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyLogin(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/login", body, authHeader));
    }

    @POST
    @Path("/auth/pre-registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/pre-registro", body, authHeader));
    }

    @POST
    @Path("/auth/verificar-codigo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyVerificarCodigo(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/verificar-codigo", body, authHeader));
    }

    @POST
    @Path("/auth/completar-registro")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyCompletarRegistro(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/completar-registro", body, authHeader));
    }

    @POST
    @Path("/auth/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyRegister(String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyUpdateProfile(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/profile", body, authHeader));
    }

    @PUT
    @Path("/auth/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyChangePassword(String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response putAuthConfig(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/config", body, authHeader));
    }

    @POST
    @Path("/auth/forgot-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxyForgotPassword(String body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/forgot-password", body, null));
    }

    @POST
    @Path("/auth/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxyResetPassword(String body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/reset-password", body, null));
    }

    @POST
    @Path("/auth/self-register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxySelfRegister(String body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/self-register", body, null));
    }

    @POST
    @Path("/auth/register-init")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response proxyRegisterInit(String body) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/register-init", body, null));
    }

    @POST
    @Path("/auth/pre-registro-personal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response proxyPreRegistroPersonal(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "auth/pre-registro-personal", body, authHeader));
    }

    @PUT
    @Path("/auth/pendientes/{id}/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostPacientes")
    public Response postPacientes(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-pacientes"), "pacientes", body, authHeader));
    }

    public Response fallbackPostPacientes(String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @PUT
    @Path("/pacientes/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackPutPacientesPath")
    public Response putPacientesPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "pacientes/" + path, body, authHeader));
    }

    public Response fallbackPutPacientesPath(String path, String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo actualizar el paciente\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
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
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostCitas")
    public Response postCitas(String body, @HeaderParam("Authorization") String authHeader,
            @HeaderParam("Idempotency-Key") String idempotencyKey) {
        return handleResponse(buildPost(url("ms-atencion"), "citas", body, authHeader, idempotencyKey));
    }

    public Response fallbackPostCitas(String body, String authHeader, String idempotencyKey, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear la cita\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackPostCitas(String path, String body, String authHeader, String idempotencyKey, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"No se pudo crear la cita\",\"mensaje\":\"Servicio no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    @POST
    @Path("/citas/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
    @Fallback(fallbackMethod = "fallbackPostCitas")
    public Response postCitasPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader,
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postConsultas(String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postTriajes(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-atencion"), "triajes", body, authHeader));
    }

    @PUT
    @Path("/triajes/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putTriajesPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-atencion"), "triajes/" + path, body, authHeader));
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postOrdenesExamen(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-atencion"), "ordenes-examen", body, authHeader));
    }

    @PUT
    @Path("/ordenes-examen/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putOrdenesExamenPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postRecetas(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-recetas"), "recetas", body, authHeader));
    }

    @PUT
    @Path("/recetas/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response putRecetasPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postMedicamentos(String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postDispensaciones(String body, @HeaderParam("Authorization") String authHeader) {
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCobrosPagoUnico(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "cobros/pago-unico", body, authHeader));
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postCobros(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-cobros"), "cobros", body, authHeader));
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificaciones(String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-notificaciones"), "notificaciones", body, authHeader));
    }

    @POST
    @Path("/notificaciones/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackGenerico")
    public Response postNotificacionesPath(@PathParam("path") String path, String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPost(url("ms-notificaciones"), "notificaciones/" + path, body, authHeader));
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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postAudit(String body) {
        return handleResponse(buildPost(url("ms-pacientes"), "audit", body, null));
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response putAuthUsuariosId(@PathParam("id") Long id, String body, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildPut(url("ms-pacientes"), "auth/usuarios/" + id, body, authHeader));
    }

    @DELETE
    @Path("/auth/usuarios/{id}")
    @Retry(maxRetries = 2, delay = 200)
    @Fallback(fallbackMethod = "fallbackAuth")
    public Response deleteAuthUsuariosId(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        return handleResponse(buildDelete(url("ms-pacientes"), "auth/usuarios/" + id, authHeader));
    }

    public Response fallbackAuth(Long id, String body, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio de autenticación no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(Long pacienteId, String authHeader, Throwable t) {
        return Response.status(503)
            .entity("{\"error\":\"Servicio temporalmente no disponible\",\"type\":\"FALLBACK\"}")
            .build();
    }

    public Response fallbackGenerico(String path, String body, String authHeader, Throwable t) {
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
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
        return request.get();
    }

    private Response buildPost(String baseUrl, String path, String body, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
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
        addCorrelationId(request);
        return request.post(Entity.entity(body, MediaType.APPLICATION_JSON));
    }

    private Response buildPut(String baseUrl, String path, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
        return request.put(Entity.entity("", MediaType.APPLICATION_JSON));
    }

    private Response buildPut(String baseUrl, String path, String body, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
        return request.put(Entity.entity(body, MediaType.APPLICATION_JSON));
    }

    private Response buildDelete(String baseUrl, String path, String authHeader) {
        var request = client.target(baseUrl)
            .path(path)
            .request(MediaType.APPLICATION_JSON);
        if (authHeader != null && !authHeader.isEmpty()) {
            request = request.header("Authorization", authHeader);
        }
        addCorrelationId(request);
        return request.delete();
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
