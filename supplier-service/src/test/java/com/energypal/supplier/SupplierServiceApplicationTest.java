package com.energypal.supplier;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupplierServiceApplicationTest {
    private final SupplierRepository repository = mock(SupplierRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final SupplierController controller = new SupplierController(repository, eventPublisher);

    @Test
    void createsSupplierAndPublishesEvent() {
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var supplier = controller.create(new SupplierRequest("Green Grid", "hello@grid.test", "560001", true)).getBody().data();

        assertThat(supplier.getId()).isNotBlank();
        assertThat(supplier.getName()).isEqualTo("Green Grid");
        assertThat(supplier.getContactEmail()).isEqualTo("hello@grid.test");
        assertThat(supplier.getServiceArea()).isEqualTo("560001");
        assertThat(supplier.isGreenEnergy()).isTrue();
        verify(eventPublisher).publish(eq("supplier.events"), argThat(event -> "SupplierCreated".equals(event.eventType())));
    }

    @Test
    void listsSuppliersByAreaOrAll() {
        var supplier = new Supplier("Green Grid", "hello@grid.test", "560001", true);
        when(repository.findAll()).thenReturn(List.of(supplier));
        when(repository.findByServiceAreaIgnoreCase("560001")).thenReturn(List.of(supplier));

        assertThat(controller.all(null).data()).containsExactly(supplier);
        assertThat(controller.all("560001").data()).containsExactly(supplier);
    }
}
