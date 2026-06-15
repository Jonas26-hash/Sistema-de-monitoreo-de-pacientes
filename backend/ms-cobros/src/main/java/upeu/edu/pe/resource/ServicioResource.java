package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Servicio;
import java.util.List;

@Path("/servicios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicioResource {

    @GET
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Servicio> listar() {
        return Servicio.list("activo", true);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response buscar(@PathParam("id") Long id) {
        Servicio s = Servicio.findById(id);
        if (s == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Servicio no encontrado\"}").build();
        return Response.ok(s).build();
    }

    @GET
    @Path("/tipo/{tipo}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public List<Servicio> porTipo(@PathParam("tipo") String tipo) {
        return Servicio.list("tipo = ?1 AND activo = true", tipo);
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response crear(@Valid Servicio s) {
        s.persist();
        return Response.status(Response.Status.CREATED).entity(s).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response actualizar(@PathParam("id") Long id, @Valid Servicio data) {
        Servicio s = Servicio.findById(id);
        if (s == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Servicio no encontrado\"}").build();
        s.codigo = data.codigo;
        s.nombre = data.nombre;
        s.tipo = data.tipo;
        s.precio = data.precio;
        s.activo = data.activo;
        return Response.ok(s).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        Servicio s = Servicio.findById(id);
        if (s == null) return Response.status(Response.Status.NOT_FOUND)
            .entity("{\"error\":\"Servicio no encontrado\"}").build();
        s.activo = false;
        return Response.noContent().build();
    }
}
