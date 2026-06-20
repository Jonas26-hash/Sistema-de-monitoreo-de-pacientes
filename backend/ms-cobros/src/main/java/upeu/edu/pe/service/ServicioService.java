package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import upeu.edu.pe.entity.Servicio;
import java.util.List;

@ApplicationScoped
public class ServicioService {

    public List<Servicio> listar() {
        return Servicio.list("activo", true);
    }

    public Servicio buscar(Long id) {
        Servicio s = Servicio.findById(id);
        if (s == null) throw new NotFoundException("Servicio no encontrado");
        return s;
    }

    public List<Servicio> porTipo(String tipo) {
        return Servicio.list("tipo = ?1 AND activo = true", tipo);
    }

    @Transactional
    public Servicio crear(Servicio s) {
        s.persist();
        return s;
    }

    @Transactional
    public Servicio actualizar(Long id, Servicio data) {
        Servicio s = buscar(id);
        s.codigo = data.codigo;
        s.nombre = data.nombre;
        s.tipo = data.tipo;
        s.precio = data.precio;
        s.activo = data.activo;
        return s;
    }

    @Transactional
    public void eliminar(Long id) {
        Servicio s = buscar(id);
        s.activo = false;
    }
}
