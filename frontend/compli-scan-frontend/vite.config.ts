import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // ✅ keep this so /api/... on the FE hits /... on the BE
        rewrite: p => p.replace(/^\/api/, ''),
      },
    },
  },
});
