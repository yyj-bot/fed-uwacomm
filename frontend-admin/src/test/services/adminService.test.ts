/**
 * 管理员服务单元测试
 * 使用Vitest测试所有管理员服务接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { AdminUserService } from '@/services/admin/adminService'
import { admin } from '@/api/admin'
import { mockUsers, mockPermissions, mockAdminApi } from '../../mocks/services/adminMock'
import type { 
  CreateUserRequest, 
  UpdateUserRequest, 
  LockUserRequest, 
  ResetPasswordRequest, 
  GrantPermissionRequest,
  UserListParams 
} from '@/services/admin/type'

// Mock admin API
vi.mock('@/api/admin', () => ({
  admin: {
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
  }
}))

describe('AdminUserService', () => {
  let adminUserService: AdminUserService
  
  beforeEach(() => {
    adminUserService = new AdminUserService()
    vi.clearAllMocks()
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getUserList', () => {
    it('应该成功获取用户列表', async () => {
      // 准备测试数据
      const params: UserListParams = { page: 1, size: 10 }
      const mockResponse = mockAdminApi.getUserList(params).data
      
      // Mock API调用
      vi.mocked(admin.getUserList).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await adminUserService.getUserList(params)
      
      // 验证结果
      expect(admin.getUserList).toHaveBeenCalledWith(params)
      expect(result).toEqual(mockResponse)
      expect(result.records).toBeDefined()
      expect(Array.isArray(result.records)).toBe(true)
      expect(result.total).toBeGreaterThanOrEqual(0)
      expect(result.page).toBe(1)
      expect(result.size).toBe(10)
    })

    it('应该支持用户名过滤', async () => {
      const params: UserListParams = { username: 'admin' }
      const mockResponse = mockAdminApi.getUserList(params).data
      
      vi.mocked(admin.getUserList).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.getUserList(params)
      
      expect(admin.getUserList).toHaveBeenCalledWith(params)
      expect(result.records?.every(user => 
        user.username.toLowerCase().includes('admin')
      )).toBe(true)
    })

    it('应该支持角色过滤', async () => {
      const params: UserListParams = { role: 'ADMIN' }
      const mockResponse = mockAdminApi.getUserList(params).data
      
      vi.mocked(admin.getUserList).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.getUserList(params)
      
      expect(admin.getUserList).toHaveBeenCalledWith(params)
      expect(result.records?.every(user => user.role === 'ADMIN')).toBe(true)
    })

    it('应该处理API错误', async () => {
      const error = new Error('网络错误')
      vi.mocked(admin.getUserList).mockRejectedValue(error)
      
      await expect(adminUserService.getUserList()).rejects.toThrow('获取用户列表失败')
    })
  })

  describe('getUserDetail', () => {
    it('应该成功获取用户详情', async () => {
      const userId = mockUsers[0].userId
      const mockUser = mockUsers[0]
      
      vi.mocked(admin.getUserDetail).mockResolvedValue(mockUser)
      
      const result = await adminUserService.getUserDetail(userId)
      
      expect(admin.getUserDetail).toHaveBeenCalledWith(userId)
      expect(result).toEqual(mockUser)
      expect(result.userId).toBe(userId)
    })

    it('应该验证用户ID不能为空', async () => {
      await expect(adminUserService.getUserDetail('')).rejects.toThrow('用户ID不能为空')
      await expect(adminUserService.getUserDetail('   ')).rejects.toThrow('用户ID不能为空')
    })

    it('应该处理用户不存在的情况', async () => {
      const error = new Error('用户不存在')
      vi.mocked(admin.getUserDetail).mockRejectedValue(error)
      
      await expect(adminUserService.getUserDetail('nonexistent')).rejects.toThrow('获取用户详情失败')
    })
  })

  describe('createUser', () => {
    it('应该成功创建用户', async () => {
      const userData: CreateUserRequest = {
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        role: 'RESEARCHER'
      }
      
      const mockResponse = mockAdminApi.createUser(userData).data
      vi.mocked(admin.createUser).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.createUser(userData)
      
      expect(admin.createUser).toHaveBeenCalledWith({
        username: userData.username,
        email: userData.email,
        password: userData.password,
        role: userData.role,
        status: 'ACTIVE'
      })
      expect(result.username).toBe(userData.username)
      expect(result.email).toBe(userData.email)
      expect(result.role).toBe(userData.role)
    })

    it('应该验证必填字段', async () => {
      const invalidData = {
        username: '',
        email: 'test@example.com',
        password: 'password123',
        role: 'RESEARCHER' as const
      }
      
      await expect(adminUserService.createUser(invalidData)).rejects.toThrow('用户名不能为空')
    })

    it('应该验证邮箱不能为空', async () => {
      const invalidData = {
        username: 'testuser',
        email: '',
        password: 'password123',
        role: 'RESEARCHER' as const
      }
      
      await expect(adminUserService.createUser(invalidData)).rejects.toThrow('邮箱不能为空')
    })

    it('应该验证密码长度', async () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        password: '123',
        role: 'RESEARCHER' as const
      }
      
      await expect(adminUserService.createUser(invalidData)).rejects.toThrow('密码长度至少6位')
    })

    it('应该验证角色类型', async () => {
      const invalidData = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
        role: 'INVALID_ROLE' as any
      }
      
      await expect(adminUserService.createUser(invalidData)).rejects.toThrow('角色类型无效')
    })
  })

  describe('updateUser', () => {
    it('应该成功更新用户', async () => {
      const userId = mockUsers[0].userId
      const updateData: UpdateUserRequest = {
        username: 'updateduser',
        email: 'updated@example.com'
      }
      
      const mockResponse = { ...mockUsers[0], ...updateData }
      vi.mocked(admin.updateUser).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.updateUser(userId, updateData)
      
      expect(admin.updateUser).toHaveBeenCalledWith(userId, updateData)
      expect(result.username).toBe(updateData.username)
      expect(result.email).toBe(updateData.email)
    })

    it('应该验证更新数据', async () => {
      const userId = mockUsers[0].userId
      const invalidData: UpdateUserRequest = {
        username: '',
        email: 'test@example.com'
      }
      
      await expect(adminUserService.updateUser(userId, invalidData)).rejects.toThrow('用户名不能为空')
    })

    it('应该验证用户ID', async () => {
      const updateData: UpdateUserRequest = { username: 'test' }
      
      await expect(adminUserService.updateUser('', updateData)).rejects.toThrow('用户ID不能为空')
    })
  })

  describe('deleteUser', () => {
    it('应该成功删除用户', async () => {
      const userId = mockUsers[0].userId
      
      vi.mocked(admin.deleteUser).mockResolvedValue()
      
      await expect(adminUserService.deleteUser(userId)).resolves.not.toThrow()
      expect(admin.deleteUser).toHaveBeenCalledWith(userId)
    })

    it('应该验证用户ID', async () => {
      await expect(adminUserService.deleteUser('')).rejects.toThrow('用户ID不能为空')
    })
  })

  describe('lockUser', () => {
    it('应该成功锁定用户', async () => {
      const userId = mockUsers[0].userId
      const lockRequest: LockUserRequest = { duration: 3600 }
      
      const mockResponse = {
        userId,
        lockedUntil: new Date(Date.now() + 3600000).toISOString()
      }
      vi.mocked(admin.lockUser).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.lockUser(userId, lockRequest)
      
      expect(admin.lockUser).toHaveBeenCalledWith(userId, lockRequest.duration)
      expect(result.userId).toBe(userId)
      expect(result.lockedUntil).toBeDefined()
      expect(result.duration).toBe(lockRequest.duration)
    })

    it('应该使用默认锁定时长', async () => {
      const userId = mockUsers[0].userId
      
      const mockResponse = {
        userId,
        lockedUntil: new Date(Date.now() + 3600000).toISOString()
      }
      vi.mocked(admin.lockUser).mockResolvedValue(mockResponse)
      
      await adminUserService.lockUser(userId)
      
      expect(admin.lockUser).toHaveBeenCalledWith(userId, undefined)
    })
  })

  describe('unlockUser', () => {
    it('应该成功解锁用户', async () => {
      const userId = mockUsers[0].userId
      
      const mockResponse = {
        userId,
        status: 'ACTIVE'
      }
      vi.mocked(admin.unlockUser).mockResolvedValue(mockResponse)
      
      const result = await adminUserService.unlockUser(userId)
      
      expect(admin.unlockUser).toHaveBeenCalledWith(userId)
      expect(result.userId).toBe(userId)
      expect(result.status).toBe('ACTIVE')
    })
  })

  describe('resetUserPassword', () => {
    it('应该成功重置用户密码', async () => {
      const userId = mockUsers[0].userId
      const resetRequest: ResetPasswordRequest = {
        newPassword: 'newpassword123'
      }
      
      vi.mocked(admin.resetUserPassword).mockResolvedValue()
      
      await expect(adminUserService.resetUserPassword(userId, resetRequest)).resolves.not.toThrow()
      expect(admin.resetUserPassword).toHaveBeenCalledWith(userId, resetRequest.newPassword)
    })

    it('应该验证新密码长度', async () => {
      const userId = mockUsers[0].userId
      const invalidRequest: ResetPasswordRequest = {
        newPassword: '123'
      }
      
      await expect(adminUserService.resetUserPassword(userId, invalidRequest))
        .rejects.toThrow('新密码长度至少6位')
    })
  })

  describe('getUserPermissions', () => {
    it('应该成功获取用户权限', async () => {
      const userId = mockUsers[0].userId
      const mockUserPermissions = [mockPermissions[0], mockPermissions[1]]
      
      vi.mocked(admin.getUserPermissions).mockResolvedValue(mockUserPermissions)
      
      const result = await adminUserService.getUserPermissions(userId)
      
      expect(admin.getUserPermissions).toHaveBeenCalledWith(userId)
      expect(result).toEqual(mockUserPermissions)
      expect(Array.isArray(result)).toBe(true)
    })
  })

  describe('grantUserPermission', () => {
    it('应该成功授予用户权限', async () => {
      const userId = mockUsers[0].userId
      const grantRequest: GrantPermissionRequest = {
        permissionName: 'READ_DATA'
      }
      
      const mockPermission = mockPermissions[0]
      vi.mocked(admin.grantUserPermission).mockResolvedValue(mockPermission)
      
      const result = await adminUserService.grantUserPermission(userId, grantRequest)
      
      expect(admin.grantUserPermission).toHaveBeenCalledWith(userId, grantRequest.permissionName)
      expect(result).toEqual(mockPermission)
      expect(result.permissionName).toBe(grantRequest.permissionName)
    })

    it('应该验证权限名称', async () => {
      const userId = mockUsers[0].userId
      const invalidRequest: GrantPermissionRequest = {
        permissionName: ''
      }
      
      await expect(adminUserService.grantUserPermission(userId, invalidRequest))
        .rejects.toThrow('权限名称不能为空')
    })
  })

  describe('revokeUserPermission', () => {
    it('应该成功撤销用户权限', async () => {
      const userId = mockUsers[0].userId
      const permissionId = mockPermissions[0].permissionId
      
      vi.mocked(admin.revokeUserPermission).mockResolvedValue()
      
      await expect(adminUserService.revokeUserPermission(userId, permissionId)).resolves.not.toThrow()
      expect(admin.revokeUserPermission).toHaveBeenCalledWith(userId, permissionId)
    })

    it('应该验证权限ID', async () => {
      const userId = mockUsers[0].userId
      
      await expect(adminUserService.revokeUserPermission(userId, ''))
        .rejects.toThrow('权限ID不能为空')
    })
  })

  describe('错误处理', () => {
    it('应该正确处理API响应错误', async () => {
      const error = {
        response: {
          data: {
            message: 'API错误信息'
          }
        }
      }
      
      vi.mocked(admin.getUserList).mockRejectedValue(error)
      
      await expect(adminUserService.getUserList()).rejects.toThrow('获取用户列表失败: API错误信息')
    })

    it('应该正确处理通用错误', async () => {
      const error = new Error('网络连接失败')
      
      vi.mocked(admin.getUserList).mockRejectedValue(error)
      
      await expect(adminUserService.getUserList()).rejects.toThrow('获取用户列表失败: 网络连接失败')
    })

    it('应该处理未知错误', async () => {
      vi.mocked(admin.getUserList).mockRejectedValue('未知错误')
      
      await expect(adminUserService.getUserList()).rejects.toThrow('获取用户列表失败')
    })
  })

  describe('数据验证', () => {
    it('应该验证分页响应数据格式', async () => {
      const invalidResponse = {
        total: -1,
        page: 0,
        size: 0,
        records: []
      }
      
      vi.mocked(admin.getUserList).mockResolvedValue(invalidResponse as any)
      
      await expect(adminUserService.getUserList()).rejects.toThrow('分页数据格式错误')
    })
  })

  describe('数据转换', () => {
    it('应该正确转换用户数据', async () => {
      const mockUser = mockUsers[0]
      vi.mocked(admin.getUserDetail).mockResolvedValue(mockUser)
      
      const result = await adminUserService.getUserDetail(mockUser.userId)
      
      expect(result).toEqual(mockUser)
      expect(result.userId).toBe(mockUser.userId)
      expect(result.username).toBe(mockUser.username)
      expect(result.email).toBe(mockUser.email)
      expect(result.role).toBe(mockUser.role)
      expect(result.status).toBe(mockUser.status)
    })

    it('应该正确转换权限数据', async () => {
      const userId = mockUsers[0].userId
      const mockPermissionList = [mockPermissions[0]]
      
      vi.mocked(admin.getUserPermissions).mockResolvedValue(mockPermissionList)
      
      const result = await adminUserService.getUserPermissions(userId)
      
      expect(result).toEqual(mockPermissionList)
      expect(result[0].permissionId).toBe(mockPermissionList[0].permissionId)
      expect(result[0].permissionName).toBe(mockPermissionList[0].permissionName)
      expect(result[0].description).toBe(mockPermissionList[0].description)
      expect(result[0].grantedAt).toBe(mockPermissionList[0].grantedAt)
    })
  })
})
