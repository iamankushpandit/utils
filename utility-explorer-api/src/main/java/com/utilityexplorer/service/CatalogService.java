package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import com.utilityexplorer.shared.persistence.*;
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

    @Autowired
    private SourceConfigRepository sourceConfigRepository;

    @Autowired
    private SourceRunRepository sourceRunRepository;
    
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
        Optional<SourceConfig> config = sourceConfigRepository.findById(source.getSourceId());
        Optional<SourceRun> lastRun = sourceRunRepository.findLatestBySourceId(source.getSourceId());

        String scheduleStatus = "Unknown";
        String nextRunAt = null;

        if (config.isPresent()) {
            SourceConfig c = config.get();
            if (!c.getEnabled()) {
                scheduleStatus = "Disabled";
            } else {
                scheduleStatus = "Scheduled: " + describeCron(c.getScheduleCron());
                // Simple next run estimation (just description for now as full cron parsing needs more libs)
                nextRunAt = "Follows schedule " + c.getScheduleCron();
            }
        }

        String lastRunAt = lastRun.map(r -> r.getStartedAt().toString()).orElse(null);

        return new SourceDto(
            source.getSourceId(),
            source.getName(),
            source.getType(),
            source.getTermsUrl(),
            source.getAttributionText(),
            source.getNotes(),
            source.isMock(),
            scheduleStatus,
            lastRunAt,
            nextRunAt
        );
    }

    private String describeCron(String cron) {
        if ("0 0 9 * * MON".equals(cron)) return "Weekly on Monday at 9 AM UTC";
        if ("0 0 9 1 * *".equals(cron)) return "Monthly on 1st at 9 AM UTC";
        if ("0 0 9 15 * *".equals(cron)) return "Monthly on 15th at 9 AM UTC";
        if ("0 0 6 * * *".equals(cron)) return "Daily at 6 AM UTC";
        return cron;
    }
}
