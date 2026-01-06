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
class RegionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void searchRegions_withKan_returnsKansas() throws Exception {
        mockMvc.perform(get("/api/v1/regions/search").param("q", "Kan"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("Kansas"))
            .andExpect(jsonPath("$[0].geoId").value("20"));
    }
    
    @Test
    void getRegion_withValidStateId_returnsKansas() throws Exception {
        mockMvc.perform(get("/api/v1/regions/STATE/20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Kansas"))
            .andExpect(jsonPath("$.geoLevel").value("STATE"))
            .andExpect(jsonPath("$.geoId").value("20"));
    }
    
    @Test
    void getRegion_withInvalidId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/regions/STATE/99"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void getChildren_withNoChildren_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/regions/STATE/20/children"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }
}