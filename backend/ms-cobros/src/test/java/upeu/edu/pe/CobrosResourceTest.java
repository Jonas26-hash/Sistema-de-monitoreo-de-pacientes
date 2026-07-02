package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class CobrosResourceTest {

    @Test
    public void testUnauthenticatedReturns401() {
        given()
            .when().get("/cobros")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetCobros() {
        given()
            .when().get("/cobros")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetDeudasByPaciente() {
        given()
            .when().get("/cobros/deudas/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetServicios() {
        given()
            .when().get("/servicios")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateServicio() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"CON001\",\"nombre\":\"Consulta General\",\"tipo\":\"CONSULTA\",\"precio\":50.00}")
            .when().post("/servicios")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("codigo", equalTo("CON001"))
                .body("precio", equalTo(50.00f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetServiciosByTipo() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"LAB001\",\"nombre\":\"Analisis Sangre\",\"tipo\":\"LABORATORIO\",\"precio\":80.00}")
            .when().post("/servicios")
            .then().statusCode(201);

        given()
            .when().get("/servicios/tipo/LABORATORIO")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndUpdateServicio() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"IMG001\",\"nombre\":\"Resonancia\",\"tipo\":\"IMAGEN\",\"precio\":200.00}")
            .when().post("/servicios")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"codigo\":\"IMG001\",\"nombre\":\"Resonancia\",\"tipo\":\"IMAGEN\",\"precio\":250.00}")
            .when().put("/servicios/" + id)
            .then()
                .statusCode(200)
                .body("precio", equalTo(250.00f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetCampanias() {
        given()
            .when().get("/campanias")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetCampaniasActivas() {
        given()
            .when().get("/campanias/activas")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateCampania() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"DESC10\",\"nombre\":\"Descuento 10%%\",\"descuentoPorcentaje\":10.0,\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\"}")
            .when().post("/campanias")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("codigo", equalTo("DESC10"))
                .body("descuentoPorcentaje", equalTo(10));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndToggleCampania() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"DESC20\",\"nombre\":\"Descuento 20%%\",\"descuentoPorcentaje\":20.0,\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\",\"activo\":true}")
            .when().post("/campanias")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().put("/campanias/" + id + "/toggle")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateCobro() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"monto\":150.00,\"tipoComprobante\":\"BOLETA\",\"descripcion\":\"Pago consulta\"}")
            .when().post("/cobros")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("monto", equalTo(150.00f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetCobro() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"monto\":75.50,\"descripcion\":\"Pago examen\"}")
            .when().post("/cobros")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().get("/cobros/" + id)
            .then()
                .statusCode(200)
                .body("monto", equalTo(75.50f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testDeleteServicio() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"DEL001\",\"nombre\":\"Temp Service\",\"tipo\":\"OTRO\",\"precio\":10.00}")
            .when().post("/servicios")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().delete("/servicios/" + id)
            .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testDeleteCampania() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"DELCAMP\",\"nombre\":\"Camp Temp\",\"descuentoPorcentaje\":5.0,\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\"}")
            .when().post("/campanias")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().delete("/campanias/" + id)
            .then()
                .statusCode(204);
    }
}
