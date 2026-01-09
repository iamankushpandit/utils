package com.utilityexplorer.ingestion.adapter;

import com.utilityexplorer.adapter.eia.EiaApiAdapter;
import com.utilityexplorer.shared.adapter.IngestionAdapter;
import com.utilityexplorer.shared.dto.IngestionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {AdapterRegistry.class, EiaApiAdapter.class})
class AdapterRegistryTest {

    @Autowired
    private AdapterRegistry adapterRegistry;

    @MockBean
    private KafkaTemplate<String, IngestionEvent> kafkaTemplate;

    @MockBean
    private com.utilityexplorer.shared.persistence.FactValueRepository factValueRepository;

    @Test
    void registry_shouldDiscoverEiaAdapter() {
        Optional<IngestionAdapter> adapter = adapterRegistry.getAdapter("EIA_API");
        assertTrue(adapter.isPresent(), "EIA_API adapter should be present in registry");
        assertEquals("EIA_API", adapter.get().getAdapterId());
    }
}
