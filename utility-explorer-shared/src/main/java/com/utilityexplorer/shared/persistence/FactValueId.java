package com.utilityexplorer.shared.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class FactValueId implements Serializable {
    private String metricId;
    private String sourceId;
    private String geoLevel;
    private String geoId;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    public FactValueId() {}

    public FactValueId(String metricId, String sourceId, String geoLevel, String geoId, LocalDate periodStart, LocalDate periodEnd) {
        this.metricId = metricId;
        this.sourceId = sourceId;
        this.geoLevel = geoLevel;
        this.geoId = geoId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactValueId that = (FactValueId) o;
        return Objects.equals(metricId, that.metricId) &&
                Objects.equals(sourceId, that.sourceId) &&
                Objects.equals(geoLevel, that.geoLevel) &&
                Objects.equals(geoId, that.geoId) &&
                Objects.equals(periodStart, that.periodStart) &&
                Objects.equals(periodEnd, that.periodEnd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricId, sourceId, geoLevel, geoId, periodStart, periodEnd);
    }
}
