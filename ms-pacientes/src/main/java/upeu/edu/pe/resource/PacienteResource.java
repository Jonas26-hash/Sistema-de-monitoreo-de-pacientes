package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import upeu.edu.pe.Paciente;
import java.util.List;

@Path("/pacientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PacienteResource {

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Paciente> listar() {
        return Paciente.list("activo", true);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Paciente buscar(@PathParam("id") Long id) {
        return Paciente.findById(id);
    }

    @GET
    @Path("/dni/{dni}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Paciente buscarPorDni(@PathParam("dni") String dni) {
        return Paciente.find("dni = ?1 and activo = ?2", dni, true).firstResult();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Paciente crear(Paciente paciente) {
        paciente.activo = true;
        paciente.persist();
        return paciente;
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Paciente actualizar(@PathParam("id") Long id, Paciente pacienteActualizado) {
        Paciente paciente = Paciente.findById(id);
        if (paciente == null) {
            throw new NotFoundException("Paciente no encontrado");
        }
        paciente.nombres = pacienteActualizado.nombres;
        paciente.apellidoPaterno = pacienteActualizado.apellidoPaterno;
        paciente.apellidoMaterno = pacienteActualizado.apellidoMaterno;
        paciente.direccion = pacienteActualizado.direccion;
        paciente.telefono = pacienteActualizado.telefono;
        paciente.email = pacienteActualizado.email;
        paciente.antecedentesFamiliares = pacienteActualizado.antecedentesFamiliares;
        paciente.alergias = pacienteActualizado.alergias;
        paciente.condiciones = pacienteActualizado.condiciones;
        paciente.medicamentosActual = pacienteActualizado.medicamentosActual;
        paciente.nombreSeguro = pacienteActualizado.nombreSeguro;
        paciente.numeroPoliza = pacienteActualizado.numeroPoliza;
        paciente.vigenciaSeguro = pacienteActualizado.vigenciaSeguro;
        return paciente;
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public void eliminar(@PathParam("id") Long id) {
        Paciente paciente = Paciente.findById(id);
        if (paciente != null) {
            paciente.activo = false;
            paciente.persist();
        }
    }
}