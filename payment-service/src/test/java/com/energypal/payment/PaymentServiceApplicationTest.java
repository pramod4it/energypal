package com.energypal.payment;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceApplicationTest {
    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final PaymentController controller = new PaymentController(repository, eventPublisher);

    @Test
    void recordsPaymentAndPublishesEvent() {
        when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var payment = controller.pay(new PaymentRequest("b1", "c1", new BigDecimal("112.00"))).getBody().data();

        assertThat(payment.getId()).isNotBlank();
        assertThat(payment.getBillId()).isEqualTo("b1");
        assertThat(payment.getCustomerId()).isEqualTo("c1");
        assertThat(payment.getAmount()).isEqualByComparingTo("112.00");
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getStatus().name()).isEqualTo("PAID");
        verify(eventPublisher).publish(eq("payment.events"), argThat(event -> "PaymentCompleted".equals(event.eventType())));
    }

    @Test
    void listsPaymentsByCustomerOrAll() {
        var payment = new Payment("b1", "c1", BigDecimal.TEN);
        when(repository.findAll()).thenReturn(List.of(payment));
        when(repository.findByCustomerId("c1")).thenReturn(List.of(payment));

        assertThat(controller.payments(null).data()).containsExactly(payment);
        assertThat(controller.payments("c1").data()).containsExactly(payment);
    }
}
