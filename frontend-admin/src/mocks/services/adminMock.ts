/**
 * 管理员API Mock数据
 * 提供与接口文档一致的Mock数据，用于开发和测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { 
  User, 
  PaginatedResponse, 
  ApiResponse 
} from '@/types'
import type { Permission } from '@/services/admin/type'

// ==================== Mock数据生成工具 ====================

/**
 * 生成随机用户ID
 */
const generateUserId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机权限ID
 */
const generatePermissionId = (): string => {
  return 'p' + Array.from({ length: 31 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString()
}

// ==================== Mock用户数据 ====================

/**
 * Mock用户列表
 */
export const mockUsers: User[] = [
  {
    userId: 'a1b2c3d4e5f678901234567890123456',
    username: 'admin',
    email: 'admin@example.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(1),
    lastLoginIp: '192.168.1.100',
    createdAt: '2024-01-01T10:00:00Z',
    updatedAt: '2024-01-01T13:00:00Z'
  },
  {
    userId: 'b2c3d4e5f6789012345678901234567a',
    username: 'researcher01',
    email: 'researcher01@example.com',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(0),
    lastLoginIp: '192.168.1.101',
    createdAt: '2024-01-02T09:00:00Z',
    updatedAt: '2024-01-02T14:30:00Z'
  },
  {
    userId: 'c3d4e5f67890123456789012345678ab',
    username: 'operator01',
    email: 'operator01@example.com',
    role: 'OPERATOR',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(2),
    lastLoginIp: '192.168.1.102',
    createdAt: '2024-01-03T11:00:00Z',
    updatedAt: '2024-01-03T16:00:00Z'
  },
  {
    userId: 'd4e5f678901234567890123456789abc',
    username: 'viewer01',
    email: 'viewer01@example.com',
    role: 'VIEWER',
    status: 'INACTIVE',
    lastLoginTime: generateTimestamp(7),
    lastLoginIp: '192.168.1.103',
    createdAt: '2024-01-04T08:00:00Z',
    updatedAt: '2024-01-04T12:00:00Z'
  },
  {
    userId: 'e5f678901234567890123456789abcd',
    username: 'locked_user',
    email: 'locked@example.com',
    role: 'RESEARCHER',
    status: 'LOCKED',
    lastLoginTime: generateTimestamp(3),
    lastLoginIp: '192.168.1.104',
    createdAt: '2024-01-05T10:00:00Z',
    updatedAt: '2024-01-05T15:30:00Z'
  }
]

// ==================== Mock权限数据 ====================

/**
 * Mock权限列表
 */
export const mockPermissions: Permission[] = [
  {
    permissionId: 'p1b2c3d4e5f678901234567890123456',
    permissionName: 'READ_DATA',
    description: '读取数据权限',
    grantedAt: '2024-01-01T10:00:00Z'
  },
  {
    permissionId: 'p2c3d4e5f6789012345678901234567a',
    permissionName: 'WRITE_DATA',
    description: '写入数据权限',
    grantedAt: '2024-01-01T10:05:00Z'
  },
  {
    permissionId: 'p3d4e5f67890123456789012345678ab',
    permissionName: 'MANAGE_USERS',
    description: '用户管理权限',
    grantedAt: '2024-01-01T10:10:00Z'
  },
  {
    permissionId: 'p4e5f678901234567890123456789abc',
    permissionName: 'MANAGE_TASKS',
    description: '任务管理权限',
    grantedAt: '2024-01-01T10:15:00Z'
  },
  {
    permissionId: 'p5f678901234567890123456789abcd',
    permissionName: 'SYSTEM_ADMIN',
    description: '系统管理权限',
    grantedAt: '2024-01-01T10:20:00Z'
  }
]

// ==================== Mock API响应 ====================

/**
 * 创建成功响应
 */
export const createSuccessResponse = <T>(data: T, message: string = '操作成功'): ApiResponse<T> => ({
  code: 200,
  message,
  data
})

/**
 * 创建错误响应
 */
export const createErrorResponse = <T = null>(code: number, message: string, data?: T): ApiResponse<T> => ({
  code,
  message,
  data: data || null as T
})

// ==================== Mock服务方法 ====================

/**
 * Mock管理员API服务
 */
export const mockAdminApi = {
  /**
   * Mock获取用户列表
   */
  getUserList: (params: {
    page?: number
    size?: number
    username?: string
    email?: string
    role?: string
    status?: string
  } = {}): ApiResponse<PaginatedResponse<User>> => {
    const { page = 1, size = 10, username, email, role, status } = params
    
    let filteredUsers = [...mockUsers]
    
    // 应用过滤条件
    if (username) {
      filteredUsers = filteredUsers.filter(user => 
        user.username.toLowerCase().includes(username.toLowerCase())
      )
    }
    if (email) {
      filteredUsers = filteredUsers.filter(user => 
        user.email.toLowerCase().includes(email.toLowerCase())
      )
    }
    if (role) {
      filteredUsers = filteredUsers.filter(user => user.role === role)
    }
    if (status) {
      filteredUsers = filteredUsers.filter(user => user.status === status)
    }
    
    // 分页处理
    const total = filteredUsers.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const records = filteredUsers.slice(startIndex, endIndex)
    
    return createSuccessResponse({
      total,
      page,
      size,
      pages: Math.ceil(total / size),
      records
    }, '获取成功')
  },

  /**
   * Mock获取用户详情
   */
  getUserDetail: (userId: string): ApiResponse<User> => {
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return createErrorResponse<User>(404, '用户不存在')
    }
    return createSuccessResponse(user, '获取成功')
  },

  /**
   * Mock创建用户
   */
  createUser: (userData: {
    username: string
    email: string
    password: string
    role: string
    status?: string
  }): ApiResponse<User> => {
    // 检查用户名是否已存在
    if (mockUsers.some(u => u.username === userData.username)) {
      return createErrorResponse<User>(409, '用户名已存在')
    }
    
    // 检查邮箱是否已存在
    if (mockUsers.some(u => u.email === userData.email)) {
      return createErrorResponse<User>(409, '邮箱已存在')
    }
    
    const newUser: User = {
      userId: generateUserId(),
      username: userData.username,
      email: userData.email,
      role: userData.role as User['role'],
      status: (userData.status as User['status']) || 'ACTIVE',
      lastLoginTime: undefined,
      lastLoginIp: undefined,
      createdAt: generateTimestamp(),
      updatedAt: generateTimestamp()
    }
    
    mockUsers.push(newUser)
    return createSuccessResponse(newUser, '创建成功')
  },

  /**
   * Mock更新用户
   */
  updateUser: (userId: string, userData: {
    username?: string
    email?: string
    role?: string
    status?: string
    password?: string
  }): ApiResponse<User> => {
    const userIndex = mockUsers.findIndex(u => u.userId === userId)
    if (userIndex === -1) {
      return createErrorResponse<User>(404, '用户不存在')
    }
    
    const user = mockUsers[userIndex]
    
    // 检查用户名冲突
    if (userData.username && userData.username !== user.username) {
      if (mockUsers.some(u => u.username === userData.username && u.userId !== userId)) {
        return createErrorResponse<User>(409, '用户名已存在')
      }
    }
    
    // 检查邮箱冲突
    if (userData.email && userData.email !== user.email) {
      if (mockUsers.some(u => u.email === userData.email && u.userId !== userId)) {
        return createErrorResponse<User>(409, '邮箱已存在')
      }
    }
    
    const updatedUser: User = {
      ...user,
      username: userData.username || user.username,
      email: userData.email || user.email,
      role: (userData.role as User['role']) || user.role,
      status: (userData.status as User['status']) || user.status,
      updatedAt: generateTimestamp()
    }
    
    mockUsers[userIndex] = updatedUser
    return createSuccessResponse(updatedUser, '更新成功')
  },

  /**
   * Mock删除用户
   */
  deleteUser: (userId: string): ApiResponse<null> => {
    const userIndex = mockUsers.findIndex(u => u.userId === userId)
    if (userIndex === -1) {
      return createErrorResponse<null>(404, '用户不存在')
    }
    
    mockUsers.splice(userIndex, 1)
    return createSuccessResponse(null, '删除成功')
  },

  /**
   * Mock锁定用户
   */
  lockUser: (userId: string, duration: number = 3600): ApiResponse<{ userId: string; lockedUntil: string }> => {
    const userIndex = mockUsers.findIndex(u => u.userId === userId)
    if (userIndex === -1) {
      return createErrorResponse<{ userId: string; lockedUntil: string }>(404, '用户不存在')
    }
    
    const lockedUntil = new Date(Date.now() + duration * 1000).toISOString()
    
    mockUsers[userIndex] = {
      ...mockUsers[userIndex],
      status: 'LOCKED',
      updatedAt: generateTimestamp()
    }
    
    return createSuccessResponse({
      userId,
      lockedUntil
    }, '用户已锁定')
  },

  /**
   * Mock解锁用户
   */
  unlockUser: (userId: string): ApiResponse<{ userId: string; status: string }> => {
    const userIndex = mockUsers.findIndex(u => u.userId === userId)
    if (userIndex === -1) {
      return createErrorResponse<{ userId: string; status: string }>(404, '用户不存在')
    }
    
    mockUsers[userIndex] = {
      ...mockUsers[userIndex],
      status: 'ACTIVE',
      updatedAt: generateTimestamp()
    }
    
    return createSuccessResponse({
      userId,
      status: 'ACTIVE'
    }, '用户已解锁')
  },

  /**
   * Mock重置用户密码
   */
  resetUserPassword: (userId: string, newPassword: string): ApiResponse<null> => {
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return createErrorResponse<null>(404, '用户不存在')
    }
    
    return createSuccessResponse(null, '密码重置成功')
  },

  /**
   * Mock获取用户权限
   */
  getUserPermissions: (userId: string): ApiResponse<Permission[]> => {
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return createErrorResponse<Permission[]>(404, '用户不存在')
    }
    
    // 根据用户角色返回不同权限
    let userPermissions: Permission[] = []
    
    switch (user.role) {
      case 'ADMIN':
        userPermissions = [...mockPermissions]
        break
      case 'RESEARCHER':
        userPermissions = mockPermissions.filter(p => 
          ['READ_DATA', 'WRITE_DATA', 'MANAGE_TASKS'].includes(p.permissionName)
        )
        break
      case 'OPERATOR':
        userPermissions = mockPermissions.filter(p => 
          ['READ_DATA', 'MANAGE_TASKS'].includes(p.permissionName)
        )
        break
      case 'VIEWER':
        userPermissions = mockPermissions.filter(p => 
          p.permissionName === 'READ_DATA'
        )
        break
    }
    
    return createSuccessResponse(userPermissions, '获取成功')
  },

  /**
   * Mock授予用户权限
   */
  grantUserPermission: (userId: string, permissionName: string): ApiResponse<Permission> => {
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return createErrorResponse<Permission>(404, '用户不存在')
    }
    
    const permission = mockPermissions.find(p => p.permissionName === permissionName)
    if (!permission) {
      return createErrorResponse<Permission>(404, '权限不存在')
    }
    
    const newPermission: Permission = {
      ...permission,
      grantedAt: generateTimestamp()
    }
    
    return createSuccessResponse(newPermission, '权限授予成功')
  },

  /**
   * Mock撤销用户权限
   */
  revokeUserPermission: (userId: string, permissionId: string): ApiResponse<null> => {
    const user = mockUsers.find(u => u.userId === userId)
    if (!user) {
      return createErrorResponse<null>(404, '用户不存在')
    }
    
    const permission = mockPermissions.find(p => p.permissionId === permissionId)
    if (!permission) {
      return createErrorResponse<null>(404, '权限不存在')
    }
    
    return createSuccessResponse(null, '权限撤销成功')
  }
}

// ==================== 导出Mock数据 ====================

export default mockAdminApi
