package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.TimeSeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "TimeSeries", description = "Timeseries and CSV export endpoints")
public class TimeSeriesController {
    
    @Autowired
    private TimeSeriesService timeSeriesService;
    
    @GetMapping("/timeseries")
    @Operation(
        summary = "Get timeseries values",
        description = "Returns timeseries for metric/source/geo over the requested date range (ISO dates)."
    )
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
            @RequestParam String metricId,
            @RequestParam String sourceId,
            @RequestParam String geoLevel,
            @RequestParam String geoId,
            @RequestParam String from,
            @RequestParam String to) {
        
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            
            return timeSeriesService.getTimeSeries(metricId, sourceId, geoLevel, geoId, fromDate, toDate)
                .map(timeSeries -> ResponseEntity.ok(timeSeries))
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/export/csv")
    @Operation(
        summary = "Export timeseries as CSV",
        description = "Exports timeseries for metric/source/geo to CSV over the requested date range (ISO dates)."
    )
    public ResponseEntity<String> exportCsv(
            @RequestParam String metricId,
            @RequestParam String sourceId,
            @RequestParam String geoLevel,
            @RequestParam String geoId,
            @RequestParam String from,
            @RequestParam String to) {
        
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            
            String csv = timeSeriesService.generateCsv(metricId, sourceId, geoLevel, geoId, fromDate, toDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "data.csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
