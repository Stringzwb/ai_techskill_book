import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 启用 Vue 单文件组件。
  plugins: [vue()],
  // 本地开发服务器配置。
  server: {
    host: '127.0.0.1',
    port: 5173,
    // 将本地 API 请求代理到 Spring Boot。
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
