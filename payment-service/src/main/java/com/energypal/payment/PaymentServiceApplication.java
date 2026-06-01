package com.energypal.payment;

import com.energypal.common.domain.Status;
import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class PaymentServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

@Entity
class Payment {
    @Id
    private String id;
    private String billId;
    private String customerId;
    private BigDecimal amount;
    private Instant paidAt;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected Payment() {}
    Payment(String billId, String customerId, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.billId = billId;
        this.customerId = customerId;
        this.amount = amount;
        this.paidAt = Instant.now();
        this.status = Status.PAID;
    }
    public String getId() { return id; }
    public String getBillId() { return billId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
    public Status getStatus() { return status; }
}

interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByCustomerId(String customerId);
}

@RestController
@RequestMapping("/api/payments")
class PaymentController {
    private final PaymentRepository repository;
    private final EventPublisher eventPublisher;

    PaymentController(PaymentRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<Payment>> payments(@RequestParam(required = false) String customerId) {
        return ApiResponse.ok(customerId == null ? repository.findAll() : repository.findByCustomerId(customerId));
    }

    @PostMapping
    ResponseEntity<ApiResponse<Payment>> pay(@RequestBody PaymentRequest request) {
        var payment = repository.save(new Payment(request.billId(), request.customerId(), request.amount()));
        eventPublisher.publish(TopicNames.PAYMENT_EVENTS, EventEnvelope.of("PaymentCompleted", "payment-service", payment.getId(),
                Map.of("paymentId", payment.getId(), "billId", payment.getBillId(), "customerId", payment.getCustomerId(), "amount", payment.getAmount())));
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }
}

record PaymentRequest(String billId, String customerId, BigDecimal amount) {}
