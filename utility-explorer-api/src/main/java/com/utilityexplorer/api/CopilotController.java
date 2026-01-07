package com.utilityexplorer.api;

import com.utilityexplorer.copilot.CopilotService;
import com.utilityexplorer.dto.ApiDtos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/copilot")
@ConditionalOnProperty(name = "COPILOT_ENABLED", havingValue = "true")
@Tag(name = "Copilot", description = "Text-based copilot queries (conditional on COPILOT_ENABLED)")
public class CopilotController {
    
    @Autowired
    private CopilotService copilotService;
    
    @Value("${COPILOT_API_KEY:dev_key_change_me}")
    private String apiKey;
    
    @PostMapping("/query")
    @Operation(
        summary = "Ask the copilot",
        description = "Submit a text question. Requires X-API-Key header matching COPILOT_API_KEY."
    )
    public ResponseEntity<?> query(
            @RequestHeader(value = "X-API-Key", required = false) String providedKey,
            @RequestBody Map<String, String> request) {
        
        // API key validation
        if (providedKey == null || !apiKey.equals(providedKey)) {
            return ResponseEntity.status(401)
                .body(new ErrorResponse("UNAUTHORIZED", "Valid API key required"));
        }
        
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "Question is required"));
        }
        
        try {
            CopilotResponse response = copilotService.processQuery(question);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "Query processing failed"));
        }
    }
}
