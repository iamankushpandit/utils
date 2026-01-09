import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const port = Number(env.VITE_DEV_PORT) || 5173

  return {
    plugins: [vue()],
    server: {
      port,
      strictPort: true
    },
    test: {
      environment: 'jsdom',
      globals: true
    },
    define: {
      'process.env': process.env
    }
  }
})
