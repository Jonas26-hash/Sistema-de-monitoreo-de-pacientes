package upeu.edu.pe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import upeu.edu.pe.entity.Cita;

@ApplicationScoped
public class SagaCompensationService {

    @Inject
    ObjectMapper mapper;

    public void cancelarCitaPorFacturaFallida(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            Long citaId = node.has("citaId") ? node.get("citaId").asLong() : null;
            if (citaId == null) return;

            Cita cita = Cita.findById(citaId);
            if (cita != null) {
                cita.estado = "CANCELADA";
                cita.persist();
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing compensation payload", e);
        }
    }
}
