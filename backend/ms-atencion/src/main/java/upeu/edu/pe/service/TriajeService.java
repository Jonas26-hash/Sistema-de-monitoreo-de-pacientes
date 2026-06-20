package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Triaje;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class TriajeService {

    public List<Triaje> listar() {
        return Triaje.listAll();
    }

    public List<Triaje> findByPaciente(Long pacienteId) {
        return Triaje.findByPaciente(pacienteId);
    }

    public List<Triaje> findByCita(Long citaId) {
        return Triaje.findByCita(citaId);
    }

    public Triaje buscar(Long id) {
        Triaje t = Triaje.findById(id);
        if (t == null) throw new NotFoundException("Triaje no encontrado");
        return t;
    }

    @Transactional
    public Triaje crear(Triaje triaje) {
        if (triaje.fechaTriaje == null) {
            triaje.fechaTriaje = LocalDateTime.now();
        }
        triaje.persist();
        return triaje;
    }

    @Transactional
    public Triaje actualizar(Long id, Triaje data) {
        Triaje t = buscar(id);
        t.peso = data.peso;
        t.talla = data.talla;
        t.presionSistolica = data.presionSistolica;
        t.presionDiastolica = data.presionDiastolica;
        t.temperatura = data.temperatura;
        t.frecuenciaCardiaca = data.frecuenciaCardiaca;
        t.spo2 = data.spo2;
        t.frecuenciaRespiratoria = data.frecuenciaRespiratoria;
        t.motivoConsulta = data.motivoConsulta;
        t.observaciones = data.observaciones;
        return t;
    }
}
