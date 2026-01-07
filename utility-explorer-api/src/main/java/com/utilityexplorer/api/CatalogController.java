package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catalog", description = "Metric and source catalog")
public class CatalogController {
    
    @Autowired
    private CatalogService catalogService;
    
    @GetMapping("/metrics")
    @Operation(summary = "List metrics")
    public List<MetricDto> getMetrics() {
        return catalogService.getAllMetrics();
    }
    
    @GetMapping("/sources")
    @Operation(summary = "List sources")
    public List<SourceDto> getSources() {
        return catalogService.getAllSources();
    }
    
    @GetMapping("/coverage")
    @Operation(summary = "Get coverage for a metric/source")
    public ResponseEntity<?> getCoverage(
            @RequestParam String metricId,
            @RequestParam String sourceId) {
        
        return catalogService.getCoverage(metricId, sourceId)
            .map(coverage -> ResponseEntity.ok(coverage))
            .orElse(ResponseEntity.notFound().build());
    }
}
