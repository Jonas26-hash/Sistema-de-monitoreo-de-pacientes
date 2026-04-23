package upeu.edu.pe.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Medicamento;
import java.util.List;

@Path("/medicamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MedicamentoResource {

    @GET
    public List<Medicamento> listar() {
        return Medicamento.listAll();
    }

    @GET
    @Path("/{id}")
    public Medicamento buscar(@PathParam("id") Long id) {
        return Medicamento.findById(id);
    }

    @GET
    @Path("/codigo/{codigo}")
    public Medicamento buscarPorCodigo(@PathParam("codigo") String codigo) {
        return Medicamento.find("codigo", codigo).firstResult();
    }

    @POST
    public Response crear(Medicamento medicamento) {
        medicamento.persist();
        return Response.status(Response.Status.CREATED).entity(medicamento).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizar(@PathParam("id") Long id, Medicamento med) {
        Medicamento m = Medicamento.findById(id);
        if (m == null) return Response.status(Response.Status.NOT_FOUND).build();
        m.nombre = med.nombre;
        m.descripcion = med.descripcion;
        m.stock = med.stock;
        m.stockMinimo = med.stockMinimo;
        return Response.ok(m).build();
    }
}