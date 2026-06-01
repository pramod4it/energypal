package com.energypal.console.domain;

import java.math.BigDecimal;

public record Price(BigDecimal rate, Integer threshold) {
    public boolean hasThreshold() {
        return threshold != null;
    }
}
