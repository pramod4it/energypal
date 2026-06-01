package com.energypal.billing;

import com.energypal.common.domain.Status;
import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class BillingServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(BillingServiceApplication.class, args);
    }
}

@Entity
class Bill {
    @Id
    private String id;
    private String customerId;
    private String billingMonth;
    private BigDecimal amount;
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    private Status status = Status.GENERATED;

    protected Bill() {}
    Bill(String customerId, String billingMonth, BigDecimal amount, LocalDate dueDate) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.billingMonth = billingMonth;
        this.amount = amount;
        this.dueDate = dueDate;
    }
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getBillingMonth() { return billingMonth; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public Status getStatus() { return status; }
    public void markPaid() { this.status = Status.PAID; }
}

interface BillRepository extends JpaRepository<Bill, String> {
    List<Bill> findByCustomerId(String customerId);
}

@RestController
@RequestMapping("/api/bills")
class BillingController {
    private final BillRepository repository;
    private final EventPublisher eventPublisher;

    BillingController(BillRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<Bill>> bills(@RequestParam(required = false) String customerId) {
        return ApiResponse.ok(customerId == null ? repository.findAll() : repository.findByCustomerId(customerId));
    }

    @PostMapping
    ResponseEntity<ApiResponse<Bill>> generate(@RequestBody BillRequest request) {
        var bill = repository.save(new Bill(request.customerId(), request.billingMonth(), request.amount(), request.dueDate()));
        publishGenerated(bill);
        return ResponseEntity.ok(ApiResponse.ok(bill));
    }

    @KafkaListener(topics = "usage.events", groupId = "billing-service")
    void onUsage(EventEnvelope event) {
        if (!"MeterReadingSubmitted".equals(event.eventType())) {
            return;
        }
        var customerId = String.valueOf(event.payload().get("customerId"));
        var kwh = new BigDecimal(String.valueOf(event.payload().get("kwh")));
        var bill = repository.save(new Bill(customerId, LocalDate.now().withDayOfMonth(1).toString(), kwh.multiply(new BigDecimal("0.32")), LocalDate.now().plusDays(14)));
        publishGenerated(bill);
    }

    private void publishGenerated(Bill bill) {
        eventPublisher.publish(TopicNames.BILLING_EVENTS, EventEnvelope.of("BillGenerated", "billing-service", bill.getId(),
                Map.of("billId", bill.getId(), "customerId", bill.getCustomerId(), "amount", bill.getAmount(), "dueDate", bill.getDueDate().toString(), "status", bill.getStatus().name())));
    }
}

record BillRequest(String customerId, String billingMonth, BigDecimal amount, LocalDate dueDate) {}
