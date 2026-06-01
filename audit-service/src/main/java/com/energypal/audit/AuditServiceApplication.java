package com.energypal.audit;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class AuditServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(AuditServiceApplication.class, args);
    }
}

@Entity
class AuditLog {
    @Id
    private String id;
    private String eventType;
    private String source;
    private String correlationId;
    private Instant occurredAt;
    @Lob
    private String payload;

    protected AuditLog() {}
    AuditLog(EventEnvelope event) {
        this.id = UUID.randomUUID().toString();
        this.eventType = event.eventType();
        this.source = event.source();
        this.correlationId = event.correlationId();
        this.occurredAt = event.occurredAt();
        this.payload = event.payload().toString();
    }
    public String getId() { return id; }
    public String getEventType() { return eventType; }
    public String getSource() { return source; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getPayload() { return payload; }
}

interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByEventType(String eventType);
}

@RestController
@RequestMapping("/api/audit")
class AuditController {
    private final AuditLogRepository repository;

    AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    ApiResponse<List<AuditLog>> logs(@RequestParam(required = false) String eventType) {
        return ApiResponse.ok(eventType == null ? repository.findAll() : repository.findByEventType(eventType));
    }

    @KafkaListener(topics = {"customer.events", "supplier.events", "tariff.events", "usage.events", "billing.events", "payment.events", "notification.events"}, groupId = "audit-service")
    void audit(EventEnvelope event) {
        repository.save(new AuditLog(event));
    }
}
