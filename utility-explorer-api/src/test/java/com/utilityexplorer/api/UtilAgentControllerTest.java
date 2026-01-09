package com.utilityexplorer.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "UTIL_AGENT_ENABLED=true")
class UtilAgentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        // Create a MockRestServiceServer to mock the RestTemplate calls
        // ignoreExpectOrder is useful when multiple calls might happen (though here it's 1)
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }
    
    @Test
    void queryUtilAgent_viaIntelligenceService_returnsResponse() throws Exception {
        // Mock successful Python response
        String jsonResponse = "{\"answer\": \"Mocked Intelligence Answer\", \"sources\": []}";
        
        mockServer.expect(requestTo(containsString("/query")))
            .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"why is sky blue?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.summary").value("Mocked Intelligence Answer"))
            .andExpect(jsonPath("$.responseOrigin").value("intelligence-service-v1"));
        
        mockServer.reset();
    }
    
    @Test
    void queryUtilAgent_withoutApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/util-agent/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"test question\"}"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void queryUtilAgent_withValidKey_returnsResponse() throws Exception {
        // Force fallback by simulating error from Python service
        mockServer.expect(requestTo(containsString("/query")))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"high electricity and low broadband\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.summary").exists());
            
        mockServer.reset();
    }
    
    @Test
    void queryUtilAgent_withInvalidQuery_returnsInsufficientData() throws Exception {
        // Force fallback
        mockServer.expect(requestTo(containsString("/query")))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"unsupported query type\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));
            
        mockServer.reset();
    }

    @Test
    void queryUtilAgent_stateWithLeastCentPerKwh_returnsOk() throws Exception {
        // Force fallback so deterministic logic runs (Legacy behavior)
        mockServer.expect(requestTo(containsString("/query")))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/v1/util-agent/query")
                .header("X-API-Key", "dev_key_change_me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\": \"state with least cent/kWh\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.table.rows").isNotEmpty());
            
        mockServer.reset();
    }
}
