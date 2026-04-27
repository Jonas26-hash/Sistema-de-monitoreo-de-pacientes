package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.List;
import upeu.edu.pe.Paciente;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = "username"),
    @UniqueConstraint(columnNames = "email")
})
public class Usuario extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String password;

    @Column(nullable = false, unique = true)
    public String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    public List<Rol> roles;

    @ManyToOne
    @JoinColumn(name = "paciente_id")
    public Paciente paciente;

    @Column(nullable = false)
    public Boolean activo = true;

    public static Usuario findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static Usuario findByEmail(String email) {
        return find("email", email).firstResult();
    }
}