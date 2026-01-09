package com.utilityexplorer.persistence;

import com.utilityexplorer.shared.persistence.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
}