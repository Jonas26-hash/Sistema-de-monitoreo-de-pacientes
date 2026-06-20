package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Medicamento;
import java.util.List;

@ApplicationScoped
public class MedicamentoService {

    public List<Medicamento> listar() {
        return Medicamento.listAll();
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
