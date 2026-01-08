package com.utilityexplorer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "UTIL_AGENT_ENABLED=true")
class UtilAgentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void queryUtilAgent_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/util-agent/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"test question\"}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void queryUtilAgent_withValidKey_returnsResponse() throws Exception {
        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"high electricity and low broadband\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.summary").exists());
    }
    
    @Test
    void queryUtilAgent_withInvalidQuery_returnsInsufficientData() throws Exception {
        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"unsupported query type\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));
    }

    @Test
    void queryUtilAgent_stateWithLeastCentPerKwh_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"state with least cent/kWh\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.table.rows").isNotEmpty());
    }
}
