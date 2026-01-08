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
class StatusControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void getSourcesStatus_returnsSourceWithConfig() throws Exception {
        mockMvc.perform(get("/api/v1/status/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sourceId").value("EIA"))
            .andExpect(jsonPath("$[0].enabled").value(true))
            .andExpect(jsonPath("$[0].scheduleCron").value("0 0 9 * * MON"))
            .andExpect(jsonPath("$[0].timezone").value("UTC"));
    }
    
    @Test
    void getSourcesStatus_includesLastRun() throws Exception {
        mockMvc.perform(get("/api/v1/status/sources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].lastRun").exists())
            .andExpect(jsonPath("$[0].lastRun.status").exists())
            .andExpect(jsonPath("$[0].lastRun.startedAt").exists())
            .andExpect(jsonPath("$[0].lastSuccessAt").exists());
    }
}