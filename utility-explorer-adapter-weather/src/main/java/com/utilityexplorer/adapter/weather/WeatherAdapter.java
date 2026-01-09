package com.utilityexplorer.adapter.weather;

import com.utilityexplorer.shared.adapter.IngestionAdapter;
import com.utilityexplorer.shared.dto.IngestionEvent;
import com.utilityexplorer.shared.dto.MetricDefinition;
import com.utilityexplorer.shared.persistence.Metric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
public class WeatherAdapter implements IngestionAdapter {

    private final KafkaTemplate<String, IngestionEvent> kafkaTemplate;
    private final Random random = new Random();

    @Value("${ingestion.kafka.topic:raw-utility-data}")
    private String topic;

    private static final java.util.Map<String, String> STATE_FIPS = new java.util.HashMap<>();
    static {
        STATE_FIPS.put("AL", "01"); STATE_FIPS.put("AK", "02"); STATE_FIPS.put("AZ", "04"); STATE_FIPS.put("AR", "05");
        STATE_FIPS.put("CA", "06"); STATE_FIPS.put("CO", "08"); STATE_FIPS.put("CT", "09"); STATE_FIPS.put("DE", "10");
        STATE_FIPS.put("FL", "12"); STATE_FIPS.put("GA", "13"); STATE_FIPS.put("HI", "15"); STATE_FIPS.put("ID", "16");
        STATE_FIPS.put("IL", "17"); STATE_FIPS.put("IN", "18"); STATE_FIPS.put("IA", "19"); STATE_FIPS.put("KS", "20");
        STATE_FIPS.put("KY", "21"); STATE_FIPS.put("LA", "22"); STATE_FIPS.put("ME", "23"); STATE_FIPS.put("MD", "24");
        STATE_FIPS.put("MA", "25"); STATE_FIPS.put("MI", "26"); STATE_FIPS.put("MN", "27"); STATE_FIPS.put("MS", "28");
        STATE_FIPS.put("MO", "29"); STATE_FIPS.put("MT", "30"); STATE_FIPS.put("NE", "31"); STATE_FIPS.put("NV", "32");
        STATE_FIPS.put("NH", "33"); STATE_FIPS.put("NJ", "34"); STATE_FIPS.put("NM", "35"); STATE_FIPS.put("NY", "36");
        STATE_FIPS.put("NC", "37"); STATE_FIPS.put("ND", "38"); STATE_FIPS.put("OH", "39"); STATE_FIPS.put("OK", "40");
        STATE_FIPS.put("OR", "41"); STATE_FIPS.put("PA", "42"); STATE_FIPS.put("RI", "44"); STATE_FIPS.put("SC", "45");
        STATE_FIPS.put("SD", "46"); STATE_FIPS.put("TN", "47"); STATE_FIPS.put("TX", "48"); STATE_FIPS.put("UT", "49");
        STATE_FIPS.put("VT", "50"); STATE_FIPS.put("VA", "51"); STATE_FIPS.put("WA", "53"); STATE_FIPS.put("WV", "54");
        STATE_FIPS.put("WI", "55"); STATE_FIPS.put("WY", "56");
    }

    public WeatherAdapter(KafkaTemplate<String, IngestionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String getAdapterId() {
        return "WEATHER_INSIGHTS";
    }

    @Override
    public List<MetricDefinition> getMetricDefinitions() {
        return List.of(
            new MetricDefinition("WEATHER_STRESS_INDEX", "Composite index of weather stress on infrastructure", "Index", "Weather Stress Index", "WEATHER_INSIGHTS"),
            new MetricDefinition("TEMP_CURRENT_F", "Current temperature in Fahrenheit", "F", "Current Temperature", "WEATHER_INSIGHTS"),
            new MetricDefinition("TEMP_ANOMALY_F", "Deviation from historical average temperature", "F", "Temperature Anomaly", "WEATHER_INSIGHTS")
        );
    }

    @Override
    public void collect(Metric metric) {
        System.out.println("WeatherAdapter: Collecting data for " + metric.getMetricId());

        // Publish NATIONAL total
        publishEvent(metric, "NATIONAL", "US-TOTAL");

        // Publish STATE values
        for (String state : STATE_FIPS.keySet()) {
             publishEvent(metric, "STATE", STATE_FIPS.get(state));
        }
    }

    private void publishEvent(Metric metric, String geoLevel, String geoId) {
        BigDecimal value = generateMockValue(metric.getMetricId());
        
        if (value == null) {
            System.out.println("WeatherAdapter: No mock logic for metric " + metric.getMetricId());
            return;
        }

        IngestionEvent event = new IngestionEvent();
        event.setMetricId(metric.getMetricId());
        event.setSourceId("WEATHER_INSIGHTS_API");
        event.setGeoLevel(geoLevel);
        event.setGeoId(geoId);
        event.setPeriodStart(LocalDate.now());
        event.setPeriodEnd(LocalDate.now());
        event.setValue(value);
        // event.setUnit(metric.getUnit()); // Not in DTO
        // event.setIngestionTimestamp(Instant.now().toString()); // Not in DTO

        if ("US-TOTAL".equals(geoId) || "01".equals(geoId)) {
             System.out.println("WeatherAdapter: Publishing event: " + event + " (GeoID: " + geoId + ")");
        }
        
        kafkaTemplate.send(topic, event);
    }

    private BigDecimal generateMockValue(String metricId) {
        switch (metricId) {
            case "WEATHER_STRESS_INDEX":
                // 0.0 to 1.0
                return BigDecimal.valueOf(random.nextDouble());
            case "TEMP_CURRENT_F":
                // 20.0 to 100.0
                return BigDecimal.valueOf(20.0 + (80.0 * random.nextDouble()));
            case "TEMP_ANOMALY_F":
                // -10.0 to +10.0
                return BigDecimal.valueOf(-10.0 + (20.0 * random.nextDouble()));
            default:
                return null;
        }
    }
}
