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
      <div class="section-header">
        <div>
          <h2>Live Data Status</h2>
          <p class="section-subtitle">Trigger a fresh ingest or review the latest source runs.</p>
        </div>
        <button class="run-now-btn" :disabled="running" @click="runNow">
          {{ running ? 'Running...' : 'Run now' }}
        </button>
      </div>
      <div v-if="runMessage" :class="['run-message', runMessageTone]">{{ runMessage }}</div>
      <div v-if="loading" class="loading">Loading status...</div>
      <div v-else-if="error" class="error">Failed to load status: {{ error }}</div>
      <div v-else class="status-table">
        <table>
          <thead>
            <tr>
              <th>Source</th>
              <th>Data</th>
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
                <span :class="['data-badge', isSourceMock(source.sourceId) ? 'mock' : 'live']">
                  {{ isSourceMock(source.sourceId) ? 'Mock' : 'Live' }}
                </span>
              </td>
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
                <div class="row-actions">
                  <button
                    class="run-now-mini"
                    :disabled="runningSource === source.sourceId"
                    @click="runNowSource(source.sourceId)"
                  >
                    {{ runningSource === source.sourceId ? 'Running...' : 'Run now' }}
                  </button>
                </div>
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
      error: null,
      running: false,
      runMessage: null,
      runMessageTone: 'info',
      runningSource: null
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

    async runNow() {
      try {
        this.running = true
        this.runMessage = null
        this.runMessageTone = 'info'

        const result = await apiService.runIngestion()
        this.runMessage = result.message || 'Ingestion dispatched.'
        this.runMessageTone = result.status === 'disabled' ? 'warning' : 'success'

        await this.loadStatus()
      } catch (error) {
        this.runMessage = error.response?.data?.message || error.message
        this.runMessageTone = 'error'
      } finally {
        this.running = false
      }
    },

    async runNowSource(sourceId) {
      if (!sourceId) return
      try {
        this.runningSource = sourceId
        this.runMessage = null
        this.runMessageTone = 'info'

        const result = await apiService.runIngestionForSource(sourceId)
        this.runMessage = result.message || `Ingestion dispatched for ${sourceId}.`
        this.runMessageTone = result.status === 'disabled' ? 'warning' : 'success'

        await this.loadStatus()
      } catch (error) {
        this.runMessage = error.response?.data?.message || error.message
        this.runMessageTone = 'error'
      } finally {
        this.runningSource = null
      }
    },
    
    getSourceName(sourceId) {
      const source = this.sources.find(s => s.sourceId === sourceId)
      return source ? source.name : sourceId
    },

    isSourceMock(sourceId) {
      const source = this.sources.find(s => s.sourceId === sourceId)
      return source ? source.isMock : false
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
  animation: fadeUp 0.6s ease both;
}

section {
  background: var(--surface-2);
  padding: 2rem;
  border-radius: 16px;
  margin-bottom: 2rem;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
}

h1 {
  color: var(--ink);
  margin-bottom: 2rem;
}

h2 {
  color: var(--ink);
  margin-bottom: 0.5rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.section-subtitle {
  color: var(--ink-muted);
  font-size: 0.95rem;
}

.principle-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.principle-card {
  padding: 1.5rem;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
}

.principle-card h3 {
  color: var(--ink);
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
  border-bottom: 1px solid var(--border);
}

th {
  background: var(--surface-3);
  font-weight: 600;
  color: var(--ink);
}

.source-name {
  font-weight: 500;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.875rem;
  font-weight: 500;
  background: var(--surface-3);
}

.data-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  background: var(--surface-3);
}

.data-badge.mock {
  background: rgba(200, 137, 42, 0.18);
  color: var(--warning);
}

.data-badge.live {
  background: rgba(47, 143, 104, 0.15);
  color: var(--success);
}

.status-badge.enabled {
  background: rgba(47, 143, 104, 0.15);
  color: var(--success);
}

.status-badge.disabled {
  background: rgba(177, 76, 76, 0.15);
  color: var(--danger);
}

.schedule {
  font-family: monospace;
  font-size: 0.875rem;
}

.timestamp {
  font-size: 0.875rem;
  color: var(--ink-muted);
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
  background: rgba(47, 143, 104, 0.15);
  color: var(--success);
}

.run-status.no_change {
  background: rgba(200, 137, 42, 0.18);
  color: var(--warning);
}

.run-status.failed {
  background: rgba(177, 76, 76, 0.15);
  color: var(--danger);
}

.run-time {
  font-size: 0.75rem;
  color: var(--ink-muted);
}

.no-runs {
  font-style: italic;
  color: var(--ink-muted);
}

.loading, .error {
  padding: 1rem;
  text-align: center;
  border-radius: 8px;
}

.loading {
  background: var(--surface-3);
  color: var(--ink-muted);
}

.error {
  background: rgba(177, 76, 76, 0.12);
  color: var(--danger);
}

.run-now-btn {
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: #ffffff;
  border: none;
  padding: 0.65rem 1.25rem;
  border-radius: 999px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(26, 138, 122, 0.25);
  transition: transform 0.2s, box-shadow 0.2s;
}

.run-now-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(26, 138, 122, 0.3);
}

.run-now-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.run-message {
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  font-size: 0.9rem;
  border: 1px solid transparent;
}

.run-message.success {
  background: rgba(47, 143, 104, 0.12);
  color: var(--success);
  border-color: rgba(47, 143, 104, 0.3);
}

.run-message.warning {
  background: rgba(200, 137, 42, 0.12);
  color: var(--warning);
  border-color: rgba(200, 137, 42, 0.35);
}

.run-message.error {
  background: rgba(177, 76, 76, 0.12);
  color: var(--danger);
  border-color: rgba(177, 76, 76, 0.3);
}

.run-message.info {
  background: rgba(26, 138, 122, 0.1);
  color: var(--accent-strong);
  border-color: rgba(26, 138, 122, 0.25);
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

ul {
  padding-left: 1.5rem;
}

li {
  margin-bottom: 0.5rem;
  line-height: 1.5;
}
</style>
