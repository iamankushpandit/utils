package com.utilityexplorer.dto;

import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public static class MetricDto {
        private String metricId;
        private String name;
        private String unit;
        private String description;
        private String defaultGranularity;
        private List<String> supportedGeoLevels;
        private List<String> sourceIds;

        public MetricDto() {}

        public MetricDto(String metricId, String name, String unit, String description,
                         String defaultGranularity, List<String> supportedGeoLevels,
                         List<String> sourceIds) {
            this.metricId = metricId;
            this.name = name;
            this.unit = unit;
            this.description = description;
            this.defaultGranularity = defaultGranularity;
            this.supportedGeoLevels = supportedGeoLevels;
            this.sourceIds = sourceIds;
        }

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDefaultGranularity() { return defaultGranularity; }
        public void setDefaultGranularity(String defaultGranularity) { this.defaultGranularity = defaultGranularity; }

        public List<String> getSupportedGeoLevels() { return supportedGeoLevels; }
        public void setSupportedGeoLevels(List<String> supportedGeoLevels) { this.supportedGeoLevels = supportedGeoLevels; }

        public List<String> getSourceIds() { return sourceIds; }
        public void setSourceIds(List<String> sourceIds) { this.sourceIds = sourceIds; }
    }

    public static class SourceDto {
        private String sourceId;
        private String name;
        private String type;
        private String termsUrl;
        private String attributionText;
        private String notes;
        private boolean isMock;

        public SourceDto() {}

        public SourceDto(String sourceId, String name, String type, String termsUrl,
                         String attributionText, String notes, boolean isMock) {
            this.sourceId = sourceId;
            this.name = name;
            this.type = type;
            this.termsUrl = termsUrl;
            this.attributionText = attributionText;
            this.notes = notes;
            this.isMock = isMock;
        }

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTermsUrl() { return termsUrl; }
        public void setTermsUrl(String termsUrl) { this.termsUrl = termsUrl; }

        public String getAttributionText() { return attributionText; }
        public void setAttributionText(String attributionText) { this.attributionText = attributionText; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public boolean isMock() { return isMock; }
        public void setMock(boolean mock) { isMock = mock; }
    }

    public static class RegionDto {
        private String geoLevel;
        private String geoId;
        private String name;
        private String parentGeoLevel;
        private String parentGeoId;
        private Double centroidLat;
        private Double centroidLon;

        public RegionDto() {}

        public RegionDto(String geoLevel, String geoId, String name, String parentGeoLevel,
                         String parentGeoId, Double centroidLat, Double centroidLon) {
            this.geoLevel = geoLevel;
            this.geoId = geoId;
            this.name = name;
            this.parentGeoLevel = parentGeoLevel;
            this.parentGeoId = parentGeoId;
            this.centroidLat = centroidLat;
            this.centroidLon = centroidLon;
        }

        public String getGeoLevel() { return geoLevel; }
        public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

        public String getGeoId() { return geoId; }
        public void setGeoId(String geoId) { this.geoId = geoId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getParentGeoLevel() { return parentGeoLevel; }
        public void setParentGeoLevel(String parentGeoLevel) { this.parentGeoLevel = parentGeoLevel; }

        public String getParentGeoId() { return parentGeoId; }
        public void setParentGeoId(String parentGeoId) { this.parentGeoId = parentGeoId; }

        public Double getCentroidLat() { return centroidLat; }
        public void setCentroidLat(Double centroidLat) { this.centroidLat = centroidLat; }

        public Double getCentroidLon() { return centroidLon; }
        public void setCentroidLon(Double centroidLon) { this.centroidLon = centroidLon; }
    }

    public static class CoverageDto {
        private String metricId;
        private String sourceId;
        private List<String> supportedGeoLevels;
        private List<String> supportedGranularities;

        public CoverageDto() {}

        public CoverageDto(String metricId, String sourceId, List<String> supportedGeoLevels,
                           List<String> supportedGranularities) {
            this.metricId = metricId;
            this.sourceId = sourceId;
            this.supportedGeoLevels = supportedGeoLevels;
            this.supportedGranularities = supportedGranularities;
        }

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public List<String> getSupportedGeoLevels() { return supportedGeoLevels; }
        public void setSupportedGeoLevels(List<String> supportedGeoLevels) { this.supportedGeoLevels = supportedGeoLevels; }

        public List<String> getSupportedGranularities() { return supportedGranularities; }
        public void setSupportedGranularities(List<String> supportedGranularities) { this.supportedGranularities = supportedGranularities; }
    }

    public static class ErrorResponse {
        private String error;
        private String message;
        private String timestamp;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = java.time.Instant.now().toString();
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class MapResponse {
        private MetricInfo metric;
        private SourceInfo source;
        private String geoLevel;
        private String parent;
        private PeriodInfo period;
        private String retrievedAt;
        private String sourcePublishedAt;
        private LegendStats legend;
        private List<MapValue> values;
        private List<String> notes;

        public MapResponse() {}

        public MetricInfo getMetric() { return metric; }
        public void setMetric(MetricInfo metric) { this.metric = metric; }

        public SourceInfo getSource() { return source; }
        public void setSource(SourceInfo source) { this.source = source; }

        public String getGeoLevel() { return geoLevel; }
        public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

        public String getParent() { return parent; }
        public void setParent(String parent) { this.parent = parent; }

        public PeriodInfo getPeriod() { return period; }
        public void setPeriod(PeriodInfo period) { this.period = period; }

        public String getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(String retrievedAt) { this.retrievedAt = retrievedAt; }

        public String getSourcePublishedAt() { return sourcePublishedAt; }
        public void setSourcePublishedAt(String sourcePublishedAt) { this.sourcePublishedAt = sourcePublishedAt; }

        public LegendStats getLegend() { return legend; }
        public void setLegend(LegendStats legend) { this.legend = legend; }

        public List<MapValue> getValues() { return values; }
        public void setValues(List<MapValue> values) { this.values = values; }

        public List<String> getNotes() { return notes; }
        public void setNotes(List<String> notes) { this.notes = notes; }
    }

    public static class MetricInfo {
        private String metricId;
        private String name;
        private String unit;

        public MetricInfo(String metricId, String name, String unit) {
            this.metricId = metricId;
            this.name = name;
            this.unit = unit;
        }

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class SourceInfo {
        private String sourceId;
        private String name;
        private String termsUrl;
        private boolean isMock;

        public SourceInfo(String sourceId, String name, String termsUrl, boolean isMock) {
            this.sourceId = sourceId;
            this.name = name;
            this.termsUrl = termsUrl;
            this.isMock = isMock;
        }

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTermsUrl() { return termsUrl; }
        public void setTermsUrl(String termsUrl) { this.termsUrl = termsUrl; }

        public boolean isMock() { return isMock; }
        public void setMock(boolean mock) { isMock = mock; }
    }

    public static class PeriodInfo {
        private String start;
        private String end;

        public PeriodInfo(String start, String end) {
            this.start = start;
            this.end = end;
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class LegendStats {
        private Double min;
        private Double max;

        public LegendStats(Double min, Double max) {
            this.min = min;
            this.max = max;
        }

        public Double getMin() { return min; }
        public void setMin(Double min) { this.min = min; }

        public Double getMax() { return max; }
        public void setMax(Double max) { this.max = max; }
    }

    public static class MapValue {
        private String geoId;
        private String name;
        private Double value;
        private String retrievedAt;

        public MapValue(String geoId, String name, Double value, String retrievedAt) {
            this.geoId = geoId;
            this.name = name;
            this.value = value;
            this.retrievedAt = retrievedAt;
        }

        public String getGeoId() { return geoId; }
        public void setGeoId(String geoId) { this.geoId = geoId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public String getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(String retrievedAt) { this.retrievedAt = retrievedAt; }
    }

    public static class TimeSeriesResponse {
        private MetricInfo metric;
        private SourceInfo source;
        private RegionInfo region;
        private List<TimeSeriesPoint> points;

        public TimeSeriesResponse() {}

        public MetricInfo getMetric() { return metric; }
        public void setMetric(MetricInfo metric) { this.metric = metric; }

        public SourceInfo getSource() { return source; }
        public void setSource(SourceInfo source) { this.source = source; }

        public RegionInfo getRegion() { return region; }
        public void setRegion(RegionInfo region) { this.region = region; }

        public List<TimeSeriesPoint> getPoints() { return points; }
        public void setPoints(List<TimeSeriesPoint> points) { this.points = points; }
    }

    public static class RegionInfo {
        private String geoLevel;
        private String geoId;
        private String name;

        public RegionInfo(String geoLevel, String geoId, String name) {
            this.geoLevel = geoLevel;
            this.geoId = geoId;
            this.name = name;
        }

        public String getGeoLevel() { return geoLevel; }
        public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

        public String getGeoId() { return geoId; }
        public void setGeoId(String geoId) { this.geoId = geoId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class TimeSeriesPoint {
        private String periodStart;
        private String periodEnd;
        private Double value;
        private String retrievedAt;
        private String sourcePublishedAt;

        public TimeSeriesPoint(String periodStart, String periodEnd, Double value,
                               String retrievedAt, String sourcePublishedAt) {
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.value = value;
            this.retrievedAt = retrievedAt;
            this.sourcePublishedAt = sourcePublishedAt;
        }

        public String getPeriodStart() { return periodStart; }
        public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

        public String getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public String getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(String retrievedAt) { this.retrievedAt = retrievedAt; }

        public String getSourcePublishedAt() { return sourcePublishedAt; }
        public void setSourcePublishedAt(String sourcePublishedAt) { this.sourcePublishedAt = sourcePublishedAt; }
    }

    public static class SourceStatusResponse {
        private String sourceId;
        private Boolean enabled;
        private String scheduleCron;
        private String timezone;
        private LastRunInfo lastRun;
        private String lastSuccessAt;
        private String nextRunAt;

        public SourceStatusResponse() {}

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getScheduleCron() { return scheduleCron; }
        public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }

        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }

        public LastRunInfo getLastRun() { return lastRun; }
        public void setLastRun(LastRunInfo lastRun) { this.lastRun = lastRun; }

        public String getLastSuccessAt() { return lastSuccessAt; }
        public void setLastSuccessAt(String lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }

        public String getNextRunAt() { return nextRunAt; }
        public void setNextRunAt(String nextRunAt) { this.nextRunAt = nextRunAt; }
    }

    public static class MetricStatusResponse {
        private String metricId;
        private String metricName;
        private List<SourceStatusResponse> sources;

        public MetricStatusResponse() {}

        public MetricStatusResponse(String metricId, String metricName, List<SourceStatusResponse> sources) {
            this.metricId = metricId;
            this.metricName = metricName;
            this.sources = sources;
        }

        public String getMetricId() { return metricId; }
        public void setMetricId(String metricId) { this.metricId = metricId; }

        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }

        public List<SourceStatusResponse> getSources() { return sources; }
        public void setSources(List<SourceStatusResponse> sources) { this.sources = sources; }
    }

    public static class LastRunInfo {
        private String runId;
        private String status;
        private String startedAt;
        private String endedAt;
        private Integer rowsUpserted;
        private String errorSummary;

        public LastRunInfo(String runId, String status, String startedAt, String endedAt,
                           Integer rowsUpserted, String errorSummary) {
            this.runId = runId;
            this.status = status;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.rowsUpserted = rowsUpserted;
            this.errorSummary = errorSummary;
        }

        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

        public String getEndedAt() { return endedAt; }
        public void setEndedAt(String endedAt) { this.endedAt = endedAt; }

        public Integer getRowsUpserted() { return rowsUpserted; }
        public void setRowsUpserted(Integer rowsUpserted) { this.rowsUpserted = rowsUpserted; }

        public String getErrorSummary() { return errorSummary; }
        public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    }

    public static class CopilotResponse {
        private String status;
        private String summary;
        private PeriodInfo period;
        private TableResult table;
        private List<HighlightRegion> highlightRegions;
        private List<CitationInfo> citations;
        private List<String> notes;

        public CopilotResponse() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public PeriodInfo getPeriod() { return period; }
        public void setPeriod(PeriodInfo period) { this.period = period; }

        public TableResult getTable() { return table; }
        public void setTable(TableResult table) { this.table = table; }

        public List<HighlightRegion> getHighlightRegions() { return highlightRegions; }
        public void setHighlightRegions(List<HighlightRegion> highlightRegions) { this.highlightRegions = highlightRegions; }

        public List<CitationInfo> getCitations() { return citations; }
        public void setCitations(List<CitationInfo> citations) { this.citations = citations; }

        public List<String> getNotes() { return notes; }
        public void setNotes(List<String> notes) { this.notes = notes; }
    }

    public static class TableResult {
        private List<String> columns;
        private List<List<Object>> rows;

        public TableResult(List<String> columns, List<List<Object>> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }

        public List<List<Object>> getRows() { return rows; }
        public void setRows(List<List<Object>> rows) { this.rows = rows; }
    }

    public static class HighlightRegion {
        private String geoLevel;
        private String geoId;

        public HighlightRegion(String geoLevel, String geoId) {
            this.geoLevel = geoLevel;
            this.geoId = geoId;
        }

        public String getGeoLevel() { return geoLevel; }
        public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

        public String getGeoId() { return geoId; }
        public void setGeoId(String geoId) { this.geoId = geoId; }
    }

    public static class CitationInfo {
        private String sourceId;
        private String retrievedAt;
        private String termsUrl;

        public CitationInfo(String sourceId, String retrievedAt, String termsUrl) {
            this.sourceId = sourceId;
            this.retrievedAt = retrievedAt;
            this.termsUrl = termsUrl;
        }

        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }

        public String getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(String retrievedAt) { this.retrievedAt = retrievedAt; }

        public String getTermsUrl() { return termsUrl; }
        public void setTermsUrl(String termsUrl) { this.termsUrl = termsUrl; }
    }
}
