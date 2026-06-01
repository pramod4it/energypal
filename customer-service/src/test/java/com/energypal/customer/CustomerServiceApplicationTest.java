package com.energypal.customer;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerServiceApplicationTest {
    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final EventPublisher eventPublisher = mock();
    private final CustomerController controller = new CustomerController(repository, eventPublisher);

    @Test
    void createsCustomerAndPublishesEvent() {
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.create(new CustomerRequest("Asha Rao", "asha@example.com", "999", "560001", "MG Road"));
        var customer = response.getBody().data();

        assertThat(customer.getId()).isNotBlank();
        assertThat(customer.getFullName()).isEqualTo("Asha Rao");
        assertThat(customer.getEmail()).isEqualTo("asha@example.com");
        assertThat(customer.getPhone()).isEqualTo("999");
        assertThat(customer.getPostcode()).isEqualTo("560001");
        assertThat(customer.getServiceAddress()).isEqualTo("MG Road");
        assertThat(customer.getStatus().name()).isEqualTo("ACTIVE");
        verify(eventPublisher).publish(eq("customer.events"), argThat(event -> "CustomerCreated".equals(event.eventType())));
    }

    @Test
    void listsCustomersByPostcodeOrAll() {
        var customer = new Customer("Asha", "a@example.com", "1", "560001", "Road");
        when(repository.findAll()).thenReturn(List.of(customer));
        when(repository.findByPostcodeIgnoreCase("560001")).thenReturn(List.of(customer));

        assertThat(controller.all(null).data()).containsExactly(customer);
        assertThat(controller.all("560001").data()).containsExactly(customer);
    }

    @Test
    void returnsCustomerById() {
        var customer = new Customer("Asha", "a@example.com", "1", "560001", "Road");
        when(repository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThat(controller.one(customer.getId()).data()).isSameAs(customer);
    }
}
