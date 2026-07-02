package upeu.edu.pe.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class GlobalExceptionMapper {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    @Inject
    ObjectMapper mapper;

    @ServerExceptionMapper
    public Response handleException(Exception e) {
        log.error("Unhandled exception", e);
        if (e instanceof jakarta.ws.rs.WebApplicationException wae) {
            Response original = wae.getResponse();
            String msg = e.getMessage() != null ? e.getMessage() : "Error";
            try {
                String json = mapper.writeValueAsString(Map.of("error", msg, "mensaje", msg, "code", original.getStatus()));
                return Response.fromResponse(original).type(MediaType.APPLICATION_JSON).entity(json).build();
            } catch (JsonProcessingException ex) {
                return Response.fromResponse(original).type(MediaType.APPLICATION_JSON)
                    .entity("{\"error\":\"" + msg + "\",\"mensaje\":\"" + msg + "\",\"code\":" + original.getStatus() + "}").build();
            }
        }
        try {
            String json = mapper.writeValueAsString(Map.of(
                "error", "Error interno del servidor",
                "mensaje", e.getMessage() != null ? e.getMessage() : "Ocurrio un error inesperado",
                "code", 500
            ));
            return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(json)
                .build();
        } catch (JsonProcessingException ex) {
            return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\":\"Error interno del servidor\",\"mensaje\":\"Error inesperado\",\"code\":500}")
                .build();
        }
    }
}
