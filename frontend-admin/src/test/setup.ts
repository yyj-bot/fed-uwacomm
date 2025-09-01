/**
 * Vitest 测试环境设置
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import '@testing-library/jest-dom'
import { vi } from 'vitest'

// 全局测试配置
global.console = {
  ...console,
  // 在测试中静默某些日志输出
  log: vi.fn(),
  debug: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
}

// Mock atob and btoa functions for JWT token parsing
global.atob = (str: string) => Buffer.from(str, 'base64').toString('binary')
global.btoa = (str: string) => Buffer.from(str, 'binary').toString('base64')

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}

Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock
})

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    href: 'http://localhost:3000',
    origin: 'http://localhost:3000',
    pathname: '/',
    search: '',
    hash: '',
    replace: vi.fn(),
    assign: vi.fn(),
    reload: vi.fn(),
  },
  writable: true,
})

// 测试前的全局设置
beforeEach(() => {
  // 清理所有模拟
  vi.clearAllMocks()
  
  // 重置localStorage和sessionStorage
  localStorageMock.getItem.mockClear()
  localStorageMock.setItem.mockClear()
  localStorageMock.removeItem.mockClear()
  localStorageMock.clear.mockClear()
  
  sessionStorageMock.getItem.mockClear()
  sessionStorageMock.setItem.mockClear()
  sessionStorageMock.removeItem.mockClear()
  sessionStorageMock.clear.mockClear()
})

// 测试后的清理
afterEach(() => {
  vi.restoreAllMocks()
})
