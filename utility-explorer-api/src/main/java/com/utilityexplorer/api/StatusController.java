package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/status")
@Tag(name = "Status", description = "Source status and schedule")
public class StatusController {
    
    @Autowired
    private StatusService statusService;
    
    @GetMapping("/sources")
    @Operation(summary = "Get source statuses")
    public List<SourceStatusResponse> getSourcesStatus() {
        return statusService.getSourcesStatus();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get metric ingestion statuses (aggregated from sources)")
    public List<MetricStatusResponse> getMetricsStatus() {
        return statusService.getMetricStatuses();
    }
}
