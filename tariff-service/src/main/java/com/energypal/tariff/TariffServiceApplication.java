package com.energypal.tariff;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class TariffServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(TariffServiceApplication.class, args);
    }
}

@Entity
class EnergyPlan {
    @Id
    private String id;
    private String supplierId;
    private String planName;
    private String planType;
    private BigDecimal standingCharge;
    private BigDecimal unitRate;
    private boolean greenEnergy;

    protected EnergyPlan() {}
    EnergyPlan(String supplierId, String planName, String planType, BigDecimal standingCharge, BigDecimal unitRate, boolean greenEnergy) {
        this.id = UUID.randomUUID().toString();
        this.supplierId = supplierId;
        this.planName = planName;
        this.planType = planType;
        this.standingCharge = standingCharge;
        this.unitRate = unitRate;
        this.greenEnergy = greenEnergy;
    }
    public String getId() { return id; }
    public String getSupplierId() { return supplierId; }
    public String getPlanName() { return planName; }
    public String getPlanType() { return planType; }
    public BigDecimal getStandingCharge() { return standingCharge; }
    public BigDecimal getUnitRate() { return unitRate; }
    public boolean isGreenEnergy() { return greenEnergy; }
}

interface EnergyPlanRepository extends JpaRepository<EnergyPlan, String> {
    List<EnergyPlan> findBySupplierId(String supplierId);
}

@RestController
@RequestMapping("/api/tariffs")
class TariffController {
    private final EnergyPlanRepository repository;
    private final EventPublisher eventPublisher;

    TariffController(EnergyPlanRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<EnergyPlan>> all(@RequestParam(required = false) String supplierId) {
        return ApiResponse.ok(supplierId == null ? repository.findAll() : repository.findBySupplierId(supplierId));
    }

    @GetMapping("/compare")
    ApiResponse<List<PlanComparison>> compare(@RequestParam BigDecimal monthlyKwh) {
        return ApiResponse.ok(repository.findAll().stream()
                .map(plan -> new PlanComparison(plan.getId(), plan.getPlanName(), plan.getUnitRate().multiply(monthlyKwh).add(plan.getStandingCharge())))
                .sorted(Comparator.comparing(PlanComparison::estimatedMonthlyCost))
                .toList());
    }

    @PostMapping
    ResponseEntity<ApiResponse<EnergyPlan>> create(@RequestBody EnergyPlanRequest request) {
        var plan = repository.save(new EnergyPlan(request.supplierId(), request.planName(), request.planType(), request.standingCharge(), request.unitRate(), request.greenEnergy()));
        eventPublisher.publish(TopicNames.TARIFF_EVENTS, EventEnvelope.of("EnergyPlanCreated", "tariff-service", plan.getId(),
                Map.of("planId", plan.getId(), "supplierId", plan.getSupplierId(), "unitRate", plan.getUnitRate())));
        return ResponseEntity.ok(ApiResponse.ok(plan));
    }
}

record EnergyPlanRequest(String supplierId, String planName, String planType, BigDecimal standingCharge, BigDecimal unitRate, boolean greenEnergy) {}
record PlanComparison(String planId, String planName, BigDecimal estimatedMonthlyCost) {}
