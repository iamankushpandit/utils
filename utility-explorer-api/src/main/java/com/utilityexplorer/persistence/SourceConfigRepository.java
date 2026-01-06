package com.utilityexplorer.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceConfigRepository extends JpaRepository<SourceConfig, String> {
}