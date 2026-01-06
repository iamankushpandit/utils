<template>
  <div class="map-explorer">
    <h1>Map Explorer</h1>
    
    <div class="controls-panel">
      <div class="control-group">
        <label>Metric:</label>
        <select v-model="selectedMetric" @change="loadData">
          <option value="">Select metric...</option>
          <option v-for="metric in metrics" :key="metric.metricId" :value="metric.metricId">
            {{ metric.name }} ({{ metric.unit }})
          </option>
        </select>
      </div>
      
      <div class="control-group">
        <label>Source:</label>
        <select v-model="selectedSource" @change="loadData">
          <option value="">Select source...</option>
          <option v-for="source in sources" :key="source.sourceId" :value="source.sourceId">
            {{ source.name }}
          </option>
        </select>
      </div>
    </div>
    
    <div class="map-placeholder">
      <div class="placeholder-content">
        <h3>Map Visualization</h3>
        <p>Interactive map will be implemented in Day 11</p>
        <p v-if="selectedMetric && selectedSource">
          Selected: {{ selectedMetric }} from {{ selectedSource }}
        </p>
        <p v-else class="hint">
          Select a metric and source to view data on the map
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { apiService } from '../services/api.js'

export default {
  name: 'MapExplorer',
  data() {
    return {
      metrics: [],
      sources: [],
      selectedMetric: '',
      selectedSource: '',
      loading: false
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
    
    loadData() {
      if (this.selectedMetric && this.selectedSource) {
        console.log('Would load map data for:', this.selectedMetric, this.selectedSource)
      }
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