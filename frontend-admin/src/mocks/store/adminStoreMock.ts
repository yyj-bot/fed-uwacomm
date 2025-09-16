/**
 * Admin Store Mock 数据
 * 用于测试管理员功能相关的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { User } from '@/services'

// ==================== 模拟用户数据 ====================

export const mockUserList: User[] = [
  {
    userId: 'user-001',
    username: 'testuser',
    email: 'test@example.com',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    lastLoginTime: '2024-01-15T08:00:00Z',
    lastLoginIp: '192.168.1.100'
  },
  {
    userId: 'user-002',
    username: 'researcher',
    email: 'researcher@example.com',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2024-01-02T00:00:00Z',
    updatedAt: '2024-01-10T00:00:00Z',
    lastLoginTime: '2024-01-14T10:00:00Z',
    lastLoginIp: '192.168.1.101'
  },
  {
    userId: 'user-003',
    username: 'operator',
    email: 'operator@example.com',
    role: 'OPERATOR',
    status: 'ACTIVE',
    createdAt: '2024-01-03T00:00:00Z',
    updatedAt: '2024-01-12T00:00:00Z',
    lastLoginTime: '2024-01-15T09:00:00Z',
    lastLoginIp: '192.168.1.102'
  },
  {
    userId: 'user-004',
    username: 'viewer',
    email: 'viewer@example.com',
    role: 'VIEWER',
    status: 'INACTIVE',
    createdAt: '2024-01-04T00:00:00Z',
    updatedAt: '2024-01-08T00:00:00Z',
    lastLoginTime: '2024-01-10T15:00:00Z',
    lastLoginIp: '192.168.1.103'
  },
  {
    userId: 'admin-001',
    username: 'admin',
    email: 'admin@example.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: '2023-12-01T00:00:00Z',
    updatedAt: '2024-01-15T00:00:00Z',
    lastLoginTime: '2024-01-15T09:30:00Z',
    lastLoginIp: '192.168.1.1'
  }
]

// ==================== 模拟请求数据 ====================

export const mockCreateUserRequest = {
  username: 'newuser',
  email: 'newuser@example.com',
  password: 'password123',
  role: 'RESEARCHER' as const,
  displayName: '新用户',
  department: '计算机学院',
  phone: '13800138888'
}

export const mockUpdateUserRequest = {
  displayName: '更新用户名',
  department: '软件学院',
  phone: '13800139999',
  role: 'OPERATOR' as const,
  status: 'ACTIVE' as const
}

export const mockUserListParams = {
  page: 1,
  size: 20,
  role: undefined,
  status: undefined,
  department: undefined,
  keyword: undefined
}

// ==================== 模拟响应数据 ====================

export const mockUserListResponse = {
  list: mockUserList,
  total: mockUserList.length,
  page: 1,
  size: 20,
  totalPages: 1
}

export const mockUserStats = {
  totalUsers: 50,
  activeUsers: 45,
  inactiveUsers: 5,
  roleDistribution: {
    ADMIN: 2,
    RESEARCHER: 25,
    OPERATOR: 15,
    VIEWER: 8
  },
  departmentDistribution: {
    '计算机学院': 20,
    '人工智能学院': 15,
    '运维部': 8,
    '业务部': 5,
    '技术部': 2
  },
  recentRegistrations: 5,
  lastWeekLogins: 38
}

// ==================== 模拟系统设置 ====================

export const mockSystemSettings = {
  siteName: 'FedUWAComm 管理系统',
  siteDescription: '联邦学习水声通信管理平台',
  maxVMsPerUser: 5,
  maxTasksPerUser: 10,
  defaultUserRole: 'VIEWER' as const,
  allowUserRegistration: false,
  sessionTimeout: 7200, // 2小时
  maxFileUploadSize: 104857600, // 100MB
  enableEmailNotification: true,
  enableSMSNotification: false,
  maintenanceMode: false,
  systemVersion: '1.0.0'
}

// ==================== 模拟操作日志 ====================

export const mockOperationLogs = [
  {
    id: 'log-001',
    operator: {
      userId: 'admin-001',
      username: 'admin',
      displayName: '系统管理员'
    },
    operation: 'CREATE_USER',
    target: {
      type: 'USER',
      id: 'user-005',
      name: 'newuser'
    },
    details: '创建新用户 newuser',
    result: 'SUCCESS',
    ip: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    timestamp: '2024-01-15T09:30:00Z'
  },
  {
    id: 'log-002',
    operator: {
      userId: 'admin-001',
      username: 'admin',
      displayName: '系统管理员'
    },
    operation: 'UPDATE_USER',
    target: {
      type: 'USER',
      id: 'user-004',
      name: 'viewer'
    },
    details: '更新用户信息',
    result: 'SUCCESS',
    ip: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    timestamp: '2024-01-15T09:15:00Z'
  },
  {
    id: 'log-003',
    operator: {
      userId: 'admin-001',
      username: 'admin',
      displayName: '系统管理员'
    },
    operation: 'DELETE_USER',
    target: {
      type: 'USER',
      id: 'user-006',
      name: 'olduser'
    },
    details: '删除用户 olduser',
    result: 'FAILED',
    error: '用户仍有关联的虚拟机',
    ip: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    timestamp: '2024-01-15T09:00:00Z'
  }
]

// ==================== 模拟错误响应 ====================

export const mockAdminErrors = {
  USER_NOT_FOUND: new Error('用户不存在'),
  USERNAME_ALREADY_EXISTS: new Error('用户名已存在'),
  EMAIL_ALREADY_EXISTS: new Error('邮箱已存在'),
  CANNOT_DELETE_ADMIN: new Error('无法删除管理员用户'),
  CANNOT_DELETE_SELF: new Error('无法删除自己'),
  INSUFFICIENT_PERMISSION: new Error('权限不足'),
  INVALID_ROLE: new Error('无效的角色'),
  USER_HAS_RESOURCES: new Error('用户仍有关联资源'),
  NETWORK_ERROR: new Error('网络连接失败'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟状态数据 ====================

export const mockInitialAdminState = {
  userList: [],
  userListTotal: 0,
  userListLoading: false,
  userListError: null,
  currentUser: null,
  currentUserLoading: false,
  currentUserError: null,
  userStats: null,
  userStatsLoading: false,
  userStatsError: null,
  systemSettings: null,
  systemSettingsLoading: false,
  systemSettingsError: null,
  operationLogs: [],
  operationLogsLoading: false,
  operationLogsError: null,
  operationLoading: {},
  operationError: {},
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  queryParams: {}
}

export const mockLoadedAdminState = {
  ...mockInitialAdminState,
  userList: mockUserList,
  userListTotal: mockUserList.length,
  userStats: mockUserStats,
  systemSettings: mockSystemSettings,
  operationLogs: mockOperationLogs,
  pagination: {
    page: 1,
    size: 20,
    total: mockUserList.length
  }
}

export const mockLoadingAdminState = {
  ...mockInitialAdminState,
  userListLoading: true,
  userStatsLoading: true,
  systemSettingsLoading: true
}

export const mockErrorAdminState = {
  ...mockInitialAdminState,
  userListError: '获取用户列表失败',
  userStatsError: '获取用户统计失败',
  systemSettingsError: '获取系统设置失败'
}

// ==================== 模拟工具函数 ====================

export const createMockUser = (overrides: Partial<User> = {}): User => ({
  ...mockUserList[0],
  userId: `user-${Date.now()}`,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...overrides
})

export const createMockUserListResponse = (users: User[] = mockUserList) => ({
  list: users,
  total: users.length,
  page: 1,
  size: 20,
  totalPages: Math.ceil(users.length / 20)
})

export const createMockOperationLog = (overrides: Partial<typeof mockOperationLogs[0]> = {}) => ({
  ...mockOperationLogs[0],
  id: `log-${Date.now()}`,
  timestamp: new Date().toISOString(),
  ...overrides
})

// ==================== 导出默认 Mock ====================
export default {
  mockUserList,
  mockCreateUserRequest,
  mockUpdateUserRequest,
  mockUserListParams,
  mockUserListResponse,
  mockUserStats,
  mockSystemSettings,
  mockOperationLogs,
  mockAdminErrors,
  mockInitialAdminState,
  mockLoadedAdminState,
  mockLoadingAdminState,
  mockErrorAdminState,
  createMockUser,
  createMockUserListResponse,
  createMockOperationLog
}
