package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import upeu.edu.pe.entity.Notificacion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class RecordatorioScheduler {

    @Inject
    EmailService emailService;

    @Inject
    ObjectMapper mapper;

    @Scheduled(cron = "0 8 * * *")
    @Transactional
    public void verificarRecordatorios() {
        try { recordatorioCitas(); } catch (Exception e) { System.err.println("[Scheduler] Error citas: " + e.getMessage()); }
        try { recordatorioDeudas(); } catch (Exception e) { System.err.println("[Scheduler] Error deudas: " + e.getMessage()); }
    }

    private void recordatorioCitas() {
        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-atencion:8080")
                .path("citas/proximas")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode citas;
            try { citas = mapper.readTree(json); } catch (Exception e) { return; }
            if (citas.isArray()) {
                for (JsonNode cita : citas) {
                    Long pacienteId = cita.get("pacienteId").asLong();
                    String fechaHora = cita.has("fechaHora") ? cita.get("fechaHora").asText() : "";
                    String motivo = cita.has("motivo") ? cita.get("motivo").asText() : "consulta";

                    String email = obtenerEmailPaciente(pacienteId);
                    if (email != null && !email.isBlank()) {
                        String asunto = "Recordatorio de Cita - Sistema de Monitoreo de Pacientes";
                        String mensaje = "Estimado paciente,\n\n"
                            + "Le recordamos que tiene una cita programada:\n"
                            + "Fecha: " + fechaHora + "\n"
                            + "Motivo: " + motivo + "\n\n"
                            + "Por favor, asista puntualmente.\n\n"
                            + "Saludos,\nSistema de Monitoreo de Pacientes";
                        emailService.enviarNotificacion(email, asunto, mensaje);

                        Notificacion n = new Notificacion();
                        n.pacienteId = pacienteId;
                        n.tipo = "RECORDATORIO";
                        n.mensaje = "Recordatorio: Tiene una cita el " + fechaHora + " - " + motivo;
                        n.fechaEnvio = LocalDateTime.now();
                        n.enviada = true;
                        n.canal = "EMAIL";
                        n.remitenteTipo = "SISTEMA";
                        n.remitenteNombre = "Sistema";
                        n.persist();
                    }
                }
            }
        }
    }

    private void recordatorioDeudas() {
        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-cobros:8080")
                .path("cobros/pendientes-agrupados")
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode cobros;
            try { cobros = mapper.readTree(json); } catch (Exception e) { return; }
            if (cobros.isArray()) {
                Long lastPacienteId = null;
                double totalMonto = 0;
                int count = 0;
                for (JsonNode c : cobros) {
                    Long pid = c.get("pacienteId").asLong();
                    if (lastPacienteId != null && !pid.equals(lastPacienteId)) {
                        enviarRecordatorioDeuda(lastPacienteId, count, totalMonto);
                        totalMonto = 0; count = 0;
                    }
                    totalMonto += c.has("monto") ? c.get("monto").asDouble() : 0;
                    count++;
                    lastPacienteId = pid;
                }
                if (lastPacienteId != null) {
                    enviarRecordatorioDeuda(lastPacienteId, count, totalMonto);
                }
            }
        }
    }

    private void enviarRecordatorioDeuda(Long pacienteId, int count, double totalMonto) {
        String email = obtenerEmailPaciente(pacienteId);
        if (email != null && !email.isBlank()) {
            String asunto = "Recordatorio de Pago - Sistema de Monitoreo de Pacientes";
            String mensaje = "Estimado paciente,\n\n"
                + "Usted tiene " + count + " comprobante(s) pendiente(s) de pago por un total de S/ "
                + String.format("%.2f", totalMonto) + ".\n\n"
                + "Puede realizar el pago de las siguientes maneras:\n"
                + "1. Acercarse a nuestra clínica y pagar en ventanilla.\n"
                + "2. Pagar desde el sistema mediante Yape QR, ingresando a su portal de paciente.\n\n"
                + "Saludos,\nSistema de Monitoreo de Pacientes";
            emailService.enviarNotificacion(email, asunto, mensaje);

            Notificacion n = new Notificacion();
            n.pacienteId = pacienteId;
            n.tipo = "RECORDATORIO";
            n.mensaje = "Recordatorio: Tiene " + count + " deuda(s) pendiente(s) por S/ " + String.format("%.2f", totalMonto);
            n.fechaEnvio = LocalDateTime.now();
            n.enviada = true;
            n.canal = "EMAIL";
            n.remitenteTipo = "SISTEMA";
            n.remitenteNombre = "Sistema";
            n.persist();
        }
    }

    private String obtenerEmailPaciente(Long pacienteId) {
        try (Client client = ClientBuilder.newClient()) {
            String json = client.target("http://ms-pacientes:8080")
                .path("pacientes/" + pacienteId)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
            JsonNode p = mapper.readTree(json);
            return p.has("email") ? p.get("email").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
