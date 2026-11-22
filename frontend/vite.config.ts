import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Calls to /api/... and /actuator/... are forwarded to the Spring Boot app,
      // so the browser sees one origin and no CORS setup is needed in development.
      '/api': 'http://localhost:8081',
      '/actuator': 'http://localhost:8081',
    },
  },
})
