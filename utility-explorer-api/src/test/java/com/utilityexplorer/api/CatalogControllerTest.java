package com.utilityexplorer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void getMetrics_returnsSeededMetric() throws Exception {
        mockMvc.perform(get("/api/v1/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"));
    }
    
    @Test
    void getSources_returnsSeededSource() throws Exception {
        mockMvc.perform(get("/api/v1/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sourceId").value("EIA"));
    }
    
    @Test
    void getCoverage_withValidIds_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/coverage")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"))
            .andExpect(jsonPath("$.sourceId").value("EIA"));
    }
    
    @Test
    void getCoverage_withInvalidIds_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/coverage")
                .param("metricId", "INVALID")
                .param("sourceId", "INVALID"))
            .andExpect(status().isNotFound());
    }
}