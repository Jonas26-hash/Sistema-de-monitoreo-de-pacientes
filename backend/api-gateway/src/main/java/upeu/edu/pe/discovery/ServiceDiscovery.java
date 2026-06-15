package upeu.edu.pe.discovery;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@ApplicationScoped
public class ServiceDiscovery {

    private static final Logger log = Logger.getLogger(ServiceDiscovery.class.getName());

    @Inject
    EurekaService eureka;

    private final Map<String, List<EurekaService.ServiceInstance>> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    private static final Map<String, String> FALLBACK = Map.of(
        "ms-pacientes", "http://ms-pacientes:8080",
        "ms-atencion", "http://ms-atencion:8080",
        "ms-recetas", "http://ms-recetas:8080",
        "ms-farmacia", "http://ms-farmacia:8080",
        "ms-cobros", "http://ms-cobros:8080",
        "ms-notificaciones", "http://ms-notificaciones:8080"
    );

    @PostConstruct
    void init() {
        refreshAll();
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::refreshAll, 15, 15, TimeUnit.SECONDS);
    }

    void refreshAll() {
        for (String service : FALLBACK.keySet()) {
            List<EurekaService.ServiceInstance> instances = eureka.getInstances(service);
            if (!instances.isEmpty()) {
                cache.put(service, new CopyOnWriteArrayList<>(instances));
            }
        }
    }

    public String getUrl(String service) {
        List<EurekaService.ServiceInstance> instances = cache.get(service);
        if (instances == null || instances.isEmpty()) {
            String fallback = FALLBACK.get(service);
            log.fine("ServiceDiscovery fallback -> " + service + " = " + fallback);
            return fallback;
        }
        int idx = counters.computeIfAbsent(service, k -> new AtomicInteger(0))
            .getAndIncrement() % instances.size();
        String url = instances.get(idx).url();
        log.fine("ServiceDiscovery -> " + service + "[" + idx + "] = " + url);
        return url;
    }
}
