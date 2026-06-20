package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Campania;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class CampaniaService {

    public List<Campania> listar() {
        return Campania.listAll();
    }

    public List<Campania> activas() {
        LocalDate today = LocalDate.now();
        return Campania.list("activo = true AND fechaInicio <= ?1 AND fechaFin >= ?1", today);
    }

    public Campania buscar(Long id) {
        Campania c = Campania.findById(id);
        if (c == null) throw new NotFoundException("Campa\u00f1a no encontrada");
        return c;
    }

    @Transactional
    public Campania crear(Campania c) {
        c.persist();
        return c;
    }

    @Transactional
    public Campania actualizar(Long id, Campania data) {
        Campania c = buscar(id);
        c.codigo = data.codigo;
        c.nombre = data.nombre;
        c.descripcion = data.descripcion;
        c.descuentoPorcentaje = data.descuentoPorcentaje;
        c.fechaInicio = data.fechaInicio;
        c.fechaFin = data.fechaFin;
        c.activo = data.activo;
        return c;
    }

    @Transactional
    public Campania toggle(Long id) {
        Campania c = buscar(id);
        c.activo = !c.activo;
        return c;
    }

    @Transactional
    public void eliminar(Long id) {
        Campania c = buscar(id);
        c.delete();
    }
}
