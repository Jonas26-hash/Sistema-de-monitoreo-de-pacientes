package upeu.edu.pe.seed;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mindrot.jbcrypt.BCrypt;
import upeu.edu.pe.entity.Paciente;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;
import java.time.LocalDate;

@ApplicationScoped
public class DataSeeder {

    @Inject
    @ConfigProperty(name = "quarkus.datasource.db-kind", defaultValue = "postgresql")
    String dbKind;

    void migrarCajeros() {
        EntityManager em = Usuario.getEntityManager();
        var ids = em.createNativeQuery("SELECT usuario_id FROM usuario_roles WHERE rol = 'CAJERO'").getResultList();
        for (var id : ids) {
            Long userId = ((Number) id).longValue();
            em.createNativeQuery("DELETE FROM usuario_roles WHERE usuario_id = ?1").setParameter(1, userId).executeUpdate();
            em.createNativeQuery("DELETE FROM usuarios WHERE id = ?1").setParameter(1, userId).executeUpdate();
        }
        if (!ids.isEmpty()) {
            System.out.println("[DataSeeder] Migración: " + ids.size() + " usuario(s) CAJERO eliminado(s)");
        }
    }

    private Paciente crearPaciente(String nombres, String apellidoPaterno, String apellidoMaterno, String dni, String email, String telefono) {
        Paciente p = new Paciente();
        p.nombres = nombres;
        p.apellidoPaterno = apellidoPaterno;
        p.apellidoMaterno = apellidoMaterno;
        p.dni = dni;
        p.email = email;
        p.telefono = telefono;
        p.activo = true;
        p.persist();
        return p;
    }

    private void crearUsuario(String username, String password, String email, Rol rol, Paciente paciente, String nombres, String apellidos) {
        Usuario u = new Usuario();
        u.username = username;
        u.password = BCrypt.hashpw(password, BCrypt.gensalt());
        u.email = email;
        u.roles = java.util.List.of(rol);
        u.paciente = paciente;
        u.activo = true;
        u.nombres = nombres;
        u.apellidos = apellidos;
        u.persist();
    }

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (!"h2".equals(dbKind)) {
            migrarCajeros();
        }

        if (Usuario.count() > 0) return;

        System.out.println("[DataSeeder] Sembrando datos iniciales...");

        Paciente pPedro = crearPaciente("Pedro", "Ramirez", "", "87654321", "pedro@correo.com", "+51999000001");
        Paciente pHomero = crearPaciente("Homero", "Simpson", "Bouvier", "12345678", "chavezjonas500@gmail.com", "+51999000002");
        Paciente pMaria = crearPaciente("Maria", "Lopez", "Garcia", "23456789", "maria@correo.com", "+51999000003");
        Paciente pJuan = crearPaciente("Juan", "Rodriguez", "Perez", "34567890", "juan@correo.com", "+51999000004");
        crearPaciente("Lisa", "Simpson", "Bouvier", "45678901", "lisa@correo.com", "+51999000005");
        crearPaciente("Ned", "Flanders", "Van Houten", "56789012", "ned@correo.com", "+51999000006");
        crearPaciente("Barney", "Gomez", "Sanchez", "67890123", "barney@correo.com", "+51999000007");
        crearPaciente("Marge", "Simpson", "Bouvier", "78901234", "marge@correo.com", "+51999000008");
        crearPaciente("Apu", "Nahasapeemapetilon", "Singh", "89012345", "apu@correo.com", "+51999000009");
        crearPaciente("Milhouse", "Vargas", "Martinez", "90123456", "milhouse@correo.com", "+51999000010");

        crearUsuario("admin", "admin123", "admin@clinica.com", Rol.ADMIN, null, "Admin", "Sistema");

        crearUsuario("medico", "medico123", "medico@clinica.com", Rol.DOCTOR, null, "Dr. Juan", "Perez");
        crearUsuario("ppicapiedra", "pedro123", "ppicapiedra@clinica.com", Rol.DOCTOR, null, "Pedro", "Picapiedra");
        crearUsuario("doctor1", "doctor123", "doctor1@clinica.com", Rol.DOCTOR, null, "Juan", "Perez Garcia");
        crearUsuario("doctor2", "doctor123", "doctor2@clinica.com", Rol.DOCTOR, null, "Maria", "Garcia Lopez");
        crearUsuario("doctor3", "doctor123", "doctor3@clinica.com", Rol.DOCTOR, null, "Carlos", "Lopez Martinez");
        crearUsuario("doctor4", "doctor123", "doctor4@clinica.com", Rol.DOCTOR, null, "Ana", "Martinez Torres");
        crearUsuario("doctor5", "doctor123", "doctor5@clinica.com", Rol.DOCTOR, null, "Luis", "Torres Sanchez");
        crearUsuario("medico2", "medico123", "medico2@clinica.com", Rol.DOCTOR, null, "Roberto", "Sanchez Ortiz");

        crearUsuario("farmaceutico", "farma123", "farmaceutico@clinica.com", Rol.FARMACEUTICO, null, "Carlos", "Lopez");
        crearUsuario("farma2", "farma123", "farma2@clinica.com", Rol.FARMACEUTICO, null, "Carlos", "Farmacia Perez");

        crearUsuario("enfermero", "enfermero123", "enfermero@clinica.com", Rol.ENFERMERO, null, "Rosa", "Martinez");

        crearUsuario("atencion", "atencion123", "atencion@clinica.com", Rol.ATENCION_CLIENTE, null, "Ana", "Torres");

        crearUsuario("paciente1", "paciente123", "paciente1@correo.com", Rol.PACIENTE, pPedro, "Pedro", "Ramirez");
        crearUsuario("Homero", "Homero123", "chavezjonas500@gmail.com", Rol.PACIENTE, pHomero, "Homero", "Simpson Bouvier");
        crearUsuario("mlopez", "mlopez123", "mlopez@correo.com", Rol.PACIENTE, pMaria, "Maria", "Lopez Garcia");
        crearUsuario("jrodriguez", "juan123", "jrodriguez@correo.com", Rol.PACIENTE, pJuan, "Juan", "Rodriguez Perez");

        System.out.println("[DataSeeder] Seed completado: 10 pacientes, 17 usuarios");
    }
}
