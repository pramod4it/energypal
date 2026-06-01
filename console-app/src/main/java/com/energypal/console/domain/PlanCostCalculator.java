package com.energypal.console.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PlanCostCalculator {
    private static final BigDecimal DAYS_IN_YEAR = BigDecimal.valueOf(365);
    private static final BigDecimal VAT_MULTIPLIER = BigDecimal.valueOf(1.05);
    private static final BigDecimal PENCE_PER_POUND = BigDecimal.valueOf(100);

    public List<PlanCost> annualCosts(List<Plan> plans, BigDecimal annualConsumptionKwh) {
        return plans.stream()
                .map(plan -> annualCost(plan, annualConsumptionKwh))
                .sorted()
                .toList();
    }

    public PlanCost annualCost(Plan plan, BigDecimal annualConsumptionKwh) {
        var energyCostPence = calculateEnergyCostPence(plan.prices(), annualConsumptionKwh);
        var standingChargePence = plan.dailyStandingChargePence().multiply(DAYS_IN_YEAR);
        var totalPounds = energyCostPence.add(standingChargePence)
                .multiply(VAT_MULTIPLIER)
                .divide(PENCE_PER_POUND, 2, RoundingMode.HALF_EVEN);
        return new PlanCost(plan.supplierName(), plan.planName(), totalPounds);
    }

    private BigDecimal calculateEnergyCostPence(List<Price> prices, BigDecimal annualConsumptionKwh) {
        var remainingKwh = annualConsumptionKwh;
        var totalPence = BigDecimal.ZERO;

        for (Price price : prices) {
            if (remainingKwh.signum() <= 0) {
                break;
            }
            var tierKwh = price.hasThreshold()
                    ? remainingKwh.min(BigDecimal.valueOf(price.threshold()))
                    : remainingKwh;
            totalPence = totalPence.add(tierKwh.multiply(price.rate()));
            remainingKwh = remainingKwh.subtract(tierKwh);
        }

        return totalPence;
    }
}
