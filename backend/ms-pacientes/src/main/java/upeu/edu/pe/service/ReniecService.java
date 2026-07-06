package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import upeu.edu.pe.dto.PeruApiResponse;
import upeu.edu.pe.dto.ReniecResponse;
import java.util.Optional;

@ApplicationScoped
public class ReniecService {

    @Inject
    @RestClient
    ReniecClient client;

    @Inject
    @ConfigProperty(name = "reniec.api.token")
    String apiToken;

    public Optional<ReniecResponse> consultar(String dni) {
        try {
            PeruApiResponse res = client.consultarPorDni(dni, apiToken);
            if (res == null || res.nombres == null || res.apellido_paterno == null) {
                return Optional.empty();
            }
            ReniecResponse r = new ReniecResponse();
            r.names = res.nombres;
            r.paternalLastName = res.apellido_paterno;
            r.maternalLastName = res.apellido_materno;
            r.fullName = res.cliente;
            r.documentID = res.dni;
            r.surnames = (res.apellido_paterno != null ? res.apellido_paterno : "")
                + " " + (res.apellido_materno != null ? res.apellido_materno : "");
            return Optional.of(r);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
