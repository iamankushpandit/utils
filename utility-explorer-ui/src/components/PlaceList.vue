<template>
  <div class="place-list">
    <div class="place-controls">
      <input
        v-model="query"
        type="text"
        placeholder="Search places"
        class="place-search"
      />
      <span class="place-count">{{ filteredValues.length }} places</span>
    </div>

    <div v-if="!values.length" class="no-data-message">
      <p>No data available for the selected metric, source, and period.</p>
    </div>

    <div v-else class="place-table">
      <div class="place-row header">
        <span>Place</span>
        <span>Value</span>
      </div>
      <button
        v-for="place in displayValues"
        :key="place.geoId"
        class="place-row"
        type="button"
        @click="selectPlace(place)"
      >
        <span>{{ place.name }}</span>
        <span>{{ formatValue(place.value) }}</span>
      </button>
      <div v-if="filteredValues.length > maxRows" class="place-footer">
        Showing top {{ maxRows }} by value.
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PlaceList',
  props: {
    mapData: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      query: '',
      maxRows: 60
    }
  },
  computed: {
    values() {
      return this.mapData?.values || []
    },
    filteredValues() {
      const term = this.query.trim().toLowerCase()
      if (!term) return this.values
      return this.values.filter((value) => value.name?.toLowerCase().includes(term))
    },
    displayValues() {
      return [...this.filteredValues]
        .sort((a, b) => (b.value || 0) - (a.value || 0))
        .slice(0, this.maxRows)
    }
  },
  methods: {
    selectPlace(place) {
      this.$emit('regionClick', {
        geoLevel: 'PLACE',
        geoId: place.geoId,
        name: place.name
      })
    },
    formatValue(value) {
      if (value === null || value === undefined) return 'N/A'
      const unit = this.mapData?.metric?.unit ? ` ${this.mapData.metric.unit}` : ''
      return `${value.toFixed(2)}${unit}`
    }
  }
}
</script>

<style scoped>
.place-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.place-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.place-search {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
  font-size: 0.9rem;
}

.place-count {
  font-size: 0.85rem;
  color: var(--ink-muted);
}

.place-table {
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.place-row {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.8rem;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
  text-align: left;
}

.place-row.header {
  background: var(--surface-3);
  font-weight: 600;
  border-bottom: 1px solid var(--border);
}

button.place-row {
  cursor: pointer;
  transition: background 0.2s ease;
}

button.place-row:hover {
  background: var(--surface-2);
}

.place-footer {
  padding: 0.6rem 0.8rem;
  font-size: 0.8rem;
  color: var(--ink-muted);
  background: var(--surface-2);
}

.no-data-message {
  text-align: center;
  padding: 1.5rem;
  color: var(--ink-muted);
}
</style>
