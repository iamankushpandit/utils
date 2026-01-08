package com.utilityexplorer.utilagent;

import jakarta.annotation.PostConstruct;
import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private final Map<String, Metric> metricCache = new LinkedHashMap<>();
    
    @PostConstruct
    private void initMetricCache() {
        metricRepository.findAll().forEach(metric -> metricCache.put(metric.getMetricId(), metric));
    }
    
    public UtilAgentResponse processQuery(String question) {
        QuerySpec querySpec = parseQuestion(question);
        if (querySpec == null) {
            return createInsufficientDataResponse("Unable to understand the query");
        }
        return executeQuery(querySpec);
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
}
