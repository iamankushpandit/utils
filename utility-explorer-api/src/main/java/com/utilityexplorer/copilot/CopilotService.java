package com.utilityexplorer.copilot;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopilotService {
    
    @Autowired
    private FactValueRepository factValueRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private SourceRepository sourceRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    public CopilotResponse processQuery(String question) {
        // Simple pattern matching for MVP
        QuerySpec querySpec = parseQuestion(question);
        
        if (querySpec == null) {
            return createInsufficientDataResponse("Unable to understand the query");
        }
        
        return executeQuery(querySpec);
    }
    
    private QuerySpec parseQuestion(String question) {
        String lower = question.toLowerCase();
        
        // Simple pattern: "high electricity"
        if (lower.contains("high") && lower.contains("electricity")) {
            
            QuerySpec spec = new QuerySpec();
            spec.setQueryType("INTERSECTION");
            spec.setGeoLevel("STATE");
            spec.setLimit(10);
            
            TimeSpec time = new TimeSpec();
            time.setMode("LATEST_COMMON");
            spec.setTime(time);
            
            List<MetricSpec> metrics = Arrays.asList(
                createMetricSpec("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH", "EIA")
            );
            spec.setMetrics(metrics);
            
            return spec;
        }
        
        return null; // Unsupported query
    }
    
    private MetricSpec createMetricSpec(String metricId, String sourceId) {
        MetricSpec spec = new MetricSpec();
        spec.setMetricId(metricId);
        spec.setSourceId(sourceId);
        return spec;
    }
    
    private CopilotResponse executeQuery(QuerySpec querySpec) {
        if ("INTERSECTION".equals(querySpec.getQueryType())) {
            return executeIntersectionQuery(querySpec);
        }
        
        return createInsufficientDataResponse("Query type not supported");
    }
    
    private CopilotResponse executeIntersectionQuery(QuerySpec querySpec) {
        // Find latest common period for electricity data
        LocalDate latestPeriod = findLatestPeriod("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH", "EIA");
        
        if (latestPeriod == null) {
            return createInsufficientDataResponse("No electricity data available");
        }
        
        // Get electricity data for latest period
        List<FactValue> electricityFacts = factValueRepository.findMapData(
            "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH", "EIA", "STATE", 
            latestPeriod, latestPeriod.withDayOfMonth(latestPeriod.lengthOfMonth())
        );
        
        if (electricityFacts.isEmpty()) {
            return createInsufficientDataResponse("No electricity data for latest period");
        }
        
        // Create response with electricity data only
        CopilotResponse response = new CopilotResponse();
        response.setStatus("OK");
        response.setSummary("States with electricity pricing data");
        
        PeriodInfo period = new PeriodInfo(latestPeriod.toString(), 
            latestPeriod.withDayOfMonth(latestPeriod.lengthOfMonth()).toString());
        response.setPeriod(period);
        
        // Build table
        List<String> columns = Arrays.asList("State", "Electricity Price (cents/kWh)");
        List<List<Object>> rows = electricityFacts.stream()
            .map(fact -> {
                Optional<Region> region = regionRepository.findByGeoLevelAndGeoId("STATE", fact.getGeoId());
                String stateName = region.map(Region::getName).orElse("Unknown");
                return Arrays.<Object>asList(stateName, fact.getValueNumeric().doubleValue());
            })
            .collect(Collectors.toList());
        
        response.setTable(new TableResult(columns, rows));
        
        // Highlight regions
        List<HighlightRegion> highlights = electricityFacts.stream()
            .map(fact -> new HighlightRegion("STATE", fact.getGeoId()))
            .collect(Collectors.toList());
        response.setHighlightRegions(highlights);
        
        // Citations
        Optional<Source> source = sourceRepository.findById("EIA");
        if (source.isPresent()) {
            List<CitationInfo> citations = Arrays.asList(
                new CitationInfo("EIA", electricityFacts.get(0).getRetrievedAt().toString(), 
                    source.get().getTermsUrl())
            );
            response.setCitations(citations);
        }
        
        response.setNotes(Arrays.asList(
            "No values were estimated. Results include only regions with available data."
        ));
        
        return response;
    }
    
    private LocalDate findLatestPeriod(String metricId, String sourceId) {
        // Simple approach: find the latest period_start for this metric/source
        List<FactValue> allFacts = factValueRepository.findAll();
        return allFacts.stream()
            .filter(f -> metricId.equals(f.getMetricId()) && sourceId.equals(f.getSourceId()))
            .map(FactValue::getPeriodStart)
            .max(LocalDate::compareTo)
            .orElse(null);
    }
    
    private CopilotResponse createInsufficientDataResponse(String message) {
        CopilotResponse response = new CopilotResponse();
        response.setStatus("INSUFFICIENT_DATA");
        response.setSummary(message);
        response.setNotes(Arrays.asList("Unable to process query with available data"));
        return response;
    }
}
