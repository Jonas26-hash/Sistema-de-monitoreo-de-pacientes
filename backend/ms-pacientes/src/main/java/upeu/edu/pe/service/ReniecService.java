package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import upeu.edu.pe.dto.ReniecResponse;
import java.util.Optional;

@ApplicationScoped
public class ReniecService {

    @Inject
    @RestClient
    ReniecClient client;

    public Optional<ReniecResponse> consultar(String dni) {
        try {
            ReniecResponse res = client.consultarPorDni(dni);
            return Optional.ofNullable(res);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
