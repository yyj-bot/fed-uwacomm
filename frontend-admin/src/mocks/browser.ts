/**
 * Mock Service Worker 浏览器配置
 * 用于在开发环境下模拟 API 响应
 */

import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

export const worker = setupWorker(...handlers)

