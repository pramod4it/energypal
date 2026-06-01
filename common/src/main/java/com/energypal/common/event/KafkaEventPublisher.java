package com.energypal.common.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, EventEnvelope event) {
        kafkaTemplate.send(topic, event);
    }
}
