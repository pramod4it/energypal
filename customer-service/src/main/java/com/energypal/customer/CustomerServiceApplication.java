package com.energypal.customer;

import com.energypal.common.domain.Status;
import com.energypal.common.event.EventEnvelope;
import com.energypal.common.event.EventPublisher;
import com.energypal.common.event.TopicNames;
import com.energypal.common.web.ApiResponse;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class CustomerServiceApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(CustomerServiceApplication.class, args);
    }
}

@Entity
class Customer {
    @Id
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private String postcode;
    private String serviceAddress;
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    protected Customer() {}

    Customer(String fullName, String email, String phone, String postcode, String serviceAddress) {
        this.id = UUID.randomUUID().toString();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.postcode = postcode;
        this.serviceAddress = serviceAddress;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPostcode() { return postcode; }
    public String getServiceAddress() { return serviceAddress; }
    public Status getStatus() { return status; }
}

interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByPostcodeIgnoreCase(String postcode);
}

@RestController
@RequestMapping("/api/customers")
class CustomerController {
    private final CustomerRepository repository;
    private final EventPublisher eventPublisher;

    CustomerController(CustomerRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping
    ApiResponse<List<Customer>> all(@RequestParam(required = false) String postcode) {
        return ApiResponse.ok(postcode == null ? repository.findAll() : repository.findByPostcodeIgnoreCase(postcode));
    }

    @GetMapping("/{id}")
    ApiResponse<Customer> one(@PathVariable String id) {
        return ApiResponse.ok(repository.findById(id).orElseThrow());
    }

    @PostMapping
    ResponseEntity<ApiResponse<Customer>> create(@RequestBody CustomerRequest request) {
        var customer = repository.save(new Customer(request.fullName(), request.email(), request.phone(), request.postcode(), request.serviceAddress()));
        eventPublisher.publish(TopicNames.CUSTOMER_EVENTS, EventEnvelope.of("CustomerCreated", "customer-service", customer.getId(),
                Map.of("customerId", customer.getId(), "email", customer.getEmail(), "postcode", customer.getPostcode())));
        return ResponseEntity.ok(ApiResponse.ok(customer));
    }
}

record CustomerRequest(String fullName, String email, String phone, String postcode, String serviceAddress) {}
