<template>
  <div class="transparency">
    <h1>Transparency & Methodology</h1>
    
    <section class="principles">
      <h2>Our Principles</h2>
      <div class="principle-grid">
        <div class="principle-card">
          <h3>No Data Invention</h3>
          <p>We never forecast, estimate, or fill gaps in data. If a data point is missing, we show "No data available."</p>
        </div>
        
        <div class="principle-card">
          <h3>No Silent Blending</h3>
          <p>When multiple sources provide similar metrics, we show them separately, never blend them without clear labeling.</p>
        </div>
        
        <div class="principle-card">
          <h3>Provenance Everywhere</h3>
          <p>Every displayed value includes the source, when we retrieved it, and when the source published it (if available).</p>
        </div>
        
        <div class="principle-card">
          <h3>Free Public Data Only</h3>
          <p>We only use free, publicly available datasets. No paid data sources or proprietary information.</p>
        </div>
      </div>
    </section>
    
    <section class="data-status">
      <h2>Live Data Status</h2>
      <div v-if="loading" class="loading">Loading status...</div>
      <div v-else-if="error" class="error">Failed to load status: {{ error }}</div>
      <div v-else class="status-table">
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>Status</th>
              <th>Schedule</th>
              <th>Last Success</th>
              <th>Last Run</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="source in sourcesStatus" :key="source.sourceId">
              <td class="source-name">{{ getSourceName(source.sourceId) }}</td>
              <td>
                <span :class="['status-badge', source.enabled ? 'enabled' : 'disabled']">
                  {{ source.enabled ? 'Enabled' : 'Disabled' }}
                </span>
              </td>
              <td class="schedule">{{ source.scheduleCron || 'Not configured' }}</td>
              <td class="timestamp">{{ formatTimestamp(source.lastSuccessAt) }}</td>
              <td>
                <div v-if="source.lastRun" class="last-run">
                  <span :class="['run-status', source.lastRun.status.toLowerCase()]">
                    {{ source.lastRun.status }}
                  </span>
                  <div class="run-time">{{ formatTimestamp(source.lastRun.startedAt) }}</div>
                </div>
                <span v-else class="no-runs">No runs yet</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
    
    <section class="methodology">
      <h2>What We Do</h2>
      <ul>
        <li>Collect data from official government sources on their published schedules</li>
        <li>Store raw data with timestamps and source attribution</li>
        <li>Display data exactly as published, with clear geographic and temporal coverage</li>
        <li>Provide CSV exports with full provenance information</li>
      </ul>
      
      <h2>What We Don't Do</h2>
      <ul>
        <li>Forecast future values or trends</li>
        <li>Estimate missing data points</li>
        <li>Smooth or adjust published values</li>
        <li>Combine data from different sources without clear labeling</li>
      </ul>
    </section>
  </div>
</template>

<script>
import { apiService } from '../services/api.js'

export default {
  name: 'Transparency',
  data() {
    return {
      sourcesStatus: [],
      sources: [],
      loading: false,
      error: null
    }
  },
  async mounted() {
    await this.loadStatus()
  },
  methods: {
    async loadStatus() {
      try {
        this.loading = true
        this.error = null
        
        const [status, sources] = await Promise.all([
          apiService.getSourcesStatus(),
          apiService.getSources()
        ])
        
        this.sourcesStatus = status
        this.sources = sources
      } catch (error) {
        this.error = error.message
        console.error('Failed to load status:', error)
      } finally {
        this.loading = false
      }
    },
    
    getSourceName(sourceId) {
      const source = this.sources.find(s => s.sourceId === sourceId)
      return source ? source.name : sourceId
    },
    
    formatTimestamp(timestamp) {
      if (!timestamp) return 'Never'
      return new Date(timestamp).toLocaleString()
    }
  }
}
</script>

<style scoped>
.transparency {
  max-width: 100%;
}

section {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  margin-bottom: 2rem;
}

h1 {
  color: #2c3e50;
  margin-bottom: 2rem;
}

h2 {
  color: #34495e;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #3498db;
  padding-bottom: 0.5rem;
}

.principle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.principle-card {
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: #f9f9f9;
}

.principle-card h3 {
  color: #2c3e50;
  margin-bottom: 0.5rem;
}

.status-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  background: #f8f9fa;
  font-weight: 600;
  color: #2c3e50;
}

.source-name {
  font-weight: 500;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status-badge.enabled {
  background: #d4edda;
  color: #155724;
}

.status-badge.disabled {
  background: #f8d7da;
  color: #721c24;
}

.schedule {
  font-family: monospace;
  font-size: 0.875rem;
}

.timestamp {
  font-size: 0.875rem;
  color: #666;
}

.last-run {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.run-status {
  padding: 0.125rem 0.375rem;
  border-radius: 3px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.run-status.success {
  background: #d4edda;
  color: #155724;
}

.run-status.no_change {
  background: #fff3cd;
  color: #856404;
}

.run-status.failed {
  background: #f8d7da;
  color: #721c24;
}

.run-time {
  font-size: 0.75rem;
  color: #666;
}

.no-runs {
  font-style: italic;
  color: #999;
}

.loading, .error {
  padding: 1rem;
  text-align: center;
  border-radius: 4px;
}

.loading {
  background: #f8f9fa;
  color: #666;
}

.error {
  background: #f8d7da;
  color: #721c24;
}

ul {
  padding-left: 1.5rem;
}

li {
  margin-bottom: 0.5rem;
  line-height: 1.5;
}
</style>