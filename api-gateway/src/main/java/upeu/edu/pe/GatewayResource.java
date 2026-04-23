package upeu.edu.pe;

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

    @GET
    public Response status() {
        return Response.ok("{\"status\":\"API Gateway running\",\"version\":\"1.0.0\"}").build();
    }

    // ============ PACIENTES ============
    @GET
    @Path("/pacientes")
    public Response getPacientes() {
        try {
            return client.target("http://localhost:8081")
                .path("pacientes")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @GET
    @Path("/pacientes/{path:.*}")
    public Response getPacientesPath(@PathParam("path") String path) {
        try {
            return client.target("http://localhost:8081")
                .path("pacientes")
                .path(path)
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/pacientes")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postPacientes(String body) {
        try {
            return client.target("http://localhost:8081")
                .path("pacientes")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ AUTH ============
    @POST
    @Path("/auth/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postAuth(@PathParam("path") String path, String body) {
        try {
            return client.target("http://localhost:8081")
                .path("auth")
                .path(path)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ CITAS ============
    @GET
    @Path("/citas")
    public Response getCitas() {
        try {
            return client.target("http://localhost:8082")
                .path("citas")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/citas")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCitas(String body) {
        try {
            return client.target("http://localhost:8082")
                .path("citas")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ RECETAS ============
    @GET
    @Path("/recetas")
    public Response getRecetas() {
        try {
            return client.target("http://localhost:8083")
                .path("recetas")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/recetas")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postRecetas(String body) {
        try {
            return client.target("http://localhost:8083")
                .path("recetas")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ MEDICAMENTOS ============
    @GET
    @Path("/medicamentos")
    public Response getMedicamentos() {
        try {
            return client.target("http://localhost:8084")
                .path("medicamentos")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/medicamentos")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postMedicamentos(String body) {
        try {
            return client.target("http://localhost:8084")
                .path("medicamentos")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ COBROS ============
    @GET
    @Path("/cobros")
    public Response getCobros() {
        try {
            return client.target("http://localhost:8085")
                .path("cobros")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/cobros")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postCobros(String body) {
        try {
            return client.target("http://localhost:8085")
                .path("cobros")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    // ============ NOTIFICACIONES ============
    @GET
    @Path("/notificaciones")
    public Response getNotificaciones() {
        try {
            return client.target("http://localhost:8086")
                .path("notificaciones")
                .request(MediaType.APPLICATION_JSON)
                .get();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }

    @POST
    @Path("/notificaciones")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postNotificaciones(String body) {
        try {
            return client.target("http://localhost:8086")
                .path("notificaciones")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(body, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }
}