package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PacienteResourceTest {

    @Test
    public void testLoginEndpoint() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"\",\"password\":\"\"}")
            .when().post("/auth/login")
            .then()
                .statusCode(401);
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
    public void testHealthEndpoint() {
        given()
            .when().get("/pacientes")
            .then()
                .statusCode(401);
    }
}
