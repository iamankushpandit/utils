package com.utilityexplorer.utilagent;

import jakarta.annotation.PostConstruct;
import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import com.utilityexplorer.shared.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UtilAgentService {
    
    private static final String ELECTRICITY_PRICE = "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH";
    private static final String ACS_ELECTRICITY_COST = "ELECTRICITY_MONTHLY_COST_USD_ACS";

    private static final Map<String, List<String>> METRIC_KEYWORDS = createMetricKeywordMap();
    private static final Map<String, String> METRIC_SOURCES = Map.of(
        ELECTRICITY_PRICE, "EIA",
        ACS_ELECTRICITY_COST, "CENSUS_ACS"
    );

    @Autowired
    private FactValueRepository factValueRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private SourceRepository sourceRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    @Autowired
    private UserQueryRepository userQueryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${intelligence.url:http://localhost:8092}")
    private String intelligenceUrl;

    private final Map<String, Metric> metricCache = new LinkedHashMap<>();
    
    @PostConstruct
    private void initMetricCache() {
        metricRepository.findAll().forEach(metric -> metricCache.put(metric.getMetricId(), metric));
    }
    
    public UtilAgentResponse processQuery(String question) {
        /**
         * Hybrid Architectue Decision:
         * 1. Primary: Logic-driven "Intelligence Service" (Python/FastAPI) which processes natural language
         *    and returns answers using RAG or specialized models.
         * 2. Fallback: If Python service fails or is disabled, falls back to internal 'deterministic' Java logic
         *    (regex parsing) to ensure basic functionality (uptime reliability).
         */
        // Try Python Intelligence Service first
        if (intelligenceUrl != null && !intelligenceUrl.isEmpty()) {
            try {
                return callIntelligenceService(question);
            } catch (Exception e) {
                System.err.println("Warning: Intelligence service unavailable (" + e.getMessage() + "). Falling back to deterministic logic.");
            }
        }

        // Fallback to internal logic
        UtilAgentResponse response;
        QuerySpec querySpec = parseQuestion(question);
        if (querySpec == null) {
            response = createInsufficientDataResponse("Unable to understand the query");
        } else {
            response = executeQuery(querySpec);
        }
        enrichAndLogResponse(response, question, querySpec);
        return response;
    }

    private UtilAgentResponse callIntelligenceService(String question) {
        String url = intelligenceUrl + "/query";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> body = new HashMap<>();
        body.put("question", question);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        
        // Response format matching Python Pydantic model
        // { "answer": "...", "sources": [], "visualization": {} }
        Map<String, Object> pythonResponse = restTemplate.postForObject(url, request, Map.class);
        
        if (pythonResponse == null) {
            throw new RuntimeException("Empty response from intelligence service");
        }

        UtilAgentResponse response = new UtilAgentResponse();
        response.setSummary((String) pythonResponse.get("answer"));
        response.setStatus("OK");
        
        // Simple mapping for Story 2 - will be enriched in future stories
        response.setResponseOrigin("intelligence-service-v1");
        response.setConfidence(1.0); 
        
        // Map sources if present
        if (pythonResponse.get("sources") instanceof List) {
           // Basic string list mapping for now, can be expanded
        }
        
        // Capture query for history
        enrichAndLogResponse(response, question, null);
        
        return response;
    }

    public void submitFeedback(Long queryId, String feedback) {
        userQueryRepository.findById(queryId).ifPresent(userQuery -> {
            userQuery.setFeedback(feedback);
            userQueryRepository.save(userQuery);
        });
    }

    private void enrichAndLogResponse(UtilAgentResponse response, String question, QuerySpec querySpec) {
        response.setResponseTimestamp(java.time.Instant.now().toString());
        if (response.getResponseOrigin() == null) {
            response.setResponseOrigin("deterministic");
        }
        if (response.getConfidence() == null) {
            response.setConfidence(1.0);
        }
        if (response.getDisclaimer() == null) {
            response.setDisclaimer("Generated from structured database facts.");
        }

        try {
            UserQuery userQuery = new UserQuery(question, false);
            if (querySpec != null && querySpec.getMetrics() != null && !querySpec.getMetrics().isEmpty()) {
                MetricSpec first = querySpec.getMetrics().get(0);
                if (first != null) {
                    userQuery.setMetricId(first.getMetricId());
                    userQuery.setSourceId(first.getSourceId());
                }
            }
            userQueryRepository.save(userQuery);
            if (userQuery.getId() != null) {
                response.setQueryId(userQuery.getId());
            }
        } catch (Exception e) {
            // Non-blocking logging failure
            System.err.println("Failed to log user query: " + e.getMessage());
        }
    }
    
    private QuerySpec parseQuestion(String question) {
        String normalized = question.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        List<String> metrics = METRIC_KEYWORDS.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(normalized::contains))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if (metrics.isEmpty()) {
            if (normalized.contains("cent") && normalized.contains("kwh")) {
                metrics.add(ELECTRICITY_PRICE);
            } else {
                return null; // No supported metric keywords found
            }
        }

        List<MetricSpec> metricSpecs = metrics.stream()
            .map(this::createMetricSpecForId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (metricSpecs.isEmpty()) {
            return null;
        }

        QuerySpec spec = new QuerySpec();
        spec.setQueryType("INTERSECTION");
        spec.setGeoLevel("STATE");
        spec.setLimit(20);
        
        TimeSpec time = new TimeSpec();
        time.setMode("LATEST_COMMON");
        spec.setTime(time);
        spec.setMetrics(metricSpecs);

        return spec;
    }

    private MetricSpec createMetricSpecForId(String metricId) {
        String sourceId = METRIC_SOURCES.get(metricId);
        if (sourceId == null) return null;
        MetricSpec spec = new MetricSpec();
        spec.setMetricId(metricId);
        spec.setSourceId(sourceId);
        return spec;
    }

    private UtilAgentResponse executeQuery(QuerySpec querySpec) {
        if ("INTERSECTION".equals(querySpec.getQueryType())) {
            return executeIntersectionQuery(querySpec);
        }
        return createInsufficientDataResponse("Query type not supported");
    }

    private UtilAgentResponse executeIntersectionQuery(QuerySpec querySpec) {
        if (querySpec.getMetrics() == null || querySpec.getMetrics().isEmpty()) {
            return createInsufficientDataResponse("No metrics requested");
        }

        Map<String, MetricSpec> specByMetric = querySpec.getMetrics().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(MetricSpec::getMetricId, Function.identity(), (first, second) -> first, LinkedHashMap::new));

        Map<String, Map<String, FactValue>> metricValues = new LinkedHashMap<>();
        Map<String, FactValue> sampleFactByMetric = new LinkedHashMap<>();
        Map<String, LocalDate> latestPeriods = new LinkedHashMap<>();
        Map<String, Source> citations = new LinkedHashMap<>();

        for (Map.Entry<String, MetricSpec> entry : specByMetric.entrySet()) {
            String metricId = entry.getKey();
            MetricSpec spec = entry.getValue();
            String sourceId = spec.getSourceId();

            LocalDate latestStart = factValueRepository.findLatestPeriodForMetricAndSource(metricId, sourceId);
            if (latestStart == null) {
                continue;
            }

            List<FactValue> facts = factValueRepository.findByMetricSourceGeoLevelAndPeriod(
                metricId, sourceId, querySpec.getGeoLevel(), latestStart);
            if (facts.isEmpty()) {
                continue;
            }

            Map<String, FactValue> factByGeo = facts.stream()
                .collect(Collectors.toMap(FactValue::getGeoId, Function.identity()));
            metricValues.put(metricId, factByGeo);
            sampleFactByMetric.put(metricId, facts.get(0));
            latestPeriods.put(metricId, latestStart);

            sourceRepository.findById(sourceId).ifPresent(source -> {
                citations.putIfAbsent(sourceId, source);
            });
        }

        if (metricValues.isEmpty()) {
            return createInsufficientDataResponse("No data available for requested metrics");
        }

        List<String> columns = new ArrayList<>();
        columns.add("State");
        metricValues.keySet().forEach(metricId -> {
            Metric metric = metricCache.get(metricId);
            String label = metric != null
                ? String.format("%s (%s)", metric.getName(), metric.getUnit())
                : metricId;
            columns.add(label);
        });

        Set<String> geoIds = metricValues.values().stream()
            .flatMap(map -> map.keySet().stream())
            .collect(Collectors.toCollection(TreeSet::new));

        List<List<Object>> rows = new ArrayList<>();
        List<HighlightRegion> highlights = new ArrayList<>();
        int limit = querySpec.getLimit() != null ? querySpec.getLimit() : 20;

        for (String geoId : geoIds) {
            if (rows.size() >= limit) break;
            Optional<Region> regionOpt = regionRepository.findByGeoLevelAndGeoId(querySpec.getGeoLevel(), geoId);
            String label = regionOpt.map(Region::getName).orElse(geoId);

            List<Object> row = new ArrayList<>();
            row.add(label);
            for (String metricId : metricValues.keySet()) {
                FactValue fact = metricValues.get(metricId).get(geoId);
                row.add(fact != null ? fact.getValueNumeric().doubleValue() : "N/A");
            }
            rows.add(row);
            highlights.add(new HighlightRegion(querySpec.getGeoLevel(), geoId));
        }

        UtilAgentResponse response = new UtilAgentResponse();
        response.setStatus("OK");

        List<String> metricNames = metricValues.keySet().stream()
            .map(id -> Optional.ofNullable(metricCache.get(id)).map(Metric::getName).orElse(id))
            .collect(Collectors.toList());
        response.setSummary(buildSummary(metricNames));

        LocalDate overallStart = latestPeriods.values().stream()
            .max(LocalDate::compareTo)
            .orElse(null);
        String overallEnd = null;
        if (overallStart != null) {
            for (String metricId : latestPeriods.keySet()) {
                if (overallStart.equals(latestPeriods.get(metricId))) {
                    FactValue sample = sampleFactByMetric.get(metricId);
                    if (sample != null && sample.getPeriodEnd() != null) {
                        overallEnd = sample.getPeriodEnd().toString();
                        break;
                    }
                }
            }
        }
        if (overallStart != null) {
            response.setPeriod(new PeriodInfo(overallStart.toString(),
                overallEnd != null ? overallEnd : overallStart.toString()));
        }

        response.setTable(new TableResult(columns, rows));
        response.setHighlightRegions(highlights);

        List<CitationInfo> citationList = new ArrayList<>();
        Set<String> seenSources = new HashSet<>();
        for (String metricId : metricValues.keySet()) {
            MetricSpec spec = specByMetric.get(metricId);
            if (spec == null) continue;
            String sourceId = spec.getSourceId();
            if (seenSources.contains(sourceId)) continue;
            Source source = citations.get(sourceId);
            FactValue sample = sampleFactByMetric.get(metricId);
            if (source != null && sample != null) {
                citationList.add(new CitationInfo(sourceId, sample.getRetrievedAt().toString(), source.getTermsUrl()));
                seenSources.add(sourceId);
            }
        }
        response.setCitations(citationList);

        List<String> notes = new ArrayList<>();
        latestPeriods.forEach((metricId, startDate) -> {
            FactValue sample = sampleFactByMetric.get(metricId);
            Metric metric = metricCache.get(metricId);
            String metricLabel = metric != null ? metric.getName() : metricId;
            String endDate = sample != null && sample.getPeriodEnd() != null
                ? sample.getPeriodEnd().toString()
                : startDate.toString();
            notes.add(String.format("Latest %s data covers %s to %s.", metricLabel, startDate, endDate));
        });
        notes.add("Values are provided for the available regions; missing data is shown as N/A.");
        response.setNotes(notes);

        return response;
    }

    private String buildSummary(List<String> metricNames) {
        if (metricNames.isEmpty()) {
            return "Latest data for requested metrics";
        }
        if (metricNames.size() == 1) {
            return String.format("Latest data for %s by state", metricNames.get(0));
        }
        String joined = String.join(", ", metricNames.subList(0, metricNames.size() - 1));
        return String.format("Latest data for %s and %s by state",
            joined, metricNames.get(metricNames.size() - 1));
    }

    private static Map<String, List<String>> createMetricKeywordMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put(ELECTRICITY_PRICE, Arrays.asList(
            "electricity price", "retail price", "cents per kwh", "cent kwh", "cent per kwh",
            "electricity retail", "electricity"));
        map.put(ACS_ELECTRICITY_COST, Arrays.asList(
            "monthly electricity cost", "electricity cost", "acs", "census", "monthly cost", "electricity"));
        return map;
    }

    private UtilAgentResponse createInsufficientDataResponse(String message) {
        UtilAgentResponse response = new UtilAgentResponse();
        response.setStatus("INSUFFICIENT_DATA");
        response.setSummary(message);
        response.setNotes(Arrays.asList("Unable to process query with available data"));
        return response;
    }

    public void captureFeedback(Long queryId, String feedback) {
        userQueryRepository.findById(queryId).ifPresent(userQuery -> {
            userQuery.setFeedback(feedback);
            userQueryRepository.save(userQuery);
        });
    }
}
