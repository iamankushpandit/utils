package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.scheduling.support.CronExpression;

@Service
public class StatusService {
    
    @Autowired
    private SourceRepository sourceRepository;
    
    @Autowired
    private SourceConfigRepository sourceConfigRepository;
    
    @Autowired
    private SourceRunRepository sourceRunRepository;
    
    public List<SourceStatusResponse> getSourcesStatus() {
        return sourceRepository.findAll().stream()
            .map(this::buildSourceStatus)
            .toList();
    }
    
    private SourceStatusResponse buildSourceStatus(Source source) {
        SourceStatusResponse status = new SourceStatusResponse();
        status.setSourceId(source.getSourceId());
        
        // Get config
        Optional<SourceConfig> config = sourceConfigRepository.findById(source.getSourceId());
        if (config.isPresent()) {
            status.setEnabled(config.get().getEnabled());
            status.setScheduleCron(config.get().getScheduleCron());
            status.setTimezone(config.get().getTimezone());
            String next = computeNextRun(config.get().getScheduleCron(), config.get().getTimezone());
            status.setNextRunAt(next);
        } else {
            status.setEnabled(false);
            status.setScheduleCron(null);
            status.setTimezone("UTC");
            status.setNextRunAt(null);
        }
        
        // Get last run
        Optional<SourceRun> lastRun = sourceRunRepository.findLatestBySourceId(source.getSourceId());
        if (lastRun.isPresent()) {
            SourceRun run = lastRun.get();
            status.setLastRun(new LastRunInfo(
                run.getRunId().toString(),
                run.getStatus(),
                run.getStartedAt().toString(),
                run.getEndedAt() != null ? run.getEndedAt().toString() : null,
                run.getRowsUpserted(),
                run.getErrorSummary()
            ));
        } else {
            status.setLastRun(null);
        }
        
        // Get last success
        Optional<SourceRun> lastSuccess = sourceRunRepository.findLatestSuccessBySourceId(source.getSourceId());
        if (lastSuccess.isPresent()) {
            status.setLastSuccessAt(lastSuccess.get().getStartedAt().toString());
        } else {
            status.setLastSuccessAt(null);
        }
        
        return status;
    }

    private String computeNextRun(String cron, String timezone) {
        if (cron == null || cron.isBlank()) return null;
        try {
            CronExpression expr = CronExpression.parse(cron);
            ZoneId zone = timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");
            ZonedDateTime now = ZonedDateTime.now(zone);
            ZonedDateTime next = expr.next(now);
            return next != null ? next.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
