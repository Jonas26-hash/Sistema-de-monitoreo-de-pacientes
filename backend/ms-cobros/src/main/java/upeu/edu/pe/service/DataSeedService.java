package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import upeu.edu.pe.entity.Servicio;
import upeu.edu.pe.entity.Campania;
import upeu.edu.pe.entity.Cobro;
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
            int servCount = (int) Servicio.count();
            if (servCount == 0) {
                Servicio s = new Servicio(); s.codigo = "CONS-01"; s.nombre = "Consulta General"; s.tipo = "CONSULTA"; s.precio = 50.00; s.activo = true; s.persist();
                Servicio s2 = new Servicio(); s2.codigo = "CONS-02"; s2.nombre = "Consulta Especialista"; s2.tipo = "CONSULTA"; s2.precio = 80.00; s2.activo = true; s2.persist();
                Servicio s3 = new Servicio(); s3.codigo = "LAB-01"; s3.nombre = "Hemograma Completo"; s3.tipo = "EXAMEN"; s3.precio = 35.00; s3.activo = true; s3.persist();
                Servicio s4 = new Servicio(); s4.codigo = "LAB-02"; s4.nombre = "Radiograf\u00eda de T\u00f3rax"; s4.tipo = "EXAMEN"; s4.precio = 60.00; s4.activo = true; s4.persist();
                Servicio s5 = new Servicio(); s5.codigo = "LAB-03"; s5.nombre = "Ecograf\u00eda Abdominal"; s5.tipo = "EXAMEN"; s5.precio = 90.00; s5.activo = true; s5.persist();
                Servicio s6 = new Servicio(); s6.codigo = "LAB-04"; s6.nombre = "An\u00e1lisis de Orina"; s6.tipo = "EXAMEN"; s6.precio = 20.00; s6.activo = true; s6.persist();
                Servicio s7 = new Servicio(); s7.codigo = "REC-01"; s7.nombre = "Receta M\u00e9dica"; s7.tipo = "RECETA"; s7.precio = 0.00; s7.activo = true; s7.persist();
                servCount = 7;
            }
            int campCount = (int) Campania.count();
            if (campCount == 0) {
                Campania c1 = new Campania(); c1.codigo = "CAMP-01"; c1.nombre = "Campa\u00f1a de Salud Preventiva"; c1.descuentoPorcentaje = 20; c1.fechaInicio = LocalDate.now().minusDays(5); c1.fechaFin = LocalDate.now().plusMonths(1); c1.activo = true; c1.persist();
                Campania c2 = new Campania(); c2.codigo = "CAMP-02"; c2.nombre = "Descuento en Ex\u00e1menes de Laboratorio"; c2.descripcion = "50% de descuento en todos los ex\u00e1menes de laboratorio"; c2.descuentoPorcentaje = 50; c2.fechaInicio = LocalDate.now().minusDays(10); c2.fechaFin = LocalDate.now().plusMonths(2); c2.activo = true; c2.persist();
                Campania c3 = new Campania(); c3.codigo = "CAMP-03"; c3.nombre = "Campa\u00f1a de Vacunaci\u00f3n"; c3.descuentoPorcentaje = 15; c3.fechaInicio = LocalDate.now().minusDays(2); c3.fechaFin = LocalDate.now().plusWeeks(3); c3.activo = true; c3.persist();
                campCount = 3;
            }
            int cobroCount = (int) Cobro.count();
            if (cobroCount == 0) {
                Map<String, Long> pacientes = new HashMap<>();
                String[] dnis = {"87654321", "12345678", "23456789", "34567890", "45678901"};
                for (String dni : dnis) {
                    Long id = httpGetId(msPacientesUrl + "/pacientes/dni/" + dni);
                    if (id != null) pacientes.put(dni, id);
                }
                if (pacientes.size() >= 2) {
                    Cobro cb1 = new Cobro(); cb1.pacienteId = pacientes.get("87654321"); cb1.tipo = "CONSULTA"; cb1.monto = 50.0; cb1.estado = "PAGADO"; cb1.fechaCobro = LocalDate.of(2026,6,20); cb1.descripcion = "Consulta General - Dr. Picapiedra"; cb1.tipoComprobante = "BOLETA"; cb1.numDocumento = "B001-00000001"; cb1.persist();
                    Cobro cb2 = new Cobro(); cb2.pacienteId = pacientes.get("87654321"); cb2.tipo = "EXAMEN"; cb2.monto = 35.0; cb2.estado = "PAGADO"; cb2.fechaCobro = LocalDate.of(2026,6,20); cb2.descripcion = "Hemograma Completo"; cb2.tipoComprobante = "BOLETA"; cb2.numDocumento = "B001-00000002"; cb2.persist();
                    Cobro cb3 = new Cobro(); cb3.pacienteId = pacientes.get("12345678"); cb3.tipo = "CONSULTA"; cb3.monto = 50.0; cb3.estado = "PENDIENTE"; cb3.descripcion = "Consulta General - Dr. Juan Perez"; cb3.persist();
                    Cobro cb4 = new Cobro(); cb4.pacienteId = pacientes.get("12345678"); cb4.tipo = "EXAMEN"; cb4.monto = 60.0; cb4.estado = "PENDIENTE"; cb4.descripcion = "Radiograf\u00eda de T\u00f3rax"; cb4.persist();
                    Cobro cb5 = new Cobro(); cb5.pacienteId = pacientes.get("23456789"); cb5.tipo = "CONSULTA"; cb5.monto = 80.0; cb5.estado = "PENDIENTE"; cb5.descripcion = "Consulta Especialista - Dra. Maria Garcia"; cb5.persist();
                    cobroCount = 5;
                }
            }
            return Response.ok("{\"servicios\":" + servCount + ",\"campanias\":" + campCount + ",\"cobros\":" + cobroCount + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}").build();
        }
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
