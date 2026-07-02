package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Medicamento;
import upeu.edu.pe.service.MedicamentoService;
import java.util.List;

@Path("/medicamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MedicamentoResource {

    @Inject
    MedicamentoService service;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "FARMACEUTICO"})
    public List<Medicamento> listar(@QueryParam("search") String search) {
        return service.listar(search);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "FARMACEUTICO"})
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.buscar(id)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/codigo/{codigo}")
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "FARMACEUTICO"})
    public Response buscarPorCodigo(@PathParam("codigo") String codigo) {
        try {
            return Response.ok(service.buscarPorCodigo(codigo)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response crear(@Valid Medicamento medicamento) {
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(medicamento)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response actualizar(@PathParam("id") Long id, @Valid Medicamento med) {
        try {
            return Response.ok(service.actualizar(id, med)).build();
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
