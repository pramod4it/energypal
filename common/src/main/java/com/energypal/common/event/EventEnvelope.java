package com.energypal.common.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventEnvelope(
        @NotBlank String eventId,
        @NotBlank String eventType,
        int eventVersion,
        @NotNull Instant occurredAt,
        @NotBlank String source,
        @NotBlank String correlationId,
        @NotNull Map<String, Object> payload
) {
    public static EventEnvelope of(String eventType, String source, String correlationId, Map<String, Object> payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                1,
                Instant.now(),
                source,
                correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId,
                payload
        );
    }
}
