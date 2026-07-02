package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class NotificacionResourceTest {

    @Test
    public void testUnauthenticatedReturns401() {
        given()
            .when().get("/notificaciones")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetNotificaciones() {
        given()
            .when().get("/notificaciones")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateNotificacion() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"tipo\":\"RECORDATORIO\",\"mensaje\":\"Su cita es mañana a las 10:00\",\"canal\":\"CORREO\"}")
            .when().post("/notificaciones")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tipo", equalTo("RECORDATORIO"))
                .body("mensaje", equalTo("Su cita es mañana a las 10:00"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetNotificacionesByPaciente() {
        given()
            .when().get("/notificaciones/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetNotificacion() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"tipo\":\"ALERTA\",\"mensaje\":\"Resultados de examen disponibles\"}")
            .when().post("/notificaciones")
            .then().statusCode(201)
                .body("tipo", equalTo("ALERTA"))
                .body("mensaje", equalTo("Resultados de examen disponibles"));
    }

    @Test
    public void testEnviarCorreoUnauthenticated() {
        given()
            .contentType("application/json")
            .body("{\"to\":\"test@clinica.com\",\"codigo\":\"123456\",\"nombres\":\"Test\",\"apellidos\":\"User\",\"esStaff\":false}")
            .when().post("/notificaciones/enviar-correo")
            .then()
                .statusCode(200)
                .body("mensaje", containsString("exitosamente"));
    }

    @Test
    public void testEnviarCorreoPersonalizadoUnauthenticated() {
        given()
            .contentType("application/json")
            .body("{\"to\":\"test@clinica.com\",\"asunto\":\"Bienvenido\",\"mensaje\":\"Gracias por registrarse\"}")
            .when().post("/notificaciones/enviar-correo-personalizado")
            .then()
                .statusCode(200)
                .body("mensaje", containsString("exitosamente"));
    }

    @Test
    @TestSecurity(user = "paciente1", roles = {"PACIENTE"})
    public void testPacienteCanReadOwnNotificaciones() {
        given()
            .when().get("/notificaciones/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateMultipleNotificaciones() {
        for (int i = 0; i < 3; i++) {
            given()
                .contentType("application/json")
                .body("{\"pacienteId\":1,\"tipo\":\"GENERAL\",\"mensaje\":\"Notificacion " + i + "\"}")
                .when().post("/notificaciones")
                .then().statusCode(201);
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateNotificacionSinPacienteId() {
        given()
            .contentType("application/json")
            .body("{\"tipo\":\"GENERAL\",\"mensaje\":\"Sin paciente\"}")
            .when().post("/notificaciones")
            .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testNotificacionRequiresMensaje() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"tipo\":\"GENERAL\"}")
            .when().post("/notificaciones")
            .then()
                .statusCode(400);
    }
}
