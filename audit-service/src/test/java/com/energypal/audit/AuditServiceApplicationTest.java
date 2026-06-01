package com.energypal.audit;

import com.energypal.common.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceApplicationTest {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditController controller = new AuditController(repository);

    @Test
    void storesAuditEvent() {
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var event = EventEnvelope.of("BillGenerated", "billing-service", "corr-1", Map.of("billId", "b1"));

        controller.audit(event);

        verify(repository).save(argThat(log ->
                log.getId() != null
                        && log.getEventType().equals("BillGenerated")
                        && log.getSource().equals("billing-service")
                        && log.getCorrelationId().equals("corr-1")
                        && log.getOccurredAt() != null
                        && log.getPayload().contains("billId")));
    }

    @Test
    void listsAuditLogsByTypeOrAll() {
        var log = new AuditLog(EventEnvelope.of("BillGenerated", "billing-service", "corr-1", Map.of("billId", "b1")));
        when(repository.findAll()).thenReturn(List.of(log));
        when(repository.findByEventType("BillGenerated")).thenReturn(List.of(log));

        assertThat(controller.logs(null).data()).containsExactly(log);
        assertThat(controller.logs("BillGenerated").data()).containsExactly(log);
    }
}
