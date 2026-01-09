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
          :disabled="loading || !question.trim"
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
        <!-- Markdown Answer -->
        <div class="markdown-answer" v-html="renderMarkdown(response.summary)"></div>
        
        <div class="header-badges">
          <span :class="['status-badge', response.status.toLowerCase()]">
            {{ response.status }}
          </span>
          <span v-if="response.responseOrigin" class="origin-badge">
            {{ response.responseOrigin }}
          </span>
        </div>
      </div>
      
      <!-- Debug / Thinking Toggle -->
      <div v-if="response.debug_meta && response.debug_meta.generated_sql" class="debug-toggle-area">
        <button @click="toggleThinking" class="toggle-thinking-btn">
          {{ showThinking ? 'Hide SQL' : 'Show SQL Logic' }}
        </button>
        <div v-if="showThinking" class="thinking-box">
          <pre><code>{{ response.debug_meta.generated_sql }}</code></pre>
          <div v-if="response.debug_meta.sql_result" class="raw-result">
            <small>Raw Rows: {{ response.debug_meta.sql_result.length }}</small>
          </div>
        </div>
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

      <div v-if="response.disclaimer" class="response-disclaimer">
        {{ response.disclaimer }}
      </div>

      <div class="feedback-section" v-if="response.queryId && !feedbackSent">
        <span class="feedback-label">Was this helpful?</span>
        <button class="feedback-btn" @click="sendFeedback('THUMBS_UP')" title="Yes">👍</button>
        <button class="feedback-btn" @click="sendFeedback('THUMBS_DOWN')" title="No">👎</button>
      </div>
      <div v-if="feedbackSent" class="feedback-thankyou">
        Thanks for your feedback!
      </div>
    </div>
    
    <div v-if="error" class="error-section">
      <p class="error">{{ error }}</p>
    </div>
  </div>
</template>

<!-- 
  API Documentation for UI Developers:
  
  Component: UtilAgentPanel
  Purpose: Provides a conversational interface for the user to query utility data.
  
  Props: None
  
  State:
  - messages: Array of { id, text, isUser, timestamp }
  - isLoading: Boolean
  
  Key Methods:
  - sendMessage(): Posts payload to /api/v1/util-agent/query
  - handleFeedback(id, type): Posts to /api/v1/util-agent/feedback
-->
<script>
import { apiService } from '../services/api.js'
import { marked } from 'marked'

export default {
  name: 'UtilAgentPanel',
  data() {
    return {
      question: '',
      response: null,
      loading: false,
      error: null,
      feedbackSent: false,
      showThinking: false,
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
      
      this.loading = true
      this.error = null
      this.response = null
      this.feedbackSent = false
      this.showThinking = false

      try {
        // Use the generic queryAgent wrapper, or queryUtilAgent if that's what is in api.js
        // Based on previous file reads, it seems 'queryUtilAgent' is used here, but let's stick to the existing method call pattern
        // The read_file showed: this.response = await apiService.queryUtilAgent(this.question, this.apiKey)
        const rawRes = await apiService.queryUtilAgent(this.question, this.apiKey)
        
        // Enrich
        this.response = {
          ...rawRes,
          summary: rawRes.answer,
          status: rawRes.sources?.length ? 'SUCCESS' : 'NO_DATA',
          responseOrigin: rawRes.sources?.includes('GEN_AI_SQL') ? 'GenAI' : 'RuleEngine'
        }

      } catch (err) {
        this.error = err.response?.data?.message || 'Failed to get response from Util Agent'
        console.error(err)
      } finally {
        this.loading = false
      }
    },

    toggleThinking() {
      this.showThinking = !this.showThinking
    },

    renderMarkdown(text) {
      if (!text) return ''
      try {
        return marked.parse(text)
      } catch (e) {
        return text
      }
    },

    async sendFeedback(type) {
        if (!this.response || !this.response.queryId) return;
        try {
            await apiService.submitAgentFeedback(this.response.queryId, type, this.apiKey);
            this.feedbackSent = true;
        } catch (e) {
            console.error("Feedback failed", e);
        }
    },

    formatCell(cell) {
      if (typeof cell === 'number') {
        return cell.toFixed(1)
      }
      return cell
    },
    
    formatDate(dateStr) {
      if (!dateStr) return 'Unknown'
      return new Date(dateStr).toLocaleString()
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

.header-badges {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.origin-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  background-color: var(--surface-3);
  color: var(--ink);
  border: 1px solid var(--border);
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

.response-disclaimer {
  font-size: 0.8rem;
  color: var(--ink-light);
  font-style: italic;
  margin-top: 1rem;
  border-top: 1px solid var(--ice);
  padding-top: 0.5rem;
}

.feedback-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--ice);
}

.feedback-label {
    font-size: 0.9rem;
    color: var(--ink-muted);
}

.feedback-btn {
    background: none;
    border: 1px solid var(--ice);
    border-radius: 4px;
    cursor: pointer;
    padding: 0.2rem 0.5rem;
    font-size: 1.2rem;
}
.feedback-btn:hover {
    background: var(--sky-light);
}

.feedback-thankyou {
    margin-top: 1rem;
    color: var(--green);
    font-size: 0.9rem;
    font-style: italic;
}

/* Added via script for LLM support */
.markdown-answer {
  font-size: 1.1rem;
  line-height: 1.6;
  color: var(--ink);
  margin-bottom: 1rem;
}
.markdown-answer p {
  margin-bottom: 0.75rem;
}
.markdown-answer code {
  background: #f4f4f4;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-family: monospace;
}
.markdown-answer pre {
  background: #f4f4f4;
  padding: 1rem;
  border-radius: 8px;
  overflow-x: auto;
}

.debug-toggle-area {
  margin: 1rem 0;
}
.toggle-thinking-btn {
  background: none;
  border: 1px solid var(--border);
  padding: 0.25rem 0.75rem;
  border-radius: 99px;
  font-size: 0.8rem;
  cursor: pointer;
  color: var(--ink-muted);
}
.toggle-thinking-btn:hover {
  background: var(--surface-2);
  color: var(--ink);
}
.thinking-box {
  margin-top: 0.5rem;
  background: #2d2d2d;
  color: #ccc;
  padding: 1rem;
  border-radius: 8px;
  font-family: monospace;
  font-size: 0.9rem;
  overflow-x: auto;
}
</style>
