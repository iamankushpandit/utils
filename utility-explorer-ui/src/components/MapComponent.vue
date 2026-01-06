<template>
  <div class="map-container">
    <div ref="mapElement" class="map"></div>
    
    <div v-if="mapData && mapData.values.length > 0" class="legend">
      <h4>{{ mapData.metric.metricId }}</h4>
      <div class="legend-scale">
        <div class="legend-labels">
          <span>{{ mapData.legend.min }}</span>
          <span>{{ mapData.legend.max }}</span>
        </div>
        <div class="legend-bar"></div>
        <div class="legend-unit">{{ mapData.metric.unit }}</div>
      </div>
      <div class="legend-provenance">
        <small>Retrieved: {{ formatDate(mapData.retrievedAt) }}</small>
        <br>
        <small>Source: {{ mapData.source.name }}</small>
      </div>
    </div>
    
    <div v-else-if="mapData && mapData.values.length === 0" class="no-data-message">
      <p>No data available for the selected metric, source, and period.</p>
    </div>
  </div>
</template>

<script>
import L from 'leaflet'

export default {
  name: 'MapComponent',
  props: {
    mapData: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      map: null,
      geoJsonLayer: null,
      boundaries: null
    }
  },
  async mounted() {
    await this.initMap()
  },
  watch: {
    mapData: {
      handler: 'updateMap',
      deep: true
    }
  },
  methods: {
    async initMap() {
      // Initialize Leaflet map
      this.map = L.map(this.$refs.mapElement, {
        center: [39.8283, -98.5795], // Center of US
        zoom: 4,
        zoomControl: true
      })
      
      // Add base tile layer
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
      }).addTo(this.map)
      
      // Load boundaries
      await this.loadBoundaries()
      this.updateMap()
    },
    
    async loadBoundaries() {
      try {
        const response = await fetch('/boundaries/states.geojson')
        this.boundaries = await response.json()
      } catch (error) {
        console.error('Failed to load boundaries:', error)
      }
    },
    
    updateMap() {
      if (!this.map || !this.boundaries) return
      
      // Remove existing layer
      if (this.geoJsonLayer) {
        this.map.removeLayer(this.geoJsonLayer)
      }
      
      // Add boundaries with data
      this.geoJsonLayer = L.geoJSON(this.boundaries, {
        style: (feature) => this.getFeatureStyle(feature),
        onEachFeature: (feature, layer) => {
          const value = this.getFeatureValue(feature)
          const popup = value 
            ? `<b>${feature.properties.NAME}</b><br/>${value.value} ${this.mapData?.metric?.unit || ''}`
            : `<b>${feature.properties.NAME}</b><br/>No data available`
          layer.bindPopup(popup)
          
          layer.on('click', () => {
            this.$emit('regionClick', {
              geoLevel: 'STATE',
              geoId: feature.properties.STATEFP,
              name: feature.properties.NAME
            })
          })
        }
      }).addTo(this.map)
    },
    
    getFeatureStyle(feature) {
      const value = this.getFeatureValue(feature)
      
      if (!value || !this.mapData?.legend) {
        return {
          fillColor: '#f0f0f0',
          weight: 1,
          opacity: 1,
          color: '#666',
          fillOpacity: 0.3
        }
      }
      
      // Calculate color based on value
      const { min, max } = this.mapData.legend
      const ratio = (value.value - min) / (max - min)
      const color = this.interpolateColor(ratio)
      
      return {
        fillColor: color,
        weight: 2,
        opacity: 1,
        color: '#666',
        fillOpacity: 0.7
      }
    },
    
    getFeatureValue(feature) {
      if (!this.mapData?.values) return null
      return this.mapData.values.find(v => v.geoId === feature.properties.STATEFP)
    },
    
    interpolateColor(ratio) {
      // Green to red color scale
      const r = Math.round(255 * ratio)
      const g = Math.round(255 * (1 - ratio))
      return `rgb(${r}, ${g}, 0)`
    },
    
    formatDate(dateString) {
      if (!dateString) return 'Unknown'
      return new Date(dateString).toLocaleString()
    }
  },
  beforeUnmount() {
    if (this.map) {
      this.map.remove()
    }
  }
}
</script>

<style scoped>
.map-container {
  position: relative;
  height: 500px;
  border-radius: 8px;
  overflow: hidden;
}

.map {
  height: 100%;
  width: 100%;
}

.legend {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: white;
  padding: 15px;
  border-radius: 6px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  min-width: 200px;
}

.legend h4 {
  margin: 0 0 10px 0;
  font-size: 0.9rem;
  color: #333;
}

.legend-scale {
  margin-bottom: 10px;
}

.legend-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  margin-bottom: 5px;
}

.legend-bar {
  height: 10px;
  background: linear-gradient(to right, rgb(0,255,0), rgb(255,0,0));
  border-radius: 2px;
}

.legend-unit {
  text-align: center;
  font-size: 0.8rem;
  margin-top: 5px;
  color: #666;
}

.legend-provenance {
  border-top: 1px solid #eee;
  padding-top: 8px;
  color: #666;
}

.no-data-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  text-align: center;
}
</style>

<style>
/* Global Leaflet styles */
@import 'leaflet/dist/leaflet.css';
</style>