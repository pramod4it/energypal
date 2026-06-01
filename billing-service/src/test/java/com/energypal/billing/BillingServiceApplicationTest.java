package com.energypal.billing;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingServiceApplicationTest {
    private final BillRepository repository = mock(BillRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final BillingController controller = new BillingController(repository, eventPublisher);

    @Test
    void generatesBillAndPublishesEvent() {
        when(repository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var bill = controller.generate(new BillRequest("c1", "2026-06", new BigDecimal("112.00"), LocalDate.of(2026, 6, 20)))
                .getBody().data();

        assertThat(bill.getId()).isNotBlank();
        assertThat(bill.getCustomerId()).isEqualTo("c1");
        assertThat(bill.getBillingMonth()).isEqualTo("2026-06");
        assertThat(bill.getAmount()).isEqualByComparingTo("112.00");
        assertThat(bill.getDueDate()).isEqualTo("2026-06-20");
        assertThat(bill.getStatus().name()).isEqualTo("GENERATED");
        verify(eventPublisher).publish(eq("billing.events"), argThat(event -> "BillGenerated".equals(event.eventType())));
    }

    @Test
    void listsBillsByCustomerOrAllAndCanMarkPaid() {
        var bill = new Bill("c1", "2026-06", BigDecimal.TEN, LocalDate.now());
        bill.markPaid();
        when(repository.findAll()).thenReturn(List.of(bill));
        when(repository.findByCustomerId("c1")).thenReturn(List.of(bill));

        assertThat(controller.bills(null).data()).containsExactly(bill);
        assertThat(controller.bills("c1").data()).containsExactly(bill);
        assertThat(bill.getStatus().name()).isEqualTo("PAID");
    }

    @Test
    void createsBillFromMeterReadingEvent() {
        when(repository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var event = EventEnvelope.of("MeterReadingSubmitted", "usage-service", "r1",
                Map.of("customerId", "c1", "kwh", new BigDecimal("100")));

        controller.onUsage(event);

        verify(repository).save(argThat(bill -> bill.getCustomerId().equals("c1") && bill.getAmount().compareTo(new BigDecimal("32.00")) == 0));
        verify(eventPublisher).publish(eq("billing.events"), argThat(sent -> "BillGenerated".equals(sent.eventType())));
    }

    @Test
    void ignoresOtherUsageEvents() {
        controller.onUsage(EventEnvelope.of("UsageCalculated", "usage-service", "u1", Map.of()));

        verifyNoInteractions(repository, eventPublisher);
    }
}
