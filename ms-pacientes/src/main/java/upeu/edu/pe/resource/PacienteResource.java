package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.Paciente;
import java.util.List;
import java.util.stream.Collectors;

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
    public Response crear(@Valid Paciente paciente) {
        paciente.activo = true;
        paciente.persist();
        return Response.status(Response.Status.CREATED).entity(paciente).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response actualizar(@PathParam("id") Long id, @Valid Paciente pacienteActualizado) {
        Paciente paciente = Paciente.findById(id);
        if (paciente == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Paciente no encontrado").build();
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
        return Response.ok(paciente).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        Paciente paciente = Paciente.findById(id);
        if (paciente != null) {
            paciente.activo = false;
            paciente.persist();
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Paciente no encontrado").build();
    }
}