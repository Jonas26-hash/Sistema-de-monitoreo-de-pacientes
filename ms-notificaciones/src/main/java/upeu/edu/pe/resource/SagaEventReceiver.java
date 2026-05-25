package upeu.edu.pe.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import upeu.edu.pe.dto.SagaEvent;
import upeu.edu.pe.service.SagaService;

@Path("/eventos/saga")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SagaEventReceiver {

    @Inject
    SagaService sagaService;

    @POST
    public Response recibirEvento(SagaEvent event) {
        try {
            if ("FACTURA_CREADA".equals(event.eventType)) {
                sagaService.procesarFacturaCreada(event.payload);
                return Response.ok("{\"status\":\"notified\"}").build();
            }
            return Response.ok("{\"status\":\"ignored\"}").build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("{\"error\":\"" + e.getMessage() + "\"}")
                .build();
        }
    }
}
