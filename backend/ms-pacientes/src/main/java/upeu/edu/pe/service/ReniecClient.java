package upeu.edu.pe.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import upeu.edu.pe.dto.PeruApiResponse;

@RegisterRestClient(configKey = "reniec-api")
public interface ReniecClient {

    @GET
    @Path("/api/dni/{dni}")
    @Produces(MediaType.APPLICATION_JSON)
    PeruApiResponse consultarPorDni(@PathParam("dni") String dni, @QueryParam("api_token") String apiToken);
}
