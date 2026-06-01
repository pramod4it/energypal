package com.energypal.console.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCostCalculatorTest {
    private final PlanCostCalculator calculator = new PlanCostCalculator();

    @Test
    void calculatesAnnualCostsInAscendingOrderForSampleConsumption() {
        var results = calculator.annualCosts(samplePlans(), BigDecimal.valueOf(1000));

        assertThat(results).extracting(PlanCost::toCsvLine).containsExactly(
                "energyOne,planOne,108.68",
                "energyThree,planThree,111.25",
                "energyTwo,planTwo,120.22",
                "energyFour,planFour,121.33"
        );
    }

    @Test
    void calculatesAnnualCostsForSecondSampleConsumption() {
        var results = calculator.annualCosts(samplePlans(), BigDecimal.valueOf(2000));

        assertThat(results).extracting(PlanCost::toCsvLine).containsExactly(
                "energyThree,planThree,205.75",
                "energyOne,planOne,213.68",
                "energyFour,planFour,215.83",
                "energyTwo,planTwo,235.72"
        );
    }

    @Test
    void handlesZeroConsumptionWithStandingChargeOnly() {
        var plan = new Plan("supplier", "standingOnly", List.of(new Price(BigDecimal.TEN, null)), BigDecimal.valueOf(7));

        assertThat(calculator.annualCost(plan, BigDecimal.ZERO).toCsvLine()).isEqualTo("supplier,standingOnly,26.83");
    }

    private List<Plan> samplePlans() {
        return List.of(
                new Plan("energyOne", "planOne", List.of(
                        new Price(BigDecimal.valueOf(13.5), 100),
                        new Price(BigDecimal.valueOf(10), null)
                ), null),
                new Plan("energyTwo", "planTwo", List.of(
                        new Price(BigDecimal.valueOf(12.5), 300),
                        new Price(BigDecimal.valueOf(11), null)
                ), null),
                new Plan("energyThree", "planThree", List.of(
                        new Price(BigDecimal.valueOf(14.5), 250),
                        new Price(BigDecimal.valueOf(10.1), 200),
                        new Price(BigDecimal.valueOf(9), null)
                ), null),
                new Plan("energyFour", "planFour", List.of(
                        new Price(BigDecimal.valueOf(9), null)
                ), BigDecimal.valueOf(7))
        );
    }
}
