package com.utilityexplorer.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "metric")
public class Metric {
    
    @Id
    @Column(name = "metric_id")
    private String metricId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String unit;
    
    private String description;
    
    @Column(name = "default_granularity", nullable = false)
    private String defaultGranularity;
    
    @Column(name = "supported_geo_levels", nullable = false)
    private String supportedGeoLevels;
    
    // Constructors
    public Metric() {}
    
    public Metric(String metricId, String name, String unit, String description, 
                  String defaultGranularity, String supportedGeoLevels) {
        this.metricId = metricId;
        this.name = name;
        this.unit = unit;
        this.description = description;
        this.defaultGranularity = defaultGranularity;
        this.supportedGeoLevels = supportedGeoLevels;
    }
    
    // Getters and setters
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
    
    public String getSupportedGeoLevels() { return supportedGeoLevels; }
    public void setSupportedGeoLevels(String supportedGeoLevels) { this.supportedGeoLevels = supportedGeoLevels; }
}