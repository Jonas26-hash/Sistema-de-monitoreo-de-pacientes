package upeu.edu.pe.seed;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import upeu.edu.pe.entity.*;
import java.time.LocalDateTime;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

@Path("/internal/seed")
@ApplicationScoped
public class DataSeedService {

    @Inject
    @ConfigProperty(name = "ms.pacientes.url", defaultValue = "http://localhost:8081")
    String msPacientesUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Transactional
    public Response seed() {
        System.out.println("[DataSeedService] seed triggered via HTTP");
        try {
            if (Cita.count() > 0) {
                return Response.ok("{\"mensaje\":\"Ya existen datos\"}").build();
            }
            internalSeed();
            long citas = Cita.count();
            long triajes = Triaje.count();
            long consultas = Consulta.count();
            long ordenes = OrdenExamen.count();
            return Response.ok("{\"citas\":" + citas + ",\"triajes\":" + triajes +
                ",\"consultas\":" + consultas + ",\"ordenes\":" + ordenes + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }

    private void internalSeed() throws Exception {
        Map<String, Long> pacientes = new HashMap<>();
        String[][] dnis = {
            {"87654321", "Pedro"}, {"12345678", "Homero"}, {"23456789", "Maria"},
            {"34567890", "Juan"}, {"45678901", "Lisa"}
        };
        for (String[] e : dnis) {
            Long id = httpGetId(msPacientesUrl + "/pacientes/dni/" + e[0]);
            if (id != null) pacientes.put(e[0], id);
            System.out.println("[DataSeedService] Paciente " + e[1] + " = ID " + id);
        }

        Map<String, Long> usuarios = new HashMap<>();
        String[] usernames = {"ppicapiedra", "doctor1", "doctor2", "enfermero"};
        for (String u : usernames) {
            Long id = httpGetId(msPacientesUrl + "/internal/usuario/" + u);
            if (id != null) usuarios.put(u, id);
            System.out.println("[DataSeedService] Usuario " + u + " = ID " + id);
        }

        if (pacientes.size() < 3)
            throw new RuntimeException("Solo " + pacientes.size() + " pacientes encontrados");
        if (!usuarios.containsKey("ppicapiedra"))
            throw new RuntimeException("Usuario ppicapiedra no encontrado");

        long pPedro = pacientes.get("87654321");
        long pHomero = pacientes.get("12345678");
        long pMaria = pacientes.get("23456789");
        long pJuan = pacientes.get("34567890");
        long pLisa = pacientes.get("45678901");
        long drPicapiedra = usuarios.get("ppicapiedra");
        long drJuan = usuarios.get("doctor1");
        long draMaria = usuarios.get("doctor2");
        long enfRosa = usuarios.get("enfermero");

        Cita c1 = new Cita(); c1.pacienteId = pPedro; c1.doctorId = drPicapiedra; c1.fechaHora = LocalDateTime.of(2026,6,20,10,0); c1.estado = "COMPLETADA"; c1.motivo = "Control general"; c1.precio = 50.0; c1.persist();
        Cita c2 = new Cita(); c2.pacienteId = pHomero; c2.doctorId = drJuan; c2.fechaHora = LocalDateTime.of(2026,6,21,11,0); c2.estado = "COMPLETADA"; c2.motivo = "Dolor de cabeza persistente"; c2.precio = 50.0; c2.persist();
        Cita c3 = new Cita(); c3.pacienteId = pMaria; c3.doctorId = draMaria; c3.fechaHora = LocalDateTime.of(2026,6,22,9,0); c3.estado = "COMPLETADA"; c3.motivo = "Chequeo prenatal"; c3.precio = 80.0; c3.persist();
        Cita c4 = new Cita(); c4.pacienteId = pJuan; c4.doctorId = drPicapiedra; c4.fechaHora = LocalDateTime.of(2026,7,10,15,0); c4.estado = "PROGRAMADA"; c4.motivo = "Dolor lumbar"; c4.precio = 50.0; c4.persist();
        Cita c5 = new Cita(); c5.pacienteId = pLisa; c5.doctorId = drJuan; c5.fechaHora = LocalDateTime.of(2026,7,11,8,0); c5.estado = "PROGRAMADA"; c5.motivo = "Revision escolar"; c5.precio = 50.0; c5.persist();
        Cita c6 = new Cita(); c6.pacienteId = pHomero; c6.doctorId = drPicapiedra; c6.fechaHora = LocalDateTime.of(2026,7,12,10,30); c6.estado = "PROGRAMADA"; c6.motivo = "Control de presion arterial"; c6.precio = 50.0; c6.persist();

        Triaje t1 = new Triaje(); t1.pacienteId = pPedro; t1.citaId = c1.id; t1.enfermeroId = enfRosa; t1.fechaTriaje = LocalDateTime.of(2026,6,20,9,45); t1.peso = 78.5; t1.talla = 1.75; t1.presionSistolica = 120; t1.presionDiastolica = 80; t1.temperatura = 36.6; t1.frecuenciaCardiaca = 72; t1.spo2 = 98.0; t1.frecuenciaRespiratoria = 16; t1.motivoConsulta = "Control general"; t1.persist();
        Triaje t2 = new Triaje(); t2.pacienteId = pHomero; t2.citaId = c2.id; t2.enfermeroId = enfRosa; t2.fechaTriaje = LocalDateTime.of(2026,6,21,10,45); t2.peso = 95.0; t2.talla = 1.80; t2.presionSistolica = 135; t2.presionDiastolica = 85; t2.temperatura = 37.0; t2.frecuenciaCardiaca = 80; t2.spo2 = 97.0; t2.frecuenciaRespiratoria = 18; t2.motivoConsulta = "Dolor de cabeza persistente"; t2.persist();
        Triaje t3 = new Triaje(); t3.pacienteId = pMaria; t3.citaId = c3.id; t3.enfermeroId = enfRosa; t3.fechaTriaje = LocalDateTime.of(2026,6,22,8,45); t3.peso = 62.0; t3.talla = 1.60; t3.presionSistolica = 110; t3.presionDiastolica = 70; t3.temperatura = 36.8; t3.frecuenciaCardiaca = 76; t3.spo2 = 99.0; t3.frecuenciaRespiratoria = 16; t3.motivoConsulta = "Chequeo prenatal"; t3.persist();

        Consulta cs1 = new Consulta(); cs1.pacienteId = pPedro; cs1.citaId = c1.id; cs1.doctorId = drPicapiedra; cs1.fechaConsulta = LocalDateTime.of(2026,6,20,10,30); cs1.sintomas = "Paciente asintomatico, control rutinario"; cs1.diagnostico = "Paciente en buen estado general"; cs1.tratamiento = "Continuar con habitos saludables. Control en 6 meses"; cs1.persist();
        Consulta cs2 = new Consulta(); cs2.pacienteId = pHomero; cs2.citaId = c2.id; cs2.doctorId = drJuan; cs2.fechaConsulta = LocalDateTime.of(2026,6,21,11,30); cs2.sintomas = "Cefalea tensional frecuente, empeora con estres"; cs2.diagnostico = "Cefalea tensional cronica"; cs2.tratamiento = "Ibuprofeno 400mg cada 8h por 7 dias. Reducir estres."; cs2.persist();
        Consulta cs3 = new Consulta(); cs3.pacienteId = pMaria; cs3.citaId = c3.id; cs3.doctorId = draMaria; cs3.fechaConsulta = LocalDateTime.of(2026,6,22,9,30); cs3.sintomas = "Embarazo de 12 semanas, control prenatal"; cs3.diagnostico = "Embarazo en curso, aparentemente normal"; cs3.tratamiento = "Suplemento de acido folico. Proxima ecografia en 4 semanas."; cs3.persist();

        OrdenExamen oe1 = new OrdenExamen(); oe1.pacienteId = pPedro; oe1.citaId = c1.id; oe1.doctorId = drPicapiedra; oe1.tipo = "LABORATORIO"; oe1.descripcion = "Hemograma completo, perfil lipidico"; oe1.estado = "COMPLETADO"; oe1.fechaOrden = LocalDateTime.of(2026,6,20,11,0); oe1.costo = 35.0; oe1.pagado = true; oe1.fechaResultado = LocalDateTime.of(2026,6,22,8,0); oe1.resultado = "Hemoglobina 14.5 g/dL, Colesterol total 190 mg/dL - Normal"; oe1.persist();
        OrdenExamen oe2 = new OrdenExamen(); oe2.pacienteId = pHomero; oe2.citaId = c2.id; oe2.doctorId = drJuan; oe2.tipo = "IMAGEN"; oe2.descripcion = "Radiografia de torax AP y lateral"; oe2.estado = "PENDIENTE"; oe2.fechaOrden = LocalDateTime.of(2026,6,21,12,0); oe2.costo = 60.0; oe2.pagado = false; oe2.persist();
        OrdenExamen oe3 = new OrdenExamen(); oe3.pacienteId = pMaria; oe3.citaId = c3.id; oe3.doctorId = draMaria; oe3.tipo = "IMAGEN"; oe3.descripcion = "Ecografia obstetrica transabdominal"; oe3.estado = "PENDIENTE"; oe3.fechaOrden = LocalDateTime.of(2026,6,22,10,0); oe3.costo = 90.0; oe3.pagado = false; oe3.persist();

        System.out.println("[DataSeedService] Seed completado: 6 citas, 3 triajes, 3 consultas, 3 ordenes");
    }

    private Long httpGetId(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                JsonNode node = mapper.readTree(conn.getInputStream());
                conn.disconnect();
                JsonNode idNode = node.get("id");
                if (idNode != null && !idNode.isNull()) return idNode.asLong();
            } else {
                conn.disconnect();
            }
        } catch (Exception e) {
            System.out.println("[DataSeedService] HTTP error: " + e.getMessage());
        }
        return null;
    }
}
