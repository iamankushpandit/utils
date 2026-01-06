import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import MapExplorer from './views/MapExplorer.vue'
import Transparency from './views/Transparency.vue'
import Highcharts from 'highcharts'
import HighchartsMap from 'highcharts/modules/map'

const routes = [
  { path: '/', component: MapExplorer },
  { path: '/transparency', component: Transparency }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

HighchartsMap(Highcharts)

const app = createApp(App)
app.use(router)
app.mount('#app')
