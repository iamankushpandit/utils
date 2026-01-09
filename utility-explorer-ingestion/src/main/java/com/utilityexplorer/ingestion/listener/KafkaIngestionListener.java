package com.utilityexplorer.ingestion.listener;

import com.utilityexplorer.shared.dto.IngestionEvent;
import com.utilityexplorer.shared.persistence.FactValue;
import com.utilityexplorer.shared.persistence.FactValueRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class KafkaIngestionListener {

    private final FactValueRepository factValueRepository;

    public KafkaIngestionListener(FactValueRepository factValueRepository) {
        this.factValueRepository = factValueRepository;
    }

    @KafkaListener(topics = "${ingestion.kafka.topic:raw-utility-data}", groupId = "${spring.kafka.consumer.group-id:ingestion-group}")
    public void listen(IngestionEvent event) {
        try {
            System.out.println("Received event: " + event.getMetricId() + " - " + event.getValue());

            FactValue fact = new FactValue();
            fact.setMetricId(event.getMetricId());
            fact.setSourceId(event.getSourceId());
            fact.setGeoLevel(event.getGeoLevel());
            fact.setGeoId(event.getGeoId());
            fact.setPeriodStart(event.getPeriodStart());
            fact.setPeriodEnd(event.getPeriodEnd());
            fact.setValueNumeric(event.getValue());
            fact.setIsAggregated(event.isAggregated());
            fact.setAggregationMethod(event.getAggregationMethod());
            
            fact.setRetrievedAt(Instant.now());
            // In a real scenario, source published date might come from the event payload
            fact.setSourcePublishedAt(Instant.now()); 

            factValueRepository.save(fact);

        } catch (Exception e) {
            System.err.println("Error processing ingestion event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
