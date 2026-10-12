import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Browserul vorbește doar cu Vite; Vite trimite /api și /ws la serverul Spring Boot.
// Așa, pentru browser totul vine de la aceeași adresă și nu e nevoie de CORS.
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/ws': { target: 'ws://127.0.0.1:8080', ws: true },
    },
  },
})
