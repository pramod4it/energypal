package com.energypal.search;

import com.energypal.common.event.EventEnvelope;
import com.energypal.common.web.ApiResponse;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@org.springframework.boot.autoconfigure.SpringBootApplication(scanBasePackages = "com.energypal")
public class SearchIndexerApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SearchIndexerApplication.class, args);
    }
}

@Document(indexName = "energypal-search")
class SearchDocument {
    @org.springframework.data.annotation.Id
    private String id;
    private String domain;
    private String entityId;
    private String eventType;
    private String content;
    private Instant updatedAt;

    protected SearchDocument() {}
    SearchDocument(String domain, String entityId, String eventType, String content) {
        this.id = UUID.randomUUID().toString();
        this.domain = domain;
        this.entityId = entityId;
        this.eventType = eventType;
        this.content = content;
        this.updatedAt = Instant.now();
    }
    public String getId() { return id; }
    public String getDomain() { return domain; }
    public String getEntityId() { return entityId; }
    public String getEventType() { return eventType; }
    public String getContent() { return content; }
    public Instant getUpdatedAt() { return updatedAt; }
}

interface SearchDocumentRepository extends ElasticsearchRepository<SearchDocument, String> {
    List<SearchDocument> findByContentContaining(String term);
    List<SearchDocument> findByDomain(String domain);
}

@RestController
@RequestMapping("/api/search")
class SearchController {
    private final SearchDocumentRepository repository;

    SearchController(SearchDocumentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    ApiResponse<List<SearchDocument>> search(@RequestParam String q) {
        return ApiResponse.ok(repository.findByContentContaining(q));
    }

    @GetMapping("/{domain}")
    ApiResponse<List<SearchDocument>> byDomain(@PathVariable String domain) {
        return ApiResponse.ok(repository.findByDomain(domain));
    }

    @KafkaListener(topics = {"customer.events", "supplier.events", "tariff.events", "billing.events", "payment.events"}, groupId = "search-indexer-service")
    void index(EventEnvelope event) {
        var domain = event.source().replace("-service", "");
        var entityId = entityIdFrom(event.payload());
        repository.save(new SearchDocument(domain, entityId, event.eventType(), event.payload().toString()));
    }

    private String entityIdFrom(Map<String, Object> payload) {
        return payload.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith("Id"))
                .map(entry -> String.valueOf(entry.getValue()))
                .findFirst()
                .orElse(UUID.randomUUID().toString());
    }
}
