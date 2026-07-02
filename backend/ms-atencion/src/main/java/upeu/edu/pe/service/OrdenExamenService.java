package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.OrdenExamen;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class OrdenExamenService {

    public List<OrdenExamen> listar(String search) {
        if (search == null || search.isBlank()) {
            return OrdenExamen.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return OrdenExamen.list("LOWER(descripcion) LIKE ?1 OR LOWER(tipo) LIKE ?1", pattern);
    }

    public List<OrdenExamen> findByPaciente(Long pacienteId) {
        return OrdenExamen.findByPaciente(pacienteId);
    }

    public List<OrdenExamen> findByCita(Long citaId) {
        return OrdenExamen.findByCita(citaId);
    }

    public List<OrdenExamen> pendientesByPaciente(Long pacienteId) {
        return OrdenExamen.findPendientesByPaciente(pacienteId);
    }

    public OrdenExamen buscar(Long id) {
        OrdenExamen e = OrdenExamen.findById(id);
        if (e == null) throw new NotFoundException("Orden de examen no encontrada");
        return e;
    }

    @Transactional
    public OrdenExamen crear(OrdenExamen examen) {
        if (examen.fechaOrden == null) {
            examen.fechaOrden = LocalDateTime.now();
        }
        if (examen.estado == null || examen.estado.isBlank()) {
            examen.estado = "PENDIENTE";
        }
        if (examen.pagado == null) {
            examen.pagado = false;
        }
        examen.persist();
        return examen;
    }

    @Transactional
    public OrdenExamen actualizar(Long id, OrdenExamen data) {
        OrdenExamen e = buscar(id);
        e.tipo = data.tipo;
        e.descripcion = data.descripcion;
        e.costo = data.costo;
        return e;
    }

    @Transactional
    public OrdenExamen ingresarResultado(Long id, String body) {
        OrdenExamen e = buscar(id);
        try {
            ObjectMapper localMapper = new ObjectMapper();
            JsonNode json = localMapper.readTree(body);
            if (json.has("resultado")) {
                e.resultado = json.get("resultado").asText();
            }
            e.estado = "COMPLETADO";
            e.fechaResultado = LocalDateTime.now();
            return e;
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON inv\u00e1lido");
        }
    }

    @Transactional
    public OrdenExamen pagar(Long id) {
        OrdenExamen e = buscar(id);
        e.pagado = true;
        return e;
    }

    @Transactional
    public void eliminar(Long id) {
        OrdenExamen e = buscar(id);
        e.delete();
    }
}
