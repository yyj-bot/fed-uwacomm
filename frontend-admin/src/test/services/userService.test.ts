/**
 * 用户服务单元测试
 * 使用Vitest测试所有用户服务接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { UserService } from '@/services/user/userService'
import { userApi } from '@/api/user'
import { mockUsers, mockUserApi } from '../../mocks/services/userMock'
import type { 
  RegisterUserRequest,
  UpdateProfileRequest,
  ChangePasswordRequest
} from '@/services/user/type'
import type { LoginRequest } from '@/types'

// Mock userApi
vi.mock('@/api/user', () => ({
  userApi: {
    register: vi.fn(),
    login: vi.fn(),
    refreshToken: vi.fn(),
    logout: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn()
  }
}))

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

describe('UserService', () => {
  let userService: UserService
  
  beforeEach(() => {
    userService = new UserService()
    vi.clearAllMocks()
    localStorageMock.getItem.mockClear()
    localStorageMock.setItem.mockClear()
    localStorageMock.removeItem.mockClear()
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('register', () => {
    it('应该成功注册用户', async () => {
      // 准备测试数据
      const registerData: RegisterUserRequest = {
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'password123',
        confirmPassword: 'password123'
      }
      
      const mockResponse = mockUserApi.register(registerData).data
      
      // Mock API调用
      vi.mocked(userApi.register).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await userService.register(registerData)
      
      // 验证结果
      expect(userApi.register).toHaveBeenCalledWith({
        username: registerData.username,
        email: registerData.email,
        password: registerData.password,
        confirmPassword: registerData.confirmPassword
      })
      expect(result).toEqual(mockResponse)
      expect(result.username).toBe(registerData.username)
      expect(result.email).toBe(registerData.email)
      expect(result.role).toBe('VIEWER')
      expect(result.status).toBe('ACTIVE')
    })

    it('应该验证用户名长度', async () => {
      const invalidData: RegisterUserRequest = {
        username: 'ab', // 太短
        email: 'test@example.com',
        password: 'password123',
        confirmPassword: 'password123'
      }
      
      await expect(userService.register(invalidData)).rejects.toThrow('用户名长度必须在3-50字符之间')
    })

    it('应该验证邮箱格式', async () => {
      const invalidData: RegisterUserRequest = {
        username: 'testuser',
        email: 'invalid-email', // 无效邮箱
        password: 'password123',
        confirmPassword: 'password123'
      }
      
      await expect(userService.register(invalidData)).rejects.toThrow('邮箱格式不正确')
    })

    it('应该验证密码长度', async () => {
      const invalidData: RegisterUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: '123', // 太短
        confirmPassword: '123'
      }
      
      await expect(userService.register(invalidData)).rejects.toThrow('密码长度必须在6-20字符之间')
    })

    it('应该验证密码一致性', async () => {
      const invalidData: RegisterUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
        confirmPassword: 'differentpassword' // 不一致
      }
      
      await expect(userService.register(invalidData)).rejects.toThrow('两次输入的密码不一致')
    })

    it('应该处理API错误', async () => {
      const registerData: RegisterUserRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'password123',
        confirmPassword: 'password123'
      }
      
      const error = new Error('网络错误')
      vi.mocked(userApi.register).mockRejectedValue(error)
      
      await expect(userService.register(registerData)).rejects.toThrow('用户注册失败')
    })
  })

  describe('login', () => {
    it('应该成功登录用户', async () => {
      // 准备测试数据
      const loginData: LoginRequest = {
        loginIdentifier: 'testuser',
        password: 'password123'
      }
      
      const mockResponse = mockUserApi.login(loginData).data
      
      // Mock API调用
      vi.mocked(userApi.login).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await userService.login(loginData)
      
      // 验证结果
      expect(userApi.login).toHaveBeenCalledWith(loginData)
      expect(result).toEqual(mockResponse)
      expect(result.token).toBeDefined()
      expect(result.refreshToken).toBeDefined()
      expect(result.user).toBeDefined()
      
      // 验证令牌保存
      expect(localStorageMock.setItem).toHaveBeenCalledWith('access_token', result.token)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', result.refreshToken)
    })

    it('应该验证登录标识符', async () => {
      const invalidData: LoginRequest = {
        loginIdentifier: '', // 空值
        password: 'password123'
      }
      
      await expect(userService.login(invalidData)).rejects.toThrow('登录标识符不能为空')
    })

    it('应该验证密码', async () => {
      const invalidData: LoginRequest = {
        loginIdentifier: 'testuser',
        password: '' // 空值
      }
      
      await expect(userService.login(invalidData)).rejects.toThrow('密码不能为空')
    })

    it('应该处理登录失败', async () => {
      const loginData: LoginRequest = {
        loginIdentifier: 'testuser',
        password: 'wrongpassword'
      }
      
      const error = { response: { data: { message: '账号或密码错误' } } }
      vi.mocked(userApi.login).mockRejectedValue(error)
      
      await expect(userService.login(loginData)).rejects.toThrow('用户登录失败: 账号或密码错误')
    })
  })

  describe('refreshToken', () => {
    it('应该成功刷新令牌', async () => {
      const mockResponse = mockUserApi.refreshToken().data
      
      // Mock API调用
      vi.mocked(userApi.refreshToken).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await userService.refreshToken()
      
      // 验证结果
      expect(userApi.refreshToken).toHaveBeenCalled()
      expect(result).toEqual(mockResponse)
      expect(result.token).toBeDefined()
      expect(result.refreshToken).toBeDefined()
      
      // 验证令牌更新
      expect(localStorageMock.setItem).toHaveBeenCalledWith('access_token', result.token)
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refresh_token', result.refreshToken)
    })

    it('应该在刷新失败时清除令牌', async () => {
      const error = new Error('刷新失败')
      vi.mocked(userApi.refreshToken).mockRejectedValue(error)
      
      await expect(userService.refreshToken()).rejects.toThrow('刷新令牌失败')
      
      // 验证令牌清除
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('access_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refresh_token')
    })
  })

  describe('logout', () => {
    it('应该成功登出用户', async () => {
      // Mock API调用
      vi.mocked(userApi.logout).mockResolvedValue()
      
      // 执行测试
      await expect(userService.logout()).resolves.not.toThrow()
      
      // 验证API调用
      expect(userApi.logout).toHaveBeenCalled()
      
      // 验证令牌清除
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('access_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refresh_token')
    })

    it('应该在API失败时仍然清除本地令牌', async () => {
      const error = new Error('网络错误')
      vi.mocked(userApi.logout).mockRejectedValue(error)
      
      // 执行测试
      await expect(userService.logout()).resolves.not.toThrow()
      
      // 验证令牌仍然被清除
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('access_token')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refresh_token')
    })
  })

  describe('getProfile', () => {
    it('应该成功获取用户信息', async () => {
      const mockUser = mockUsers[0]
      
      // Mock API调用
      vi.mocked(userApi.getProfile).mockResolvedValue(mockUser)
      
      // 执行测试
      const result = await userService.getProfile()
      
      // 验证结果
      expect(userApi.getProfile).toHaveBeenCalled()
      expect(result).toEqual(mockUser)
      expect(result.userId).toBe(mockUser.userId)
      expect(result.username).toBe(mockUser.username)
      expect(result.email).toBe(mockUser.email)
    })

    it('应该处理API错误', async () => {
      const error = new Error('未授权')
      vi.mocked(userApi.getProfile).mockRejectedValue(error)
      
      await expect(userService.getProfile()).rejects.toThrow('获取用户信息失败')
    })
  })

  describe('updateProfile', () => {
    it('应该成功更新用户信息', async () => {
      const updateData: UpdateProfileRequest = {
        username: 'updateduser',
        email: 'updated@example.com'
      }
      
      const mockResponse = mockUserApi.updateProfile(updateData).data
      
      // Mock API调用
      vi.mocked(userApi.updateProfile).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await userService.updateProfile(updateData)
      
      // 验证结果
      expect(userApi.updateProfile).toHaveBeenCalledWith(updateData)
      expect(result).toEqual(mockResponse)
      expect(result.username).toBe(updateData.username)
      expect(result.email).toBe(updateData.email)
    })

    it('应该验证用户名长度', async () => {
      const invalidData: UpdateProfileRequest = {
        username: 'ab' // 太短
      }
      
      await expect(userService.updateProfile(invalidData)).rejects.toThrow('用户名长度必须在3-50字符之间')
    })

    it('应该验证邮箱格式', async () => {
      const invalidData: UpdateProfileRequest = {
        email: 'invalid-email' // 无效邮箱
      }
      
      await expect(userService.updateProfile(invalidData)).rejects.toThrow('邮箱格式不正确')
    })

    it('应该处理空更新请求', async () => {
      const emptyData: UpdateProfileRequest = {}
      
      const mockResponse = mockUsers[0]
      vi.mocked(userApi.updateProfile).mockResolvedValue(mockResponse)
      
      const result = await userService.updateProfile(emptyData)
      
      expect(userApi.updateProfile).toHaveBeenCalledWith(emptyData)
      expect(result).toEqual(mockResponse)
    })
  })

  describe('changePassword', () => {
    it('应该成功修改密码', async () => {
      const passwordData: ChangePasswordRequest = {
        oldPassword: 'password123',
        newPassword: 'newpassword123',
        confirmPassword: 'newpassword123'
      }
      
      // Mock API调用
      vi.mocked(userApi.changePassword).mockResolvedValue()
      
      // 执行测试
      await expect(userService.changePassword(passwordData)).resolves.not.toThrow()
      
      // 验证API调用
      expect(userApi.changePassword).toHaveBeenCalledWith({
        oldPassword: passwordData.oldPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      })
    })

    it('应该验证旧密码', async () => {
      const invalidData: ChangePasswordRequest = {
        oldPassword: '', // 空值
        newPassword: 'newpassword123',
        confirmPassword: 'newpassword123'
      }
      
      await expect(userService.changePassword(invalidData)).rejects.toThrow('旧密码不能为空')
    })

    it('应该验证新密码长度', async () => {
      const invalidData: ChangePasswordRequest = {
        oldPassword: 'password123',
        newPassword: '123', // 太短
        confirmPassword: '123'
      }
      
      await expect(userService.changePassword(invalidData)).rejects.toThrow('新密码长度必须在6-20字符之间')
    })

    it('应该验证新密码一致性', async () => {
      const invalidData: ChangePasswordRequest = {
        oldPassword: 'password123',
        newPassword: 'newpassword123',
        confirmPassword: 'differentpassword' // 不一致
      }
      
      await expect(userService.changePassword(invalidData)).rejects.toThrow('两次输入的新密码不一致')
    })

    it('应该验证新旧密码不能相同', async () => {
      const invalidData: ChangePasswordRequest = {
        oldPassword: 'password123',
        newPassword: 'password123', // 与旧密码相同
        confirmPassword: 'password123'
      }
      
      await expect(userService.changePassword(invalidData)).rejects.toThrow('新密码不能与旧密码相同')
    })
  })

  describe('认证状态管理', () => {
    describe('isAuthenticated', () => {
      it('应该在有有效令牌时返回true', () => {
        // Mock有效令牌
        const validToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjMiLCJleHAiOjk5OTk5OTk5OTksImlhdCI6MTYwMDAwMDAwMH0.mock-signature'
        localStorageMock.getItem.mockReturnValue(validToken)
        
        const result = userService.isAuthenticated()
        
        expect(result).toBe(true)
        expect(localStorageMock.getItem).toHaveBeenCalledWith('access_token')
      })

      it('应该在没有令牌时返回false', () => {
        localStorageMock.getItem.mockReturnValue(null)
        
        const result = userService.isAuthenticated()
        
        expect(result).toBe(false)
      })

      it('应该在令牌过期时返回false', () => {
        // Mock过期令牌
        const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjMiLCJleHAiOjE2MDAwMDAwMDAsImlhdCI6MTYwMDAwMDAwMH0.mock-signature'
        localStorageMock.getItem.mockReturnValue(expiredToken)
        
        const result = userService.isAuthenticated()
        
        expect(result).toBe(false)
      })
    })

    describe('getAccessToken', () => {
      it('应该返回访问令牌', () => {
        const token = 'mock-access-token'
        localStorageMock.getItem.mockReturnValue(token)
        
        const result = userService.getAccessToken()
        
        expect(result).toBe(token)
        expect(localStorageMock.getItem).toHaveBeenCalledWith('access_token')
      })

      it('应该在没有令牌时返回null', () => {
        localStorageMock.getItem.mockReturnValue(null)
        
        const result = userService.getAccessToken()
        
        expect(result).toBeNull()
      })
    })

    describe('getRefreshToken', () => {
      it('应该返回刷新令牌', () => {
        const token = 'mock-refresh-token'
        localStorageMock.getItem.mockReturnValue(token)
        
        const result = userService.getRefreshToken()
        
        expect(result).toBe(token)
        expect(localStorageMock.getItem).toHaveBeenCalledWith('refresh_token')
      })

      it('应该在没有令牌时返回null', () => {
        localStorageMock.getItem.mockReturnValue(null)
        
        const result = userService.getRefreshToken()
        
        expect(result).toBeNull()
      })
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
      
      vi.mocked(userApi.getProfile).mockRejectedValue(error)
      
      await expect(userService.getProfile()).rejects.toThrow('获取用户信息失败: API错误信息')
    })

    it('应该正确处理通用错误', async () => {
      const error = new Error('网络连接失败')
      
      vi.mocked(userApi.getProfile).mockRejectedValue(error)
      
      await expect(userService.getProfile()).rejects.toThrow('获取用户信息失败: 网络连接失败')
    })

    it('应该处理未知错误', async () => {
      vi.mocked(userApi.getProfile).mockRejectedValue('未知错误')
      
      await expect(userService.getProfile()).rejects.toThrow('获取用户信息失败')
    })
  })

  describe('数据转换', () => {
    it('应该正确转换用户数据', async () => {
      const mockUser = mockUsers[0]
      vi.mocked(userApi.getProfile).mockResolvedValue(mockUser)
      
      const result = await userService.getProfile()
      
      expect(result).toEqual(mockUser)
      expect(result.userId).toBe(mockUser.userId)
      expect(result.username).toBe(mockUser.username)
      expect(result.email).toBe(mockUser.email)
      expect(result.role).toBe(mockUser.role)
      expect(result.status).toBe(mockUser.status)
      expect(result.createdAt).toBe(mockUser.createdAt)
      expect(result.updatedAt).toBe(mockUser.updatedAt)
    })
  })
})
