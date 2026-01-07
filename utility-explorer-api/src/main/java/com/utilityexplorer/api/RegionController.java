package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
@Tag(name = "Regions", description = "Region lookup and hierarchy")
public class RegionController {
    
    @Autowired
    private RegionService regionService;
    
    @GetMapping("/search")
    @Operation(summary = "Search regions by name or code")
    public List<RegionDto> searchRegions(@RequestParam String q) {
        return regionService.searchRegions(q);
    }
    
    @GetMapping("/{geoLevel}/{geoId}")
    @Operation(summary = "Get a single region by level and id")
    public ResponseEntity<RegionDto> getRegion(
            @PathVariable String geoLevel,
            @PathVariable String geoId) {
        
        return regionService.getRegion(geoLevel, geoId)
            .map(region -> ResponseEntity.ok(region))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{geoLevel}/{geoId}/children")
    @Operation(summary = "List child regions for a given parent")
    public List<RegionDto> getChildren(
            @PathVariable String geoLevel,
            @PathVariable String geoId) {
        
        return regionService.getChildren(geoLevel, geoId);
    }
}
