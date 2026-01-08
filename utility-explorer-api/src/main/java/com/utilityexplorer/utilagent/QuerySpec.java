package com.utilityexplorer.utilagent;

import java.util.List;

public class QuerySpec {
    private String queryType;
    private String geoLevel;
    private TimeSpec time;
    private List<MetricSpec> metrics;
    private List<FilterSpec> filters;
    private Integer limit;
    
    // Constructors
    public QuerySpec() {}
    
    // Getters and setters
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    
    public String getGeoLevel() { return geoLevel; }
    public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }
    
    public TimeSpec getTime() { return time; }
    public void setTime(TimeSpec time) { this.time = time; }
    
    public List<MetricSpec> getMetrics() { return metrics; }
    public void setMetrics(List<MetricSpec> metrics) { this.metrics = metrics; }
    
    public List<FilterSpec> getFilters() { return filters; }
    public void setFilters(List<FilterSpec> filters) { this.filters = filters; }
    
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}

class TimeSpec {
    private String mode;
    private String period;
    
    public TimeSpec() {}
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}

class MetricSpec {
    private String metricId;
    private String sourceId;
    private String direction;
    
    public MetricSpec() {}
    
    public String getMetricId() { return metricId; }
    public void setMetricId(String metricId) { this.metricId = metricId; }
    
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}

class FilterSpec {
    private String metricId;
    private String sourceId;
    private String op;
    private Double value;
    
    public FilterSpec() {}
    
    public String getMetricId() { return metricId; }
    public void setMetricId(String metricId) { this.metricId = metricId; }
    
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}
