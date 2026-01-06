package com.utilityexplorer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {
    
    @Query("SELECT r FROM Region r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Region> findByNameContainingIgnoreCase(@Param("query") String query);
    
    Optional<Region> findByGeoLevelAndGeoId(String geoLevel, String geoId);
    
    List<Region> findByParentRegionPk(UUID parentRegionPk);
}