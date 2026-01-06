package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MapController {
    
    @Autowired
    private MapService mapService;
    
    @GetMapping("/map")
    public ResponseEntity<MapResponse> getMap(
            @RequestParam String metricId,
            @RequestParam String sourceId,
            @RequestParam String geoLevel,
            @RequestParam(required = false) String parentGeoLevel,
            @RequestParam(required = false) String parentGeoId,
            @RequestParam String period) {
        
        return mapService.getMapData(metricId, sourceId, geoLevel, parentGeoLevel, parentGeoId, period)
            .map(mapData -> ResponseEntity.ok(mapData))
            .orElse(ResponseEntity.badRequest().build());
    }
}