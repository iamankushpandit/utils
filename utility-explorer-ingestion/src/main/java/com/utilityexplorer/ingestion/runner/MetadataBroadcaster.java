package com.utilityexplorer.ingestion.runner;

import com.utilityexplorer.shared.adapter.IngestionAdapter;
import com.utilityexplorer.shared.dto.MetricDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MetadataBroadcaster.class);
    private static final String TOPIC = "system.metadata.metrics";

    private final List<IngestionAdapter> adapters;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MetadataBroadcaster(List<IngestionAdapter> adapters, KafkaTemplate<String, Object> kafkaTemplate) {
        this.adapters = adapters;
        this.kafkaTemplate = kafkaTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void broadcastMetadata() {
        log.info("📢 Starting Metadata Broadcast for {} adapters...", adapters.size());
        
        for (IngestionAdapter adapter : adapters) {
            List<MetricDefinition> definitions = adapter.getMetricDefinitions();
            for (MetricDefinition def : definitions) {
                log.info("Broadcasting definition: {}", def.metricId());
                kafkaTemplate.send(TOPIC, def.metricId(), def);
            }
        }
        log.info("✅ Metadata Broadcast complete.");
    }
}
