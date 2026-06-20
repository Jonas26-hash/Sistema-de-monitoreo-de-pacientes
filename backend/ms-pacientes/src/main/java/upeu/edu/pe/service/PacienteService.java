package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Paciente;
import java.util.List;

@ApplicationScoped
public class PacienteService {

    public List<Paciente> listar() {
        return Paciente.list("activo", true);
    }

    public Paciente buscar(Long id) {
        Paciente p = Paciente.findById(id);
        if (p == null) throw new NotFoundException("Paciente no encontrado");
        return p;
    }

    public Paciente buscarPorDni(String dni) {
        Paciente p = Paciente.find("dni = ?1 and activo = ?2", dni, true).firstResult();
        if (p == null) throw new NotFoundException("Paciente no encontrado");
        return p;
    }

    @Transactional
    public Paciente crear(Paciente data) {
        data.activo = true;
        data.persist();
        return data;
    }

    @Transactional
    public Paciente actualizar(Long id, Paciente data) {
        Paciente p = buscar(id);
        p.nombres = data.nombres;
        p.apellidoPaterno = data.apellidoPaterno;
        p.apellidoMaterno = data.apellidoMaterno;
        p.fechaNacimiento = data.fechaNacimiento;
        p.genero = data.genero;
        p.direccion = data.direccion;
        p.telefono = data.telefono;
        p.email = data.email;
        p.antecedentesFamiliares = data.antecedentesFamiliares;
        p.alergias = data.alergias;
        p.condiciones = data.condiciones;
        p.medicamentosActual = data.medicamentosActual;
        p.nombreSeguro = data.nombreSeguro;
        p.numeroPoliza = data.numeroPoliza;
        p.vigenciaSeguro = data.vigenciaSeguro;
        return p;
    }

    @Transactional
    public void eliminar(Long id) {
        Paciente p = buscar(id);
        p.activo = false;
    }
}
