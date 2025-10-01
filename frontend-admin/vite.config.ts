import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
import { existsSync } from 'fs'

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  // 加载环境变量 - 指定查找路径
  const env = loadEnv(mode, process.cwd(), '')
  const enableMock = env.VITE_ENABLE_MOCK === 'true'
  
  console.log('🔧 Vite配置详细信息:', { 
    mode, 
    command, 
    cwd: process.cwd(),
    enableMock, 
    envValue: env.VITE_ENABLE_MOCK,
    allEnvKeys: Object.keys(env).filter(k => k.startsWith('VITE_')),
    envFileChecked: {
      '.env': existsSync('.env'),
      '.env.local': existsSync('.env.local'),
      '.env.development': existsSync('.env.development'),
      '.env.development.local': existsSync('.env.development.local')
    }
  })

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'),
      },
    },
    define: {
      // 修复 'global is not defined' 错误
      global: 'globalThis',
    },
    server: {
      port: 5173,
      // 智能代理配置：Mock启用时不使用代理，禁用时使用代理
      proxy: enableMock ? {} : {
        // 代理所有 /api 请求到后端服务器
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('🔴 Proxy Error:', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('📤 Proxying Request:', req.method, req.url);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('📥 Proxy Response:', proxyRes.statusCode, req.url);
            });
          },
        },
        // 代理WebSocket连接
        '/ws': {
          target: 'ws://localhost:8080',
          ws: true,
          changeOrigin: true,
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['react', 'react-dom'],
            antd: ['antd'],
            router: ['react-router-dom'],
            websocket: ['sockjs-client', '@stomp/stompjs'],
          },
        },
      },
    },
  }
})