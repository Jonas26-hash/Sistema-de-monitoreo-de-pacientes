package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class FarmaciaResourceTest {

    @Test
    public void testUnauthenticatedReturns401() {
        given()
            .when().get("/medicamentos")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetMedicamentos() {
        given()
            .when().get("/medicamentos")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateMedicamento() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"PARA500\",\"nombre\":\"Paracetamol 500mg\",\"presentacion\":\"Tableta\",\"stock\":100,\"stockMinimo\":10,\"precio\":5.00}")
            .when().post("/medicamentos")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("codigo", equalTo("PARA500"))
                .body("nombre", equalTo("Paracetamol 500mg"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetByCodigo() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"IBU400\",\"nombre\":\"Ibuprofeno 400mg\",\"stock\":50,\"precio\":8.00}")
            .when().post("/medicamentos")
            .then().statusCode(201);

        given()
            .when().get("/medicamentos/codigo/IBU400")
            .then()
                .statusCode(200)
                .body("codigo", equalTo("IBU400"))
                .body("precio", equalTo(8.00f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetMedicamentoById() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"AMOX500\",\"nombre\":\"Amoxicilina 500mg\",\"stock\":30}")
            .when().post("/medicamentos")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().get("/medicamentos/" + id)
            .then()
                .statusCode(200)
                .body("nombre", equalTo("Amoxicilina 500mg"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndUpdateMedicamento() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"AZITRO\",\"nombre\":\"Azitromicina\",\"stock\":20,\"precio\":25.00}")
            .when().post("/medicamentos")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"codigo\":\"AZITRO\",\"nombre\":\"Azitromicina\",\"precio\":30.00,\"stock\":25}")
            .when().put("/medicamentos/" + id)
            .then()
                .statusCode(200)
                .body("precio", equalTo(30.00f))
                .body("stock", equalTo(25));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetDispensaciones() {
        given()
            .when().get("/dispensaciones")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateDispensacion() {
        Integer medIdInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"DIPIRONA\",\"nombre\":\"Dipirona\",\"stock\":50}")
            .when().post("/medicamentos")
            .then().statusCode(201).extract().path("id");
        Long medId = medIdInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"recetaId\":1,\"medicamentoId\":" + medId + ",\"cantidad\":10,\"fechaDispensacion\":\"2026-06-20\"}")
            .when().post("/dispensaciones")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cantidad", equalTo(10));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetDispensacionesByReceta() {
        Integer medIdInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"CLORFEN\",\"nombre\":\"Clorfenamina\",\"stock\":40}")
            .when().post("/medicamentos")
            .then().statusCode(201).extract().path("id");
        Long medId = medIdInt.longValue();

        Integer dispIdInt = given()
            .contentType("application/json")
            .body("{\"recetaId\":2,\"medicamentoId\":" + medId + ",\"cantidad\":5,\"fechaDispensacion\":\"2026-06-20\"}")
            .when().post("/dispensaciones")
            .then().statusCode(201).extract().path("id");
        Long dispId = dispIdInt.longValue();

        given()
            .when().get("/dispensaciones/receta/2")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "farmaceutico", roles = {"FARMACEUTICO"})
    public void testCreateMedicamentoAsFarmaceutico() {
        given()
            .contentType("application/json")
            .body("{\"codigo\":\"OMEPRAZOL\",\"nombre\":\"Omeprazol 20mg\",\"stock\":60,\"precio\":12.00}")
            .when().post("/medicamentos")
            .then()
                .statusCode(201);
    }

    @Test
    @TestSecurity(user = "doctor", roles = {"DOCTOR"})
    public void testDoctorCanOnlyReadMedicamentos() {
        given()
            .when().get("/medicamentos")
            .then()
                .statusCode(200);

        given()
            .contentType("application/json")
            .body("{\"codigo\":\"TESTDOC\",\"nombre\":\"Test\",\"stock\":1}")
            .when().post("/medicamentos")
            .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateDispensacionWithInvalidCantidad() {
        Integer medIdInt = given()
            .contentType("application/json")
            .body("{\"codigo\":\"VALIDADO\",\"nombre\":\"Validado\",\"stock\":5}")
            .when().post("/medicamentos")
            .then().statusCode(201).extract().path("id");
        Long medId = medIdInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"recetaId\":1,\"medicamentoId\":" + medId + ",\"cantidad\":0,\"fechaDispensacion\":\"2026-06-20\"}")
            .when().post("/dispensaciones")
            .then()
                .statusCode(400);
    }
}
