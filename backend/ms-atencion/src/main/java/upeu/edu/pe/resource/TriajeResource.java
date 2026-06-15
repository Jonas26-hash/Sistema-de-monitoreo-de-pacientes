package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Triaje;
import java.time.LocalDateTime;
import java.util.List;

@Path("/triajes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TriajeResource {

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE"})
    public List<Triaje> listar() {
        return Triaje.listAll();
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Triaje> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Triaje.findByPaciente(pacienteId);
    }

    @GET
    @Path("/cita/{citaId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE"})
    public List<Triaje> findByCita(@PathParam("citaId") Long citaId) {
        return Triaje.findByCita(citaId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ENFERMERO", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Triaje triaje = Triaje.findById(id);
        if (triaje == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Triaje no encontrado\"}").build();
        }
        return Response.ok(triaje).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "ENFERMERO"})
    public Response crear(@Valid Triaje triaje) {
        if (triaje.fechaTriaje == null) {
            triaje.fechaTriaje = LocalDateTime.now();
        }
        triaje.persist();
        return Response.status(Response.Status.CREATED).entity(triaje).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "ENFERMERO"})
    public Response actualizar(@PathParam("id") Long id, @Valid Triaje triaje) {
        Triaje existing = Triaje.findById(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Triaje no encontrado\"}").build();
        }
        existing.peso = triaje.peso;
        existing.talla = triaje.talla;
        existing.presionSistolica = triaje.presionSistolica;
        existing.presionDiastolica = triaje.presionDiastolica;
        existing.temperatura = triaje.temperatura;
        existing.frecuenciaCardiaca = triaje.frecuenciaCardiaca;
        existing.spo2 = triaje.spo2;
        existing.frecuenciaRespiratoria = triaje.frecuenciaRespiratoria;
        existing.motivoConsulta = triaje.motivoConsulta;
        existing.observaciones = triaje.observaciones;
        existing.persist();
        return Response.ok(existing).build();
    }
}
