package com.utilityexplorer.shared.dto;

import java.io.Serializable;

/**
 * Represents the metadata of a metric available in the system.
 * This is broadcasted by Adapters to the Intelligence Service.
 */
public record MetricDefinition(
    String metricId,
    String description,
    String unitLabel,
    String displayName,
    String sourceSystem
) implements Serializable {}
