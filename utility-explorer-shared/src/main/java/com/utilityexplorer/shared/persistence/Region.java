package com.utilityexplorer.shared.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "region")
public class Region {

    @Id
    @Column(name = "region_pk")
    private UUID regionPk;

    @Column(name = "geo_level", nullable = false)
    private String geoLevel;

    @Column(name = "geo_id", nullable = false)
    private String geoId;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_region_pk")
    private UUID parentRegionPk;

    @Column(name = "centroid_lat")
    private Double centroidLat;

    @Column(name = "centroid_lon")
    private Double centroidLon;

    // Constructors
    public Region() {}

    public Region(UUID regionPk, String geoLevel, String geoId, String name,
                  UUID parentRegionPk, Double centroidLat, Double centroidLon) {
        this.regionPk = regionPk;
        this.geoLevel = geoLevel;
        this.geoId = geoId;
        this.name = name;
        this.parentRegionPk = parentRegionPk;
        this.centroidLat = centroidLat;
        this.centroidLon = centroidLon;
    }

    // Getters and setters
    public UUID getRegionPk() { return regionPk; }
    public void setRegionPk(UUID regionPk) { this.regionPk = regionPk; }

    public String getGeoLevel() { return geoLevel; }
    public void setGeoLevel(String geoLevel) { this.geoLevel = geoLevel; }

    public String getGeoId() { return geoId; }
    public void setGeoId(String geoId) { this.geoId = geoId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getParentRegionPk() { return parentRegionPk; }
    public void setParentRegionPk(UUID parentRegionPk) { this.parentRegionPk = parentRegionPk; }

    public Double getCentroidLat() { return centroidLat; }
    public void setCentroidLat(Double centroidLat) { this.centroidLat = centroidLat; }

    public Double getCentroidLon() { return centroidLon; }
    public void setCentroidLon(Double centroidLon) { this.centroidLon = centroidLon; }
}
