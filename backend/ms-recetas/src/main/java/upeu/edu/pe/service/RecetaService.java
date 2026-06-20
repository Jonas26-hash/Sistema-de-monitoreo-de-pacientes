package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Receta;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RecetaService {

    public List<Receta> listar() {
        return Receta.listAll();
    }

    public List<Receta> pendientes() {
        return Receta.list("dispensada = ?1", false);
    }

    public List<Receta> findByPaciente(Long pacienteId) {
        return Receta.list("pacienteId = ?1", pacienteId);
    }

    public Receta buscar(Long id) {
        Receta r = Receta.findById(id);
        if (r == null) throw new NotFoundException("Receta no encontrada");
        return r;
    }

    @Transactional
    public Receta crear(Receta receta) {
        receta.persist();
        return receta;
    }

    @Transactional
    public Receta actualizar(Long id, Receta data) {
        Receta r = buscar(id);
        r.consultaId = data.consultaId;
        r.pacienteId = data.pacienteId;
        r.doctorId = data.doctorId;
        r.fechaEmision = data.fechaEmision;
        r.fechaVigencia = data.fechaVigencia;
        r.medicamentos = data.medicamentos;
        r.indicaciones = data.indicaciones;
        r.dispensada = data.dispensada;
        r.fechaDispensacion = data.fechaDispensacion;
        r.pagado = data.pagado;
        return r;
    }

    @Transactional
    public Receta dispensar(Long id) {
        Receta r = buscar(id);
        r.dispensada = true;
        r.fechaDispensacion = LocalDate.now();
        return r;
    }

    @Transactional
    public Receta pagar(Long id) {
        Receta r = buscar(id);
        r.pagado = true;
        return r;
    }

    public List<Receta> pendientesPagoByPaciente(Long pacienteId) {
        return Receta.list("pacienteId = ?1 AND (pagado IS NULL OR pagado = false) AND dispensada = ?2", pacienteId, false);
    }

    @Transactional
    public void eliminar(Long id) {
        boolean deleted = Receta.deleteById(id);
        if (!deleted) throw new NotFoundException("Receta no encontrada");
    }
}
