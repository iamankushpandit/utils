package com.utilityexplorer.ingestion.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utilityexplorer.ingestion.adapter.AdapterRegistry;
import com.utilityexplorer.shared.adapter.IngestionAdapter;
import com.utilityexplorer.shared.persistence.Metric;
import com.utilityexplorer.shared.persistence.MetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class IngestionScheduler {

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private AdapterRegistry adapterRegistry;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Run every 60 seconds for demo purposes
    @Scheduled(fixedDelay = 60000)
    public void runIngestionCycle() {
        System.out.println("Starting Ingestion Cycle...");
        
        Iterable<Metric> metrics = metricRepository.findAll();
        
        for (Metric metric : metrics) {
            triggerIngestionForMetric(metric);
        }
        
        System.out.println("Ingestion Cycle Complete.");
    }

    private void triggerIngestionForMetric(Metric metric) {
        String configJson = metric.getIngestionConfigJson();
        if (configJson == null) return;

        try {
            Map<String, Object> config = objectMapper.readValue(configJson, Map.class);
            String adapterId = (String) config.get("adapter");

            if (adapterId != null) {
                Optional<IngestionAdapter> adapterOpt = adapterRegistry.getAdapter(adapterId);
                adapterOpt.ifPresentOrElse(
                    adapter -> {
                        System.out.println("Dispatching job for metric: " + metric.getMetricId() + " to adapter: " + adapterId);
                        adapter.collect(metric);
                    },
                    () -> System.out.println("No adapter found for ID: " + adapterId)
                );
            }
        } catch (Exception e) {
            System.err.println("Error processing metric: " + metric.getMetricId());
            e.printStackTrace();
        }
    }
}
