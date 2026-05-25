package upeu.edu.pe.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.dto.SagaEvent;
import upeu.edu.pe.service.SagaCompensationService;

@Path("/eventos/saga")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SagaEventReceiver {

    @Inject
    SagaCompensationService compensationService;

    @POST
    @Transactional
    public Response recibirEvento(SagaEvent event) {
        if ("FACTURA_FALLIDA".equals(event.eventType)) {
            compensationService.cancelarCitaPorFacturaFallida(event.payload);
            return Response.ok("{\"status\":\"compensated\"}").build();
        }
        return Response.ok("{\"status\":\"ignored\"}").build();
    }
}
