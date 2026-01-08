<template>
  <div class="map-explorer">
    <h1>Map Explorer</h1>

    <div class="metric-tabs" role="tablist">
      <button
        v-for="metric in metrics"
        :key="metric.metricId"
        :class="['metric-tab', metric.metricId === selectedMetricId ? 'active' : '']"
        @click="selectMetric(metric.metricId)"
        role="tab"
        :aria-selected="metric.metricId === selectedMetricId"
      >
        <span class="metric-name">{{ metric.name }}</span>
        <span class="metric-unit">{{ metric.unit }}</span>
      </button>
    </div>

    <div v-if="activeMetric" class="metric-section">
      <div class="metric-meta">
        <div>
          <h2>{{ activeMetric.name }}</h2>
          <p class="metric-description">{{ activeMetric.description }}</p>
        </div>
      </div>

      <div class="source-grid">
        <div
          v-for="source in getSourcesForMetric(activeMetric.metricId)"
          :key="source.sourceId"
          class="source-card"
        >
          <div class="source-header">
            <div>
              <h3>{{ source.name }}</h3>
              <p class="source-note">
                {{ source.isMock ? 'Mock data' : 'Live source' }}
              </p>
            </div>
            <span
              :class="['data-pill', source.isMock ? 'mock' : 'live']"
            >
              {{ source.isMock ? 'Mock' : 'Live' }}
            </span>
          </div>

          <div class="source-meta">
            <span class="meta-label">Period</span>
            <span class="meta-value">{{ getPeriodLabel(activeMetric.metricId, source.sourceId) }}</span>
          </div>

          <div v-if="isLoading(activeMetric.metricId, source.sourceId)" class="loading-card">
            Loading map data...
          </div>
          <div v-else-if="getError(activeMetric.metricId, source.sourceId)" class="error-card">
            {{ getError(activeMetric.metricId, source.sourceId) }}
          </div>
          <MapComponent
            v-else
            :mapData="getMapData(activeMetric.metricId, source.sourceId)"
            @regionClick="onRegionClick"
          />
        </div>
      </div>
    </div>

    <RegionDrawer
      :isOpen="drawerOpen"
      :region="selectedRegion"
      :selectedMetric="selectedMetricId"
      :selectedSource="selectedSourceId"
      :metricMeta="activeMetric"
      @close="closeDrawer"
    />

    <UtilAgentPanel @highlightRegions="handleUtilAgentHighlights" />

  </div>
</template>

<script>
import { apiService } from '../services/api.js'
import MapComponent from '../components/MapComponent.vue'
import RegionDrawer from '../components/RegionDrawer.vue'
import UtilAgentPanel from '../components/UtilAgentPanel.vue'

export default {
  name: 'MapExplorer',
  components: {
    MapComponent,
    RegionDrawer,
    UtilAgentPanel
  },
  data() {
    const mapSearchYears = Number(import.meta.env.VITE_MAP_SEARCH_YEARS || 15)
    return {
      metrics: [],
      sources: [],
      selectedMetricId: '',
      selectedSourceId: '',
      mapDataByKey: {},
      loadingByKey: {},
      errorByKey: {},
      periodByKey: {},
      drawerOpen: false,
      selectedRegion: null,
      utilAgentHighlights: [],
      mapSearchYears
    }
  },
  computed: {
    activeMetric() {
      return this.metrics.find((metric) => metric.metricId === this.selectedMetricId) || null
    }
  },
  async mounted() {
    await this.loadCatalog()
  },
  methods: {
    async loadCatalog() {
      try {
        const [metrics, sources] = await Promise.all([
          apiService.getMetrics(),
          apiService.getSources()
        ])
        this.metrics = metrics
        this.sources = sources
        if (metrics.length > 0) {
          this.selectMetric(metrics[0].metricId)
        }
      } catch (error) {
        console.error('Failed to load catalog:', error)
      }
    },

    selectMetric(metricId) {
      if (this.selectedMetricId === metricId) return
      this.selectedMetricId = metricId
      this.selectedSourceId = ''
      this.loadMetricMaps(metricId)
    },

    async loadMetricMaps(metricId) {
      const sources = this.getSourcesForMetric(metricId)
      await Promise.all(
        sources.map((source) => this.loadSourceMap(metricId, source.sourceId))
      )
    },

    handleUtilAgentHighlights(highlights) {
      this.utilAgentHighlights = highlights || []
      // In the future we could highlight regions on the map, but for now we only store them and log.
      if (highlights && highlights.length) {
        console.info('Util Agent highlighted regions:', highlights)
      }
    },

    async loadSourceMap(metricId, sourceId) {
      const key = this.buildKey(metricId, sourceId)
      this.loadingByKey[key] = true
      this.errorByKey[key] = null

      try {
        const { mapData, periodLabel } = await this.fetchBestMap(metricId, sourceId)
        this.mapDataByKey[key] = mapData
        this.periodByKey[key] = periodLabel
      } catch (error) {
        console.error('Failed to load map data:', error)
        this.errorByKey[key] = 'Failed to load map data'
      } finally {
        this.loadingByKey[key] = false
      }
    },

    /**
     * Attempts to load the freshest map period by checking yearly snapshots (configurable lookback)
     * before falling back to monthly data. Stores a fallback map in case nothing is found.
     */
    async fetchBestMap(metricId, sourceId) {
      const metric = this.metrics.find((m) => m.metricId === metricId)
      const granularity = metric?.defaultGranularity || 'MONTH'
      const now = new Date()
      let fallback = null
      let fallbackPeriod = null
      const yearsToSearch = this.mapSearchYears > 0 ? this.mapSearchYears : 15

      // Helper to fetch and short-circuit on first hit
      const tryPeriod = async (period) => {
        const mapData = await apiService.getMap({
          metricId,
          sourceId,
          geoLevel: 'STATE',
          period
        })
        if (mapData?.values?.length) {
          return { hit: true, mapData, periodLabel: period }
        }
        if (!fallback) {
          fallback = mapData
          fallbackPeriod = period
        }
        return { hit: false }
      }

      // Prefer yearly periods for map display, search a broader window to cover older latest data
      const currentYear = now.getFullYear()
      for (let yr = currentYear; yr >= currentYear - yearsToSearch; yr--) {
        const attempt = await tryPeriod(String(yr))
        if (attempt.hit) return attempt
      }

      // Fallback to monthly search (up to configured window) if no yearly data found
      const monthsToSearch = yearsToSearch * 12
      for (let i = 0; i <= monthsToSearch - 1; i++) {
        const date = new Date(now.getFullYear(), now.getMonth() - i, 1)
        const period = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
        const attempt = await tryPeriod(period)
        if (attempt.hit) return attempt
      }

      return { mapData: fallback, periodLabel: fallbackPeriod || 'Unknown' }
    },

    buildKey(metricId, sourceId) {
      return `${metricId}::${sourceId}`
    },

    getMapData(metricId, sourceId) {
      return this.mapDataByKey[this.buildKey(metricId, sourceId)] || null
    },

    getPeriodLabel(metricId, sourceId) {
      return this.periodByKey[this.buildKey(metricId, sourceId)] || 'Loading...'
    },

    isLoading(metricId, sourceId) {
      return Boolean(this.loadingByKey[this.buildKey(metricId, sourceId)])
    },

    getError(metricId, sourceId) {
      return this.errorByKey[this.buildKey(metricId, sourceId)]
    },

    onRegionClick(region) {
      this.selectedRegion = region
      this.drawerOpen = true
      const sources = this.getSourcesForMetric(this.selectedMetricId)
      if (!this.selectedSourceId && sources.length > 0) {
        this.selectedSourceId = sources[0].sourceId
      }
    },

    closeDrawer() {
      this.drawerOpen = false
      this.selectedRegion = null
    },

    getSourcesForMetric(metricId) {
      const metric = this.metrics.find((item) => item.metricId === metricId)

      // Explicit mapping to keep the UI aligned with real data providers
      const forcedMap = {
        ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH: ['EIA'],
        ELECTRICITY_MONTHLY_COST_USD_ACS: ['CENSUS_ACS']
      }

      const sourceIds = forcedMap[metricId] || metric?.sourceIds || []

      if (sourceIds.length > 0) {
        return this.sources.filter((source) => sourceIds.includes(source.sourceId))
      }

      // If nothing is known, show nothing rather than every source (prevents wrong cards)
      return []
    }
  }
}
</script>

<style scoped>
.map-explorer {
  max-width: 100%;
}

.metric-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
  margin-bottom: 2rem;
}

.metric-tab {
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--ink);
  padding: 1rem 1.25rem;
  border-radius: 18px;
  cursor: pointer;
  display: flex;
  gap: 0.5rem;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow);
  transition: transform 0.2s, box-shadow 0.2s;
}

.metric-tab.active {
  background: linear-gradient(135deg, rgba(26, 138, 122, 0.15), rgba(240, 180, 79, 0.15));
  border-color: rgba(26, 138, 122, 0.4);
  box-shadow: 0 10px 20px rgba(10, 25, 22, 0.12);
}

.metric-tab:hover {
  transform: translateY(-1px);
}

.metric-name {
  font-weight: 600;
}

.metric-unit {
  font-size: 0.85rem;
  color: var(--ink-muted);
}

.metric-section {
  background: var(--surface-2);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow);
  min-height: 70vh;
}

.metric-meta {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.metric-description {
  color: var(--ink-muted);
  margin-top: 0.4rem;
}

.metric-badges {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.badge {
  background: var(--surface-3);
  color: var(--ink);
  border: 1px solid var(--border);
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  font-size: 0.8rem;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.source-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 1.25rem;
  box-shadow: var(--shadow);
}

.source-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.source-note {
  color: var(--ink-muted);
  font-size: 0.85rem;
}

.data-pill {
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.data-pill.mock {
  background: rgba(200, 137, 42, 0.18);
  color: var(--warning);
}

.data-pill.live {
  background: rgba(47, 143, 104, 0.15);
  color: var(--success);
}

.source-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  color: var(--ink-muted);
  font-size: 0.85rem;
}

.meta-label {
  font-weight: 600;
  color: var(--ink);
}

.loading-card,
.error-card {
  padding: 1rem;
  border-radius: 12px;
  text-align: center;
}

.loading-card {
  background: var(--surface-3);
  color: var(--ink-muted);
}

.error-card {
  background: rgba(177, 76, 76, 0.12);
  color: var(--danger);
}
</style>
