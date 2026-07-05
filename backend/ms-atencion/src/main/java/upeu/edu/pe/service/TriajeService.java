package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import upeu.edu.pe.entity.Triaje;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TriajeService {

    public List<Triaje> listar(String search) {
        if (search == null || search.isBlank()) {
            return Triaje.listAll();
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
                return Triaje.list("(LOWER(motivoConsulta) LIKE ?1 OR LOWER(observaciones) LIKE ?1) OR pacienteId = ?2", pattern, pid);
            } catch (Exception e) {
                // DNI not found, fall through
            }
        }
        return Triaje.list("LOWER(motivoConsulta) LIKE ?1 OR LOWER(observaciones) LIKE ?1", pattern);
    }

    public List<Triaje> findByPaciente(Long pacienteId) {
        return Triaje.findByPaciente(pacienteId);
    }

    public List<Triaje> findByCita(Long citaId) {
        return Triaje.findByCita(citaId);
    }

    public Triaje buscar(Long id) {
        Triaje t = Triaje.findById(id);
        if (t == null) throw new NotFoundException("Triaje no encontrado");
        return t;
    }

    @Transactional
    public Triaje crear(Triaje triaje) {
        if (triaje.fechaTriaje == null) {
            triaje.fechaTriaje = LocalDateTime.now();
        }
        triaje.persist();
        return triaje;
    }

    @Transactional
    public Triaje actualizar(Long id, Triaje data) {
        Triaje t = buscar(id);
        t.peso = data.peso;
        t.talla = data.talla;
        t.presionSistolica = data.presionSistolica;
        t.presionDiastolica = data.presionDiastolica;
        t.temperatura = data.temperatura;
        t.frecuenciaCardiaca = data.frecuenciaCardiaca;
        t.spo2 = data.spo2;
        t.frecuenciaRespiratoria = data.frecuenciaRespiratoria;
        t.motivoConsulta = data.motivoConsulta;
        t.observaciones = data.observaciones;
        return t;
    }
}
