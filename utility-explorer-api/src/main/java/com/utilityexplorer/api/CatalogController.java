package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    
    @Autowired
    private CatalogService catalogService;
    
    @GetMapping("/metrics")
    public List<MetricDto> getMetrics() {
        return catalogService.getAllMetrics();
    }
    
    @GetMapping("/sources")
    public List<SourceDto> getSources() {
        return catalogService.getAllSources();
    }
    
    @GetMapping("/coverage")
    public ResponseEntity<?> getCoverage(
            @RequestParam String metricId,
            @RequestParam String sourceId) {
        
        return catalogService.getCoverage(metricId, sourceId)
            .map(coverage -> ResponseEntity.ok(coverage))
            .orElse(ResponseEntity.notFound().build());
    }
}