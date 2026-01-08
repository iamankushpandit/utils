package com.utilityexplorer.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utilityexplorer.persistence.FactValue;
import com.utilityexplorer.persistence.FactValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jakarta.annotation.PostConstruct;

@Component
public class EiaRetailPriceSourcePlugin implements SourcePlugin {

    private static final Map<String, String> STATE_ABBR_TO_FIPS = createStateMap();
    private static final Logger logger = LoggerFactory.getLogger(EiaRetailPriceSourcePlugin.class);

    @Autowired
    private FactValueRepository factValueRepository;

    @Value("${EIA_API_KEY:}")
    private String apiKey;
    @Value("${EIA_MONTHS_BACK:72}")
    private int monthsBack;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void logInit() {
        logger.info("Initialized EIA retail price source plugin (apiKey configured: {})",
            apiKey != null && !apiKey.isBlank());
    }

    @Override
    public String getSourceId() {
        return "EIA";
    }

    @Override
    public SourceCheckResult checkForUpdates(SourceContext ctx) {
        return new SourceCheckResult(true, "eia-" + ctx.now.toEpochMilli(), ctx.now);
    }

    @Override
    public IngestResult ingest(SourceContext ctx, SourceCheckResult check) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("EIA_API_KEY is not configured");
        }

        YearMonth end = YearMonth.from(LocalDate.now(ctx.clock));
        int window = Math.max(1, monthsBack);
        YearMonth start = end.minusMonths(window - 1);

        URI requestUri = buildRequestUri(start, end);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(requestUri)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("EIA API error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("response").path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("EIA API response missing data");
        }

        int rows = 0;
        for (JsonNode row : data) {
            String sector = row.path("sectorid").asText();
            if (!"ALL".equalsIgnoreCase(sector)) {
                continue;
            }

            String state = row.path("stateid").asText();
            String fips = STATE_ABBR_TO_FIPS.get(state);
            if (fips == null) {
                continue;
            }

            String period = row.path("period").asText();
            if (period == null || period.isBlank()) {
                continue;
            }

            String priceText = row.path("price").asText();
            if (priceText == null || priceText.isBlank()) {
                continue;
            }

            BigDecimal price = new BigDecimal(priceText);
            YearMonth ym = YearMonth.parse(period);
            LocalDate periodStart = ym.atDay(1);
            LocalDate periodEnd = ym.atEndOfMonth();

            FactValue fact = new FactValue();
            fact.setMetricId("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH");
            fact.setSourceId("EIA");
            fact.setGeoLevel("STATE");
            fact.setGeoId(fips);
            fact.setPeriodStart(periodStart);
            fact.setPeriodEnd(periodEnd);
            fact.setValueNumeric(price);
            fact.setRetrievedAt(ctx.now);
            fact.setSourcePublishedAt(check.sourcePublishedAt);
            fact.setIsAggregated(false);

            factValueRepository.save(fact);
            rows++;
        }

        logger.info("EIA ingestion completed with {} row(s) upserted", rows);
        return new IngestResult(rows, UUID.randomUUID(), rows == 0);
    }

    private URI buildRequestUri(YearMonth start, YearMonth end) {
        StringBuilder query = new StringBuilder();
        appendParam(query, "api_key", apiKey);
        appendParam(query, "data[0]", "price");
        appendParam(query, "frequency", "monthly");
        appendParam(query, "facets[sectorid][]", "ALL");
        appendParam(query, "start", start.toString());
        appendParam(query, "end", end.toString());
        appendParam(query, "length", "5000");

        String uri = "https://api.eia.gov/v2/electricity/retail-sales/data/?" + query;
        return URI.create(uri);
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

    private static Map<String, String> createStateMap() {
        Map<String, String> map = new HashMap<>();
        map.put("AL", "01");
        map.put("AK", "02");
        map.put("AZ", "04");
        map.put("AR", "05");
        map.put("CA", "06");
        map.put("CO", "08");
        map.put("CT", "09");
        map.put("DE", "10");
        map.put("DC", "11");
        map.put("FL", "12");
        map.put("GA", "13");
        map.put("HI", "15");
        map.put("ID", "16");
        map.put("IL", "17");
        map.put("IN", "18");
        map.put("IA", "19");
        map.put("KS", "20");
        map.put("KY", "21");
        map.put("LA", "22");
        map.put("ME", "23");
        map.put("MD", "24");
        map.put("MA", "25");
        map.put("MI", "26");
        map.put("MN", "27");
        map.put("MS", "28");
        map.put("MO", "29");
        map.put("MT", "30");
        map.put("NE", "31");
        map.put("NV", "32");
        map.put("NH", "33");
        map.put("NJ", "34");
        map.put("NM", "35");
        map.put("NY", "36");
        map.put("NC", "37");
        map.put("ND", "38");
        map.put("OH", "39");
        map.put("OK", "40");
        map.put("OR", "41");
        map.put("PA", "42");
        map.put("RI", "44");
        map.put("SC", "45");
        map.put("SD", "46");
        map.put("TN", "47");
        map.put("TX", "48");
        map.put("UT", "49");
        map.put("VT", "50");
        map.put("VA", "51");
        map.put("WA", "53");
        map.put("WV", "54");
        map.put("WI", "55");
        map.put("WY", "56");
        return map;
    }
}
