package com.utilityexplorer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
@TestPropertySource(properties = "COPILOT_ENABLED=true")
class CopilotControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void queryCopilot_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/copilot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"test question\"}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void queryCopilot_withValidKey_returnsResponse() throws Exception {
        mockMvc.perform(post("/api/v1/copilot/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"high electricity and low broadband\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.summary").exists());
    }
    
    @Test
    void queryCopilot_withInvalidQuery_returnsInsufficientData() throws Exception {
        mockMvc.perform(post("/api/v1/copilot/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"unsupported query type\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));
    }
}