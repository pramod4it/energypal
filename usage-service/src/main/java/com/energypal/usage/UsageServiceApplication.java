package com.energypal.usage;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class UsageServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(UsageServiceApplication.class, args);
    }
}

@Entity
class MeterReading {
    @Id
    private String id;
    private String customerId;
    private LocalDate readingDate;
    private BigDecimal kwh;

    protected MeterReading() {}
    MeterReading(String customerId, LocalDate readingDate, BigDecimal kwh) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.readingDate = readingDate;
        this.kwh = kwh;
    }
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public LocalDate getReadingDate() { return readingDate; }
    public BigDecimal getKwh() { return kwh; }
}

interface MeterReadingRepository extends JpaRepository<MeterReading, String> {
    List<MeterReading> findByCustomerId(String customerId);
}

@RestController
@RequestMapping("/api/usage")
class UsageController {
    private final MeterReadingRepository repository;
    private final EventPublisher eventPublisher;

    UsageController(MeterReadingRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/readings")
    ApiResponse<List<MeterReading>> readings(@RequestParam String customerId) {
        return ApiResponse.ok(repository.findByCustomerId(customerId));
    }

    @PostMapping("/readings")
    ResponseEntity<ApiResponse<MeterReading>> submit(@RequestBody MeterReadingRequest request) {
        var reading = repository.save(new MeterReading(request.customerId(), request.readingDate(), request.kwh()));
        eventPublisher.publish(TopicNames.USAGE_EVENTS, EventEnvelope.of("MeterReadingSubmitted", "usage-service", reading.getId(),
                Map.of("readingId", reading.getId(), "customerId", reading.getCustomerId(), "kwh", reading.getKwh())));
        return ResponseEntity.ok(ApiResponse.ok(reading));
    }
}

record MeterReadingRequest(String customerId, LocalDate readingDate, BigDecimal kwh) {}
