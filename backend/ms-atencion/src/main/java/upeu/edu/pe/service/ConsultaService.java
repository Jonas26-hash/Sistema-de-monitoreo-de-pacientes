package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Consulta;
import java.util.List;

@ApplicationScoped
public class ConsultaService {

    public List<Consulta> listar() {
        return Consulta.listAll();
    }

    public List<Consulta> findByPaciente(Long pacienteId) {
        return Consulta.findByPaciente(pacienteId);
    }

    public Consulta buscar(Long id) {
        Consulta c = Consulta.findById(id);
        if (c == null) throw new NotFoundException("Consulta no encontrada");
        return c;
    }

    @Transactional
    public Consulta crear(Consulta consulta) {
        consulta.persist();
        return consulta;
    }
}
