package com.utilityexplorer.ingestion.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.utilityexplorer.shared.persistence.Metric;
import com.utilityexplorer.shared.persistence.MetricRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads the canonical "metrics.yaml" on startup and ensures the DB reflects the defined Metrics/Ingestion Configs.
 * This is the implementation of "Configuration-as-Code driven extensibility".
 */
@Component
public class YamlConfigLoader {

    @Value("classpath:config/metrics.yaml")
    private Resource metricsConfig;

    @Autowired
    private MetricRepository metricRepository;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    public void loadConfig() throws IOException {
        if (!metricsConfig.exists()) {
            System.out.println("No metrics.yaml found in classpath:config/");
            return;
        }

        Map<String, Object> config = yamlMapper.readValue(metricsConfig.getInputStream(), new TypeReference<>() {});
        System.out.println("Loaded Metadata Config: " + config.keySet());

        List<Map<String, Object>> metrics = (List<Map<String, Object>>) config.getOrDefault("metrics", Collections.emptyList());
        
        for (Map<String, Object> metricMap : metrics) {
            String id = (String) metricMap.get("id");
            String name = (String) metricMap.get("name");
            String category = (String) metricMap.get("category");
            
            // Basic fields
            Metric metric = metricRepository.findById(id).orElse(new Metric());
            metric.setMetricId(id);
            metric.setName(name);
            metric.setCategory(category);
            
            // Set defaults for now if new
            if (metric.getUnit() == null) metric.setUnit("N/A"); 
            if (metric.getDefaultGranularity() == null) metric.setDefaultGranularity("MONTHLY");
            if (metric.getSupportedGeoLevels() == null) metric.setSupportedGeoLevels("NATIONAL,STATE,COUNTY");

            // Extract sub-objects for JSON columns
            Map<String, Object> visualization = (Map<String, Object>) metricMap.get("visualization");
            Map<String, Object> ingestion = (Map<String, Object>) metricMap.get("ingestion");
            
            try {
                if (visualization != null) {
                    metric.setVisualizationJson(jsonMapper.writeValueAsString(visualization));
                }
                if (ingestion != null) {
                    metric.setIngestionConfigJson(jsonMapper.writeValueAsString(ingestion));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            metricRepository.save(metric);
            System.out.println("Upserted Metric: " + id);
        }
    }
}
