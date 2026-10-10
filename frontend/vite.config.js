import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the dev server proxies /api to the Spring Boot backend, so the browser never hits CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true } },
  },
});
