package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public List<Cobro> listar(String search) {
        if (search == null || search.isBlank()) {
            return Cobro.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        String trimmed = search.trim();
        if (trimmed.matches("\\d{8}")) {
            try {
                String json = client.target("http://ms-pacientes:8080")
                    .path("pacientes/dni/" + trimmed)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
                JsonNode paciente = mapper.readTree(json);
                Long pid = paciente.get("id").asLong();
                return Cobro.list("(LOWER(descripcion) LIKE ?1 OR LOWER(tipo) LIKE ?1) OR pacienteId = ?2", pattern, pid);
            } catch (Exception e) {
                // DNI not found, fall through
            }
        }
        return Cobro.list("LOWER(descripcion) LIKE ?1 OR LOWER(tipo) LIKE ?1", pattern);
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
            cobro.estado = "PENDIENTE";
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

            List<Cobro> cobrosPendientes = Cobro.list("pacienteId = ?1 AND estado = 'PENDIENTE'", pacienteId);
            String cobrosJson = mapper.writeValueAsString(cobrosPendientes);

            return "{\"recetas\":" + recetasJson + ",\"examenes\":" + examenesJson + ",\"cobros\":" + cobrosJson + "}";
        } catch (Exception e) {
            return "{\"recetas\":[],\"examenes\":[],\"cobros\":[],\"error\":\"" +
                e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    public List<Cobro> pendientesVerificacion() {
        return Cobro.list("estado = 'PENDIENTE_VERIFICACION' ORDER BY fechaCobro DESC");
    }

    @Transactional
    public Cobro verificar(Long id, String codigoVerificacion) {
        Cobro cobro = buscar(id);
        if (!"PENDIENTE_VERIFICACION".equals(cobro.estado)) {
            throw new IllegalArgumentException("El cobro no est\u00e1 pendiente de verificaci\u00f3n");
        }
        if (codigoVerificacion != null && codigoVerificacion.equals(cobro.codigoVerificacion)) {
            cobro.estado = "VERIFICADO";
            cobro.persist();
        } else {
            cobro.estado = "RECHAZADO";
            cobro.persist();
            throw new IllegalArgumentException("C\u00f3digo de verificaci\u00f3n incorrecto");
        }
        return cobro;
    }

    @Transactional
    public Cobro pagoUnico(String body) {
        try {
            JsonNode json = mapper.readTree(body);
            Long pacienteId = json.get("pacienteId").asLong();
            String tipoComprobante = json.has("tipoComprobante") ? json.get("tipoComprobante").asText() : "BOLETA";
            String numDocumento = json.has("numDocumento") ? json.get("numDocumento").asText() : "";
            Double monto = json.get("monto").asDouble();
            String codigoVerificacion = json.has("codigoVerificacion") ? json.get("codigoVerificacion").asText() : null;

            Cobro cobro = new Cobro();
            cobro.pacienteId = pacienteId;
            cobro.tipo = "PAGO_UNICO";
            cobro.monto = monto;
            cobro.estado = codigoVerificacion != null && !codigoVerificacion.isEmpty() ? "PENDIENTE_VERIFICACION" : "PAGADO";
            cobro.fechaCobro = LocalDate.now();
            cobro.tipoComprobante = tipoComprobante;
            cobro.numDocumento = numDocumento;
            cobro.codigoVerificacion = codigoVerificacion;
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

            if (json.has("cobroIds") && json.get("cobroIds").isArray()) {
                for (JsonNode idNode : json.get("cobroIds")) {
                    try {
                        Cobro c = Cobro.findById(idNode.asLong());
                        if (c != null) {
                            c.estado = "PAGADO";
                            c.persist();
                        }
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
