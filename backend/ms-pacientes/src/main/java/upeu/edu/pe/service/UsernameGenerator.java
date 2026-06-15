package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import upeu.edu.pe.entity.Usuario;

@ApplicationScoped
public class UsernameGenerator {

    public String generar(String nombres, String apellidos) {
        String base = buildBase(nombres, apellidos);
        String username = base;

        int suffix = 1;
        while (Usuario.findByUsername(username) != null) {
            username = base + suffix;
            suffix++;
        }

        return username;
    }

    private String buildBase(String nombres, String apellidos) {
        String primerNombre = nombres.split(" ")[0].toLowerCase();
        String apellido = apellidos.split(" ")[0].toLowerCase();
        return (primerNombre.charAt(0) + apellido).replaceAll("[^a-z0-9]", "");
    }
}
