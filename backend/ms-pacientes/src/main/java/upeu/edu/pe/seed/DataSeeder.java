package upeu.edu.pe.seed;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import io.quarkus.runtime.StartupEvent;
import org.mindrot.jbcrypt.BCrypt;
import upeu.edu.pe.entity.Rol;
import upeu.edu.pe.entity.Usuario;

@ApplicationScoped
public class DataSeeder {

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

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        migrarCajeros();

        if (Usuario.count() > 0) return;

        Usuario admin = new Usuario();
        admin.username = "admin";
        admin.password = BCrypt.hashpw("admin123", BCrypt.gensalt());
        admin.email = "admin@clinica.com";
        admin.roles = java.util.List.of(Rol.ADMIN);
        admin.activo = true;
        admin.nombres = "Admin";
        admin.apellidos = "Sistema";
        admin.persist();

        String[][] seedUsers = {
            {"medico", "medico123", "medico@clinica.com", "Dr. Juan", "Perez"},
            {"farmaceutico", "farma123", "farma@clinica.com", "Carlos", "Lopez"},
            {"enfermero", "enfermero123", "enfermero@clinica.com", "Rosa", " Martinez"},
            {"atencion", "atencion123", "atencion@clinica.com", "Ana", "Torres"},
            {"paciente1", "paciente123", "paciente1@correo.com", "Pedro", "Ramirez"}
        };
        Rol[] roles = {Rol.DOCTOR, Rol.FARMACEUTICO, Rol.ENFERMERO, Rol.ATENCION_CLIENTE, Rol.PACIENTE};
        for (int i = 0; i < seedUsers.length; i++) {
            Usuario u = new Usuario();
            u.username = seedUsers[i][0];
            u.password = BCrypt.hashpw(seedUsers[i][1], BCrypt.gensalt());
            u.email = seedUsers[i][2];
            u.roles = java.util.List.of(roles[i]);
            u.activo = true;
            u.nombres = seedUsers[i][3];
            u.apellidos = seedUsers[i][4];
            u.persist();
        }
    }
}
