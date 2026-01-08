package com.utilityexplorer.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utilityexplorer.persistence.FactValue;
import com.utilityexplorer.persistence.FactValueRepository;
import com.utilityexplorer.persistence.Region;
import com.utilityexplorer.persistence.RegionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CensusAcsElectricityCostSourcePlugin implements SourcePlugin {

    private static final Logger logger = LoggerFactory.getLogger(CensusAcsElectricityCostSourcePlugin.class);

    private static final String SOURCE_ID = "CENSUS_ACS";
    private static final String METRIC_ID = "ELECTRICITY_MONTHLY_COST_USD_ACS";
    private static final String DATASET = "acs/acs5";

    private static final String[] BUCKET_FIELDS = {
        "B25132_004E",
        "B25132_005E",
        "B25132_006E",
        "B25132_007E",
        "B25132_008E",
        "B25132_009E"
    };

    private static final double[] BUCKET_MIDPOINTS = {
        25.0, 75.0, 125.0, 175.0, 225.0, 275.0
    };

    @Autowired
    private FactValueRepository factValueRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Value("${CENSUS_API_KEY:}")
    private String apiKey;

    @Value("${CENSUS_ACS_MAX_YEAR:#{T(java.time.Year).now().value}}")
    private int maxYear;
    
    @Value("${CENSUS_ACS_MIN_YEAR:#{T(java.time.Year).now().value - 5}}")
    private int minYear;
    
    @Value("${CENSUS_ACS_YEARS_BACK:6}")
    private int yearsBack;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Region> regionCache = new HashMap<>();

    @PostConstruct
    public void logInit() {
        logger.info("Initialized Census ACS electricity cost source plugin (apiKey configured: {}, year range {}-{})",
            apiKey != null && !apiKey.isBlank(),
            minYear,
            maxYear);
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public SourceCheckResult checkForUpdates(SourceContext ctx) {
        return new SourceCheckResult(true, "acs-" + ctx.now.toEpochMilli(), ctx.now);
    }

    @Override
    public IngestResult ingest(SourceContext ctx, SourceCheckResult check) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("CENSUS_API_KEY is not configured");
        }

        int currentYear = LocalDate.now(ctx.clock).getYear();
        int preferredEnd = Math.min(currentYear - 1, maxYear);
        int endYear = findLatestAvailableYear(preferredEnd);
        int startYear = Math.max(minYear, endYear - yearsBack);

        if (startYear > endYear) {
            logger.warn("Configured ACS year range is invalid (start: {}, end: {}). Nothing to ingest.", startYear, endYear);
            return new IngestResult(0, UUID.randomUUID(), true);
        }

        logger.info("Ingesting ACS data for years {}..{}", startYear, endYear);
        int rows = 0;

        for (int year = startYear; year <= endYear; year++) {
            rows += ingestYear(ctx, check, year, "STATE");
            rows += ingestYear(ctx, check, year, "COUNTY");
            rows += ingestYear(ctx, check, year, "PLACE");
        }

        logger.info("ACS ingestion completed with {} row(s) upserted", rows);
        return new IngestResult(rows, UUID.randomUUID(), rows == 0);
    }

    private int findLatestAvailableYear(int preferredEnd) {
        int earliest = Math.max(1990, minYear); // guardrails to avoid runaway loops

        for (int year = preferredEnd; year >= earliest; year--) {
            try {
                URI uri = buildRequestUri(year, "STATE");
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404 || response.statusCode() == 400) {
                    continue; // not published yet
                }
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    if (root.isArray() && root.size() > 1) {
                        logger.info("Detected latest available ACS year from API: {}", year);
                        return year;
                    }
                } else {
                    logger.warn("Census API probe for year {} returned status {}", year, response.statusCode());
                }
            } catch (Exception ex) {
                logger.warn("Failed probe for ACS year {}: {}", year, ex.getMessage());
            }
        }

        logger.warn("Could not detect a published ACS year; falling back to preferred end year {}", preferredEnd);
        return preferredEnd;
    }

    private int ingestYear(SourceContext ctx, SourceCheckResult check, int year, String geoLevel) throws Exception {
        URI requestUri = buildRequestUri(year, geoLevel);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(requestUri)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404 || response.statusCode() == 400) {
            logger.warn("ACS data not published for year {} and geo level {}, skipping", year, geoLevel);
            return 0;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Census API error (" + geoLevel + " " + year + "): " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray() || root.size() < 2) {
            return 0;
        }

        Map<String, Integer> index = new HashMap<>();
        JsonNode header = root.get(0);
        for (int i = 0; i < header.size(); i++) {
            index.put(header.get(i).asText(), i);
        }

        int rows = 0;
        for (int i = 1; i < root.size(); i++) {
            JsonNode row = root.get(i);
            String name = readValue(row, index, "NAME");
            String stateRaw = readValue(row, index, "state");
            if (stateRaw == null) {
                continue;
            }

            String stateFips = formatFips(stateRaw, 2);
            String geoId;
            if ("COUNTY".equals(geoLevel)) {
                String countyRaw = readValue(row, index, "county");
                if (countyRaw == null) {
                    continue;
                }
                geoId = stateFips + formatFips(countyRaw, 3);
            } else if ("PLACE".equals(geoLevel)) {
                String placeRaw = readValue(row, index, "place");
                if (placeRaw == null) {
                    continue;
                }
                geoId = stateFips + formatFips(placeRaw, 5);
            } else {
                geoId = stateFips;
            }

            double total = 0.0;
            double weighted = 0.0;
            for (int b = 0; b < BUCKET_FIELDS.length; b++) {
                double count = parseNumber(readValue(row, index, BUCKET_FIELDS[b]));
                total += count;
                weighted += count * BUCKET_MIDPOINTS[b];
            }

            if (total <= 0.0) {
                continue;
            }

            double average = weighted / total;

            ensureRegion(geoLevel, geoId, name, stateFips);

            LocalDate periodStart = LocalDate.of(year, 1, 1);
            LocalDate periodEnd = LocalDate.of(year, 12, 31);

            FactValue fact = new FactValue();
            fact.setMetricId(METRIC_ID);
            fact.setSourceId(SOURCE_ID);
            fact.setGeoLevel(geoLevel);
            fact.setGeoId(geoId);
            fact.setPeriodStart(periodStart);
            fact.setPeriodEnd(periodEnd);
            fact.setValueNumeric(BigDecimal.valueOf(average));
            fact.setRetrievedAt(ctx.now);
            fact.setSourcePublishedAt(check.sourcePublishedAt);
            fact.setIsAggregated(true);
            fact.setAggregationMethod("WEIGHTED_BIN_AVERAGE");

            factValueRepository.save(fact);
            rows++;
        }

        return rows;
    }

    private void ensureRegion(String geoLevel, String geoId, String name, String stateFips) {
        String key = geoLevel + ":" + geoId;
        if (regionCache.containsKey(key)) {
            return;
        }

        Region region = regionRepository.findByGeoLevelAndGeoId(geoLevel, geoId).orElse(null);
        if (region == null) {
            Region parent = regionRepository.findByGeoLevelAndGeoId("STATE", stateFips).orElse(null);
            region = new Region(
                UUID.randomUUID(),
                geoLevel,
                geoId,
                name != null && !name.isBlank() ? name : "Unknown",
                parent != null ? parent.getRegionPk() : null,
                null,
                null
            );
            regionRepository.save(region);
        }

        regionCache.put(key, region);
    }

    private URI buildRequestUri(int year, String geoLevel) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "get", buildGetFields());
        appendParam(query, "for", buildForClause(geoLevel));
        if (!"STATE".equals(geoLevel)) {
            appendParam(query, "in", "state:*");
        }
        appendParam(query, "key", apiKey);

        String uri = "https://api.census.gov/data/" + year + "/" + DATASET + "?" + query;
        return URI.create(uri);
    }

    private String buildGetFields() {
        StringBuilder fields = new StringBuilder("NAME");
        for (String field : BUCKET_FIELDS) {
            fields.append(",").append(field);
        }
        return fields.toString();
    }

    private String buildForClause(String geoLevel) {
        return switch (geoLevel) {
            case "COUNTY" -> "county:*";
            case "PLACE" -> "place:*";
            default -> "state:*";
        };
    }

    private String readValue(JsonNode row, Map<String, Integer> index, String key) {
        Integer idx = index.get(key);
        if (idx == null || idx >= row.size()) {
            return null;
        }
        return row.get(idx).asText();
    }

    private double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String formatFips(String value, int width) {
        try {
            int number = Integer.parseInt(value);
            return String.format("%0" + width + "d", number);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void appendParam(StringBuilder query, String name, String value) {
        if (query.length() > 0) {
            query.append("&");
        }
        query.append(encode(name)).append("=").append(encode(value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
