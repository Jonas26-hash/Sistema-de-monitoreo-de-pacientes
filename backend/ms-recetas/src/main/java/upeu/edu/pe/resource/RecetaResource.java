package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Receta;
import java.time.LocalDate;
import java.util.List;

@Path("/recetas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecetaResource {

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public List<Receta> listar() {
        return Receta.listAll();
    }

    @GET
    @Path("/pendientes")
    @RolesAllowed({"ADMIN", "FARMACEUTICO", "ATENCION_CLIENTE"})
    public List<Receta> pendientes() {
        return Receta.list("dispensada = ?1", false);
    }

    @GET
    @Path("/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public List<Receta> findByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Receta.list("pacienteId = ?1", pacienteId);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "PACIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Receta receta = Receta.findById(id);
        if (receta == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Receta no encontrada\"}").build();
        }
        return Response.ok(receta).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response crear(@Valid Receta receta) {
        receta.persist();
        return Response.status(Response.Status.CREATED).entity(receta).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE"})
    public Response actualizar(@PathParam("id") Long id, @Valid Receta receta) {
        Receta entity = Receta.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Receta no encontrada\"}").build();
        }
        entity.consultaId = receta.consultaId;
        entity.pacienteId = receta.pacienteId;
        entity.doctorId = receta.doctorId;
        entity.fechaEmision = receta.fechaEmision;
        entity.fechaVigencia = receta.fechaVigencia;
        entity.medicamentos = receta.medicamentos;
        entity.indicaciones = receta.indicaciones;
        entity.dispensada = receta.dispensada;
        entity.fechaDispensacion = receta.fechaDispensacion;
        entity.pagado = receta.pagado;
        return Response.ok(entity).build();
    }

    @PUT
    @Path("/{id}/dispensar")
    @Transactional
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response dispensar(@PathParam("id") Long id) {
        Receta entity = Receta.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Receta no encontrada\"}").build();
        }
        entity.dispensada = true;
        entity.fechaDispensacion = LocalDate.now();
        return Response.ok(entity).build();
    }

    @PUT
    @Path("/{id}/pagar")
    @Transactional
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response pagar(@PathParam("id") Long id) {
        Receta entity = Receta.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Receta no encontrada\"}").build();
        }
        entity.pagado = true;
        return Response.ok(entity).build();
    }

    @GET
    @Path("/pendientes-pago/paciente/{pacienteId}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Receta> pendientesPagoByPaciente(@PathParam("pacienteId") Long pacienteId) {
        return Receta.list("pacienteId = ?1 AND (pagado IS NULL OR pagado = false) AND dispensada = ?2", pacienteId, false);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        boolean deleted = Receta.deleteById(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Receta no encontrada\"}").build();
        }
        return Response.noContent().build();
    }
}
