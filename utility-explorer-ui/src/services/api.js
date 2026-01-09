import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
})

const ingestionApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 120000
})

export const apiService = {
  async getSourcesStatus() {
    const response = await api.get('/status/sources')
    return response.data
  },
  
  async getMetrics() {
    const response = await api.get('/metrics')
    return response.data
  },
  
  async getSources() {
    const response = await api.get('/sources')
    return response.data
  },

  async runIngestion() {
    const response = await ingestionApi.post('/ingestion/run')
    return response.data
  },

  async runIngestionForSource(sourceId) {
    const response = await ingestionApi.post(`/ingestion/run/${sourceId}`)
    return response.data
  },
  
  async getTimeSeries(params) {
    const response = await api.get('/timeseries', { params })
    return response.data
  },
  
  async getMap(params) {
    const response = await api.get('/map', { params })
    return response.data
  },
  
  async getMapRange(params) {
    const response = await api.get('/map/range', { params })
    return response.data
  },
  
  async exportCsv(params) {
    const response = await api.get('/export/csv', { 
      params,
      responseType: 'blob'
    })
    return response.data
  },
  
  async queryUtilAgent(question, apiKey) {
    const response = await api.post('/util-agent/query', 
      { question },
      { headers: { 'X-API-Key': apiKey } }
    )
    return response.data
  },

  async submitAgentFeedback(queryId, feedback, apiKey) {
    await api.post('/util-agent/feedback',
       { queryId, feedback },
       { headers: { 'X-API-Key': apiKey } }
    )
  }
}

export default apiService
