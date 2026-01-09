package com.utilityexplorer.ingestion;

import com.utilityexplorer.persistence.*;
import com.utilityexplorer.shared.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "INGESTION_DISPATCHER_ENABLED", havingValue = "true")
public class IngestionDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(IngestionDispatcher.class);
    
    @Autowired
    private SourceConfigRepository sourceConfigRepository;
    
    @Autowired
    private SourceRunRepository sourceRunRepository;
    
    @Autowired
    private List<SourcePlugin> sourcePlugins;
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${INGESTION_TICK_SECONDS:600}")
    private int tickSeconds;
    
    @Scheduled(fixedDelayString = "${INGESTION_TICK_SECONDS:600}000")
    public void dispatchIngestion() {
        Instant now = Instant.now();

        if (sourcePlugins == null || sourcePlugins.isEmpty()) {
            logger.warn("Ingestion dispatcher has no source plugins registered.");
            return;
        }

        logger.info("Dispatching ingestion for {} plugin(s): {}", sourcePlugins.size(),
            sourcePlugins.stream().map(p -> p.getClass().getSimpleName()).toList());
        
        for (SourcePlugin plugin : sourcePlugins) {
            try {
                if (acquireLock(plugin.getSourceId())) {
                    logger.info("Running ingestion for source {}", plugin.getSourceId());
                    runIngestion(plugin, now);
                }
            } catch (Exception e) {
                logger.error("Ingestion failed for {}: {}", plugin.getSourceId(), e.getMessage());
            } finally {
                releaseLock(plugin.getSourceId());
            }
        }
    }

    public void runOnce() {
        dispatchIngestion();
    }

    public void runOnceForSource(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        for (SourcePlugin plugin : sourcePlugins) {
            if (!sourceId.equalsIgnoreCase(plugin.getSourceId())) {
                continue;
            }
            try {
                if (acquireLock(plugin.getSourceId())) {
                    logger.info("Running ingestion for source {}", plugin.getSourceId());
                    runIngestion(plugin, now);
                }
            } catch (Exception e) {
                logger.error("Ingestion failed for {}: {}", plugin.getSourceId(), e.getMessage());
            } finally {
                releaseLock(plugin.getSourceId());
            }
            break;
        }
    }
    
    private boolean acquireLock(String sourceId) {
        try {
            // Postgres advisory lock
            Boolean result = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_lock(hashtext(?))", Boolean.class, sourceId);
            boolean acquired = result != null && result;
            if (!acquired) {
                logger.warn("Skipped ingestion for {} because advisory lock is held", sourceId);
            }
            return acquired;
        } catch (Exception e) {
            logger.error("Failed to acquire advisory lock for {}: {}", sourceId, e.getMessage());
            return false;
        }
    }
    
    private void releaseLock(String sourceId) {
        try {
            jdbcTemplate.execute("SELECT pg_advisory_unlock(hashtext('" + sourceId + "'))");
        } catch (Exception e) {
            // Log but don't fail
        }
    }
    
    private void runIngestion(SourcePlugin plugin, Instant now) {
        UUID runId = UUID.randomUUID();
        SourceRun run = new SourceRun();
        run.setRunId(runId);
        run.setSourceId(plugin.getSourceId());
        run.setStartedAt(now);
        run.setStatus("RUNNING");
        
        sourceRunRepository.save(run);
        
        try {
            SourceContext ctx = new SourceContext(now, dataSource, Clock.systemUTC());
            SourceCheckResult check = plugin.checkForUpdates(ctx);
            
            if (!check.hasUpdates) {
                run.setStatus("NO_CHANGE");
                run.setRowsUpserted(0);
            } else {
                IngestResult result = plugin.ingest(ctx, check);
                run.setStatus(result.noChange ? "NO_CHANGE" : "SUCCESS");
                run.setRowsUpserted(result.rowsUpserted);
            }
            
        } catch (Exception e) {
            run.setStatus("FAILED");
            run.setErrorSummary(e.getMessage());
        } finally {
            run.setEndedAt(Instant.now());
            sourceRunRepository.save(run);
        }
    }
}
