package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import upeu.edu.pe.entity.Consulta;
import upeu.edu.pe.entity.EventOutbox;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ConsultaService {

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "saga.cobros.url", defaultValue = "http://ms-cobros:8080/eventos/saga")
    String cobrosSagaUrl;

    public List<Consulta> listar(String search) {
        if (search == null || search.isBlank()) {
            return Consulta.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        String trimmed = search.trim();
        if (trimmed.matches("\\d{8}")) {
            try (Client client = ClientBuilder.newClient()) {
                String json = client.target("http://ms-pacientes:8080")
                    .path("pacientes/dni/" + trimmed)
                    .request(MediaType.APPLICATION_JSON)
                    .get(String.class);
                JsonNode paciente = new ObjectMapper().readTree(json);
                Long pid = paciente.get("id").asLong();
                return Consulta.list("(LOWER(diagnostico) LIKE ?1 OR LOWER(sintomas) LIKE ?1 OR LOWER(tratamiento) LIKE ?1) OR pacienteId = ?2", pattern, pid);
            } catch (Exception e) {
                // DNI not found, fall through
            }
        }
        return Consulta.list("LOWER(diagnostico) LIKE ?1 OR LOWER(sintomas) LIKE ?1 OR LOWER(tratamiento) LIKE ?1", pattern);
    }

    public List<Consulta> findByPaciente(Long pacienteId) {
        return Consulta.findByPaciente(pacienteId);
    }

    public Consulta buscar(Long id) {
        Consulta c = Consulta.findById(id);
        if (c == null) throw new NotFoundException("Consulta no encontrada");
        return c;
    }

    @Transactional
    public Consulta crear(Consulta consulta) {
        consulta.persist();
        crearOutbox(consulta);
        return consulta;
    }

    private void crearOutbox(Consulta consulta) {
        try {
            EventOutbox outbox = new EventOutbox();
            outbox.eventId = UUID.randomUUID().toString();
            outbox.eventType = "CONSULTA_CREADA";
            outbox.source = "ms-atencion";
            outbox.targetUrl = cobrosSagaUrl;
            outbox.payload = mapper.writeValueAsString(
                mapper.createObjectNode()
                    .put("consultaId", consulta.id)
                    .put("pacienteId", consulta.pacienteId)
                    .put("citaId", consulta.citaId != null ? consulta.citaId : 0)
                    .put("doctorId", consulta.doctorId != null ? consulta.doctorId : 0)
                    .put("fechaConsulta", consulta.fechaConsulta != null ? consulta.fechaConsulta.toString() : "")
            );
            outbox.status = "PENDING";
            outbox.createdAt = LocalDateTime.now();
            outbox.retryCount = 0;
            outbox.persist();
        } catch (Exception e) {
            throw new RuntimeException("Error creating saga outbox event for consulta", e);
        }
    }
}
