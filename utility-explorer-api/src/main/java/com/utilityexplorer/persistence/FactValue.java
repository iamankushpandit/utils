package com.utilityexplorer.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fact_value")
@IdClass(FactValueId.class)
public class FactValue {
    
    @Id
    @Column(name = "metric_id")
    private String metricId;
    
    @Id
    @Column(name = "source_id")
    private String sourceId;
    
    @Id
    @Column(name = "geo_level")
    private String geoLevel;
    
    @Id
    @Column(name = "geo_id")
    private String geoId;
    
    @Id
    @Column(name = "period_start")
    private LocalDate periodStart;
    
    @Id
    @Column(name = "period_end")
    private LocalDate periodEnd;
    
    @Column(name = "value_numeric", nullable = false)
    private BigDecimal valueNumeric;
    
    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;
    
    @Column(name = "source_published_at")
    private Instant sourcePublishedAt;
    
    @Column(name = "is_aggregated", nullable = false)
    private Boolean isAggregated = false;
    
    @Column(name = "aggregation_method")
    private String aggregationMethod;
    
    @Column(name = "payload_id")
    private UUID payloadId;
    
    // Constructors
    public FactValue() {}
    
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
    
    public BigDecimal getValueNumeric() { return valueNumeric; }
    public void setValueNumeric(BigDecimal valueNumeric) { this.valueNumeric = valueNumeric; }
    
    public Instant getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(Instant retrievedAt) { this.retrievedAt = retrievedAt; }
    
    public Instant getSourcePublishedAt() { return sourcePublishedAt; }
    public void setSourcePublishedAt(Instant sourcePublishedAt) { this.sourcePublishedAt = sourcePublishedAt; }
    
    public Boolean getIsAggregated() { return isAggregated; }
    public void setIsAggregated(Boolean isAggregated) { this.isAggregated = isAggregated; }
    
    public String getAggregationMethod() { return aggregationMethod; }
    public void setAggregationMethod(String aggregationMethod) { this.aggregationMethod = aggregationMethod; }
    
    public UUID getPayloadId() { return payloadId; }
    public void setPayloadId(UUID payloadId) { this.payloadId = payloadId; }
}