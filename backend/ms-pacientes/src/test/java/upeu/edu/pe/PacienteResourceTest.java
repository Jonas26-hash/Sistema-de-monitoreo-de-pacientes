package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class PacienteResourceTest {

    @Test
    public void testLoginEmptyCredentials() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"\",\"password\":\"\"}")
            .when().post("/auth/login")
            .then()
                .statusCode(400);
    }

    @Test
    public void testLoginInvalidCredentials() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"nonexistent\",\"password\":\"wrongpass\"}")
            .when().post("/auth/login")
            .then()
                .statusCode(401)
                .body("error", containsString("Credenciales"));
    }

    @Test
    public void testLoginSuccess() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
            .when().post("/auth/login")
            .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("username", equalTo("admin"))
                .body("roles", hasItem("ADMIN"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetPacientes() {
        given()
            .when().get("/pacientes")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreatePaciente() {
        given()
            .contentType("application/json")
            .body("{\"nombres\":\"Juan\",\"apellidoPaterno\":\"Perez\",\"dni\":\"12345678\",\"telefono\":\"+51999888777\",\"email\":\"juan@test.com\"}")
            .when().post("/pacientes")
            .then()
                .statusCode(201)
                .body("nombres", equalTo("Juan"))
                .body("dni", equalTo("12345678"))
                .body("id", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetPacienteByDni() {
        String dni = "87654321";
        given()
            .contentType("application/json")
            .body("{\"nombres\":\"Maria\",\"apellidoPaterno\":\"Lopez\",\"dni\":\"" + dni + "\"}")
            .when().post("/pacientes")
            .then().statusCode(201);

        given()
            .when().get("/pacientes/dni/" + dni)
            .then()
                .statusCode(200)
                .body("dni", equalTo(dni));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetUsuarios() {
        given()
            .when().get("/auth/usuarios")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetUsuariosByRol() {
        given()
            .when().get("/auth/usuarios/rol/ADMIN")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetProfile() {
        given()
            .when().get("/auth/profile")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testUpdateProfile() {
        given()
            .contentType("application/json")
            .body("{\"nombres\":\"Admin Actualizado\",\"apellidos\":\"Sistema\"}")
            .when().put("/auth/profile")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetAudit() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when().get("/audit")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetConfig() {
        given()
            .when().get("/auth/config")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetPacienteById() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"nombres\":\"Carlos\",\"apellidoPaterno\":\"Garcia\",\"dni\":\"99999999\"}")
            .when().post("/pacientes")
            .then().statusCode(201).extract().path("id");
    Long id = idInt.longValue();

        given()
            .when().get("/pacientes/" + id)
            .then()
                .statusCode(200)
                .body("nombres", equalTo("Carlos"))
                .body("apellidoPaterno", equalTo("Garcia"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testChangePassword() {
        given()
            .contentType("application/json")
            .body("{\"oldPassword\":\"admin123\",\"newPassword\":\"newpass123\"}")
            .when().put("/auth/change-password")
            .then()
                .statusCode(200)
                .body("mensaje", containsString("actualizada exitosamente"));
    }

    @Test
    public void testHealthCheckUnauthenticated() {
        given()
            .when().get("/pacientes")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "atencion", roles = {"ATENCION_CLIENTE"})
    public void testCreatePacienteAsAtencion() {
        given()
            .contentType("application/json")
            .body("{\"nombres\":\"Ana\",\"apellidoPaterno\":\"Torres\",\"dni\":\"11223344\"}")
            .when().post("/pacientes")
            .then()
                .statusCode(201);
    }

    @Test
    @TestSecurity(user = "paciente1", roles = {"PACIENTE"})
    public void testPacienteRoleCannotListAllPacientes() {
        given()
            .when().get("/pacientes")
            .then()
                .statusCode(403);
    }
}
