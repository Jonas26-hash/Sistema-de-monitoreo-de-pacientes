package upeu.edu.pe.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import upeu.edu.pe.dto.ReniecResponse;

@RegisterRestClient(configKey = "reniec-api")
public interface ReniecClient {

    @GET
    @Path("/api/query/{dni}")
    @Produces(MediaType.APPLICATION_JSON)
    ReniecResponse consultarPorDni(@PathParam("dni") String dni);
}
