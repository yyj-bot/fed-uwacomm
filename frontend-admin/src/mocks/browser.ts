import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

// 创建worker实例
export const worker = setupWorker(...handlers)

// 导出启动函数
export async function startMockServiceWorker() {
  if (process.env.NODE_ENV === 'development') {
    return worker.start({
      onUnhandledRequest: 'bypass',
      serviceWorker: {
        url: '/mockServiceWorker.js'
      }
    })
  }
} 