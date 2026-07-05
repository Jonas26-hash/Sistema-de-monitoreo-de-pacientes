package upeu.edu.pe.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.entity.Paciente;
import upeu.edu.pe.service.PacienteService;
import java.util.List;
import java.util.Map;

@Path("/pacientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PacienteResource {

    @Inject
    PacienteService service;

    @GET
    @RolesAllowed({"ADMIN", "DOCTOR", "ATENCION_CLIENTE", "FARMACEUTICO"})
    public List<Paciente> listar(@QueryParam("search") String search) {
        return service.listar(search);
    }

    @GET
    @Path("/{id}")
    public Response buscar(@PathParam("id") Long id) {
        try {
            return Response.ok(service.buscar(id)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/dni/{dni}")
    public Response buscarPorDni(@PathParam("dni") String dni) {
        try {
            return Response.ok(service.buscarPorDni(dni)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response crear(@Valid Paciente paciente) {
        Paciente activo = Paciente.find("dni = ?1 and activo = true", paciente.dni).firstResult();
        if (activo != null) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Ya existe un paciente activo con el DNI " + paciente.dni,
                    "mensaje", "Ya existe un paciente activo con el DNI " + paciente.dni))
                .build();
        }
        return Response.status(Response.Status.CREATED)
            .entity(service.crear(paciente)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response actualizar(@PathParam("id") Long id, @Valid Paciente paciente) {
        try {
            return Response.ok(service.actualizar(id, paciente)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/{id}/solicita-cuenta")
    @RolesAllowed({"ADMIN", "ATENCION_CLIENTE"})
    public Response actualizarSolicitaCuenta(@PathParam("id") Long id, Map<String, Object> body) {
        try {
            Boolean solicitaCuenta = body.get("solicitaCuenta") != null
                ? Boolean.valueOf(body.get("solicitaCuenta").toString())
                : null;
            return Response.ok(service.actualizarSolicitaCuenta(id, solicitaCuenta)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response eliminar(@PathParam("id") Long id) {
        try {
            service.eliminar(id);
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}