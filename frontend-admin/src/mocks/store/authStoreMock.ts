/**
 * Auth Store Mock 数据
 * 用于测试认证相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { User, LoginResponse } from '@/services'

// ==================== 模拟用户数据 ====================

export const mockUser: User = {
  userId: 'user-001',
  username: 'testuser',
  email: 'test@example.com',
  role: 'RESEARCHER',
  status: 'ACTIVE',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  lastLoginTime: '2024-01-15T08:00:00Z',
  lastLoginIp: '192.168.1.100'
}

export const mockAdminUser: User = {
  ...mockUser,
  userId: 'admin-001',
  username: 'admin',
  email: 'admin@example.com',
  role: 'ADMIN'
}

export const mockOperatorUser: User = {
  ...mockUser,
  userId: 'operator-001',
  username: 'operator',
  email: 'operator@example.com',
  role: 'OPERATOR'
}

// ==================== 模拟登录响应 ====================

export const mockLoginResponse: LoginResponse = {
  user: mockUser,
  token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLTAwMSIsInVzZXJuYW1lIjoidGVzdHVzZXIiLCJyb2xlIjoiUkVTRUFSQ0hFUiIsImlhdCI6MTcwNDEwNzYwMCwiZXhwIjoxNzA0MTk0MDAwfQ.test-jwt-token',
  refreshToken: 'refresh-token-example',
  expiresIn: 86400
}

export const mockAdminLoginResponse: LoginResponse = {
  user: mockAdminUser,
  token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbi0wMDEiLCJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzA0MTA3NjAwLCJleHAiOjE3MDQxOTQwMDB9.admin-jwt-token',
  refreshToken: 'admin-refresh-token',
  expiresIn: 86400
}

// ==================== 模拟请求数据 ====================

export const mockLoginRequest = {
  loginIdentifier: 'testuser',
  password: 'password123'
}

export const mockRegisterRequest = {
  username: 'newuser',
  email: 'newuser@example.com',
  password: 'password123',
  confirmPassword: 'password123'
}

export const mockUpdateProfileRequest = {
  username: 'updateduser',
  email: 'updated@example.com'
}

export const mockChangePasswordRequest = {
  oldPassword: 'password123',
  newPassword: 'newpassword123',
  confirmPassword: 'newpassword123'
}

// ==================== 模拟错误响应 ====================

export const mockAuthErrors = {
  LOGIN_FAILED: new Error('用户名或密码错误'),
  INVALID_TOKEN: new Error('无效的令牌'),
  TOKEN_EXPIRED: new Error('令牌已过期'),
  USER_NOT_FOUND: new Error('用户不存在'),
  EMAIL_ALREADY_EXISTS: new Error('邮箱已存在'),
  USERNAME_ALREADY_EXISTS: new Error('用户名已存在'),
  WEAK_PASSWORD: new Error('密码强度不足'),
  NETWORK_ERROR: new Error('网络连接失败'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟状态数据 ====================

export const mockInitialAuthState = {
  isAuthenticated: false,
  isLoading: false,
  error: null,
  user: null,
  token: null,
  refreshToken: null,
  loginStatus: 'idle' as const,
  registerStatus: 'idle' as const,
  updateProfileStatus: 'idle' as const,
  changePasswordStatus: 'idle' as const
}

export const mockAuthenticatedState = {
  ...mockInitialAuthState,
  isAuthenticated: true,
  user: mockUser,
  token: mockLoginResponse.token,
  refreshToken: mockLoginResponse.refreshToken,
  loginStatus: 'success' as const
}

export const mockLoadingState = {
  ...mockInitialAuthState,
  isLoading: true,
  loginStatus: 'logging' as const
}

export const mockErrorState = {
  ...mockInitialAuthState,
  error: '登录失败',
  loginStatus: 'failed' as const
}

// ==================== 模拟权限数据 ====================

export const mockPermissions = {
  ADMIN: ['vm:read', 'vm:write', 'vm:delete', 'task:read', 'task:write', 'task:delete', 'user:read', 'user:write', 'user:delete'],
  RESEARCHER: ['vm:read', 'task:read', 'task:create', 'data:read', 'data:upload', 'model:read'],
  OPERATOR: ['vm:read', 'vm:operate', 'task:read', 'data:read', 'model:read'],
  VIEWER: ['vm:read', 'task:read', 'data:read', 'model:read']
}

// ==================== 模拟工具函数 ====================

export const createMockUser = (overrides: Partial<User> = {}): User => ({
  ...mockUser,
  ...overrides
})

export const createMockLoginResponse = (user: User = mockUser): LoginResponse => ({
  user,
  token: `mock-token-${user.userId}`,
  refreshToken: `mock-refresh-token-${user.userId}`,
  expiresIn: 86400
})

// ==================== 导出默认 Mock ====================
export default {
  mockUser,
  mockAdminUser,
  mockOperatorUser,
  mockLoginResponse,
  mockAdminLoginResponse,
  mockLoginRequest,
  mockRegisterRequest,
  mockUpdateProfileRequest,
  mockChangePasswordRequest,
  mockAuthErrors,
  mockInitialAuthState,
  mockAuthenticatedState,
  mockLoadingState,
  mockErrorState,
  mockPermissions,
  createMockUser,
  createMockLoginResponse
}
