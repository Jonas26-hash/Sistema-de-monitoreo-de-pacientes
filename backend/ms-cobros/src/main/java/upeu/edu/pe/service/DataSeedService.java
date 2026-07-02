package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Servicio;
import upeu.edu.pe.entity.Campania;
import java.time.LocalDate;

@Path("/internal/seed")
@ApplicationScoped
public class DataSeedService {

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
                Servicio s4 = new Servicio(); s4.codigo = "LAB-02"; s4.nombre = "Radiografía de Tórax"; s4.tipo = "EXAMEN"; s4.precio = 60.00; s4.activo = true; s4.persist();
                Servicio s5 = new Servicio(); s5.codigo = "LAB-03"; s5.nombre = "Ecografía Abdominal"; s5.tipo = "EXAMEN"; s5.precio = 90.00; s5.activo = true; s5.persist();
                Servicio s6 = new Servicio(); s6.codigo = "LAB-04"; s6.nombre = "Análisis de Orina"; s6.tipo = "EXAMEN"; s6.precio = 20.00; s6.activo = true; s6.persist();
                Servicio s7 = new Servicio(); s7.codigo = "REC-01"; s7.nombre = "Receta Médica"; s7.tipo = "RECETA"; s7.precio = 0.00; s7.activo = true; s7.persist();
                servCount = 7;
            }
            int campCount = (int) Campania.count();
            if (campCount == 0) {
                Campania c1 = new Campania(); c1.codigo = "CAMP-01"; c1.nombre = "Campaña de Salud Preventiva"; c1.descuentoPorcentaje = 20; c1.fechaInicio = LocalDate.now().minusDays(5); c1.fechaFin = LocalDate.now().plusMonths(1); c1.activo = true; c1.persist();
                Campania c2 = new Campania(); c2.codigo = "CAMP-02"; c2.nombre = "Descuento en Exámenes de Laboratorio"; c2.descripcion = "50% de descuento en todos los exámenes de laboratorio"; c2.descuentoPorcentaje = 50; c2.fechaInicio = LocalDate.now().minusDays(10); c2.fechaFin = LocalDate.now().plusMonths(2); c2.activo = true; c2.persist();
                Campania c3 = new Campania(); c3.codigo = "CAMP-03"; c3.nombre = "Campaña de Vacunación"; c3.descuentoPorcentaje = 15; c3.fechaInicio = LocalDate.now().minusDays(2); c3.fechaFin = LocalDate.now().plusWeeks(3); c3.activo = true; c3.persist();
                campCount = 3;
            }
            return Response.ok("{\"servicios\":" + servCount + ",\"campanias\":" + campCount + "}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}").build();
        }
    }
}
