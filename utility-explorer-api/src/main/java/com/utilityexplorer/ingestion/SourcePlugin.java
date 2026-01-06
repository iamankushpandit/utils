package com.utilityexplorer.ingestion;

public interface SourcePlugin {
    String getSourceId();
    SourceCheckResult checkForUpdates(SourceContext ctx) throws Exception;
    IngestResult ingest(SourceContext ctx, SourceCheckResult check) throws Exception;
}

class SourceContext {
    public final java.time.Instant now;
    public final javax.sql.DataSource dataSource;
    public final java.time.Clock clock;
    
    public SourceContext(java.time.Instant now, javax.sql.DataSource dataSource, java.time.Clock clock) {
        this.now = now;
        this.dataSource = dataSource;
        this.clock = clock;
    }
}

class SourceCheckResult {
    public final boolean hasUpdates;
    public final String updateToken;
    public final java.time.Instant sourcePublishedAt;
    
    public SourceCheckResult(boolean hasUpdates, String updateToken, java.time.Instant sourcePublishedAt) {
        this.hasUpdates = hasUpdates;
        this.updateToken = updateToken;
        this.sourcePublishedAt = sourcePublishedAt;
    }
}

class IngestResult {
    public final int rowsUpserted;
    public final java.util.UUID payloadId;
    public final boolean noChange;
    
    public IngestResult(int rowsUpserted, java.util.UUID payloadId, boolean noChange) {
        this.rowsUpserted = rowsUpserted;
        this.payloadId = payloadId;
        this.noChange = noChange;
    }
}