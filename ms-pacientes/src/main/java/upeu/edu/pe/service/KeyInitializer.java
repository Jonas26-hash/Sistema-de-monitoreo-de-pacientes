package upeu.edu.pe.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.logging.Logger;

@ApplicationScoped
public class KeyInitializer {

    private static final Logger log = Logger.getLogger(KeyInitializer.class.getName());

    @PostConstruct
    void init() {
        try {
            String keyPath = System.getenv("JWT_SIGN_KEY_PATH");
            if (keyPath == null || keyPath.isBlank()) {
                keyPath = "config/jwt/privateKey.pem";
            }
            String pubPath = System.getenv("JWT_PUBLIC_KEY_PATH");
            if (pubPath == null || pubPath.isBlank()) {
                pubPath = "config/jwt/publicKey.pem";
            }

            keyPath = stripPrefix(keyPath);
            pubPath = stripPrefix(pubPath);
            Path privFile = Paths.get(keyPath);
            Path pubFile = Paths.get(pubPath);

            if (Files.exists(privFile)) {
                log.info("privateKey.pem ya existe en " + privFile.toAbsolutePath());
                return;
            }

            Files.createDirectories(privFile.getParent());
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            var pair = gen.generateKeyPair();

            try (var os = new FileOutputStream(privFile.toFile());
                 var w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                w.write("-----BEGIN PRIVATE KEY-----\n");
                w.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded()));
                w.write("\n-----END PRIVATE KEY-----\n");
            }
            log.info("Generado " + privFile.toAbsolutePath());

            try (var os = new FileOutputStream(pubFile.toFile());
                 var w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                w.write("-----BEGIN PUBLIC KEY-----\n");
                w.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPublic().getEncoded()));
                w.write("\n-----END PUBLIC KEY-----\n");
            }
            log.info("Generado " + pubFile.toAbsolutePath());

            log.warning("SE GENERO UN NUEVO PAR DE LLAVES JWT. Copie " + pubFile.toAbsolutePath()
                + " a los demas servicios, o use un volumen compartido en Docker.");
        } catch (Exception e) {
            log.severe("Error generando llaves JWT: " + e.getMessage());
        }
    }

    private static String stripPrefix(String path) {
        if (path.startsWith("file:")) return path.substring(5);
        if (path.startsWith("classpath:")) return path.substring(10);
        return path;
    }
}
