package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TimeSeriesService {
    
    @Autowired
    private FactValueRepository factValueRepository;
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private SourceRepository sourceRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    public Optional<TimeSeriesResponse> getTimeSeries(String metricId, String sourceId, 
                                                     String geoLevel, String geoId, 
                                                     LocalDate from, LocalDate to) {
        
        // Validate metric, source, and region exist
        Optional<Metric> metric = metricRepository.findById(metricId);
        Optional<Source> source = sourceRepository.findById(sourceId);
        Optional<Region> region = regionRepository.findByGeoLevelAndGeoId(geoLevel, geoId);
        
        if (metric.isEmpty() || source.isEmpty() || region.isEmpty()) {
            return Optional.empty();
        }
        
        // Get time series data
        List<FactValue> facts = factValueRepository.findTimeSeries(metricId, sourceId, geoLevel, geoId, from, to);
        
        // Build response
        TimeSeriesResponse response = new TimeSeriesResponse();
        response.setMetric(new MetricInfo(metricId, metric.get().getName(), metric.get().getUnit()));
        response.setSource(new SourceInfo(
            sourceId,
            source.get().getName(),
            source.get().getTermsUrl(),
            source.get().isMock()
        ));
        response.setRegion(new RegionInfo(geoLevel, geoId, region.get().getName()));
        
        List<TimeSeriesPoint> points = facts.stream()
            .map(fact -> new TimeSeriesPoint(
                fact.getPeriodStart().toString(),
                fact.getPeriodEnd().toString(),
                fact.getValueNumeric().doubleValue(),
                fact.getRetrievedAt().toString(),
                fact.getSourcePublishedAt() != null ? fact.getSourcePublishedAt().toString() : null
            ))
            .toList();
        
        response.setPoints(points);
        return Optional.of(response);
    }
    
    public String generateCsv(String metricId, String sourceId, String geoLevel, String geoId, 
                             LocalDate from, LocalDate to) {
        
        List<FactValue> facts = factValueRepository.findTimeSeries(metricId, sourceId, geoLevel, geoId, from, to);
        
        StringBuilder csv = new StringBuilder();
        csv.append("periodStart,periodEnd,value,retrievedAt,sourcePublishedAt\n");
        
        for (FactValue fact : facts) {
            csv.append(fact.getPeriodStart()).append(",")
               .append(fact.getPeriodEnd()).append(",")
               .append(fact.getValueNumeric()).append(",")
               .append(fact.getRetrievedAt()).append(",")
               .append(fact.getSourcePublishedAt() != null ? fact.getSourcePublishedAt() : "")
               .append("\n");
        }
        
        return csv.toString();
    }
}
