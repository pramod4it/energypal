package com.energypal.console.domain;

import java.math.BigDecimal;

public record PlanCost(String supplierName, String planName, BigDecimal totalCost) implements Comparable<PlanCost> {
    @Override
    public int compareTo(PlanCost other) {
        return totalCost.compareTo(other.totalCost);
    }

    public String toCsvLine() {
        return supplierName + "," + planName + "," + totalCost;
    }
}
