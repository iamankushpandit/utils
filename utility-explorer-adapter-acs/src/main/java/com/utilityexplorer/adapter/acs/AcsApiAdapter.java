package com.utilityexplorer.adapter.acs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utilityexplorer.shared.adapter.IngestionAdapter;
import com.utilityexplorer.shared.dto.IngestionEvent;
import com.utilityexplorer.shared.persistence.Metric;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;

@Component
public class AcsApiAdapter implements IngestionAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final KafkaTemplate<String, IngestionEvent> kafkaTemplate;

    @Value("${CENSUS_API_KEY:}")
    private String apiKey;

    @Value("${ingestion.kafka.topic:raw-utility-data}")
    private String topic;

    public AcsApiAdapter(KafkaTemplate<String, IngestionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String getAdapterId() {
        return "CENSUS_ACS";
    }

    @Override
    public void collect(Metric metric) {
        String ingestionConfig = metric.getIngestionConfigJson();
        if (ingestionConfig == null || ingestionConfig.isEmpty()) {
            System.out.println("No ingestion config for metric: " + metric.getMetricId());
            return;
        }

        try {
            JsonNode config = objectMapper.readTree(ingestionConfig);
            String variable = config.path("variable").asText();
            
            // Default to 2021 ACS 5-year estimates if not specified
            int year = config.has("year") ? config.get("year").asInt() : 2021; 

            if (variable.isEmpty()) {
                System.out.println("Missing 'variable' in ingestion config for " + metric.getMetricId());
                return;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("Skipping ACS fetch: CENSUS_API_KEY not set.");
                return;
            }

            System.out.println("[ACS_API] Fetching data for Variable: " + variable + ", Year: " + year);
            
            // Construct URL for state-level data
            // https://api.census.gov/data/{year}/acs/acs5?get=NAME,{variable}&for=state:*&key={key}
            String url = String.format("https://api.census.gov/data/%d/acs/acs5?get=NAME,%s&for=state:*&key=%s", 
                                     year, variable, apiKey);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[ACS_API] Success! Processing response...");
                processResponse(metric, response.body(), year);
            } else {
                System.err.println("[ACS_API] Error: " + response.statusCode() + " - " + response.body());
            }

        } catch (Exception e) {
            System.err.println("Failed to fetch ACS data for metric: " + metric.getMetricId());
            e.printStackTrace();
        }
    }

    private void processResponse(Metric metric, String jsonBody, int year) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            
            if (!root.isArray() || root.size() < 2) {
                 System.out.println("Unexpected ACS response format (expected array of arrays).");
                 return;
            }

            // Census API returns:
            // [ ["NAME", "B25024_001E", "state"],
            //   ["Alabama", "150", "01"], ... ]
            
            // Find index of variable and state code from header row (index 0)
            JsonNode header = root.get(0);
            int valueIndex = -1;
            int stateIndex = -1;
            
            for (int i = 0; i < header.size(); i++) {
                String headerCol = header.get(i).asText();
                if (headerCol.equalsIgnoreCase("state")) {
                    stateIndex = i;
                } else if (!headerCol.equalsIgnoreCase("NAME") && !headerCol.equalsIgnoreCase("state")) {
                    // Assuming the variable requested is the value column
                    valueIndex = i;
                }
            }

            if (valueIndex == -1 || stateIndex == -1) {
                System.err.println("Could not find value or state column in ACS response.");
                return;
            }

            int count = 0;
            // Iterate data rows (skip header)
            for (int i = 1; i < root.size(); i++) {
                JsonNode row = root.get(i);
                try {
                    String stateFips = row.get(stateIndex).asText();
                    double value = row.get(valueIndex).asDouble(Double.NaN);

                    if (Double.isNaN(value) || value < 0) { 
                        // ACS sometimes returns -666666666 for missing data
                        continue; 
                    }

                    IngestionEvent event = new IngestionEvent();
                    event.setMetricId(metric.getMetricId());
                    event.setSourceId("CENSUS_ACS");
                    event.setGeoLevel("STATE");
                    event.setGeoId(stateFips); // FIPS code e.g. "01"
                    
                    // ACS 5-year estimate covers a range, but typically referenced by the end year
                    // For simplicity in this demo, mapping to the full year period
                    event.setPeriodStart(LocalDate.of(year, 1, 1));
                    event.setPeriodEnd(LocalDate.of(year, 12, 31));
                    
                    event.setValue(BigDecimal.valueOf(value));
                    event.setAggregated(true);
                    event.setAggregationMethod("MEDIAN"); // Or whatever the variable represents, hardcoded for now

                    kafkaTemplate.send(topic, metric.getMetricId(), event);
                    count++;
                } catch (Exception ex) {
                    System.err.println("Error publishing ACS event: " + ex.getMessage());
                }
            }
            System.out.println("Published " + count + " events for " + metric.getMetricId());

        } catch (Exception e) {
            System.err.println("Error parsing ACS response: " + e.getMessage());
        }
    }
}


