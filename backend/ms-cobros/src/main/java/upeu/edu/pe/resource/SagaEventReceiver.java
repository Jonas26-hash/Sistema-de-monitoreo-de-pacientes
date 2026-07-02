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
        if (event == null || event.eventType == null || event.payload == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"status\":\"error\",\"message\":\"Invalid event payload\"}")
                .build();
        }
        try {
            if ("CITA_CREADA".equals(event.eventType)) {
                String resultado = sagaService.procesarCitaCreada(event.payload);
                return Response.ok("{\"status\":\"" + resultado + "\"}").build();
            }
            if ("CONSULTA_CREADA".equals(event.eventType)) {
                String resultado = sagaService.procesarConsultaCreada(event.payload);
                return Response.ok("{\"status\":\"" + resultado + "\"}").build();
            }
            return Response.ok("{\"status\":\"ignored\"}").build();
        } catch (Exception e) {
            try {
                String result = sagaService.crearEventoFacturaFallida(event.payload);
                return Response.serverError()
                    .entity("{\"status\":\"compensated\"}")
                    .build();
            } catch (Exception inner) {
                return Response.serverError()
                    .entity("{\"status\":\"compensation_failed\"}")
                    .build();
            }
        }
    }
}
