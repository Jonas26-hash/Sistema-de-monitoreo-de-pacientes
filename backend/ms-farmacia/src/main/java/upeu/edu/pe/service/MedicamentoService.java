package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Medicamento;
import java.util.List;

@ApplicationScoped
public class MedicamentoService {

    public List<Medicamento> listar(String search) {
        if (search == null || search.isBlank()) {
            return Medicamento.listAll();
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return Medicamento.list("LOWER(nombre) LIKE ?1 OR LOWER(codigo) LIKE ?1", pattern);
    }

    public Medicamento buscar(Long id) {
        Medicamento m = Medicamento.findById(id);
        if (m == null) throw new NotFoundException("Medicamento no encontrado");
        return m;
    }

    public Medicamento buscarPorCodigo(String codigo) {
        Medicamento m = Medicamento.find("codigo", codigo).firstResult();
        if (m == null) throw new NotFoundException("Medicamento no encontrado");
        return m;
    }

    @Transactional
    public Medicamento crear(Medicamento medicamento) {
        medicamento.persist();
        return medicamento;
    }

    @Transactional
    public Medicamento actualizar(Long id, Medicamento data) {
        Medicamento m = buscar(id);
        m.nombre = data.nombre;
        m.descripcion = data.descripcion;
        m.stock = data.stock;
        m.stockMinimo = data.stockMinimo;
        m.precio = data.precio;
        return m;
    }
}
