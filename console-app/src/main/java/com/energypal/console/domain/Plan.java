package com.energypal.console.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record Plan(
        @JsonProperty("supplier_name") String supplierName,
        @JsonProperty("plan_name") String planName,
        List<Price> prices,
        @JsonProperty("standing_charge") BigDecimal standingCharge
) {
    public BigDecimal dailyStandingChargePence() {
        return standingCharge == null ? BigDecimal.ZERO : standingCharge;
    }
}
