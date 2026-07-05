package upeu.edu.pe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import io.quarkus.runtime.Startup;

@ApplicationScoped
@Startup
public class EurekaService {

    private static final Logger log = Logger.getLogger(EurekaService.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();

    private String eurekaUrl;
    private String appName;
    private String hostName;
    private int port;
    private String instanceId;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void init() {
        eurekaUrl = System.getenv("EUREKA_URL");
        if (eurekaUrl == null || eurekaUrl.isBlank()) {
            log.info("EUREKA_URL no configurada — discovery desactivado");
            return;
        }
        eurekaUrl = eurekaUrl.replaceAll("/+$", "");

        appName = System.getenv("EUREKA_APP_NAME");
        if (appName == null) appName = System.getenv("QUARKUS_APPLICATION_NAME");
        if (appName == null) appName = "UNKNOWN";

        hostName = System.getenv("HOSTNAME");
        if (hostName == null || hostName.isBlank()) {
            try { hostName = java.net.InetAddress.getLocalHost().getHostName(); }
            catch (Exception e) { hostName = "localhost"; }
        }

        try { port = Integer.parseInt(System.getenv().getOrDefault("QUARKUS_HTTP_PORT", "8080")); }
        catch (NumberFormatException e) { port = 8080; }

        register();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::heartbeat, 20, 30, TimeUnit.SECONDS);
        log.info("EurekaService iniciado — " + appName + " -> " + eurekaUrl);
    }

    private String getIpAddress() {
        try {
            var addr = java.net.InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    void register() {
        try {
            instanceId = appName + ":" + port;
            String ipAddr = getIpAddress();
            String body = MAPPER.writeValueAsString(Map.of("instance", Map.of(
                "instanceId", instanceId,
                "app", appName.toUpperCase(),
                "hostName", hostName,
                "ipAddr", ipAddr,
                "port", Map.of("$", port, "@enabled", "true"),
                "securePort", Map.of("$", 443, "@enabled", "false"),
                "statusPageUrl", "http://" + hostName + ":" + port + "/q/health",
                "healthCheckUrl", "http://" + hostName + ":" + port + "/q/health",
                "dataCenterInfo", Map.of(
                    "@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name", "MyOwn"
                ),
                "status", "UP"
            )));

            var req = HttpRequest.newBuilder()
                .uri(URI.create(eurekaUrl + "/apps/" + appName.toUpperCase()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Eureka registro: " + resp.statusCode() + " (" + instanceId + ")");
        } catch (Exception e) {
            log.warning("Error registrando en Eureka: " + e.getMessage());
        }
    }

    void heartbeat() {
        try {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(eurekaUrl + "/apps/" + appName.toUpperCase() + "/" + instanceId))
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 404) register();
        } catch (Exception e) {
            log.warning("Heartbeat Eureka falló: " + e.getMessage());
        }
    }

    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> instances = new ArrayList<>();
        try {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(eurekaUrl + "/apps/" + serviceId.toUpperCase()))
                .header("Accept", "application/json")
                .GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return instances;

            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode app = root.get("application");
            if (app == null) return instances;
            JsonNode inst = app.get("instance");
            if (inst == null) return instances;

            if (inst.isArray()) {
                for (JsonNode n : inst) instances.add(parseInstance(n));
            } else {
                instances.add(parseInstance(inst));
            }
        } catch (Exception e) {
            log.warning("Error consultando Eureka: " + e.getMessage());
        }
        return instances;
    }

    private ServiceInstance parseInstance(JsonNode n) {
        String id = n.get("instanceId").asText();
        String host = n.get("hostName").asText();
        int p = n.get("port").get("$").asInt();
        return new ServiceInstance(id, host, p, "http://" + host + ":" + p);
    }

    public record ServiceInstance(String instanceId, String hostName, int port, String url) {}

    @PreDestroy
    void destroy() {
        if (scheduler != null) scheduler.shutdown();
        if (instanceId == null) return;
        try {
            var req = HttpRequest.newBuilder()
                .uri(URI.create(eurekaUrl + "/apps/" + appName.toUpperCase() + "/" + instanceId))
                .DELETE().build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
            log.info("Deregistrado de Eureka: " + instanceId);
        } catch (Exception e) {
            log.warning("Error deregistrando: " + e.getMessage());
        }
    }
}
