package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {
    
    @Autowired
    private RegionService regionService;
    
    @GetMapping("/search")
    public List<RegionDto> searchRegions(@RequestParam String q) {
        return regionService.searchRegions(q);
    }
    
    @GetMapping("/{geoLevel}/{geoId}")
    public ResponseEntity<RegionDto> getRegion(
            @PathVariable String geoLevel,
            @PathVariable String geoId) {
        
        return regionService.getRegion(geoLevel, geoId)
            .map(region -> ResponseEntity.ok(region))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{geoLevel}/{geoId}/children")
    public List<RegionDto> getChildren(
            @PathVariable String geoLevel,
            @PathVariable String geoId) {
        
        return regionService.getChildren(geoLevel, geoId);
    }
}