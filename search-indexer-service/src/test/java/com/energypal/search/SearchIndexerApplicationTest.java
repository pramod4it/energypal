package com.energypal.search;

import com.energypal.common.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SearchIndexerApplicationTest {
    private final SearchDocumentRepository repository = mock(SearchDocumentRepository.class);
    private final SearchController controller = new SearchController(repository);

    @Test
    void indexesEventUsingPayloadId() {
        when(repository.save(any(SearchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.index(EventEnvelope.of("BillGenerated", "billing-service", "b1", Map.of("billId", "b1", "amount", "112")));

        verify(repository).save(argThat(document ->
                document.getId() != null
                        && document.getDomain().equals("billing")
                        && document.getEntityId().equals("b1")
                        && document.getEventType().equals("BillGenerated")
                        && document.getContent().contains("amount")
                        && document.getUpdatedAt() != null));
    }

    @Test
    void indexesEventWithGeneratedIdWhenPayloadHasNoEntityId() {
        when(repository.save(any(SearchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        controller.index(EventEnvelope.of("SupplierUpdated", "supplier-service", "s1", Map.of("name", "Grid")));

        verify(repository).save(argThat(document -> document.getEntityId() != null && document.getDomain().equals("supplier")));
    }

    @Test
    void searchesByTermAndDomain() {
        var document = new SearchDocument("billing", "b1", "BillGenerated", "amount=112");
        when(repository.findByContentContaining("112")).thenReturn(List.of(document));
        when(repository.findByDomain("billing")).thenReturn(List.of(document));

        assertThat(controller.search("112").data()).containsExactly(document);
        assertThat(controller.byDomain("billing").data()).containsExactly(document);
    }
}
