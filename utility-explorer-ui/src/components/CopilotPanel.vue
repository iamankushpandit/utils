<template>
  <div class="copilot-panel">
    <h3>Ask Questions About the Data</h3>
    
    <div class="query-section">
      <div class="input-group">
        <input 
          v-model="question" 
          @keyup.enter="submitQuery"
          placeholder="Try: 'high electricity and low broadband'"
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
  name: 'CopilotPanel',
  data() {
    return {
      question: '',
      response: null,
      loading: false,
      error: null,
      apiKey: 'dev_key_change_me', // In real app, this would be from config/auth
      examples: [
        'high electricity and low broadband',
        'states with highest electricity prices',
        'show me electricity data'
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
        
        this.response = await apiService.queryCopilot(this.question, this.apiKey)
        
        // Emit highlight regions if available
        if (this.response.highlightRegions) {
          this.$emit('highlightRegions', this.response.highlightRegions)
        }
        
      } catch (error) {
        console.error('Copilot query failed:', error)
        if (error.response?.status === 401) {
          this.error = 'API key required for Copilot queries'
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
.copilot-panel {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-top: 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.copilot-panel h3 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.input-group {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.query-input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.query-btn {
  background: #3498db;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.query-btn:hover:not(:disabled) {
  background: #2980b9;
}

.query-btn:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

.examples {
  margin-bottom: 1rem;
}

.examples p {
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.example-btn {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  padding: 0.25rem 0.5rem;
  margin: 0.25rem;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8rem;
  transition: background-color 0.2s;
}

.example-btn:hover {
  background: #e9ecef;
}

.response-section {
  border-top: 1px solid #e0e0e0;
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
  color: #2c3e50;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
}

.status-badge.ok {
  background: #d4edda;
  color: #155724;
}

.status-badge.insufficient_data {
  background: #fff3cd;
  color: #856404;
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
  border-bottom: 1px solid #e0e0e0;
}

.results-table th {
  background: #f8f9fa;
  font-weight: 600;
}

.citations {
  margin-bottom: 1rem;
}

.citations h5 {
  margin-bottom: 0.5rem;
  color: #2c3e50;
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
  color: #3498db;
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
  color: #666;
  margin-bottom: 0.25rem;
}

.error-section {
  border-top: 1px solid #e0e0e0;
  padding-top: 1rem;
  margin-top: 1rem;
}

.error {
  color: #e74c3c;
  margin: 0;
}
</style>