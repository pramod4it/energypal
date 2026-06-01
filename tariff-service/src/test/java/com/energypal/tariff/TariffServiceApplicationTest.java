package com.energypal.tariff;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TariffServiceApplicationTest {
    private final EnergyPlanRepository repository = mock(EnergyPlanRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final TariffController controller = new TariffController(repository, eventPublisher);

    @Test
    void createsPlanAndPublishesEvent() {
        when(repository.save(any(EnergyPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var plan = controller.create(new EnergyPlanRequest("s1", "Green Saver", "FIXED", new BigDecimal("12.50"), new BigDecimal("0.32"), true))
                .getBody().data();

        assertThat(plan.getId()).isNotBlank();
        assertThat(plan.getSupplierId()).isEqualTo("s1");
        assertThat(plan.getPlanName()).isEqualTo("Green Saver");
        assertThat(plan.getPlanType()).isEqualTo("FIXED");
        assertThat(plan.getStandingCharge()).isEqualByComparingTo("12.50");
        assertThat(plan.getUnitRate()).isEqualByComparingTo("0.32");
        assertThat(plan.isGreenEnergy()).isTrue();
        verify(eventPublisher).publish(eq("tariff.events"), argThat(event -> "EnergyPlanCreated".equals(event.eventType())));
    }

    @Test
    void listsPlansBySupplierOrAll() {
        var plan = new EnergyPlan("s1", "Green Saver", "FIXED", BigDecimal.TEN, BigDecimal.ONE, true);
        when(repository.findAll()).thenReturn(List.of(plan));
        when(repository.findBySupplierId("s1")).thenReturn(List.of(plan));

        assertThat(controller.all(null).data()).containsExactly(plan);
        assertThat(controller.all("s1").data()).containsExactly(plan);
    }

    @Test
    void comparesPlansByEstimatedMonthlyCost() {
        var expensive = new EnergyPlan("s1", "Premium", "FIXED", new BigDecimal("10"), new BigDecimal("2"), false);
        var cheap = new EnergyPlan("s2", "Saver", "FIXED", BigDecimal.ZERO, BigDecimal.ONE, true);
        when(repository.findAll()).thenReturn(List.of(expensive, cheap));

        var result = controller.compare(new BigDecimal("10")).data();

        assertThat(result).extracting(PlanComparison::planName).containsExactly("Saver", "Premium");
        assertThat(result.get(0).estimatedMonthlyCost()).isEqualByComparingTo("10");
    }
}
