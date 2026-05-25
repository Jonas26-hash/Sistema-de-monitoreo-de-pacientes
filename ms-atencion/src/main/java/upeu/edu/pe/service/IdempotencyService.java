package upeu.edu.pe.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import upeu.edu.pe.entity.IdempotencyRecord;
import java.time.LocalDateTime;

@ApplicationScoped
public class IdempotencyService {

    public IdempotencyRecord findExisting(String key) {
        if (key == null || key.isBlank()) return null;
        return IdempotencyRecord.findByKey(key);
    }

    @Transactional
    public IdempotencyRecord saveRecord(String key, String responseBody, int status) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.idempotencyKey = key;
        record.responseBody = responseBody;
        record.responseStatus = status;
        record.createdAt = LocalDateTime.now();
        record.persistAndFlush();
        return record;
    }
}
