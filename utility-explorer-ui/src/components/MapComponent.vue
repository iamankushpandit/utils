<template>
  <div class="map-container">
    <div ref="mapElement" class="map"></div>

    <div v-if="mapData && mapData.values.length > 0" class="legend">
      <h4>{{ mapData.metric.name || mapData.metric.metricId }}</h4>
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
import usMap from '@highcharts/map-collection/countries/us/us-all.geo.json'

HighchartsMap(Highcharts)

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
      chart: null
    }
  },
  watch: {
    mapData: {
      handler: 'renderChart',
      deep: true,
      immediate: true
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
      if (!this.$refs.mapElement) return

      const values = this.mapData?.values || []
      const unit = this.mapData?.metric?.unit || ''
      const min = this.mapData?.legend?.min ?? 0
      const max = this.mapData?.legend?.max ?? 0

      const data = values
        .map((value) => {
          const abbrev = this.fipsToAbbrev(value.geoId)
          if (!abbrev) return null
          return {
            'hc-key': `us-${abbrev.toLowerCase()}`,
            value: value.value,
            geoId: value.geoId
          }
        })
        .filter(Boolean)

      const options = {
        chart: {
          map: usMap,
          backgroundColor: 'transparent',
          spacing: [0, 0, 0, 0]
        },
        title: {
          text: null
        },
        credits: {
          enabled: false
        },
        mapNavigation: {
          enabled: true,
          enableButtons: true,
          buttonOptions: {
            align: 'left'
          }
        },
        tooltip: {
          formatter() {
            if (this.point.value === undefined || this.point.value === null) {
              return `<b>${this.point.name}</b><br/>No data available`
            }
            return `<b>${this.point.name}</b><br/>${this.point.value} ${unit}`
          }
        },
        colorAxis: {
          min,
          max,
          minColor: '#1a8a7a',
          maxColor: '#f0b44f'
        },
        series: [
          {
            type: 'map',
            name: 'States',
            mapData: usMap,
            joinBy: 'hc-key',
            data,
            borderColor: '#6f675c',
            borderWidth: 0.8,
            nullColor: '#e8e2d7',
            states: {
              hover: {
                color: '#f3c66a'
              }
            },
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
            }
          }
        ]
      }

      if (this.chart) {
        this.chart.update(options, true, true)
      } else {
        this.chart = Highcharts.mapChart(this.$refs.mapElement, options)
      }
    },

    onResize() {
      if (this.chart) {
        this.chart.reflow()
      }
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
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.92);
  padding: 10px 12px;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(10, 25, 22, 0.18);
  min-width: 150px;
  max-width: 180px;
  border: 1px solid rgba(226, 218, 208, 0.7);
  backdrop-filter: blur(6px);
}

.legend h4 {
  margin: 0 0 10px 0;
  font-size: 0.78rem;
  line-height: 1.2;
  color: var(--ink);
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
