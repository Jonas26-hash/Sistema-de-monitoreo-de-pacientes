package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Dispensacion;
import upeu.edu.pe.entity.Medicamento;
import java.util.List;

@ApplicationScoped
public class DispensacionService {

    public List<Dispensacion> listar(String search) {
        if (search == null || search.isBlank()) {
            return Dispensacion.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return Dispensacion.list("LOWER(observaciones) LIKE ?1", pattern);
    }

    public List<Dispensacion> findByReceta(Long recetaId) {
        return Dispensacion.list("recetaId = ?1", recetaId);
    }

    @Transactional
    public Dispensacion crear(Dispensacion dispensacion) {
        if (dispensacion.medicamentoId != null && dispensacion.cantidad != null) {
            Medicamento med = Medicamento.findById(dispensacion.medicamentoId);
            if (med != null) {
                if (med.stock < dispensacion.cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente");
                }
                med.stock -= dispensacion.cantidad;
            }
        }
        dispensacion.persist();
        return dispensacion;
    }
}
