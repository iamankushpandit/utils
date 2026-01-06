package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

@Service
public class MapService {
    
    @Autowired
    private FactValueRepository factValueRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private SourceRepository sourceRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    public Optional<MapResponse> getMapData(String metricId, String sourceId, String geoLevel, 
                                           String parentGeoLevel, String parentGeoId, String period) {
        
        // Validate metric and source exist
        Optional<Metric> metric = metricRepository.findById(metricId);
        Optional<Source> source = sourceRepository.findById(sourceId);
        
        if (metric.isEmpty() || source.isEmpty()) {
            return Optional.empty();
        }
        
        // Parse period (assuming YYYY-MM format for now)
        LocalDate periodStart, periodEnd;
        try {
            String[] parts = period.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            periodStart = LocalDate.of(year, month, 1);
            periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        } catch (Exception e) {
            return Optional.empty();
        }
        
        // Get fact values
        List<FactValue> facts = factValueRepository.findMapData(metricId, sourceId, geoLevel, periodStart, periodEnd);
        
        // Build response
        MapResponse response = new MapResponse();
        response.setMetric(new MetricInfo(metricId, metric.get().getName(), metric.get().getUnit()));
        response.setSource(new SourceInfo(
            sourceId,
            source.get().getName(),
            source.get().getTermsUrl(),
            source.get().isMock()
        ));
        response.setGeoLevel(geoLevel);
        response.setParent(parentGeoId);
        response.setPeriod(new PeriodInfo(periodStart.toString(), periodEnd.toString()));
        
        if (!facts.isEmpty()) {
            // Set provenance from first fact (all should have same retrieved_at for same period)
            FactValue firstFact = facts.get(0);
            response.setRetrievedAt(firstFact.getRetrievedAt().toString());
            if (firstFact.getSourcePublishedAt() != null) {
                response.setSourcePublishedAt(firstFact.getSourcePublishedAt().toString());
            }
            
            // Calculate legend stats
            DoubleSummaryStatistics stats = facts.stream()
                .mapToDouble(f -> f.getValueNumeric().doubleValue())
                .summaryStatistics();
            response.setLegend(new LegendStats(stats.getMin(), stats.getMax()));
            
            // Build values with region names
            List<MapValue> values = facts.stream()
                .map(fact -> {
                    Optional<Region> region = regionRepository.findByGeoLevelAndGeoId(fact.getGeoLevel(), fact.getGeoId());
                    String regionName = region.map(Region::getName).orElse("Unknown");
                    return new MapValue(
                        fact.getGeoId(),
                        regionName,
                        fact.getValueNumeric().doubleValue(),
                        fact.getRetrievedAt().toString()
                    );
                })
                .toList();
            response.setValues(values);
        } else {
            response.setValues(List.of());
            response.setLegend(new LegendStats(null, null));
        }
        
        response.setNotes(List.of("If a region has no value for this period, it will be omitted from values and displayed as 'No data' in the UI."));
        
        return Optional.of(response);
    }
}
