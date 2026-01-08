<template>
  <div v-if="isOpen" class="drawer-overlay" @click="closeDrawer">
    <div class="drawer" @click.stop>
      <div class="drawer-header">
        <h2>{{ region?.name || 'Region Details' }}</h2>
        <button class="close-btn" @click="closeDrawer">&times;</button>
      </div>

      <div class="drawer-content">
        <div v-if="loading" class="loading">Loading...</div>

        <div v-else-if="error" class="error">
          {{ error }}
        </div>

        <div v-else-if="timeSeriesData">
          <div class="current-value">
            <h3>Current Value</h3>
            <div class="value-card">
              <div class="value">
                {{ getCurrentValue() }} {{ timeSeriesData.metric.unit }}
              </div>
              <div class="provenance">
                <small>{{ timeSeriesData.source.name }}</small><br>
                <small class="data-flag">Data: {{ timeSeriesData.source.isMock ? 'Mock' : 'Live' }}</small><br>
                <small>Retrieved: {{ formatDate(getCurrentPoint()?.retrievedAt) }}</small>
              </div>
            </div>
          </div>

          <div class="chart-section">
            <h3>Historical Data</h3>
            <div class="chart-container">
              <div ref="chartContainer"></div>
            </div>
          </div>

          <div class="actions">
            <button @click="exportData" class="export-btn" :disabled="exporting">
              {{ exporting ? 'Exporting...' : 'Export CSV' }}
            </button>
          </div>
        </div>

        <div v-else class="no-data">
          <p>No data available for this region.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { apiService } from '../services/api.js'
import Highcharts from 'highcharts'

export default {
  name: 'RegionDrawer',
  props: {
    isOpen: Boolean,
    region: Object,
    selectedMetric: String,
    selectedSource: String,
    metricMeta: {
      type: Object,
      default: null
    }
  },
  data() {
    const displayYears = Number(import.meta.env.VITE_DISPLAY_YEARS || 6)
    const requestWindowYears = Number(import.meta.env.VITE_TIME_WINDOW_YEARS || Math.max(10, displayYears + 4))
    return {
      timeSeriesData: null,
      loading: false,
      error: null,
      chart: null,
      exporting: false,
      displayYears,
      requestWindowYears
    }
  },
  watch: {
    isOpen: {
      handler: 'onOpenChange',
      immediate: true
    },
    region: 'loadTimeSeriesData',
    selectedMetric: 'loadTimeSeriesData',
    selectedSource: 'loadTimeSeriesData'
  },
  methods: {
    onOpenChange() {
      if (this.isOpen && this.region) {
        this.loadTimeSeriesData()
      } else {
        this.destroyChart()
      }
    },

    async loadTimeSeriesData() {
      if (!this.isOpen || !this.region || !this.selectedMetric || !this.selectedSource) {
        return
      }

      try {
        this.loading = true
        this.error = null

        const { from, to } = this.getDateRange()
        const params = {
          metricId: this.selectedMetric,
          sourceId: this.selectedSource,
          geoLevel: this.region.geoLevel,
          geoId: this.region.geoId,
          from,
          to
        }

        const raw = await apiService.getTimeSeries(params)

        // Aggregate to yearly averages for charting and keep last 6 years of available data
        const aggregated = this.aggregateByYear(raw?.points || [])
        this.timeSeriesData = {
          ...raw,
          points: aggregated
        }

        // Wait for next tick to ensure canvas is rendered
        this.$nextTick(() => {
          this.createChart()
        })
      } catch (error) {
        console.error('Failed to load time series:', error)
        this.error = 'Failed to load data'
        this.timeSeriesData = null
      } finally {
        this.loading = false
      }
    },

    createChart() {
      if (!this.timeSeriesData?.points || !this.$refs.chartContainer) return

      this.destroyChart()

      const points = this.timeSeriesData.points
      const categories = points.map(p => this.formatPeriod(p.periodStart))
      const seriesData = points.map(p => p.value)
      const unit = this.timeSeriesData.metric.unit
      const formatValue = (value) => this.formatNumber(value)

      this.chart = Highcharts.chart(this.$refs.chartContainer, {
        chart: {
          type: 'line',
          backgroundColor: 'transparent',
          height: 300
        },
        title: { text: null },
        credits: { enabled: false },
        xAxis: {
          categories,
          title: { text: 'Period' }
        },
        yAxis: {
          title: { text: unit }
        },
        tooltip: {
          formatter() {
            const point = points[this.point.index]
            const retrieved = point?.retrievedAt ? new Date(point.retrievedAt).toLocaleString() : 'Unknown'
            return `Value: ${formatValue(this.y)} ${unit}<br/>Retrieved: ${retrieved}`
          }
        },
        legend: { enabled: false },
        series: [
          {
            name: unit,
            data: seriesData,
            color: '#1a8a7a'
          }
        ]
      })
    },

    destroyChart() {
      if (this.chart) {
        this.chart.destroy()
        this.chart = null
      }
    },

    getCurrentValue() {
      if (!this.timeSeriesData?.points?.length) return 'N/A'
      const latest = this.timeSeriesData.points[this.timeSeriesData.points.length - 1]
      return this.formatNumber(latest.value)
    },

    formatNumber(value) {
      if (value === null || value === undefined || Number.isNaN(value)) return 'N/A'
      return Number(value).toFixed(2)
    },

    getCurrentPoint() {
      if (!this.timeSeriesData?.points?.length) return null
      return this.timeSeriesData.points[this.timeSeriesData.points.length - 1]
    },

    async exportData() {
      if (!this.region || !this.selectedMetric || !this.selectedSource) return

      try {
        this.exporting = true

        const { from, to } = this.getDateRange()
        const params = {
          metricId: this.selectedMetric,
          sourceId: this.selectedSource,
          geoLevel: this.region.geoLevel,
          geoId: this.region.geoId,
          from,
          to
        }

        const blob = await apiService.exportCsv(params)

        // Create download link
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${this.region.name}_${this.selectedMetric}.csv`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.error('Export failed:', error)
      } finally {
        this.exporting = false
      }
    },

    closeDrawer() {
      this.$emit('close')
    },

    formatDate(dateString) {
      if (!dateString) return 'Unknown'
      return new Date(dateString).toLocaleString()
    },

    formatPeriod(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      if (this.metricMeta?.defaultGranularity === 'YEAR') {
        return date.getFullYear().toString()
      }
      return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short' })
    },

    /**
     * Builds the query window that is sent to the API so that we can guarantee a six-year snapshot
     * even when the latest data is older.
     */
    getDateRange() {
      const endYear = new Date().getFullYear()
      const windowYears = this.requestWindowYears > 0 ? this.requestWindowYears : 10
      const startYear = endYear - (windowYears - 1)
      const from = `${startYear}-01-01`
      const to = `${endYear}-12-31`
      return { from, to }
    },

    aggregateByYear(points) {
      if (!points?.length) return []
      const buckets = {}
      points.forEach((p) => {
        const year = this.extractYear(p.periodStart)
        if (!year) return
        if (!buckets[year]) {
          buckets[year] = { sum: 0, count: 0, latest: p }
        }
        buckets[year].sum += Number(p.value)
        buckets[year].count += 1
        const currLatest = buckets[year].latest
        if (!currLatest?.retrievedAt || (p.retrievedAt && p.retrievedAt > currLatest.retrievedAt)) {
          buckets[year].latest = p
        }
      })
      return Object.entries(buckets)
        .map(([year, bucket]) => {
          const avg = bucket.count ? bucket.sum / bucket.count : null
          return {
            ...bucket.latest,
            periodStart: `${year}-01-01`,
            periodEnd: `${year}-12-31`,
            value: avg
          }
        })
        .sort((a, b) => this.extractYear(a.periodStart) - this.extractYear(b.periodStart))
        .filter((p, _, arr) => {
          const maxYear = arr.length ? this.extractYear(arr[arr.length - 1].periodStart) : null
          if (!maxYear) return true
          const yr = this.extractYear(p.periodStart)
          const yearsToKeep = this.displayYears > 0 ? this.displayYears : 6
          return yr >= maxYear - (yearsToKeep - 1)
        })
    },

    extractYear(dateString) {
      if (!dateString) return null
      const y = String(dateString).slice(0, 4)
      const n = Number(y)
      return Number.isFinite(n) ? n : null
    }
  },

  beforeUnmount() {
    this.destroyChart()
  }
}
</script>

<style scoped>
.drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(7, 20, 18, 0.55);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.drawer {
  background: var(--surface-2);
  width: 500px;
  height: 100%;
  overflow-y: auto;
  box-shadow: -10px 0 30px rgba(10, 25, 22, 0.2);
}

.drawer-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-3);
}

.drawer-header h2 {
  margin: 0;
  color: var(--ink);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--ink-muted);
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: var(--ink);
}

.drawer-content {
  padding: 1.5rem;
}

.current-value {
  margin-bottom: 2rem;
}

.current-value h3 {
  margin-bottom: 1rem;
  color: var(--ink);
}

.value-card {
  background: var(--surface);
  padding: 1.5rem;
  border-radius: 6px;
  border-left: 4px solid var(--accent);
}

.value {
  font-size: 2rem;
  font-weight: bold;
  color: var(--ink);
  margin-bottom: 0.5rem;
}

.provenance {
  color: var(--ink-muted);
}

.data-flag {
  font-weight: 600;
  color: var(--ink);
}

.chart-section {
  margin-bottom: 2rem;
}

.chart-section h3 {
  margin-bottom: 1rem;
  color: var(--ink);
}

.chart-container {
  height: 300px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 1rem;
}

.actions {
  display: flex;
  gap: 1rem;
}

.export-btn {
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 999px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
  box-shadow: 0 8px 18px rgba(26, 138, 122, 0.25);
}

.export-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1b9c8a, #147a6b);
}

.export-btn:disabled {
  background: #c9c1b7;
  cursor: not-allowed;
  box-shadow: none;
}

.loading,
.error,
.no-data {
  text-align: center;
  padding: 2rem;
  color: var(--ink-muted);
}

.error {
  color: var(--danger);
}
</style>
