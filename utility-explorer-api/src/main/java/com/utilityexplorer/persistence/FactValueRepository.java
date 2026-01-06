package com.utilityexplorer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FactValueRepository extends JpaRepository<FactValue, FactValueId> {
    
    @Query("SELECT f FROM FactValue f WHERE f.metricId = :metricId AND f.sourceId = :sourceId " +
           "AND f.geoLevel = :geoLevel AND f.periodStart = :periodStart AND f.periodEnd = :periodEnd")
    List<FactValue> findMapData(@Param("metricId") String metricId,
                               @Param("sourceId") String sourceId,
                               @Param("geoLevel") String geoLevel,
                               @Param("periodStart") LocalDate periodStart,
                               @Param("periodEnd") LocalDate periodEnd);
}