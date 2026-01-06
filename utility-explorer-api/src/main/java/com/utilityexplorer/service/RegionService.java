package com.utilityexplorer.service;

import com.utilityexplorer.dto.ApiDtos.*;
import com.utilityexplorer.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegionService {
    
    @Autowired
    private RegionRepository regionRepository;
    
    public List<RegionDto> searchRegions(String query) {
        return regionRepository.findByNameContainingIgnoreCase(query).stream()
            .map(this::toRegionDto)
            .toList();
    }
    
    public Optional<RegionDto> getRegion(String geoLevel, String geoId) {
        return regionRepository.findByGeoLevelAndGeoId(geoLevel, geoId)
            .map(this::toRegionDto);
    }
    
    public List<RegionDto> getChildren(String geoLevel, String geoId) {
        Optional<Region> parent = regionRepository.findByGeoLevelAndGeoId(geoLevel, geoId);
        if (parent.isEmpty()) {
            return List.of();
        }
        
        return regionRepository.findByParentRegionPk(parent.get().getRegionPk()).stream()
            .map(this::toRegionDto)
            .toList();
    }
    
    private RegionDto toRegionDto(Region region) {
        String parentGeoLevel = null;
        String parentGeoId = null;
        
        if (region.getParentRegionPk() != null) {
            Optional<Region> parent = regionRepository.findById(region.getParentRegionPk());
            if (parent.isPresent()) {
                parentGeoLevel = parent.get().getGeoLevel();
                parentGeoId = parent.get().getGeoId();
            }
        }
        
        return new RegionDto(
            region.getGeoLevel(),
            region.getGeoId(),
            region.getName(),
            parentGeoLevel,
            parentGeoId,
            region.getCentroidLat(),
            region.getCentroidLon()
        );
    }
}