package com.utilityexplorer.api;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Map", description = "Map data endpoints")
public class MapController {
    
    @Autowired
    private MapService mapService;
    
    @GetMapping("/map")
    @Operation(
        summary = "Get map values",
        description = "Returns choropleth values for the requested metric/source and geo level. Use geoLevel=STATE for national view, or geoLevel=COUNTY with parentGeoLevel=STATE and parentGeoId (FIPS) for drilldown."
    )
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

    @GetMapping("/map/range")
    @Operation(
        summary = "Get map values over a period range",
        description = "Returns a list of map snapshots for each period between startPeriod and endPeriod (inclusive). Pass geoLevel=STATE for national view or geoLevel=COUNTY with parentGeoLevel=STATE + parentGeoId for drilldowns."
    )
    public ResponseEntity<MapRangeResponse> getMapRange(
            @RequestParam String metricId,
            @RequestParam String sourceId,
            @RequestParam String geoLevel,
            @RequestParam(required = false) String parentGeoLevel,
            @RequestParam(required = false) String parentGeoId,
            @RequestParam String startPeriod,
            @RequestParam String endPeriod) {
        return mapService.getMapDataRange(metricId, sourceId, geoLevel, parentGeoLevel, parentGeoId, startPeriod, endPeriod)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.badRequest().build());
    }
}
