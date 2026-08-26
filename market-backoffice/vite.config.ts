import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
  },
  test: {
    environment: 'node',
    // environment.ts exige VITE_API_BASE_URL al importarse (import.meta.env) —
    // sin esto cualquier test que toque la capa HTTP falla al cargar el módulo.
    env: {
      VITE_API_BASE_URL: 'http://localhost:8080',
      VITE_API_TIMEOUT: '15000',
    },
  },
})
