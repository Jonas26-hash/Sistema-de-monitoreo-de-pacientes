package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import upeu.edu.pe.entity.Cobro;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class CobroService {

    private final Client client = ClientBuilder.newClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Cobro> listar() {
        return Cobro.listAll();
    }

    public List<Cobro> findByPaciente(Long pacienteId) {
        return Cobro.list("pacienteId = ?1", pacienteId);
    }

    public Cobro buscar(Long id) {
        Cobro c = Cobro.findById(id);
        if (c == null) throw new NotFoundException("Cobro no encontrado");
        return c;
    }

    @Transactional
    public Cobro crear(Cobro cobro) {
        if (cobro.fechaCobro == null) {
            cobro.fechaCobro = LocalDate.now();
        }
        if (cobro.estado == null) {
            cobro.estado = "PAGADO";
        }
        cobro.persist();
        return cobro;
    }

    public String deudasPaciente(Long pacienteId) {
        try {
            String recetasJson = client.target("http://ms-recetas:8080")
                .path("recetas/pendientes-pago/paciente/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

            String examenesJson = client.target("http://ms-atencion:8080")
                .path("ordenes-examen/pendientes/paciente/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);

            return "{\"recetas\":" + recetasJson + ",\"examenes\":" + examenesJson + "}";
        } catch (Exception e) {
            return "{\"recetas\":[],\"examenes\":[],\"error\":\"" +
                e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    @Transactional
    public Cobro pagoUnico(String body) {
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
            cobro.descripcion = json.has("descripcion") ? json.get("descripcion").asText() : "Pago \u00fanico";
            cobro.persist();

            // Mark related entities as paid
            if (json.has("recetaIds") && json.get("recetaIds").isArray()) {
                for (JsonNode idNode : json.get("recetaIds")) {
                    try {
                        client.target("http://ms-recetas:8080")
                            .path("recetas/" + idNode.asLong() + "/pagar")
                            .request(MediaType.APPLICATION_JSON)
                            .put(Entity.entity("", MediaType.APPLICATION_JSON));
                    } catch (Exception ignored) {}
                }
            }

            if (json.has("examenIds") && json.get("examenIds").isArray()) {
                for (JsonNode idNode : json.get("examenIds")) {
                    try {
                        client.target("http://ms-atencion:8080")
                            .path("ordenes-examen/" + idNode.asLong() + "/pagar")
                            .request(MediaType.APPLICATION_JSON)
                            .put(Entity.entity("", MediaType.APPLICATION_JSON));
                    } catch (Exception ignored) {}
                }
            }

            return cobro;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al procesar pago \u00fanico: " +
                e.getMessage().replace("\"", "'"));
        }
    }
}
