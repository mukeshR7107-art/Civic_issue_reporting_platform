import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({plugins:[react()],server:{proxy:{'/api':'http://localhost:8080'}},build:{target:'es2022',rollupOptions:{output:{manualChunks:{maps:['leaflet'],auth:['@supabase/supabase-js'],react:['react','react-dom']}}}}});
