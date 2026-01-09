package com.utilityexplorer.persistence;

import com.utilityexplorer.shared.persistence.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricRepository extends JpaRepository<Metric, String> {
}