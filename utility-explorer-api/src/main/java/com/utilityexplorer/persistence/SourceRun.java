package com.utilityexplorer.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_run")
public class SourceRun {
    
    @Id
    @Column(name = "run_id")
    private UUID runId;
    
    @Column(name = "source_id", nullable = false)
    private String sourceId;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "ended_at")
    private Instant endedAt;
    
    @Column(nullable = false)
    private String status;
    
    @Column(name = "rows_upserted", nullable = false)
    private Integer rowsUpserted = 0;
    
    @Column(name = "error_summary")
    private String errorSummary;
    
    // Constructors
    public SourceRun() {}
    
    // Getters and setters
    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getRowsUpserted() { return rowsUpserted; }
    public void setRowsUpserted(Integer rowsUpserted) { this.rowsUpserted = rowsUpserted; }
    
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
}