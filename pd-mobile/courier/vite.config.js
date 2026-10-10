import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      // H5 开发态：相对 /api 反代到网关，避免跨域
      '/api': {
        target: 'http://192.168.20.130:8760',
        changeOrigin: true,
      },
    },
  },
})
