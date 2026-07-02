package upeu.edu.pe;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;

@QuarkusTest
public class AtencionResourceTest {

    @Test
    public void testUnauthenticatedReturns401() {
        given()
            .when().get("/citas")
            .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetCitas() {
        given()
            .when().get("/citas")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetDoctoresOcupados() {
        given()
            .queryParam("fechaHora", "2026-06-20T10:00:00")
            .when().get("/citas/doctores-ocupados")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateCita() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaHora\":\"2026-06-20T10:00:00\",\"estado\":\"PENDIENTE\",\"motivo\":\"Control general\"}")
            .when().post("/citas")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("motivo", equalTo("Control general"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndGetCita() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaHora\":\"2026-06-21T11:00:00\",\"estado\":\"PENDIENTE\",\"motivo\":\"Revision\"}")
            .when().post("/citas")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .when().get("/citas/" + id)
            .then()
                .statusCode(200)
                .body("motivo", equalTo("Revision"))
                .body("estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetTriajes() {
        given()
            .when().get("/triajes")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateTriaje() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"fechaTriaje\":\"2026-06-20T10:00:00\",\"peso\":70.5,\"talla\":1.75,\"presionSistolica\":120,\"presionDiastolica\":80,\"temperatura\":36.5,\"frecuenciaCardiaca\":72,\"spo2\":98.0}")
            .when().post("/triajes")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("peso", equalTo(70.5f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndUpdateTriaje() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"fechaTriaje\":\"2026-06-20T12:00:00\",\"peso\":65.0}")
            .when().post("/triajes")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"peso\":66.0,\"temperatura\":37.0}")
            .when().put("/triajes/" + id)
            .then()
                .statusCode(200)
                .body("peso", equalTo(66.0f));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetConsultas() {
        given()
            .when().get("/consultas")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateConsulta() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"doctorId\":1,\"fechaConsulta\":\"2026-06-20T14:00:00\",\"sintomas\":\"Dolor de cabeza\",\"diagnostico\":\"Migraña\"}")
            .when().post("/consultas")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("diagnostico", equalTo("Migraña"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetOrdenesExamen() {
        given()
            .when().get("/ordenes-examen")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateOrdenExamen() {
        given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"tipo\":\"Analisis de sangre\",\"estado\":\"PENDIENTE\",\"fechaOrden\":\"2026-06-20T15:00:00\",\"costo\":50.00}")
            .when().post("/ordenes-examen")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tipo", equalTo("Analisis de sangre"))
                .body("estado", equalTo("PENDIENTE"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testCreateAndUpdateOrdenExamenResultado() {
        Integer idInt = given()
            .contentType("application/json")
            .body("{\"pacienteId\":1,\"tipo\":\"Rayos X\",\"estado\":\"PENDIENTE\",\"fechaOrden\":\"2026-06-20T16:00:00\"}")
            .when().post("/ordenes-examen")
            .then().statusCode(201).extract().path("id");
        Long id = idInt.longValue();

        given()
            .contentType("application/json")
            .body("{\"resultado\":\"Normal sin hallazgos\",\"estado\":\"COMPLETADO\",\"tipo\":\"Rayos X\"}")
            .when().put("/ordenes-examen/" + id)
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetTriajeByPaciente() {
        given()
            .when().get("/triajes/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetConsultasByPaciente() {
        given()
            .when().get("/consultas/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetOrdenesExamenByPaciente() {
        given()
            .when().get("/ordenes-examen/paciente/1")
            .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"ADMIN"})
    public void testGetCitasByPaciente() {
        given()
            .when().get("/citas/paciente/1")
            .then()
                .statusCode(200);
    }
}
