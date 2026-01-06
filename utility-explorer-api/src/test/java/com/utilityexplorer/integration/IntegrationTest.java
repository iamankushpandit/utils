package com.utilityexplorer.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
class IntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void healthEndpoint_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
    
    @Test
    void metricsEndpoint_returnsSeededData() throws Exception {
        mockMvc.perform(get("/api/v1/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"));
    }
    
    @Test
    void sourcesEndpoint_returnsSeededData() throws Exception {
        mockMvc.perform(get("/api/v1/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sourceId").value("EIA"));
    }
    
    @Test
    void mapEndpoint_returnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/map")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("period", "2025-12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric.metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"))
            .andExpect(jsonPath("$.source.sourceId").value("EIA"))
            .andExpect(jsonPath("$.retrievedAt").exists());
    }
    
    @Test
    void timeseriesEndpoint_returnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/timeseries")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("geoId", "20")
                .param("from", "2025-10-01")
                .param("to", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.region.name").value("Kansas"))
            .andExpect(jsonPath("$.points").isArray());
    }
    
    @Test
    void statusEndpoint_returnsSourceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/status/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sourceId").value("EIA"))
            .andExpect(jsonPath("$[0].enabled").value(true));
    }
}