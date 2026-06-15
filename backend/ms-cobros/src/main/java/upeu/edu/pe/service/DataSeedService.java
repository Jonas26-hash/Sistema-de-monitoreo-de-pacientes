package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.Servicio;
import upeu.edu.pe.entity.Campania;
import java.time.LocalDate;

@ApplicationScoped
public class DataSeedService {

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (Servicio.count() == 0) {
            crear("CONS-01", "Consulta General", "CONSULTA", 50.00);
            crear("CONS-02", "Consulta Especialista", "CONSULTA", 80.00);
            crear("LAB-01", "Hemograma Completo", "EXAMEN", 35.00);
            crear("LAB-02", "Radiografía de Tórax", "EXAMEN", 60.00);
            crear("LAB-03", "Ecografía Abdominal", "EXAMEN", 90.00);
            crear("LAB-04", "Análisis de Orina", "EXAMEN", 20.00);
            crear("REC-01", "Receta Médica", "RECETA", 0.00);
        }

        if (Campania.count() == 0) {
            crearCampania("CAMP-01", "Campaña de Salud Preventiva", null, 20,
                LocalDate.now().minusDays(5), LocalDate.now().plusMonths(1));
            crearCampania("CAMP-02", "Descuento en Exámenes de Laboratorio",
                "50% de descuento en todos los exámenes de laboratorio", 50,
                LocalDate.now().minusDays(10), LocalDate.now().plusMonths(2));
            crearCampania("CAMP-03", "Campaña de Vacunación", null, 15,
                LocalDate.now().minusDays(2), LocalDate.now().plusWeeks(3));
        }
    }

    void crear(String codigo, String nombre, String tipo, double precio) {
        Servicio s = new Servicio();
        s.codigo = codigo;
        s.nombre = nombre;
        s.tipo = tipo;
        s.precio = precio;
        s.activo = true;
        s.persist();
    }

    void crearCampania(String codigo, String nombre, String descripcion, int descuento, LocalDate inicio, LocalDate fin) {
        Campania c = new Campania();
        c.codigo = codigo;
        c.nombre = nombre;
        c.descripcion = descripcion;
        c.descuentoPorcentaje = descuento;
        c.fechaInicio = inicio;
        c.fechaFin = fin;
        c.activo = true;
        c.persist();
    }
}