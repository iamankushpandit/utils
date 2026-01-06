package com.utilityexplorer.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "source_config")
public class SourceConfig {
    
    @Id
    @Column(name = "source_id")
    private String sourceId;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "schedule_cron", nullable = false)
    private String scheduleCron;
    
    @Column(nullable = false)
    private String timezone = "UTC";
    
    @Column(name = "check_strategy", nullable = false)
    private String checkStrategy;
    
    @Column(name = "max_lookback_periods", nullable = false)
    private Integer maxLookbackPeriods = 3;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    // Constructors
    public SourceConfig() {}
    
    // Getters and setters
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getCheckStrategy() { return checkStrategy; }
    public void setCheckStrategy(String checkStrategy) { this.checkStrategy = checkStrategy; }
    
    public Integer getMaxLookbackPeriods() { return maxLookbackPeriods; }
    public void setMaxLookbackPeriods(Integer maxLookbackPeriods) { this.maxLookbackPeriods = maxLookbackPeriods; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}