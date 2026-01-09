package com.utilityexplorer.shared.persistence;

import jakarta.persistence.*;
import java.util.Map;
// If using Postgres JSON types, you will need Hypersistence Utils annotations
// or a custom Converter. Using simple String + Jackson for simplicity/portability 
// in this phase or relying on transient fields if JPA doesn't support jsonb natively without libs.

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
    
    // NEW: Categorization
    @Column(name = "category")
    private String category;
    
    @Column(name = "subcategory")
    private String subCategory;

    @Column(name = "default_granularity", nullable = false)
    private String defaultGranularity;

    @Column(name = "supported_geo_levels", nullable = false)
    private String supportedGeoLevels;

    // NEW: Visualization metadata (stored as JSON string for now to avoid dependency hell)
    @Column(name = "visualization_json", columnDefinition = "TEXT")
    private String visualizationJson;
    
    // NEW: Ingestion config (stored as JSON string)
    @Column(name = "ingestion_config_json", columnDefinition = "TEXT")
    private String ingestionConfigJson;

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

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getVisualizationJson() { return visualizationJson; }
    public void setVisualizationJson(String visualizationJson) { this.visualizationJson = visualizationJson; }

    public String getIngestionConfigJson() { return ingestionConfigJson; }
    public void setIngestionConfigJson(String ingestionConfigJson) { this.ingestionConfigJson = ingestionConfigJson; }
}
