package com.utilityexplorer.shared.adapter;

import com.utilityexplorer.shared.persistence.Metric;
import com.utilityexplorer.shared.dto.MetricDefinition;
import java.util.List;

public interface IngestionAdapter {
    /**
     * Identifies which adapter implementation this is (e.g., "EIA_API", "FCC_API").
     * Must match the 'ingestion.adapter' field in metrics.yaml.
     */
    String getAdapterId();

    /**
     * Triggers the collection logic for the given metric.
     * @param metric The metadata defining what to fetch (series ID, variable name, etc.)
     */
    void collect(Metric metric);

    /**
     * Returns the definitions of all metrics managed by this adapter.
     * Used for automated metadata discovery.
     */
    List<MetricDefinition> getMetricDefinitions();
}
