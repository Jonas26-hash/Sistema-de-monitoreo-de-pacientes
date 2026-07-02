package upeu.edu.pe.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import upeu.edu.pe.dto.HistorialEntry;
import upeu.edu.pe.entity.Cita;
import upeu.edu.pe.entity.Consulta;
import upeu.edu.pe.entity.OrdenExamen;
import upeu.edu.pe.entity.Triaje;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;

@Path("/historial")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistorialResource {

    @Inject
    ObjectMapper mapper;

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<HistorialEntry> obtenerHistorial(@PathParam("pacienteId") Long pacienteId) {
        List<HistorialEntry> entries = new ArrayList<>();

        for (Cita c : Cita.findByPaciente(pacienteId)) {
            entries.add(new HistorialEntry("CITA",
                c.fechaHora.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Cita " + c.estado,
                c.motivo != null ? c.motivo : "Sin motivo",
                c));
        }

        for (Triaje t : Triaje.findByPaciente(pacienteId)) {
            entries.add(new HistorialEntry("TRIAJE",
                t.fechaTriaje.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Triaje",
                String.format("Peso: %.1fkg | Talla: %.1fm | Temp: %.1f°C | SpO2: %.0f%% | Pulso: %d",
                    t.peso != null ? t.peso : 0,
                    t.talla != null ? t.talla : 0,
                    t.temperatura != null ? t.temperatura : 0,
                    t.spo2 != null ? t.spo2 : 0,
                    t.frecuenciaCardiaca != null ? t.frecuenciaCardiaca : 0),
                t));
        }

        for (Consulta c : Consulta.findByPaciente(pacienteId)) {
            entries.add(new HistorialEntry("CONSULTA",
                c.fechaConsulta.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Consulta Médica",
                c.diagnostico != null ? c.diagnostico : "Sin diagnóstico",
                c));
        }

        for (OrdenExamen o : OrdenExamen.findByPaciente(pacienteId)) {
            entries.add(new HistorialEntry("EXAMEN",
                o.fechaOrden.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Examen: " + o.tipo,
                (o.resultado != null ? "Resultado: " + o.resultado : "Pendiente"),
                o));
        }

        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-recetas:8080")
                .path("recetas/paciente/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode recetas = mapper.readTree(json);
            if (recetas.isArray()) {
                for (JsonNode r : recetas) {
                    String fecha = r.has("fechaEmision") ? r.get("fechaEmision").asText() + "T00:00:00" : "";
                    entries.add(new HistorialEntry("RECETA",
                        fecha,
                        "Receta Médica",
                        r.has("medicamentos") ? r.get("medicamentos").asText().substring(0, Math.min(100, r.get("medicamentos").asText().length())) : "",
                        mapper.treeToValue(r, Map.class)));
                }
            }
        } catch (Exception e) {
        }

        entries.sort((a, b) -> b.fecha.compareTo(a.fecha));
        return entries;
    }
}
