/**
 * Mock Service Worker 浏览器配置
 * 用于在开发环境下模拟 API 响应
 */

import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

export const worker = setupWorker(...handlers)

// 添加生命周期事件监听
worker.events.on('request:start', ({ request }) => {
  console.log('[MSW] 请求开始:', request.method, request.url)
})

worker.events.on('request:match', ({ request }) => {
  console.log('[MSW] ✅ 匹配成功:', request.method, request.url)
})

worker.events.on('request:unhandled', ({ request }) => {
  console.log('[MSW] ⚠️ 未匹配:', request.method, request.url)
})

worker.events.on('response:mocked', ({ request, response }) => {
  console.log('[MSW] 📤 返回Mock响应:', request.method, request.url, response.status)
})
