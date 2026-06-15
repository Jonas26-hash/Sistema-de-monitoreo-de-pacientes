package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Campania;
import java.util.List;

@Path("/campanias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampaniaResource {

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Campania> listar() {
        return Campania.listAll();
    }

    @GET
    @Path("/activas")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Campania> activas() {
        java.time.LocalDate today = java.time.LocalDate.now();
        return Campania.list("activo = true AND fechaInicio <= ?1 AND fechaFin >= ?1", today);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Campania c = Campania.findById(id);
        if (c == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Campaña no encontrada\"}").build();
        return Response.ok(c).build();
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response crear(@Valid Campania c) {
        c.persist();
        return Response.status(Response.Status.CREATED).entity(c).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response actualizar(@PathParam("id") Long id, @Valid Campania data) {
        Campania c = Campania.findById(id);
        if (c == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Campaña no encontrada\"}").build();
        c.codigo = data.codigo;
        c.nombre = data.nombre;
        c.descripcion = data.descripcion;
        c.descuentoPorcentaje = data.descuentoPorcentaje;
        c.fechaInicio = data.fechaInicio;
        c.fechaFin = data.fechaFin;
        c.activo = data.activo;
        return Response.ok(c).build();
    }

    @PUT
    @Path("/{id}/toggle")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response toggle(@PathParam("id") Long id) {
        Campania c = Campania.findById(id);
        if (c == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Campaña no encontrada\"}").build();
        c.activo = !c.activo;
        return Response.ok(c).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        Campania c = Campania.findById(id);
        if (c == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Campaña no encontrada\"}").build();
        c.delete();
        return Response.noContent().build();
    }
}