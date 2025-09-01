import '@testing-library/jest-dom'
import { beforeAll, afterAll, afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { handlers } from '../mocks/handlers'

// 模拟 matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// 模拟 ResizeObserver
class ResizeObserverMock {
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
}

window.ResizeObserver = ResizeObserverMock

// 模拟 IntersectionObserver
class IntersectionObserverMock {
  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
  root = null
  rootMargin = ''
  thresholds = []
  takeRecords = vi.fn()
}

window.IntersectionObserver = IntersectionObserverMock

// 模拟 getComputedStyle
Object.defineProperty(window, 'getComputedStyle', {
  value: () => ({
    getPropertyValue: () => '',
  }),
})

// 模拟 scrollTo
window.scrollTo = vi.fn().mockImplementation((x: number, y: number) => {})

// 设置MSW服务器
export const server = setupServer(...handlers)

// 在所有测试之前启动服务器
beforeAll(() => {
  // 设置全局超时时间
  vi.setConfig({ testTimeout: 10000 })
  server.listen({ onUnhandledRequest: 'error' })
})

// 每个测试后重置处理程序
afterEach(() => {
  server.resetHandlers()
  cleanup()
  vi.clearAllMocks()
})

// 所有测试完成后关闭服务器
afterAll(() => {
  server.close()
}) 