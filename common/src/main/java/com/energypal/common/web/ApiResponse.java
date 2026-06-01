package com.energypal.common.web;

import java.time.Instant;

public record ApiResponse<T>(Instant timestamp, String status, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(Instant.now(), "OK", data);
    }
}
