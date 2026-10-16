import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server runs on port 5173. The Java backend allows this origin (CORS).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
});
