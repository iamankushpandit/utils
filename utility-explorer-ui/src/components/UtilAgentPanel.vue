<template>
  <div class="util-agent-panel">
    <h3>Ask Questions About the Data</h3>
    
    <div class="query-section">
      <div class="input-group">
        <input 
          v-model="question" 
          @keyup.enter="submitQuery"
        placeholder="Try: 'show me the latest electricity data'"
          class="query-input"
          :disabled="loading"
        />
        <button 
          @click="submitQuery" 
          :disabled="loading || !question.trim()"
          class="query-btn"
        >
          {{ loading ? 'Processing...' : 'Ask' }}
        </button>
      </div>
      
        <div class="disclaimer">
          <p>
            Questions are logged to help us improve the Util Agent experience.
            If you’re uncomfortable with that, feel free not to use this feature.
          </p>
        </div>

        <div class="examples">
          <p><strong>Example queries:</strong></p>
        <button 
          v-for="example in examples" 
          :key="example"
          @click="question = example"
          class="example-btn"
        >
          {{ example }}
        </button>
      </div>
    </div>
    
    <div v-if="response" class="response-section">
      <div class="response-header">
        <h4>{{ response.summary }}</h4>
        <span :class="['status-badge', response.status.toLowerCase()]">
          {{ response.status }}
        </span>
      </div>
      
      <div v-if="response.table" class="results-table">
        <table>
          <thead>
            <tr>
              <th v-for="column in response.table.columns" :key="column">
                {{ column }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in response.table.rows" :key="index">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex">
                {{ formatCell(cell) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div v-if="response.citations" class="citations">
        <h5>Sources Used:</h5>
        <ul>
          <li v-for="citation in response.citations" :key="citation.sourceId">
            <strong>{{ citation.sourceId }}</strong> - 
            Retrieved: {{ formatDate(citation.retrievedAt) }}
            <a v-if="citation.termsUrl" :href="citation.termsUrl" target="_blank">
              (Terms)
            </a>
          </li>
        </ul>
      </div>
      
      <div v-if="response.notes" class="notes">
        <ul>
          <li v-for="note in response.notes" :key="note" class="note">
            {{ note }}
          </li>
        </ul>
      </div>
    </div>
    
    <div v-if="error" class="error-section">
      <p class="error">{{ error }}</p>
    </div>
  </div>
</template>

<script>
import { apiService } from '../services/api.js'

export default {
  name: 'UtilAgentPanel',
  data() {
    return {
      question: '',
      response: null,
      loading: false,
      error: null,
      apiKey: 'dev_key_change_me', // In real app, this would be from config/auth
      examples: [
        'show me the latest electricity data',
        'monthly electricity cost across states',
        'electricity retail price for each state'
      ]
    }
  },
  methods: {
    async submitQuery() {
      if (!this.question.trim()) return
      
      try {
        this.loading = true
        this.error = null
        this.response = null
        
        this.response = await apiService.queryUtilAgent(this.question, this.apiKey)
        
        // Emit highlight regions if available
        if (this.response.highlightRegions) {
          this.$emit('highlightRegions', this.response.highlightRegions)
        }
        
      } catch (error) {
        console.error('Util Agent query failed:', error)
        if (error.response?.status === 401) {
          this.error = 'API key required for Util Agent queries'
        } else if (error.response?.status === 400) {
          this.error = 'Invalid query format'
        } else {
          this.error = 'Query failed. Please try again.'
        }
      } finally {
        this.loading = false
      }
    },
    
    formatCell(cell) {
      if (typeof cell === 'number') {
        return cell.toFixed(1)
      }
      return cell
    },
    
    formatDate(dateString) {
      if (!dateString) return 'Unknown'
      return new Date(dateString).toLocaleString()
    }
  }
}
</script>

<style scoped>
.util-agent-panel {
  background: var(--surface-2);
  border-radius: 16px;
  padding: 1.5rem;
  margin-top: 2rem;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
}

.util-agent-panel h3 {
  margin-bottom: 1rem;
  color: var(--ink);
}

.input-group {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.query-input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 1rem;
  background: var(--surface);
  color: var(--ink);
}

.query-btn {
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

.query-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #1b9c8a, #147a6b);
}

.disclaimer {
  margin-bottom: 0.75rem;
  font-size: 0.85rem;
  color: var(--ink-muted);
  background: var(--surface-3);
  padding: 0.75rem 1rem;
  border-radius: 12px;
  border: 1px dashed var(--border);
}

.query-btn:disabled {
  background: #c9c1b7;
  cursor: not-allowed;
  box-shadow: none;
}

.examples {
  margin-bottom: 1rem;
}

.examples p {
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: var(--ink-muted);
}

.example-btn {
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 0.25rem 0.5rem;
  margin: 0.25rem;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: background-color 0.2s;
  color: var(--ink);
}

.example-btn:hover {
  background: var(--surface-3);
}

.response-section {
  border-top: 1px solid var(--border);
  padding-top: 1rem;
  margin-top: 1rem;
}

.response-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.response-header h4 {
  margin: 0;
  color: var(--ink);
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status-badge.ok {
  background: rgba(47, 143, 104, 0.15);
  color: var(--success);
}

.status-badge.insufficient_data {
  background: rgba(200, 137, 42, 0.18);
  color: var(--warning);
}

.results-table {
  margin-bottom: 1rem;
  overflow-x: auto;
}

.results-table table {
  width: 100%;
  border-collapse: collapse;
}

.results-table th,
.results-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.results-table th {
  background: var(--surface-3);
  font-weight: 600;
}

.citations {
  margin-bottom: 1rem;
}

.citations h5 {
  margin-bottom: 0.5rem;
  color: var(--ink);
}

.citations ul {
  margin: 0;
  padding-left: 1.5rem;
}

.citations li {
  margin-bottom: 0.25rem;
  font-size: 0.9rem;
}

.citations a {
  color: var(--accent-strong);
  text-decoration: none;
}

.citations a:hover {
  text-decoration: underline;
}

.notes ul {
  margin: 0;
  padding-left: 1.5rem;
}

.note {
  font-size: 0.9rem;
  color: var(--ink-muted);
  margin-bottom: 0.25rem;
}

.error-section {
  border-top: 1px solid var(--border);
  padding-top: 1rem;
  margin-top: 1rem;
}

.error {
  color: var(--danger);
  margin: 0;
}
</style>
