package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import com.utilityexplorer.shared.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
        Optional<Metric> metric = metricRepository.findById(metricId);
        Optional<Source> source = sourceRepository.findById(sourceId);
        if (metric.isEmpty() || source.isEmpty() || period == null) {
            return Optional.empty();
        }
        LocalDate periodStart = parsePeriodStart(period);
        LocalDate periodEnd = parsePeriodEnd(period);
        if (periodStart == null || periodEnd == null) {
            return Optional.empty();
        }

        List<FactValue> facts = fetchFacts(metricId, sourceId, geoLevel, parentGeoLevel, parentGeoId, periodStart, periodEnd);
        return Optional.of(buildMapResponse(metric.get(), source.get(), geoLevel, parentGeoId, periodStart, periodEnd, facts));
    }

    public Optional<MapRangeResponse> getMapDataRange(String metricId, String sourceId, String geoLevel,
                                                      String parentGeoLevel, String parentGeoId,
                                                      String startPeriod, String endPeriod) {
        Optional<Metric> metric = metricRepository.findById(metricId);
        Optional<Source> source = sourceRepository.findById(sourceId);
        if (metric.isEmpty() || source.isEmpty()) {
            return Optional.empty();
        }
        LocalDate rangeStart = parsePeriodStart(startPeriod);
        LocalDate rangeEnd = parsePeriodEnd(endPeriod);
        if (rangeStart == null || rangeEnd == null || rangeStart.isAfter(rangeEnd)) {
            return Optional.empty();
        }

        List<FactValue> facts = fetchFacts(metricId, sourceId, geoLevel, parentGeoLevel, parentGeoId, rangeStart, rangeEnd);
        if (facts.isEmpty()) {
            return Optional.empty();
        }

        Map<String, List<FactValue>> grouped = facts.stream()
            .collect(Collectors.groupingBy(
                fact -> fact.getPeriodStart().toString() + "|" + fact.getPeriodEnd().toString(),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        List<MapResponse> maps = new ArrayList<>();
        for (Map.Entry<String, List<FactValue>> entry : grouped.entrySet()) {
            String[] keyParts = entry.getKey().split("\\|");
            LocalDate periodStart = LocalDate.parse(keyParts[0]);
            LocalDate periodEnd = LocalDate.parse(keyParts[1]);
            maps.add(buildMapResponse(metric.get(), source.get(), geoLevel, parentGeoId, periodStart, periodEnd, entry.getValue()));
        }

        MapRangeResponse rangeResponse = new MapRangeResponse();
        rangeResponse.setMaps(maps);
        return Optional.of(rangeResponse);
    }

    private List<FactValue> fetchFacts(String metricId, String sourceId, String geoLevel,
                                       String parentGeoLevel, String parentGeoId,
                                       LocalDate periodStart, LocalDate periodEnd) {
        boolean hasParent = parentGeoLevel != null && parentGeoId != null && !parentGeoId.isBlank()
            && ("COUNTY".equalsIgnoreCase(geoLevel) || "PLACE".equalsIgnoreCase(geoLevel))
            && "STATE".equalsIgnoreCase(parentGeoLevel);

        if (hasParent) {
            if (periodStart.equals(periodEnd)) {
                return factValueRepository.findMapDataByPrefix(metricId, sourceId, geoLevel, parentGeoId, periodStart, periodEnd);
            } else {
                return factValueRepository.findMapDataByPrefixInRange(metricId, sourceId, geoLevel, parentGeoId, periodStart, periodEnd);
            }
        }
        if (periodStart.equals(periodEnd)) {
            return factValueRepository.findMapData(metricId, sourceId, geoLevel, periodStart, periodEnd);
        }
        return factValueRepository.findMapDataInRange(metricId, sourceId, geoLevel, periodStart, periodEnd);
    }

    private MapResponse buildMapResponse(Metric metric, Source source, String geoLevel, String parentGeoId,
                                         LocalDate periodStart, LocalDate periodEnd, List<FactValue> facts) {
        MapResponse response = new MapResponse();
        response.setMetric(new MetricInfo(metric.getMetricId(), metric.getName(), metric.getUnit()));
        response.setSource(new SourceInfo(
            source.getSourceId(),
            source.getName(),
            source.getTermsUrl(),
            source.isMock()
        ));
        response.setGeoLevel(geoLevel);
        response.setParent(parentGeoId);
        response.setPeriod(new PeriodInfo(periodStart.toString(), periodEnd.toString()));

        if (!facts.isEmpty()) {
            FactValue firstFact = facts.get(0);
            response.setRetrievedAt(firstFact.getRetrievedAt().toString());
            if (firstFact.getSourcePublishedAt() != null) {
                response.setSourcePublishedAt(firstFact.getSourcePublishedAt().toString());
            }
            DoubleSummaryStatistics stats = facts.stream()
                .mapToDouble(f -> f.getValueNumeric().doubleValue())
                .summaryStatistics();
            response.setLegend(new LegendStats(stats.getMin(), stats.getMax()));
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
        response.setNotes(List.of("If a region lacks a value for this period, it will appear as 'No data'."));
        return response;
    }

    private LocalDate parsePeriodStart(String period) {
        if (period == null) return null;
        return parsePeriod(period, true);
    }

    private LocalDate parsePeriodEnd(String period) {
        if (period == null) return null;
        return parsePeriod(period, false);
    }

    private LocalDate parsePeriod(String period, boolean start) {
        try {
            String[] parts = period.split("-");
            int year = Integer.parseInt(parts[0]);
            if (parts.length == 1) {
                return start ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 12, 31);
            }
            int month = Integer.parseInt(parts[1]);
            LocalDate date = LocalDate.of(year, month, 1);
            return start ? date : date.withDayOfMonth(date.lengthOfMonth());
        } catch (Exception e) {
            return null;
        }
    }
}
