package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Cobro;
import java.time.LocalDate;
import java.util.List;

@Path("/cobros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CobroResource {

    private final Client client = ClientBuilder.newClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Cobro> listar() {
        return Cobro.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Cobro> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Cobro.list("pacienteId = ?1", pacienteId);
    }

    @GET
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Cobro cobro = Cobro.findById(id);
        if (cobro == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Cobro no encontrado\"}").build();
        }
        return Response.ok(cobro).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(@Valid Cobro cobro) {
        if (cobro.fechaCobro == null) {
            cobro.fechaCobro = LocalDate.now();
        }
        if (cobro.estado == null) {
            cobro.estado = "PAGADO";
        }
        cobro.persist();
        return Response.status(Response.Status.CREATED).entity(cobro).build();
    }

    @GET
    @Path("/deudas/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response deudasPaciente(@PathParam("pacienteId") Long pacienteId) {
        try {
            String recetasJson = client.target("http://ms-recetas:8080")
                .path("recetas/pendientes-pago/paciente/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

            String examenesJson = client.target("http://ms-atencion:8080")
                .path("ordenes-examen/pendientes/paciente/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

            String responseJson = "{\"recetas\":" + recetasJson + ",\"examenes\":" + examenesJson + "}";
            return Response.ok(responseJson).build();
        } catch (Exception e) {
            return Response.ok("{\"recetas\":[],\"examenes\":[],\"error\":\"" +
                e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }

    @POST
    @Path("/pago-unico")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response pagoUnico(String body) {
        try {
            JsonNode json = mapper.readTree(body);
            Long pacienteId = json.get("pacienteId").asLong();
            String tipoComprobante = json.has("tipoComprobante") ? json.get("tipoComprobante").asText() : "BOLETA";
            String numDocumento = json.has("numDocumento") ? json.get("numDocumento").asText() : "";
            Double monto = json.get("monto").asDouble();

            Cobro cobro = new Cobro();
            cobro.pacienteId = pacienteId;
            cobro.tipo = "PAGO_UNICO";
            cobro.monto = monto;
            cobro.estado = "PAGADO";
            cobro.fechaCobro = LocalDate.now();
            cobro.tipoComprobante = tipoComprobante;
            cobro.numDocumento = numDocumento;
            cobro.descripcion = json.has("descripcion") ? json.get("descripcion").asText() : "Pago único";
            cobro.persist();

            if (json.has("recetaIds") && json.get("recetaIds").isArray()) {
                for (JsonNode idNode : json.get("recetaIds")) {
                    Long recetaId = idNode.asLong();
                    try {
                        client.target("http://ms-recetas:8080")
                            .path("recetas/" + recetaId + "/pagar")
                            .request(MediaType.APPLICATION_JSON)
                            .put(Entity.entity("", MediaType.APPLICATION_JSON));
                    } catch (Exception ignored) {}
                }
            }

            if (json.has("examenIds") && json.get("examenIds").isArray()) {
                for (JsonNode idNode : json.get("examenIds")) {
                    Long examenId = idNode.asLong();
                    try {
                        String patchBody = "{\"pagado\":true}";
                        client.target("http://ms-atencion:8080")
                            .path("ordenes-examen/" + examenId + "/pagar")
                            .request(MediaType.APPLICATION_JSON)
                            .put(Entity.entity(patchBody, MediaType.APPLICATION_JSON));
                    } catch (Exception ignored) {}
                }
            }

            return Response.status(Response.Status.CREATED).entity(cobro).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Error al procesar pago único: " +
                    e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }
}
