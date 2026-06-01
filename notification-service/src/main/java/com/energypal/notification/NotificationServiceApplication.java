package com.energypal.notification;

import com.energypal.common.domain.Status;
import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(NotificationServiceApplication.class, args);
    }
}

@Entity
class NotificationRecord {
    @Id
    private String id;
    private String customerId;
    private String channelName;
    private String subject;
    private String message;
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private Status status;

    protected NotificationRecord() {}
    NotificationRecord(String customerId, String channelName, String subject, String message) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.channelName = channelName;
        this.subject = subject;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = Status.GENERATED;
    }
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getChannelName() { return channelName; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
}

interface NotificationRepository extends JpaRepository<NotificationRecord, String> {
    List<NotificationRecord> findByCustomerId(String customerId);
}

@RestController
@RequestMapping("/api/notifications")
class NotificationController {
    private final NotificationRepository repository;
    private final EventPublisher eventPublisher;

    NotificationController(NotificationRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<NotificationRecord>> list(@RequestParam(required = false) String customerId) {
        return ApiResponse.ok(customerId == null ? repository.findAll() : repository.findByCustomerId(customerId));
    }

    @PostMapping
    ApiResponse<NotificationRecord> create(@RequestBody NotificationRequest request) {
        var notification = repository.save(new NotificationRecord(request.customerId(), request.channelName(), request.subject(), request.message()));
        eventPublisher.publish(TopicNames.NOTIFICATION_EVENTS, EventEnvelope.of("NotificationRequested", "notification-service", notification.getId(),
                Map.of("notificationId", notification.getId(), "customerId", notification.getCustomerId(), "channel", notification.getChannelName())));
        return ApiResponse.ok(notification);
    }

    @KafkaListener(topics = {"billing.events", "payment.events"}, groupId = "notification-service")
    void onBusinessEvent(EventEnvelope event) {
        if ("BillGenerated".equals(event.eventType())) {
            var customerId = String.valueOf(event.payload().get("customerId"));
            repository.save(new NotificationRecord(customerId, "EMAIL", "Bill generated", "Your new bill is ready."));
        }
        if ("PaymentCompleted".equals(event.eventType())) {
            var customerId = String.valueOf(event.payload().get("customerId"));
            repository.save(new NotificationRecord(customerId, "EMAIL", "Payment received", "Thank you for your payment."));
        }
    }
}

record NotificationRequest(String customerId, String channelName, String subject, String message) {}
