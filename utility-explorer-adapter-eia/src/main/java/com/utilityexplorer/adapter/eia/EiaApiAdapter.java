package com.utilityexplorer.adapter.eia;

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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

@Component
public class EiaApiAdapter implements IngestionAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final KafkaTemplate<String, IngestionEvent> kafkaTemplate;
    private final com.utilityexplorer.shared.persistence.FactValueRepository factValueRepository;

    @Value("${EIA_API_KEY:}")
    private String apiKey;

    @Value("${ingestion.kafka.topic:raw-utility-data}")
    private String topic;

    public EiaApiAdapter(KafkaTemplate<String, IngestionEvent> kafkaTemplate,
                         com.utilityexplorer.shared.persistence.FactValueRepository factValueRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.factValueRepository = factValueRepository;
    }
    
    // Constructor for testing injects mocked HttpClient? 
    // Ideally we should inject a HttpClient.Builder or factory, but for MVP we use default.
    // This allows subclassing for tests if needed, or we just trust the integration.
    
    /**
     * Collects data from EIA API.
     * 
     * Design Decision (Incremental Loading):
     * Before fetching, we query the FactValueRepository for the MAX(periodStart) for this metric.
     * If found (e.g., 2023-12-01), we ask the API for data starting 2024-01 to avoid re-fetching old data.
     * This reduces API latency and database write load.
     */
    @Override
    public String getAdapterId() {
        return "EIA_API";
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
            String seriesId = config.path("seriesId").asText();
            
            if (seriesId.isEmpty()) {
                System.out.println("Missing 'seriesId' in ingestion config for " + metric.getMetricId());
                return;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("Skipping EIA fetch: EIA_API_KEY not set.");
                return;
            }

            System.out.println("[EIA_API] Fetching data for Series ID: " + seriesId);
            
            // Construct URL (Example for EIA v2)
            // https://api.eia.gov/v2/electricity/retail-sales/data/?api_key=KEY&seriesid=...
            StringBuilder urlBuilder = new StringBuilder("https://api.eia.gov/v2/electricity/retail-sales/data/?")
                    .append("api_key=").append(apiKey)
                    .append("&seriesid=").append(seriesId);

            // Check for incremental load
            LocalDate latestDate = factValueRepository.findLatestPeriodForMetricAndSource(metric.getMetricId(), "EIA");
            if (latestDate != null) {
                // Determine start date for API request (latest + 1 month)
                // EIA API format for start parameter handles YYYY-MM
                YearMonth nextMonth = YearMonth.from(latestDate).plusMonths(1);
                System.out.println("[EIA_API] Found latest data for " + latestDate + ". Fetching from " + nextMonth);
                urlBuilder.append("&start=").append(nextMonth.toString());
            } else {
                System.out.println("[EIA_API] No existing data found. Performing full fetch.");
            }

            String url = urlBuilder.toString();
            
            // Build Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Send Request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("[EIA_API] Success! Processing response...");
                processResponse(metric, response.body());
            } else {
                System.err.println("[EIA_API] Error: " + response.statusCode() + " - " + response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("EIA fetch interrupted for metric: " + metric.getMetricId());
        } catch (Exception e) {
            System.err.println("Failed to fetch EIA data for metric: " + metric.getMetricId());
            e.printStackTrace();
        }
    }

    private void processResponse(Metric metric, String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode dataNode = root.path("response").path("data");
            
            if (dataNode.isMissingNode() || !dataNode.isArray()) {
                 // Fallback for some error responses or structure mismatched
                 System.out.println("No 'response.data' array found in EIA response.");
                 return;
            }

            Iterator<JsonNode> elements = dataNode.elements();
            int count = 0;
            while (elements.hasNext()) {
                JsonNode item = elements.next();
                try {
                    // Extract fields
                    String periodStr = item.path("period").asText(); // "2024-01"
                    double value = item.path("price").asDouble(Double.NaN);
                    if (Double.isNaN(value)) {
                         value = item.path("value").asDouble();
                    }
                    String stateId = item.path("stateid").asText("US");

                    // Parse Date (YYYY-MM)
                    YearMonth ym = YearMonth.parse(periodStr);
                    LocalDate start = ym.atDay(1);
                    LocalDate end = ym.atEndOfMonth();

                    // Create Event
                    IngestionEvent event = new IngestionEvent();
                    event.setMetricId(metric.getMetricId());
                    event.setSourceId("EIA");
                    event.setGeoLevel("STATE");
                    event.setGeoId(stateId);
                    event.setPeriodStart(start);
                    event.setPeriodEnd(end);
                    event.setValue(BigDecimal.valueOf(value));
                    event.setAggregated(false);

                    kafkaTemplate.send(topic, metric.getMetricId(), event);
                    count++;

                } catch (DateTimeParseException dtpe) {
                    System.err.println("Skipping item due to date parse error: " + dtpe.getMessage());
                } catch (Exception ex) {
                    System.err.println("Error publishing event: " + ex.getMessage());
                }
            }
            System.out.println("Published " + count + " events for " + metric.getMetricId());

        } catch (Exception e) {
            System.err.println("Error parsing EIA response: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<com.utilityexplorer.shared.dto.MetricDefinition> getMetricDefinitions() {
        return java.util.List.of(
            new com.utilityexplorer.shared.dto.MetricDefinition(
                "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH",
                "Average retail price of electricity for residential customers. This metric tracks the cost per kilowatt-hour.",
                "cents/kWh",
                "Retail Electricity Price",
                getAdapterId()
            ),
             new com.utilityexplorer.shared.dto.MetricDefinition(
                "ELECTRICITY_TOTAL_SALES_KWH",
                "Total sales of electricity to ultimate customers.",
                "kWh",
                "Electricity Sales",
                getAdapterId()
            )
        );
    }
}

