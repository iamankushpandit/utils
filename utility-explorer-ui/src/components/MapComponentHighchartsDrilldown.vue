<template>
  <div class="map-wrapper">
    <div class="map-container">
      <div ref="mapElement" class="map"></div>
    </div>

    <div v-if="mapData && mapData.values.length > 0" class="legend">
      <div class="legend-header">
        <h4>{{ mapData.metric.name || mapData.metric.metricId }}</h4>
        <span class="legend-context">{{ legendContext }}</span>
      </div>
      <div class="legend-scale">
        <div class="legend-labels">
          <span>{{ formatNumber(legendStats?.min) }}</span>
          <span>{{ formatNumber(legendStats?.max) }}</span>
        </div>
        <div class="legend-bar"></div>
        <div class="legend-unit">{{ mapData.metric.unit }}</div>
      </div>
      <div class="legend-provenance">
        <small>Retrieved: {{ formatDate(mapData.retrievedAt) }}</small>
        <br>
        <small>Source: {{ mapData.source.name }}</small>
        <br>
        <small class="data-flag">
          Data: {{ mapData.source.isMock ? 'Mock' : 'Live' }}
        </small>
      </div>
    </div>

    <div v-else-if="mapData && mapData.values.length === 0" class="no-data-message">
      <p>No data available for the selected metric, source, and period.</p>
    </div>
  </div>
</template>

<script>
import Highcharts from 'highcharts'
import HighchartsMap from 'highcharts/modules/map'
import HighchartsDrilldown from 'highcharts/modules/drilldown'
import usStatesMap from '@highcharts/map-collection/countries/us/us-all.geo.json'
import usCountiesMap from '@highcharts/map-collection/countries/us/us-all-all.geo.json'
import { apiService } from '../services/api.js'

HighchartsMap(Highcharts)
HighchartsDrilldown(Highcharts)

export default {
  name: 'MapComponent',
  emits: ['regionClick'],
  emits: ['regionClick'],
  props: {
    mapData: {
      type: Object,
      default: null
    },
    geoLevel: {
      type: String,
      default: 'STATE'
    },
    focusStateFips: {
      type: String,
      default: null
    }
  },
  data() {
    return {
      chart: null,
      legendStats: null,
      metricUnit: '',
      pendingOptions: null,
      legendContext: '',
      viewLevel: 'STATE'
    }
  },
  watch: {
    mapData: {
      handler: 'onMapDataChange',
      deep: true,
      immediate: true
    },
    geoLevel() {
      this.onMapDataChange()
    },
    focusStateFips() {
      if (this.geoLevel === 'COUNTY') {
        this.onMapDataChange()
      }
    }
  },
  mounted() {
    this.renderChart()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    if (this.chart) {
      this.chart.destroy()
      this.chart = null
    }
  },
  methods: {
    renderChart() {
      this.onMapDataChange()
    },

    onMapDataChange() {
      if (!this.mapData) return
      this.updateLegendFromMapData()
      const level = this.mapData.geoLevel || this.geoLevel || 'STATE'
      if (level === 'COUNTY' && this.focusStateFips) {
        this.renderCountyMap()
      } else {
        this.renderStateMap()
      }
    },

    updateLegendFromMapData() {
      const legend = this.mapData?.legend
      this.legendStats = {
        min: legend?.min ?? 0,
        max: legend?.max ?? 0
      }
      this.metricUnit = this.mapData?.metric?.unit || ''
      this.legendContext = 'U.S. states'
      this.viewLevel = 'STATE'
    },

    updateChart(options) {
      const mapEl = this.$refs.mapElement
      if (!mapEl) {
        this.pendingOptions = options
        return
      }
      this.pendingOptions = null

      if (this.chart) {
        this.chart.update(options, true, true)
      } else {
        this.chart = Highcharts.mapChart(mapEl, options)
      }
    },

    destroyChart() {
      if (this.chart) {
        this.chart.destroy()
        this.chart = null
      }
    },

    renderStateMap() {
      if (!this.mapData) return
      const formatValue = (num) => this.formatNumber(num)
      const values = this.mapData.values || []
      const legend = this.legendStats || { min: 0, max: 0 }
      const metricUnit = this.metricUnit
      const isAcs = this.mapData?.source?.sourceId === 'CENSUS_ACS'

      const data = values
        .map((value) => {
          const abbrev = this.fipsToAbbrev(value.geoId)
          if (!abbrev) return null
          return {
            'hc-key': `us-${abbrev.toLowerCase()}`,
            value: value.value,
            geoId: value.geoId,
            drilldown: isAcs ? `us-${abbrev.toLowerCase()}` : undefined
          }
        })
        .filter(Boolean)

      const chartEvents = isAcs
        ? {
            drilldown: (e) => this.handleDrilldown(e),
            afterDrillUp: (e) => this.handleAfterDrillUp(e)
          }
        : {}

      const options = {
        chart: {
          map: usStatesMap,
          backgroundColor: 'transparent',
          spacing: [0, 0, 0, 0],
          events: chartEvents
        },
        accessibility: { enabled: false },
        title: { text: null },
        credits: { enabled: false },
        mapNavigation: {
          enabled: true,
          enableButtons: true,
          buttonOptions: { align: 'left' }
        },
        tooltip: {
          formatter() {
            if (this.point.value === undefined || this.point.value === null) {
              return `<b>${this.point.name}</b><br/>No data available`
            }
            return `<b>${this.point.name}</b><br/>${formatValue(this.point.value)} ${metricUnit}`
          }
        },
        colorAxis: {
          min: legend.min ?? 0,
          max: legend.max ?? 0,
          minColor: '#1a8a7a',
          maxColor: '#f0b44f'
        },
        series: [
          {
            type: 'map',
            name: 'States',
            mapData: usStatesMap,
            joinBy: 'hc-key',
            data,
            borderColor: '#6f675c',
            borderWidth: 0.8,
            nullColor: '#e8e2d7',
            states: { hover: { color: '#f3c66a' } },
            dataLabels: {
              enabled: true,
              formatter() {
                return this.point?.properties?.['postal-code'] || ''
              },
              style: {
                fontSize: '10px',
                fontWeight: '600',
                color: '#10231f',
                textOutline: '1px rgba(255,255,255,0.8)'
              }
            },
            point: {
              events: {
                click: (event) => {
                  if (isAcs) {
                    // drilldown handles the click for ACS
                    return
                  }
                  const point = event.point
                  const fips = this.abbrevToFips(point.properties?.['postal-code'])
                  if (!fips) return
                  this.$emit('regionClick', {
                    geoLevel: 'STATE',
                    geoId: fips,
                    name: point.name
                  })
                }
              }
            },
            custom: {}
          }
        ]
      }

      this.updateChart(options)
    },

    renderCountyMap() {
      if (!this.mapData || !this.focusStateFips) {
        this.renderStateMap()
        return
      }

      const stateFips = String(this.focusStateFips).padStart(2, '0')
      const countyFeatures = usCountiesMap.features.filter((feature) => {
        const fips = feature?.properties?.fips
        return fips && fips.startsWith(stateFips)
      })

      if (!countyFeatures.length) {
        this.renderStateMap()
        return
      }

      const countyGeoJson = {
        type: 'FeatureCollection',
        features: countyFeatures
      }

      const values = this.mapData.values || []
      const formatValue = (num) => this.formatNumber(num)
      const legend = this.legendStats || { min: 0, max: 0 }
      const metricUnit = this.metricUnit

      const data = values
        .map((value) => {
          const match = countyFeatures.find((feature) => feature.properties?.fips === value.geoId)
          if (!match) return null
          return {
            'hc-key': match.properties['hc-key'],
            value: value.value,
            geoId: value.geoId,
            name: value.name || match.properties?.name
          }
        })
        .filter(Boolean)

      const stateFeature = this.getStateFeature(stateFips)
      const stateOutline = stateFeature ? Highcharts.geojson(stateFeature, 'mapline') : []

      const options = {
        chart: {
          map: countyGeoJson,
          backgroundColor: 'transparent',
          spacing: [0, 0, 0, 0]
        },
        accessibility: { enabled: false },
        mapView: this.buildMapView('COUNTY', stateFips),
        title: { text: null },
        credits: { enabled: false },
        mapNavigation: {
          enabled: true,
          enableButtons: true,
          buttonOptions: { align: 'left' }
        },
        tooltip: {
          formatter() {
            if (this.point.value === undefined || this.point.value === null) {
              return `<b>${this.point.name}</b><br/>No data available`
            }
            return `<b>${this.point.name}</b><br/>${formatValue(this.point.value)} ${metricUnit}`
          }
        },
        colorAxis: {
          min: legend.min ?? 0,
          max: legend.max ?? 0,
          minColor: '#1a8a7a',
          maxColor: '#f0b44f'
        },
        series: [
          {
            type: 'map',
            name: 'Counties',
            mapData: countyGeoJson,
            joinBy: 'hc-key',
            data,
            borderColor: '#6f675c',
            borderWidth: 0.5,
            nullColor: '#f0f2f5',
            states: { hover: { color: '#f3c66a' } },
            dataLabels: {
              enabled: false
            },
            point: {
              events: {
                click: (event) => {
                  const point = event.point
                  this.$emit('regionClick', {
                    geoLevel: 'COUNTY',
                    geoId: point.options?.geoId || point.name,
                    name: point.name
                  })
                }
              }
            }
          },
          ...(stateOutline.length
            ? [{
              type: 'mapline',
              name: 'State border',
              color: '#1f2933',
              lineWidth: 2,
              data: stateOutline,
              enableMouseTracking: false,
              tooltip: { enabled: false }
            }]
            : [])
        ]
      }

      this.updateChart(options)
    },

    onResize() {
      if (this.chart) {
        this.chart.reflow()
      }
    },

    getPeriodParamFromMap() {
      const start = this.mapData?.period?.start
      const end = this.mapData?.period?.end
      if (start && end && start.endsWith('-01-01') && end.endsWith('-12-31')) {
        return start.slice(0, 4)
      }
      if (start && start.length >= 7) {
        return start.slice(0, 7)
      }
      return null
    },

    async handleDrilldown(e) {
      if (e.seriesOptions) return
      if (this.mapData?.source?.sourceId !== 'CENSUS_ACS') return

      const chart = e.target
      const stateKey = e.point.drilldown || e.point['hc-key']
      const stateFips = e.point.options?.geoId
      const period = this.getPeriodParamFromMap()
      if (!stateKey || !stateFips || !period) return

      chart.showLoading('<i class="icon-spinner icon-spin icon-3x"></i>')
      try {
        const topoResp = await fetch(`https://code.highcharts.com/mapdata/countries/us/${stateKey}-all.topo.json`)
        const topology = await topoResp.json()
        const countyGeoJson = Highcharts.geojson(topology)

        const countyMap = await apiService.getMap({
          metricId: this.mapData.metric.metricId,
          sourceId: this.mapData.source.sourceId,
          geoLevel: 'COUNTY',
          parentGeoLevel: 'STATE',
          parentGeoId: String(stateFips).padStart(2, '0'),
          period
        })

        const valuesByFips = {}
        ;(countyMap.values || []).forEach((val) => {
          valuesByFips[val.geoId] = val
        })

        const drillData = countyGeoJson.map((feature) => {
          const fips = feature?.properties?.fips
          const val = fips ? valuesByFips[fips] : null
          return {
            'hc-key': feature?.properties?.['hc-key'],
            value: val?.value,
            geoId: fips,
            name: val?.name || feature?.properties?.name || feature?.name
          }
        })

        const numericValues = drillData
          .map((d) => d.value)
          .filter((v) => typeof v === 'number')
        if (numericValues.length && chart.colorAxis && chart.colorAxis[0]) {
          const min = Math.min(...numericValues)
          const max = Math.max(...numericValues)
          this.legendStats = { min, max }
          this.legendContext = `Counties in ${e.point.name}`
          this.viewLevel = 'COUNTY'
          chart.update({
            colorAxis: { min, max }
          }, false)
        }

        const recommendedView =
          topology?.objects?.default?.['hc-recommended-mapview'] || undefined
        chart.mapView.update(
          Highcharts.merge({ insets: undefined, padding: 0 }, recommendedView || {}),
          false
        )

        chart.hideLoading()
        chart.addSeriesAsDrilldown(e.point, {
          name: e.point.name,
          mapData: countyGeoJson,
          joinBy: 'hc-key',
          data: drillData,
          dataLabels: {
            enabled: true,
            format: '{point.name}'
          },
          borderColor: '#6f675c',
          borderWidth: 0.5,
          nullColor: '#f0f2f5',
          states: { hover: { color: '#f3c66a' } },
          point: {
            events: {
              click: (evt) => {
                const pt = evt.point
                this.$emit('regionClick', {
                  geoLevel: 'COUNTY',
                  geoId: pt.options?.geoId || pt.name,
                  name: pt.name
                })
              }
            }
          },
          custom: {
            mapView: recommendedView || this.buildMapView('STATE')
          }
        })
      } catch (err) {
        console.error('Drilldown failed', err)
        chart.hideLoading()
      }
    },

    handleAfterDrillUp() {
      // Reset legend to state view when drilling up
      this.updateLegendFromMapData()
      this.legendContext = 'U.S. states'
      this.viewLevel = 'STATE'
      // After Highcharts finishes its own cleanup, rebuild cleanly
      setTimeout(() => {
        this.destroyChart()
        this.$nextTick(() => this.renderStateMap())
      }, 25)
    },

    buildMapView(geoLevel, focusStateFips) {
      if (!focusStateFips || geoLevel === 'STATE') return undefined
      const stateFeature = usStatesMap.features.find((feature) => {
        const stateFips = feature?.properties?.['state-fips']
        if (!stateFips) return false
        return String(stateFips).padStart(2, '0') === focusStateFips
      })
      if (!stateFeature) return undefined
      return {
        fitToGeometry: stateFeature,
        padding: 8
      }
    },

    getStateFeature(stateFips) {
      const normalized = String(stateFips).padStart(2, '0')
      return usStatesMap.features.find((feature) => {
        const featureFips = feature?.properties?.['state-fips']
        if (!featureFips) return false
        return String(featureFips).padStart(2, '0') === normalized
      })
    },

    fipsToAbbrev(fips) {
      const map = {
        '01': 'AL', '02': 'AK', '04': 'AZ', '05': 'AR', '06': 'CA', '08': 'CO',
        '09': 'CT', '10': 'DE', '11': 'DC', '12': 'FL', '13': 'GA', '15': 'HI',
        '16': 'ID', '17': 'IL', '18': 'IN', '19': 'IA', '20': 'KS', '21': 'KY',
        '22': 'LA', '23': 'ME', '24': 'MD', '25': 'MA', '26': 'MI', '27': 'MN',
        '28': 'MS', '29': 'MO', '30': 'MT', '31': 'NE', '32': 'NV', '33': 'NH',
        '34': 'NJ', '35': 'NM', '36': 'NY', '37': 'NC', '38': 'ND', '39': 'OH',
        '40': 'OK', '41': 'OR', '42': 'PA', '44': 'RI', '45': 'SC', '46': 'SD',
        '47': 'TN', '48': 'TX', '49': 'UT', '50': 'VT', '51': 'VA', '53': 'WA',
        '54': 'WV', '55': 'WI', '56': 'WY', '72': 'PR'
      }
      return map[fips] || ''
    },

    abbrevToFips(abbrev) {
      const map = {
        'AL': '01', 'AK': '02', 'AZ': '04', 'AR': '05', 'CA': '06', 'CO': '08',
        'CT': '09', 'DE': '10', 'DC': '11', 'FL': '12', 'GA': '13', 'HI': '15',
        'ID': '16', 'IL': '17', 'IN': '18', 'IA': '19', 'KS': '20', 'KY': '21',
        'LA': '22', 'ME': '23', 'MD': '24', 'MA': '25', 'MI': '26', 'MN': '27',
        'MS': '28', 'MO': '29', 'MT': '30', 'NE': '31', 'NV': '32', 'NH': '33',
        'NJ': '34', 'NM': '35', 'NY': '36', 'NC': '37', 'ND': '38', 'OH': '39',
        'OK': '40', 'OR': '41', 'PA': '42', 'RI': '44', 'SC': '45', 'SD': '46',
        'TN': '47', 'TX': '48', 'UT': '49', 'VT': '50', 'VA': '51', 'WA': '53',
        'WV': '54', 'WI': '55', 'WY': '56', 'PR': '72'
      }
      return map[abbrev] || ''
    },

    formatDate(dateString) {
      if (!dateString) return 'Unknown'
      return new Date(dateString).toLocaleString()
    },

    formatNumber(value) {
      if (value === null || value === undefined || Number.isNaN(value)) return 'N/A'
      return Number(value).toFixed(2)
    }
  }
}
</script>

<style scoped>
.map-container {
  position: relative;
  height: 500px;
  border-radius: 16px;
  overflow: hidden;
}

.map {
  height: 100%;
  width: 100%;
}

.legend {
  margin-top: 0.75rem;
  background: rgba(255, 255, 255, 0.96);
  padding: 12px 14px;
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(10, 25, 22, 0.12);
  border: 1px solid rgba(226, 218, 208, 0.7);
}

.legend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.legend h4 {
  margin: 0 0 10px 0;
  font-size: 0.78rem;
  line-height: 1.2;
  color: var(--ink);
}

.legend-context {
  font-size: 0.75rem;
  color: var(--ink-muted);
  background: var(--surface-3);
  padding: 4px 8px;
  border-radius: 999px;
  border: 1px solid var(--border);
}

.legend-scale {
  margin-bottom: 10px;
}

.legend-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  margin-bottom: 5px;
  color: var(--ink-muted);
}

.legend-bar {
  height: 10px;
  background: linear-gradient(to right, #1a8a7a, #f0b44f);
  border-radius: 2px;
}

.legend-unit {
  text-align: center;
  font-size: 0.8rem;
  margin-top: 5px;
  color: var(--ink-muted);
}

.legend-provenance {
  border-top: 1px solid rgba(226, 218, 208, 0.6);
  padding-top: 8px;
  color: var(--ink-muted);
}

.data-flag {
  display: inline-block;
  margin-top: 2px;
  font-weight: 600;
  color: var(--ink);
}

.no-data-message {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.92);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(10, 25, 22, 0.18);
  text-align: center;
  border: 1px solid rgba(226, 218, 208, 0.7);
}
</style>
