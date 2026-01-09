package com.utilityexplorer.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class IngestionEvent implements Serializable {
    private String metricId;
    private String sourceId;
    private String geoLevel;
    private String geoId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal value;
    private String aggregationMethod;
    private boolean isAggregated;

    public IngestionEvent() {}

    public IngestionEvent(String metricId, String sourceId, String geoLevel, String geoId, 
                         LocalDate periodStart, LocalDate periodEnd, BigDecimal value) {
        this.metricId = metricId;
        this.sourceId = sourceId;
        this.geoLevel = geoLevel;
        this.geoId = geoId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.value = value;
    }

    public String getMetricId() { return metricId; }
    public void setMetricId(String metricId) { this.metricId = metricId; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getGeoLevel() { return geoLevel; }
    public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

    public String getGeoId() { return geoId; }
    public void setGeoId(String geoId) { this.geoId = geoId; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getAggregationMethod() { return aggregationMethod; }
    public void setAggregationMethod(String aggregationMethod) { this.aggregationMethod = aggregationMethod; }

    public boolean isAggregated() { return isAggregated; }
    public void setAggregated(boolean aggregated) { isAggregated = aggregated; }

    @Override
    public String toString() {
        return "IngestionEvent{metric=" + metricId + ", source=" + sourceId + ", val=" + value + "}";
    }
}
