package com.utilityexplorer.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class FactValueId implements Serializable {
    
    private String metricId;
    private String sourceId;
    private String geoLevel;
    private String geoId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    
    public FactValueId() {}
    
    public FactValueId(String metricId, String sourceId, String geoLevel, String geoId, 
                       LocalDate periodStart, LocalDate periodEnd) {
        this.metricId = metricId;
        this.sourceId = sourceId;
        this.geoLevel = geoLevel;
        this.geoId = geoId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactValueId that = (FactValueId) o;
        return Objects.equals(metricId, that.metricId) &&
               Objects.equals(sourceId, that.sourceId) &&
               Objects.equals(geoLevel, that.geoLevel) &&
               Objects.equals(geoId, that.geoId) &&
               Objects.equals(periodStart, that.periodStart) &&
               Objects.equals(periodEnd, that.periodEnd);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(metricId, sourceId, geoLevel, geoId, periodStart, periodEnd);
    }
    
    // Getters and setters
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
}