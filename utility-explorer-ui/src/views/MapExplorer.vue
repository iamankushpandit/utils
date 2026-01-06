<template>
  <div class="map-explorer">
    <h1>Map Explorer</h1>
    
    <div class="controls-panel">
      <div class="control-group">
        <label>Metric:</label>
        <select v-model="selectedMetric" @change="loadMapData">
          <option value="">Select metric...</option>
          <option v-for="metric in metrics" :key="metric.metricId" :value="metric.metricId">
            {{ metric.name }} ({{ metric.unit }})
          </option>
        </select>
      </div>
      
      <div class="control-group">
        <label>Source:</label>
        <select v-model="selectedSource" @change="loadMapData">
          <option value="">Select source...</option>
          <option v-for="source in sources" :key="source.sourceId" :value="source.sourceId">
            {{ source.name }}
          </option>
        </select>
      </div>
      
      <div class="control-group">
        <label>Period:</label>
        <select v-model="selectedPeriod" @change="loadMapData">
          <option value="2025-12">December 2025</option>
          <option value="2025-11">November 2025</option>
          <option value="2025-10">October 2025</option>
        </select>
      </div>
    </div>
    
    <div class="map-section">
      <MapComponent 
        :mapData="mapData" 
        @regionClick="onRegionClick"
      />
    </div>
    
    <RegionDrawer
      :isOpen="drawerOpen"
      :region="selectedRegion"
      :selectedMetric="selectedMetric"
      :selectedSource="selectedSource"
      @close="closeDrawer"
    />
  </div>
</template>

<script>
import { apiService } from '../services/api.js'
import MapComponent from '../components/MapComponent.vue'
import RegionDrawer from '../components/RegionDrawer.vue'

export default {
  name: 'MapExplorer',
  components: {
    MapComponent,
    RegionDrawer
  },
  data() {
    return {
      metrics: [],
      sources: [],
      selectedMetric: '',
      selectedSource: '',
      selectedPeriod: '2025-12',
      mapData: null,
      loading: false,
      drawerOpen: false,
      selectedRegion: null
    }
  },
  async mounted() {
    await this.loadCatalog()
  },
  methods: {
    async loadCatalog() {
      try {
        this.loading = true
        const [metrics, sources] = await Promise.all([
          apiService.getMetrics(),
          apiService.getSources()
        ])
        this.metrics = metrics
        this.sources = sources
      } catch (error) {
        console.error('Failed to load catalog:', error)
      } finally {
        this.loading = false
      }
    },
    
    async loadMapData() {
      if (!this.selectedMetric || !this.selectedSource) {
        this.mapData = null
        return
      }
      
      try {
        this.loading = true
        const params = {
          metricId: this.selectedMetric,
          sourceId: this.selectedSource,
          geoLevel: 'STATE',
          period: this.selectedPeriod
        }
        
        this.mapData = await apiService.getMap(params)
      } catch (error) {
        console.error('Failed to load map data:', error)
        this.mapData = null
      } finally {
        this.loading = false
      }
    },
    
    onRegionClick(region) {
      this.selectedRegion = region
      this.drawerOpen = true
    },
    
    closeDrawer() {
      this.drawerOpen = false
      this.selectedRegion = null
    }
  }
}
</script>

<style scoped>
.map-explorer {
  max-width: 100%;
}

.controls-panel {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 200px;
}

.control-group label {
  font-weight: 500;
  color: #333;
}

.control-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.map-placeholder {
  background: white;
  border-radius: 8px;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #e0e0e0;
}

.map-section {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.placeholder-content {
  text-align: center;
  color: #666;
}

.placeholder-content h3 {
  margin-bottom: 1rem;
  color: #333;
}

.hint {
  font-style: italic;
  margin-top: 1rem;
}
</style>