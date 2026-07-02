package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Paciente;
import java.util.List;

@ApplicationScoped
public class PacienteService {

    public List<Paciente> listar(String search) {
        if (search == null || search.isBlank()) {
            return Paciente.list("activo", true);
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return Paciente.list("activo = true AND (LOWER(nombres) LIKE ?1 OR LOWER(apellidoPaterno) LIKE ?1 OR LOWER(dni) LIKE ?1)", pattern);
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
        Paciente existente = Paciente.find("dni = ?1 and activo = false", data.dni).firstResult();
        if (existente != null) {
            existente.activo = true;
            existente.nombres = data.nombres;
            existente.apellidoPaterno = data.apellidoPaterno;
            existente.apellidoMaterno = data.apellidoMaterno;
            existente.fechaNacimiento = data.fechaNacimiento;
            existente.genero = data.genero;
            existente.direccion = data.direccion;
            existente.telefono = data.telefono;
            existente.email = data.email;
            existente.antecedentesFamiliares = data.antecedentesFamiliares;
            existente.alergias = data.alergias;
            existente.condiciones = data.condiciones;
            existente.medicamentosActual = data.medicamentosActual;
            existente.nombreSeguro = data.nombreSeguro;
            existente.numeroPoliza = data.numeroPoliza;
            existente.vigenciaSeguro = data.vigenciaSeguro;
            existente.solicitaCuenta = data.solicitaCuenta;
            return existente;
        }
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
        p.solicitaCuenta = data.solicitaCuenta;
        return p;
    }

    @Transactional
    public void eliminar(Long id) {
        Paciente.deleteById(id);
    }

    @Transactional
    public Paciente actualizarSolicitaCuenta(Long id, Boolean solicitaCuenta) {
        Paciente p = buscar(id);
        p.solicitaCuenta = solicitaCuenta;
        return p;
    }
}
