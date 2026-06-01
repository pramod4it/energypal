package com.energypal.common;

import com.energypal.common.domain.Status;
import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.KafkaEventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.openapi.OpenApiConfiguration;
import com.energypal.common.web.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommonContractsTest {
    @Test
    void createsApiResponse() {
        var response = ApiResponse.ok("ready");

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("ready");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void createsEventEnvelopeWithGeneratedIds() {
        var event = EventEnvelope.of("CustomerCreated", "customer-service", "", Map.of("customerId", "c1"));

        assertThat(event.eventId()).isNotBlank();
        assertThat(event.eventType()).isEqualTo("CustomerCreated");
        assertThat(event.eventVersion()).isOne();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.source()).isEqualTo("customer-service");
        assertThat(event.correlationId()).isNotBlank();
        assertThat(event.payload()).containsEntry("customerId", "c1");
    }

    @Test
    void keepsProvidedCorrelationId() {
        var event = EventEnvelope.of("BillGenerated", "billing-service", "corr-1", Map.of());

        assertThat(event.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void exposesStatuses() {
        assertThat(Status.values()).contains(Status.ACTIVE, Status.PAID, Status.OVERDUE);
    }

    @Test
    void publishesEventsThroughKafkaAdapter() {
        KafkaTemplate<String, EventEnvelope> kafkaTemplate = mock();
        var publisher = new KafkaEventPublisher(kafkaTemplate);
        var event = EventEnvelope.of("BillGenerated", "billing-service", "corr-1", Map.of("billId", "b1"));

        publisher.publish(TopicNames.BILLING_EVENTS, event);

        verify(kafkaTemplate).send(TopicNames.BILLING_EVENTS, event);
    }

    @Test
    void centralizesTopicNames() {
        assertThat(TopicNames.CUSTOMER_EVENTS).isEqualTo("customer.events");
        assertThat(TopicNames.SUPPLIER_EVENTS).isEqualTo("supplier.events");
        assertThat(TopicNames.TARIFF_EVENTS).isEqualTo("tariff.events");
        assertThat(TopicNames.USAGE_EVENTS).isEqualTo("usage.events");
        assertThat(TopicNames.BILLING_EVENTS).isEqualTo("billing.events");
        assertThat(TopicNames.PAYMENT_EVENTS).isEqualTo("payment.events");
        assertThat(TopicNames.NOTIFICATION_EVENTS).isEqualTo("notification.events");
    }

    @Test
    void createsOpenApiMetadataFromServiceName() {
        var openApi = new OpenApiConfiguration().energyPalOpenApi("customer-service");

        assertThat(openApi.getInfo().getTitle()).isEqualTo("EnergyPal Customer Service");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("0.1.0");
        assertThat(openApi.getInfo().getDescription()).isEqualTo("EnergyPal Java 17 Spring Boot API");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getSecurity()).hasSize(1);
    }

    @Test
    void ignoresBlankWordsInOpenApiTitle() {
        var openApi = new OpenApiConfiguration().energyPalOpenApi("billing--service");

        assertThat(openApi.getInfo().getTitle()).isEqualTo("EnergyPal Billing Service");
    }
}
