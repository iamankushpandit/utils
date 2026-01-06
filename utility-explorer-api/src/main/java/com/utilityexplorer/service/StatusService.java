package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
        } else {
            status.setEnabled(false);
            status.setScheduleCron(null);
            status.setTimezone("UTC");
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
}