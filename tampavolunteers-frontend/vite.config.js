import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        // Use backend service name in Docker, localhost for local development
        target: process.env.DOCKER_ENV ? 'http://backend:8080' : 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
