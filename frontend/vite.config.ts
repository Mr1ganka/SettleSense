import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');

  const clientPort = parseInt(env.CLIENT_PORT || env.PORT || '5173', 10);
  const serverPort = env.SERVER_PORT || '8081';
  const proxyTarget = env.VITE_API_PROXY_TARGET || `http://127.0.0.1:${serverPort}`;

  return {
    plugins: [react()],
    server: {
      port: clientPort,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  };
});
