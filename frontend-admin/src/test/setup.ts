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

// Mock WebSocket
const MockWebSocket = vi.fn().mockImplementation(() => ({
  close: vi.fn(),
  send: vi.fn(),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  readyState: 1, // OPEN
}))

// Add static properties to the mock constructor
Object.assign(MockWebSocket, {
  CONNECTING: 0,
  OPEN: 1,
  CLOSING: 2,
  CLOSED: 3
})

global.WebSocket = MockWebSocket as any

// Mock fetch for API calls
global.fetch = vi.fn()

// Don't mock zustand in test setup - let stores work normally

// Mock axios
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      patch: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    })),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
}))

// Mock services
vi.mock('@/services', () => ({
  ConnectionState: {
    DISCONNECTED: 'DISCONNECTED',
    CONNECTING: 'CONNECTING',
    CONNECTED: 'CONNECTED',
    RECONNECTING: 'RECONNECTING',
    ERROR: 'ERROR'
  },
  userService: {
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    refreshToken: vi.fn(),
    isAuthenticated: vi.fn(() => true)
  },
  vmService: {
    getVMList: vi.fn(),
    getVMDetail: vi.fn(),
    getVMStatus: vi.fn(),
    updateVM: vi.fn(),
    deleteVM: vi.fn(),
    startVM: vi.fn(),
    stopVM: vi.fn(),
    restartVM: vi.fn()
  },
  wsService: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    send: vi.fn(),
    subscribe: vi.fn(),
    unsubscribe: vi.fn(),
    isConnected: vi.fn(() => false),
    getState: vi.fn(() => ({ connected: false, connecting: false, error: null, lastHeartbeat: null }))
  },
  adminService: {
    getUserList: vi.fn(),
    getUserDetail: vi.fn(),
    createUser: vi.fn(),
    updateUser: vi.fn(),
    deleteUser: vi.fn(),
    lockUser: vi.fn(),
    unlockUser: vi.fn(),
    resetUserPassword: vi.fn(),
    getUserPermissions: vi.fn(),
    grantUserPermission: vi.fn(),
    revokeUserPermission: vi.fn()
  },
  systemService: {
    getSystemInfo: vi.fn(),
    getSystemLogs: vi.fn(),
    getSystemMetrics: vi.fn()
  },
  trainingDataService: {
    getDatasetList: vi.fn(),
    getDatasetDetail: vi.fn(),
    uploadDataset: vi.fn(),
    deleteDataset: vi.fn()
  },
  modelVersionService: {
    getModelList: vi.fn(),
    getModelDetail: vi.fn(),
    createModel: vi.fn(),
    updateModel: vi.fn(),
    deleteModel: vi.fn()
  },
  federatedTaskService: {
    getTaskList: vi.fn(),
    getTaskDetail: vi.fn(),
    createTask: vi.fn(),
    updateTask: vi.fn(),
    deleteTask: vi.fn(),
    startTask: vi.fn(),
    stopTask: vi.fn()
  },
  dashboardService: {
    getDashboardStats: vi.fn(),
    getRecentActivities: vi.fn(),
    getSystemStatus: vi.fn()
  }
}))

// 测试后的清理
afterEach(() => {
  vi.restoreAllMocks()
  vi.clearAllMocks()
})
