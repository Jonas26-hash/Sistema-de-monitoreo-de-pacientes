package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class RecetaResourceTest {

    @Test
    public void testUnauthenticatedReturns401() {
        given()
            .when().get("/recetas")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetRecetas() {
        given()
            .when().get("/recetas")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateReceta() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[{\\\"nombre\\\":\\\"Paracetamol\\\",\\\"dosis\\\":\\\"500mg\\\",\\\"frecuencia\\\":\\\"cada 8h\\\"}]\",\"indicaciones\":\"Tomar despues de comer\"}")
            .when().post("/recetas")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("pacienteId", equalTo(1));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetReceta() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[{\\\"nombre\\\":\\\"Ibuprofeno\\\",\\\"dosis\\\":\\\"400mg\\\"}]\",\"indicaciones\":\"Cada 12h\"}")
            .when().post("/recetas")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().get("/recetas/" + id)
            .then()
                .statusCode(200)
                .body("id", equalTo(id.intValue()))
                .body("indicaciones", equalTo("Cada 12h"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetRecetasPendientes() {
        given()
            .when().get("/recetas/pendientes")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetRecetasByPaciente() {
        given()
            .when().get("/recetas/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndUpdateReceta() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[]\",\"indicaciones\":\"Inicial\"}")
            .when().post("/recetas")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[]\",\"indicaciones\":\"Actualizado\"}")
            .when().put("/recetas/" + id)
            .then()
                .statusCode(200)
                .body("indicaciones", equalTo("Actualizado"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateRecetaAndGetPendientesPago() {
        String fecha = "2026-06-20";
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaEmision\":\"" + fecha + "\",\"medicamentos\":\"[]\",\"pagado\":false}")
            .when().post("/recetas")
            .then().statusCode(201);

        given()
            .when().get("/recetas/pendientes-pago/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateRecetaAndDelete() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[]\"}")
            .when().post("/recetas")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().delete("/recetas/" + id)
            .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testRecetaRequiresPacienteId() {
        given()
            .contentType("application/json")
            .body("{\"fechaEmision\":\"2026-06-20\",\"medicamentos\":\"[]\"}")
            .when().post("/recetas")
            .then()
                .statusCode(400);
    }
}
