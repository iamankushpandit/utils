package com.utilityexplorer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRunRepository extends JpaRepository<SourceRun, UUID> {
    
    @Query("SELECT sr FROM SourceRun sr WHERE sr.sourceId = :sourceId ORDER BY sr.startedAt DESC LIMIT 1")
    Optional<SourceRun> findLatestBySourceId(@Param("sourceId") String sourceId);
    
    @Query("SELECT sr FROM SourceRun sr WHERE sr.sourceId = :sourceId AND sr.status = 'SUCCESS' ORDER BY sr.startedAt DESC LIMIT 1")
    Optional<SourceRun> findLatestSuccessBySourceId(@Param("sourceId") String sourceId);
}