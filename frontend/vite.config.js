import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// In dev, forward /api calls to the Spring Boot backend so the browser sees one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
