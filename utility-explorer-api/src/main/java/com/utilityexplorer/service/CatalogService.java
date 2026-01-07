package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {
    
    @Autowired
    private MetricRepository metricRepository;
    
    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private FactValueRepository factValueRepository;
    
    public List<MetricDto> getAllMetrics() {
        return metricRepository.findAll().stream()
            .map(this::toMetricDto)
            .toList();
    }
    
    public List<SourceDto> getAllSources() {
        return sourceRepository.findAll().stream()
            .map(this::toSourceDto)
            .toList();
    }
    
    public Optional<CoverageDto> getCoverage(String metricId, String sourceId) {
        Optional<Metric> metric = metricRepository.findById(metricId);
        Optional<Source> source = sourceRepository.findById(sourceId);
        
        if (metric.isEmpty() || source.isEmpty()) {
            return Optional.empty();
        }
        
        Metric m = metric.get();
        List<String> geoLevels = Arrays.asList(m.getSupportedGeoLevels().split(","));
        List<String> granularities = Arrays.asList(m.getDefaultGranularity());
        
        return Optional.of(new CoverageDto(metricId, sourceId, geoLevels, granularities));
    }
    
    private MetricDto toMetricDto(Metric metric) {
        List<String> geoLevels = Arrays.asList(metric.getSupportedGeoLevels().split(","));
        List<String> sourceIds = factValueRepository.findDistinctSourceIdsByMetric(metric.getMetricId());
        if (sourceIds == null) {
            sourceIds = List.of();
        }
        return new MetricDto(
            metric.getMetricId(),
            metric.getName(),
            metric.getUnit(),
            metric.getDescription(),
            metric.getDefaultGranularity(),
            geoLevels,
            sourceIds
        );
    }
    
    private SourceDto toSourceDto(Source source) {
        return new SourceDto(
            source.getSourceId(),
            source.getName(),
            source.getType(),
            source.getTermsUrl(),
            source.getAttributionText(),
            source.getNotes(),
            source.isMock()
        );
    }
}
