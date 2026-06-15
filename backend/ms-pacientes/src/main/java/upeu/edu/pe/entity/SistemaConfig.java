package upeu.edu.pe.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "sistema_config")
public class SistemaConfig extends PanacheEntityBase {

    @Id
    public Long id;

    @Column(length = 200)
    public String hospitalName;

    @Column(columnDefinition = "TEXT")
    public String hospitalLogo;

    public static SistemaConfig ensureExists() {
        SistemaConfig config = findById(1L);
        if (config == null) {
            config = new SistemaConfig();
            config.id = 1L;
            config.hospitalName = "";
            config.hospitalLogo = "";
            config.persist();
        }
        return config;
    }
}
