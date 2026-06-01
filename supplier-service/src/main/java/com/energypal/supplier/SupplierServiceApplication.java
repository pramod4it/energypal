package com.energypal.supplier;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class SupplierServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SupplierServiceApplication.class, args);
    }
}

@Entity
class Supplier {
    @Id
    private String id;
    private String name;
    private String contactEmail;
    private String serviceArea;
    private boolean greenEnergy;

    protected Supplier() {}
    Supplier(String name, String contactEmail, String serviceArea, boolean greenEnergy) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.contactEmail = contactEmail;
        this.serviceArea = serviceArea;
        this.greenEnergy = greenEnergy;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getContactEmail() { return contactEmail; }
    public String getServiceArea() { return serviceArea; }
    public boolean isGreenEnergy() { return greenEnergy; }
}

interface SupplierRepository extends JpaRepository<Supplier, String> {
    List<Supplier> findByServiceAreaIgnoreCase(String serviceArea);
}

@RestController
@RequestMapping("/api/suppliers")
class SupplierController {
    private final SupplierRepository repository;
    private final EventPublisher eventPublisher;

    SupplierController(SupplierRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<Supplier>> all(@RequestParam(required = false) String area) {
        return ApiResponse.ok(area == null ? repository.findAll() : repository.findByServiceAreaIgnoreCase(area));
    }

    @PostMapping
    ResponseEntity<ApiResponse<Supplier>> create(@RequestBody SupplierRequest request) {
        var supplier = repository.save(new Supplier(request.name(), request.contactEmail(), request.serviceArea(), request.greenEnergy()));
        eventPublisher.publish(TopicNames.SUPPLIER_EVENTS, EventEnvelope.of("SupplierCreated", "supplier-service", supplier.getId(),
                Map.of("supplierId", supplier.getId(), "name", supplier.getName(), "serviceArea", supplier.getServiceArea())));
        return ResponseEntity.ok(ApiResponse.ok(supplier));
    }
}

record SupplierRequest(String name, String contactEmail, String serviceArea, boolean greenEnergy) {}
