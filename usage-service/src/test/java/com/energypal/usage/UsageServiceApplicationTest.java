package com.energypal.usage;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsageServiceApplicationTest {
    private final MeterReadingRepository repository = mock(MeterReadingRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final UsageController controller = new UsageController(repository, eventPublisher);

    @Test
    void submitsReadingAndPublishesEvent() {
        when(repository.save(any(MeterReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var reading = controller.submit(new MeterReadingRequest("c1", LocalDate.of(2026, 6, 1), new BigDecimal("350")))
                .getBody().data();

        assertThat(reading.getId()).isNotBlank();
        assertThat(reading.getCustomerId()).isEqualTo("c1");
        assertThat(reading.getReadingDate()).isEqualTo("2026-06-01");
        assertThat(reading.getKwh()).isEqualByComparingTo("350");
        verify(eventPublisher).publish(eq("usage.events"), argThat(event -> "MeterReadingSubmitted".equals(event.eventType())));
    }

    @Test
    void returnsReadingsForCustomer() {
        var reading = new MeterReading("c1", LocalDate.of(2026, 6, 1), BigDecimal.TEN);
        when(repository.findByCustomerId("c1")).thenReturn(List.of(reading));

        assertThat(controller.readings("c1").data()).containsExactly(reading);
    }
}
