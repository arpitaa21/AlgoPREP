import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server runs on http://localhost:5173 (the origin the backend allows through CORS).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
});
