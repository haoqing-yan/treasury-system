import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const apiTarget = env.VITE_API_TARGET || 'http://127.0.0.1:8080'
  return {
    plugins: [vue()],
    resolve: {
      alias: { '@': decodeURIComponent(new URL('./src', import.meta.url).pathname) },
    },
    server: {
      host: 'localhost',
      port: 5173,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
        '/logout': { target: apiTarget, changeOrigin: true },
        '/actuator': { target: apiTarget, changeOrigin: true },
      },
    },
  }
})
