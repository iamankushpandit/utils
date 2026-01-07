package com.utilityexplorer.api;

import com.utilityexplorer.ingestion.IngestionDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingestion")
@Tag(name = "Ingestion", description = "Trigger ingestion runs")
public class IngestionController {
    private final ObjectProvider<IngestionDispatcher> dispatcherProvider;

    public IngestionController(ObjectProvider<IngestionDispatcher> dispatcherProvider) {
        this.dispatcherProvider = dispatcherProvider;
    }

    @PostMapping("/run")
    @Operation(summary = "Run ingestion for all sources")
    public ResponseEntity<Map<String, String>> runNow() {
        IngestionDispatcher dispatcher = dispatcherProvider.getIfAvailable();
        if (dispatcher == null) {
            return ResponseEntity.status(409).body(Map.of(
                "status", "disabled",
                "message", "Ingestion is disabled. Set INGESTION_DISPATCHER_ENABLED=true to enable."
            ));
        }

        dispatcher.runOnce();
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Ingestion dispatched."
        ));
    }

    @PostMapping("/run/{sourceId}")
    @Operation(summary = "Run ingestion for a single source")
    public ResponseEntity<Map<String, String>> runNowForSource(@PathVariable String sourceId) {
        IngestionDispatcher dispatcher = dispatcherProvider.getIfAvailable();
        if (dispatcher == null) {
            return ResponseEntity.status(409).body(Map.of(
                "status", "disabled",
                "message", "Ingestion is disabled. Set INGESTION_DISPATCHER_ENABLED=true to enable."
            ));
        }

        dispatcher.runOnceForSource(sourceId);
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Ingestion dispatched for " + sourceId
        ));
    }
}
