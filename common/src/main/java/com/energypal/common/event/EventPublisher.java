package com.energypal.common.event;

public interface EventPublisher {
    void publish(String topic, EventEnvelope event);
}
