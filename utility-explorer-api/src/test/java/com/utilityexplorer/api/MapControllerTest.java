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
class MapControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void getMap_withValidParams_returnsMapData() throws Exception {
        mockMvc.perform(get("/api/v1/map")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("period", "2025-12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric.metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"))
            .andExpect(jsonPath("$.source.sourceId").value("EIA"))
            .andExpect(jsonPath("$.geoLevel").value("STATE"))
            .andExpect(jsonPath("$.values").isArray());
    }
    
    @Test
    void getMap_withNoData_returnsEmptyValues() throws Exception {
        mockMvc.perform(get("/api/v1/map")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("period", "2020-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.values").isArray());
    }
    
    @Test
    void getMap_withInvalidMetric_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/map")
                .param("metricId", "INVALID")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("period", "2025-12"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getMapRange_withValidRange_returnsSnapshots() throws Exception {
        mockMvc.perform(get("/api/v1/map/range")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("startPeriod", "2025-01")
                .param("endPeriod", "2025-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maps").isArray());
    }
}
