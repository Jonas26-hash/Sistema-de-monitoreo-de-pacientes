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

import upeu.edu.pe.entity.Receta;
import java.time.LocalDate;
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
            if (Receta.count() > 0) {
                return Response.ok("{\"mensaje\":\"Ya existen datos\"}").build();
            }
            internalSeed();
            return Response.ok("{\"recetas\":" + Receta.count() + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }

    private void internalSeed() throws Exception {
        Map<String, Long> pacientes = new HashMap<>();
        String[][] dnis = {
            {"87654321", "Pedro"}, {"12345678", "Homero"}, {"23456789", "Maria"}
        };
        for (String[] e : dnis) {
            Long id = httpGetId(msPacientesUrl + "/pacientes/dni/" + e[0]);
            if (id != null) pacientes.put(e[0], id);
            System.out.println("[DataSeedService] Paciente " + e[1] + " = ID " + id);
        }

        Map<String, Long> usuarios = new HashMap<>();
        String[] usernames = {"ppicapiedra", "doctor1"};
        for (String u : usernames) {
            Long id = httpGetId(msPacientesUrl + "/internal/usuario/" + u);
            if (id != null) usuarios.put(u, id);
            System.out.println("[DataSeedService] Usuario " + u + " = ID " + id);
        }

        if (pacientes.size() < 2)
            throw new RuntimeException("Solo " + pacientes.size() + " pacientes encontrados");
        if (!usuarios.containsKey("ppicapiedra"))
            throw new RuntimeException("Usuario ppicapiedra no encontrado");

        long pPedro = pacientes.get("87654321");
        long pHomero = pacientes.get("12345678");
        long pMaria = pacientes.get("23456789");
        long drPicapiedra = usuarios.get("ppicapiedra");
        long drJuan = usuarios.get("doctor1");

        Receta r1 = new Receta(); r1.pacienteId = pPedro; r1.doctorId = drPicapiedra; r1.fechaEmision = LocalDate.of(2026,6,20); r1.fechaVigencia = LocalDate.of(2026,7,20); r1.medicamentos = "Ibuprofeno 400mg - 1 tableta cada 8h por 7 dias\nParacetamol 500mg - 1 tableta cada 6h si dolor"; r1.indicaciones = "Tomar despues de comidas. No exceder dosis."; r1.dispensada = true; r1.pagado = true; r1.fechaDispensacion = LocalDate.of(2026,6,20); r1.costo = 25.0; r1.persist();

        Receta r2 = new Receta(); r2.pacienteId = pHomero; r2.doctorId = drJuan; r2.fechaEmision = LocalDate.of(2026,6,21); r2.fechaVigencia = LocalDate.of(2026,7,21); r2.medicamentos = "Amoxicilina 500mg - 1 capsula cada 8h por 10 dias\nLoratadina 10mg - 1 tableta cada 24h por 5 dias"; r2.indicaciones = "Completar el tratamiento aunque haya mejoria."; r2.dispensada = false; r2.pagado = false; r2.costo = 40.0; r2.persist();

        Receta r3 = new Receta(); r3.pacienteId = pMaria; r3.doctorId = drPicapiedra; r3.fechaEmision = LocalDate.of(2026,6,22); r3.fechaVigencia = LocalDate.of(2026,7,22); r3.medicamentos = "Acido Folico 5mg - 1 tableta cada 24h\nHierro 100mg - 1 tableta cada 24h"; r3.indicaciones = "Tomar durante todo el embarazo. No suspender."; r3.dispensada = false; r3.pagado = false; r3.costo = 30.0; r3.persist();

        System.out.println("[DataSeedService] Seed completado: 3 recetas");
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
