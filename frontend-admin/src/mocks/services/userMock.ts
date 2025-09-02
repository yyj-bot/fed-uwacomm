/**
 * 用户API Mock数据
 * 提供与接口文档一致的Mock数据，用于开发和测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { 
  User, 
  LoginRequest,
  LoginResponse,
  ApiResponse 
} from '@/types'

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
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString()
}

/**
 * 生成JWT Token
 */
const generateJWTToken = (userId: string, expiresIn: number = 86400): string => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = btoa(JSON.stringify({
    userId,
    exp: Math.floor(Date.now() / 1000) + expiresIn,
    iat: Math.floor(Date.now() / 1000)
  }))
  const signature = btoa('mock-signature')
  return `${header}.${payload}.${signature}`
}

// ==================== Mock用户数据 ====================

/**
 * Mock用户列表
 */
export const mockUsers: User[] = [
  {
    userId: 'a1b2c3d4e5f678901234567890123456',
    username: 'testuser',
    email: 'test@example.com',
    role: 'VIEWER',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(1),
    lastLoginIp: '192.168.1.100',
    createdAt: '2024-01-01T09:00:00Z',
    updatedAt: '2024-01-01T10:00:00Z'
  },
  {
    userId: 'b2c3d4e5f6789012345678901234567a',
    username: 'researcher',
    email: 'researcher@example.com',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(0),
    lastLoginIp: '192.168.1.101',
    createdAt: '2024-01-02T09:00:00Z',
    updatedAt: '2024-01-02T14:30:00Z'
  },
  {
    userId: 'c3d4e5f67890123456789012345678ab',
    username: 'admin',
    email: 'admin@example.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    lastLoginTime: generateTimestamp(0),
    lastLoginIp: '192.168.1.102',
    createdAt: '2024-01-03T11:00:00Z',
    updatedAt: '2024-01-03T16:00:00Z'
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
 * Mock用户API服务
 */
export const mockUserApi = {
  /**
   * Mock用户注册
   */
  register: (userData: {
    username: string
    email: string
    password: string
    confirmPassword: string
  }): ApiResponse<User> => {
    // 参数验证
    if (!userData.username || userData.username.length < 3 || userData.username.length > 50) {
      return createErrorResponse<User>(400, '用户名长度必须在3-50字符之间', {
        field: 'username',
        error: '用户名长度不符合要求'
      } as any)
    }

    if (!userData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userData.email)) {
      return createErrorResponse<User>(400, '邮箱格式不正确', {
        field: 'email',
        error: '邮箱格式不正确'
      } as any)
    }

    if (!userData.password || userData.password.length < 6 || userData.password.length > 20) {
      return createErrorResponse<User>(400, '密码长度必须在6-20字符之间', {
        field: 'password',
        error: '密码长度不符合要求'
      } as any)
    }

    if (userData.password !== userData.confirmPassword) {
      return createErrorResponse<User>(400, '两次输入的密码不一致', {
        field: 'confirmPassword',
        error: '密码不匹配'
      } as any)
    }

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
      role: 'VIEWER',
      status: 'ACTIVE',
      lastLoginTime: undefined,
      lastLoginIp: undefined,
      createdAt: generateTimestamp(),
      updatedAt: generateTimestamp()
    }

    mockUsers.push(newUser)
    return createSuccessResponse(newUser, '注册成功')
  },

  /**
   * Mock用户登录
   */
  login: (loginData: LoginRequest): ApiResponse<LoginResponse> => {
    // 参数验证
    if (!loginData.loginIdentifier || loginData.loginIdentifier.trim().length === 0) {
      return createErrorResponse<LoginResponse>(400, '登录标识符不能为空')
    }

    if (!loginData.password || loginData.password.length === 0) {
      return createErrorResponse<LoginResponse>(400, '密码不能为空')
    }

    // 查找用户（支持用户名或邮箱登录）
    const user = mockUsers.find(u => 
      u.username === loginData.loginIdentifier || 
      u.email === loginData.loginIdentifier
    )

    if (!user) {
      return createErrorResponse<LoginResponse>(401, '账号或密码错误', {
        loginAttempts: 1,
        lockedUntil: null
      } as any)
    }

    // 检查用户状态
    if (user.status === 'LOCKED') {
      return createErrorResponse<LoginResponse>(423, '账号已锁定', {
        loginAttempts: 5,
        lockedUntil: new Date(Date.now() + 3600000).toISOString()
      } as any)
    }

    if (user.status === 'INACTIVE') {
      return createErrorResponse<LoginResponse>(403, '账号已禁用')
    }

    // 模拟密码验证（在真实环境中应该验证加密后的密码）
    if (loginData.password !== 'password123') {
      return createErrorResponse<LoginResponse>(401, '账号或密码错误', {
        loginAttempts: 1,
        lockedUntil: null
      } as any)
    }

    // 更新用户登录信息
    const updatedUser = {
      ...user,
      lastLoginTime: generateTimestamp(),
      lastLoginIp: '192.168.1.100',
      updatedAt: generateTimestamp()
    }

    const userIndex = mockUsers.findIndex(u => u.userId === user.userId)
    if (userIndex !== -1) {
      mockUsers[userIndex] = updatedUser
    }

    const token = generateJWTToken(user.userId, 86400)
    const refreshToken = generateJWTToken(user.userId, 604800) // 7天

    return createSuccessResponse({
      token,
      refreshToken,
      expiresIn: 86400,
      user: updatedUser
    }, '登录成功')
  },

  /**
   * Mock刷新Token
   */
  refreshToken: (): ApiResponse<{ token: string; refreshToken: string; expiresIn: number }> => {
    // 模拟从当前用户生成新token
    const currentUser = mockUsers[0] // 假设当前用户是第一个
    const token = generateJWTToken(currentUser.userId, 86400)
    const refreshToken = generateJWTToken(currentUser.userId, 604800)

    return createSuccessResponse({
      token,
      refreshToken,
      expiresIn: 86400
    }, 'Token刷新成功')
  },

  /**
   * Mock用户登出
   */
  logout: (): ApiResponse<null> => {
    return createSuccessResponse(null, '登出成功')
  },

  /**
   * Mock获取用户信息
   */
  getProfile: (): ApiResponse<User> => {
    // 模拟返回当前用户信息
    const currentUser = mockUsers[0] // 假设当前用户是第一个
    return createSuccessResponse(currentUser, '获取成功')
  },

  /**
   * Mock更新用户信息
   */
  updateProfile: (userData: { username?: string; email?: string }): ApiResponse<User> => {
    const currentUser = mockUsers[0] // 假设当前用户是第一个

    // 参数验证
    if (userData.username !== undefined) {
      if (!userData.username || userData.username.length < 3 || userData.username.length > 50) {
        return createErrorResponse<User>(400, '用户名长度必须在3-50字符之间')
      }

      // 检查用户名是否已被其他用户使用
      if (mockUsers.some(u => u.username === userData.username && u.userId !== currentUser.userId)) {
        return createErrorResponse<User>(409, '用户名已存在')
      }
    }

    if (userData.email !== undefined) {
      if (!userData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userData.email)) {
        return createErrorResponse<User>(400, '邮箱格式不正确')
      }

      // 检查邮箱是否已被其他用户使用
      if (mockUsers.some(u => u.email === userData.email && u.userId !== currentUser.userId)) {
        return createErrorResponse<User>(409, '邮箱已存在')
      }
    }

    const updatedUser: User = {
      ...currentUser,
      username: userData.username || currentUser.username,
      email: userData.email || currentUser.email,
      updatedAt: generateTimestamp()
    }

    const userIndex = mockUsers.findIndex(u => u.userId === currentUser.userId)
    if (userIndex !== -1) {
      mockUsers[userIndex] = updatedUser
    }

    return createSuccessResponse(updatedUser, '更新成功')
  },

  /**
   * Mock修改密码
   */
  changePassword: (passwordData: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }): ApiResponse<null> => {
    // 参数验证
    if (!passwordData.oldPassword || passwordData.oldPassword.length === 0) {
      return createErrorResponse<null>(400, '旧密码不能为空')
    }

    if (!passwordData.newPassword || passwordData.newPassword.length < 6 || passwordData.newPassword.length > 20) {
      return createErrorResponse<null>(400, '新密码长度必须在6-20字符之间')
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      return createErrorResponse<null>(400, '两次输入的新密码不一致')
    }

    if (passwordData.oldPassword === passwordData.newPassword) {
      return createErrorResponse<null>(400, '新密码不能与旧密码相同')
    }

    // 验证旧密码（在真实环境中应该验证加密后的密码）
    if (passwordData.oldPassword !== 'password123') {
      return createErrorResponse<null>(401, '旧密码错误')
    }

    return createSuccessResponse(null, '密码修改成功')
  }
}

// ==================== 导出Mock数据 ====================

export default mockUserApi
