package com.utilityexplorer.api;

import com.utilityexplorer.utilagent.UtilAgentService;
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
@RequestMapping("/api/v1/util-agent")
@ConditionalOnProperty(name = "UTIL_AGENT_ENABLED", havingValue = "true", matchIfMissing = true)
@Tag(name = "Util Agent", description = "Text-based Util Agent queries (conditional on UTIL_AGENT_ENABLED)")
public class UtilAgentController {
    
    @Autowired
    private UtilAgentService utilAgentService;
    
    @Value("${UTIL_AGENT_API_KEY:dev_key_change_me}")
    private String apiKey;
    
    @PostMapping("/query")
    @Operation(
        summary = "Ask the Util Agent",
        description = "Submit a text question. Requires X-API-Key header matching UTIL_AGENT_API_KEY."
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
            UtilAgentResponse response = utilAgentService.processQuery(question);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "Query processing failed"));
        }
    }
}
