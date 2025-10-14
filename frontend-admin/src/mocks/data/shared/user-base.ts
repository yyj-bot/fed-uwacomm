/**
 * 用户基础数据 - 单一数据源
 * 
 * 本文件作为所有用户相关mock数据的单一可信数据源
 * 其他文件（userMockData, adminVmMockData等）应该从这里导入用户数据
 * 
 * @file shared/user-base.ts
 * @description 用户基础数据定义
 */

import { generateTimestamp } from '../../utils'

/**
 * 用户角色类型
 */
export type UserRole = 'ADMIN' | 'RESEARCHER' | 'OPERATOR' | 'VIEWER'

/**
 * 用户状态类型
 */
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED' | 'DELETED'

/**
 * 基础用户接口
 */
export interface BaseUser {
  userId: string
  username: string
  email: string
  password: string // 仅用于mock验证，实际API不返回
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
  lastLoginTime: string
  lastLoginIp: string
}

/**
 * 用户基础数据列表
 * 
 * 这是所有用户数据的单一数据源
 * 任何需要用户数据的mock文件都应该从这里导入
 */
export const baseUserList: BaseUser[] = [
  // ==================== 管理员用户 ====================
  {
    userId: 'a1b2c3d4e5f678901234567890123456',
    username: 'admin',
    email: 'admin@feduwacomm.com',
    password: 'ab123456', // 仅用于mock验证
    role: 'ADMIN',
    status: 'ACTIVE',
    createdAt: '2025-01-01T00:00:00.000Z',
    updatedAt: generateTimestamp(),
    lastLoginTime: generateTimestamp(),
    lastLoginIp: '192.168.1.100'
  },

  // ==================== 研究员用户 ====================
  {
    userId: 'researcher-001',
    username: 'researcher01',
    email: 'researcher01@example.com',
    password: 'password123',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2025-01-02T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -5),
    lastLoginTime: generateTimestamp(0, -1),
    lastLoginIp: '192.168.1.101'
  },
  {
    userId: 'researcher-002',
    username: 'researcher02',
    email: 'researcher02@example.com',
    password: 'password123',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2025-01-02T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -4),
    lastLoginTime: generateTimestamp(0, -2),
    lastLoginIp: '192.168.1.102'
  },
  {
    userId: 'researcher-003',
    username: 'researcher03',
    email: 'researcher03@example.com',
    password: 'password123',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2025-01-03T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -3),
    lastLoginTime: generateTimestamp(0, -3),
    lastLoginIp: '192.168.1.103'
  },
  {
    userId: 'researcher-004',
    username: 'researcher04',
    email: 'researcher04@example.com',
    password: 'password123',
    role: 'RESEARCHER',
    status: 'ACTIVE',
    createdAt: '2025-01-03T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -6),
    lastLoginTime: generateTimestamp(0, -5),
    lastLoginIp: '192.168.1.104'
  },
  {
    userId: 'researcher-005',
    username: 'researcher05',
    email: 'researcher05@example.com',
    password: 'password123',
    role: 'RESEARCHER',
    status: 'INACTIVE',
    createdAt: '2025-01-04T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -10),
    lastLoginTime: generateTimestamp(-5, 0), // 5天前
    lastLoginIp: '192.168.1.105'
  },

  // ==================== 操作员用户 ====================
  {
    userId: 'operator-001',
    username: 'operator01',
    email: 'operator01@example.com',
    password: 'password123',
    role: 'OPERATOR',
    status: 'ACTIVE',
    createdAt: '2025-01-04T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -2),
    lastLoginTime: generateTimestamp(),
    lastLoginIp: '192.168.1.106'
  },
  {
    userId: 'operator-002',
    username: 'operator02',
    email: 'operator02@example.com',
    password: 'password123',
    role: 'OPERATOR',
    status: 'ACTIVE',
    createdAt: '2025-01-05T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -1),
    lastLoginTime: generateTimestamp(0, -1),
    lastLoginIp: '192.168.1.107'
  },
  {
    userId: 'operator-003',
    username: 'operator03',
    email: 'operator03@example.com',
    password: 'password123',
    role: 'OPERATOR',
    status: 'ACTIVE',
    createdAt: '2025-01-05T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -3),
    lastLoginTime: generateTimestamp(0, -4),
    lastLoginIp: '192.168.1.108'
  },

  // ==================== 查看者用户 ====================
  {
    userId: 'viewer-001',
    username: 'viewer01',
    email: 'viewer01@example.com',
    password: 'password123',
    role: 'VIEWER',
    status: 'ACTIVE',
    createdAt: '2025-01-06T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -2),
    lastLoginTime: generateTimestamp(),
    lastLoginIp: '192.168.1.109'
  },
  {
    userId: 'viewer-002',
    username: 'viewer02',
    email: 'viewer02@example.com',
    password: 'password123',
    role: 'VIEWER',
    status: 'ACTIVE',
    createdAt: '2025-01-06T00:00:00.000Z',
    updatedAt: generateTimestamp(0, -1),
    lastLoginTime: generateTimestamp(0, -1),
    lastLoginIp: '192.168.1.110'
  },
  {
    userId: 'viewer-003',
    username: 'viewer03',
    email: 'viewer03@example.com',
    password: 'password123',
    role: 'VIEWER',
    status: 'LOCKED',
    createdAt: '2025-01-07T00:00:00.000Z',
    updatedAt: generateTimestamp(),
    lastLoginTime: generateTimestamp(-1, 0), // 1天前
    lastLoginIp: '192.168.1.111'
  }
]

/**
 * 工具函数：根据用户ID查找用户
 */
export const getUserById = (userId: string): BaseUser | undefined => {
  return baseUserList.find(user => user.userId === userId)
}

/**
 * 工具函数：根据用户名查找用户
 */
export const getUserByUsername = (username: string): BaseUser | undefined => {
  return baseUserList.find(user => user.username === username)
}

/**
 * 工具函数：根据邮箱查找用户
 */
export const getUserByEmail = (email: string): BaseUser | undefined => {
  return baseUserList.find(user => user.email === email)
}

/**
 * 工具函数：根据角色筛选用户
 */
export const getUsersByRole = (role: UserRole): BaseUser[] => {
  return baseUserList.filter(user => user.role === role)
}

/**
 * 工具函数：根据状态筛选用户
 */
export const getUsersByStatus = (status: UserStatus): BaseUser[] => {
  return baseUserList.filter(user => user.status === status)
}

/**
 * 工具函数：获取活跃用户
 */
export const getActiveUsers = (): BaseUser[] => {
  return getUsersByStatus('ACTIVE')
}

/**
 * 工具函数：获取管理员用户
 */
export const getAdminUsers = (): BaseUser[] => {
  return getUsersByRole('ADMIN')
}

/**
 * 工具函数：获取研究员用户
 */
export const getResearcherUsers = (): BaseUser[] => {
  return getUsersByRole('RESEARCHER')
}

/**
 * 工具函数：验证用户登录
 */
export const validateUserLogin = (
  loginIdentifier: string,
  password: string
): BaseUser | null => {
  const user = baseUserList.find(
    u =>
      (u.username === loginIdentifier || u.email === loginIdentifier) &&
      u.password === password
  )
  return user || null
}





