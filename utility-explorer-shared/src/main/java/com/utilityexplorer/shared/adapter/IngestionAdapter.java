package com.utilityexplorer.shared.adapter;

import com.utilityexplorer.shared.persistence.Metric;

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
}
