package com.utilityexplorer.dto;

import java.util.List;
import java.util.UUID;

public class MetricDto {
    private String metricId;
    private String name;
    private String unit;
    private String description;
    private String defaultGranularity;
    private List<String> supportedGeoLevels;
    
    public MetricDto() {}
    
    public MetricDto(String metricId, String name, String unit, String description, 
                     String defaultGranularity, List<String> supportedGeoLevels) {
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
    
    public List<String> getSupportedGeoLevels() { return supportedGeoLevels; }
    public void setSupportedGeoLevels(List<String> supportedGeoLevels) { this.supportedGeoLevels = supportedGeoLevels; }
}

class SourceDto {
    private String sourceId;
    private String name;
    private String type;
    private String termsUrl;
    private String attributionText;
    private String notes;
    
    public SourceDto() {}
    
    public SourceDto(String sourceId, String name, String type, String termsUrl, 
                     String attributionText, String notes) {
        this.sourceId = sourceId;
        this.name = name;
        this.type = type;
        this.termsUrl = termsUrl;
        this.attributionText = attributionText;
        this.notes = notes;
    }
    
    // Getters and setters
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
}

class RegionDto {
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
    
    // Getters and setters
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

class CoverageDto {
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
    
    // Getters and setters
    public String getMetricId() { return metricId; }
    public void setMetricId(String metricId) { this.metricId = metricId; }
    
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public List<String> getSupportedGeoLevels() { return supportedGeoLevels; }
    public void setSupportedGeoLevels(List<String> supportedGeoLevels) { this.supportedGeoLevels = supportedGeoLevels; }
    
    public List<String> getSupportedGranularities() { return supportedGranularities; }
    public void setSupportedGranularities(List<String> supportedGranularities) { this.supportedGranularities = supportedGranularities; }
}

class ErrorResponse {
    private String error;
    private String message;
    private String timestamp;
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = java.time.Instant.now().toString();
    }
    
    // Getters and setters
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}