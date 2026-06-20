package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_pendiente")
public class RegistroPendiente extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String nombres;

    @Column(nullable = false)
    public String apellidos;

    public String dni;

    public String telefono;

    public LocalDate fechaNacimiento;

    public String direccion;

    @Column(nullable = false)
    public String rolSolicitado;

    @Column(nullable = false)
    public String codigoVerificacion;

    @Column(nullable = false)
    public LocalDateTime codigoExpiracion;

    @Column(nullable = false)
    public boolean verificado = false;

    public String usernameSugerido;

    @Column(nullable = false)
    public LocalDateTime creadoEn;

    public String passwordHash;

    public String username;
}
