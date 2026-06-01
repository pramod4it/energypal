package com.energypal.notification;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceApplicationTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final NotificationController controller = new NotificationController(repository, eventPublisher);

    @Test
    void createsNotificationAndPublishesEvent() {
        when(repository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var notification = controller.create(new NotificationRequest("c1", "EMAIL", "Hello", "Message")).data();

        assertThat(notification.getId()).isNotBlank();
        assertThat(notification.getCustomerId()).isEqualTo("c1");
        assertThat(notification.getChannelName()).isEqualTo("EMAIL");
        assertThat(notification.getSubject()).isEqualTo("Hello");
        assertThat(notification.getMessage()).isEqualTo("Message");
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getStatus().name()).isEqualTo("GENERATED");
        verify(eventPublisher).publish(eq("notification.events"), argThat(event -> "NotificationRequested".equals(event.eventType())));
    }

    @Test
    void listsNotificationsByCustomerOrAll() {
        var notification = new NotificationRecord("c1", "EMAIL", "Hello", "Message");
        when(repository.findAll()).thenReturn(List.of(notification));
        when(repository.findByCustomerId("c1")).thenReturn(List.of(notification));

        assertThat(controller.list(null).data()).containsExactly(notification);
        assertThat(controller.list("c1").data()).containsExactly(notification);
    }

    @Test
    void createsNotificationsFromBillAndPaymentEvents() {
        when(repository.save(any(NotificationRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.onBusinessEvent(EventEnvelope.of("BillGenerated", "billing-service", "b1", Map.of("customerId", "c1")));
        controller.onBusinessEvent(EventEnvelope.of("PaymentCompleted", "payment-service", "p1", Map.of("customerId", "c1")));
        controller.onBusinessEvent(EventEnvelope.of("SupplierCreated", "supplier-service", "s1", Map.of("customerId", "c1")));

        verify(repository, times(2)).save(any(NotificationRecord.class));
    }
}
