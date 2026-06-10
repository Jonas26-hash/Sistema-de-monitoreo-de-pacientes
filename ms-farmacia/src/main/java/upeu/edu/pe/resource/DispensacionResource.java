package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Dispensacion;
import upeu.edu.pe.entity.Medicamento;
import java.util.List;

@Path("/dispensaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DispensacionResource {

    @GET
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> listar() {
        return Dispensacion.listAll();
    }

    @GET
    @Path("/receta/{recetaId}")
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public List<Dispensacion> findByReceta(@PathParam("recetaId") Long recetaId) {
        return Dispensacion.list("recetaId = ?1", recetaId);
    }

    @POST
    @Transactional
    @RolesAllowed({"ADMIN", "FARMACEUTICO"})
    public Response crear(@Valid Dispensacion dispensacion) {
        if (dispensacion.medicamentoId != null && dispensacion.cantidad != null) {
            Medicamento med = Medicamento.findById(dispensacion.medicamentoId);
            if (med != null) {
                if (med.stock < dispensacion.cantidad) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Stock insuficiente\"}")
                        .build();
                }
                med.stock -= dispensacion.cantidad;
            }
        }
        dispensacion.persist();
        return Response.status(Response.Status.CREATED).entity(dispensacion).build();
    }
}
