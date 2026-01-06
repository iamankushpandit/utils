package com.utilityexplorer.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
class TimeSeriesControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void getTimeSeries_withValidParams_returnsPoints() throws Exception {
        mockMvc.perform(get("/api/v1/timeseries")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("geoId", "20")
                .param("from", "2025-10-01")
                .param("to", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric.metricId").value("ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH"))
            .andExpect(jsonPath("$.source.sourceId").value("EIA"))
            .andExpect(jsonPath("$.region.geoId").value("20"))
            .andExpect(jsonPath("$.region.name").value("Kansas"))
            .andExpect(jsonPath("$.points").isArray())
            .andExpect(jsonPath("$.points[0].periodStart").exists())
            .andExpect(jsonPath("$.points[0].value").exists())
            .andExpect(jsonPath("$.points[0].retrievedAt").exists());
    }
    
    @Test
    void getTimeSeries_withInvalidRegion_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/timeseries")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("geoId", "99")
                .param("from", "2025-10-01")
                .param("to", "2025-12-31"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void exportCsv_withValidParams_returnsCsv() throws Exception {
        mockMvc.perform(get("/api/v1/export/csv")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("geoId", "20")
                .param("from", "2025-10-01")
                .param("to", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv"))
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"data.csv\""))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("periodStart,periodEnd,value,retrievedAt,sourcePublishedAt")));
    }
    
    @Test
    void exportCsv_withInvalidDates_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/export/csv")
                .param("metricId", "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH")
                .param("sourceId", "EIA")
                .param("geoLevel", "STATE")
                .param("geoId", "20")
                .param("from", "invalid-date")
                .param("to", "2025-12-31"))
            .andExpect(status().isBadRequest());
    }
}