package upeu.edu.pe.service;

import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import upeu.edu.pe.entity.AuditLog;

@ApplicationScoped
public class AuditService {

    @Transactional
    public void guardar(String username, String accion, String recurso,
                        Integer statusCode, Long tiempoMs, String ip,
                        String userAgent, String requestId) {
        AuditLog log = new AuditLog();
        log.username = username;
        log.accion = accion;
        log.recurso = recurso;
        log.statusCode = statusCode;
        log.tiempoMs = tiempoMs;
        log.ip = ip;
        log.userAgent = userAgent;
        log.requestId = requestId;
        log.createdAt = LocalDateTime.now();
        log.persist();
    }

    public Map<String, Object> listar(String username, String accion,
                                       LocalDateTime desde, LocalDateTime hasta,
                                       int page, int size) {
        StringBuilder query = new StringBuilder("1=1");
        Map<String, Object> params = new HashMap<>();

        if (username != null && !username.isBlank()) {
            query.append(" and username = :username");
            params.put("username", username);
        }
        if (accion != null && !accion.isBlank()) {
            query.append(" and accion = :accion");
            params.put("accion", accion.toUpperCase());
        }
        if (desde != null) {
            query.append(" and createdAt >= :desde");
            params.put("desde", desde);
        }
        if (hasta != null) {
            query.append(" and createdAt <= :hasta");
            params.put("hasta", hasta);
        }

        query.append(" order by createdAt desc");

        long total = params.isEmpty()
            ? AuditLog.count()
            : AuditLog.count(query.toString(), params);

        List<AuditLog> items = params.isEmpty()
            ? AuditLog.findAll().page(Page.of(page, size)).list()
            : AuditLog.find(query.toString(), params).page(Page.of(page, size)).list();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        return result;
    }
}
