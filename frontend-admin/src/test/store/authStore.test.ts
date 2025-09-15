/**
 * Auth Store 测试
 * 测试认证状态管理的所有功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAuthStore } from '@/store/auth/authStore'
import { userService } from '@/services'
import {
  mockUser,
  mockAdminUser,
  mockLoginResponse,
  mockLoginRequest,
  mockRegisterRequest,
  mockUpdateProfileRequest,
  mockChangePasswordRequest,
  mockAuthErrors,
  mockInitialAuthState,
  mockAuthenticatedState
} from '@/mocks/store/authStoreMock'

// Mock userService
vi.mock('@/services', () => ({
  userService: {
    login: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    refreshToken: vi.fn(),
    isAuthenticated: vi.fn()
  }
}))

describe('AuthStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useAuthStore.getState()
    store.logout()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useAuthStore.getState()
      
      expect(state.isAuthenticated).toBe(false)
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe(null)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
      expect(state.refreshToken).toBe(null)
      expect(state.loginStatus).toBe('idle')
      expect(state.registerStatus).toBe('idle')
      expect(state.updateProfileStatus).toBe('idle')
      expect(state.changePasswordStatus).toBe('idle')
    })
  })

  describe('登录功能', () => {
    it('应该成功登录并更新状态', async () => {
      // 模拟成功的登录响应
      vi.mocked(userService.login).mockResolvedValue(mockLoginResponse)
      
      const store = useAuthStore.getState()
      
      // 执行登录
      await store.login(mockLoginRequest)
      
      // 验证状态更新
      const newState = useAuthStore.getState()
      expect(newState.isAuthenticated).toBe(true)
      expect(newState.user).toEqual(mockLoginResponse.user)
      expect(newState.token).toBe(mockLoginResponse.token)
      expect(newState.refreshToken).toBe(mockLoginResponse.refreshToken)
      expect(newState.loginStatus).toBe('success')
      expect(newState.isLoading).toBe(false)
      expect(newState.error).toBe(null)
      
      // 验证service被调用
      expect(userService.login).toHaveBeenCalledWith(mockLoginRequest)
    })

    it('应该在登录过程中显示loading状态', async () => {
      // 创建一个永不resolve的Promise来测试loading状态
      const pendingPromise = new Promise<any>(() => {})
      vi.mocked(userService.login).mockReturnValue(pendingPromise)
      
      const store = useAuthStore.getState()
      
      // 开始登录但不等待
      store.login(mockLoginRequest)
      
      // 验证loading状态
      const state = useAuthStore.getState()
      expect(state.isLoading).toBe(true)
      expect(state.loginStatus).toBe('logging')
      expect(state.error).toBe(null)
    })

    it('应该处理登录失败', async () => {
      // 模拟登录失败
      vi.mocked(userService.login).mockRejectedValue(mockAuthErrors.LOGIN_FAILED)
      
      const store = useAuthStore.getState()
      
      // 执行登录并期望抛出错误
      await expect(store.login(mockLoginRequest)).rejects.toThrow('用户名或密码错误')
      
      // 验证错误状态
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(false)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
      expect(state.refreshToken).toBe(null)
      expect(state.loginStatus).toBe('failed')
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe('用户名或密码错误')
    })
  })

  describe('登出功能', () => {
    it('应该成功登出并清除状态', async () => {
      // 先设置已登录状态
      const store = useAuthStore.getState()
      store.setUser(mockUser)
      useAuthStore.setState({
        isAuthenticated: true,
        token: 'test-token',
        refreshToken: 'test-refresh-token'
      })
      
      // 模拟成功的登出响应
      vi.mocked(userService.logout).mockResolvedValue(undefined)
      
      // 执行登出
      await store.logout()
      
      // 验证状态被清除
      const newState = useAuthStore.getState()
      expect(newState.isAuthenticated).toBe(false)
      expect(newState.user).toBe(null)
      expect(newState.token).toBe(null)
      expect(newState.refreshToken).toBe(null)
      expect(newState.loginStatus).toBe('idle')
      expect(newState.registerStatus).toBe('idle')
      expect(newState.updateProfileStatus).toBe('idle')
      expect(newState.changePasswordStatus).toBe('idle')
      expect(newState.isLoading).toBe(false)
      expect(newState.error).toBe(null)
    })

    it('应该在登出API失败时仍然清除本地状态', async () => {
      // 先设置已登录状态
      useAuthStore.setState({
        isAuthenticated: true,
        user: mockUser,
        token: 'test-token'
      })
      
      // 模拟登出API失败
      vi.mocked(userService.logout).mockRejectedValue(new Error('网络错误'))
      
      const store = useAuthStore.getState()
      
      // 执行登出，不应该抛出错误
      await expect(store.logout()).resolves.toBeUndefined()
      
      // 验证本地状态仍然被清除
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(false)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
    })
  })

  describe('注册功能', () => {
    it('应该成功注册', async () => {
      // 模拟成功的注册响应
      vi.mocked(userService.register).mockResolvedValue(mockUser)
      
      const store = useAuthStore.getState()
      
      // 执行注册
      await store.register(mockRegisterRequest)
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.registerStatus).toBe('success')
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe(null)
      
      // 验证service被调用
      expect(userService.register).toHaveBeenCalledWith(mockRegisterRequest)
    })

    it('应该处理注册失败', async () => {
      // 模拟注册失败
      vi.mocked(userService.register).mockRejectedValue(mockAuthErrors.EMAIL_ALREADY_EXISTS)
      
      const store = useAuthStore.getState()
      
      // 执行注册并期望抛出错误
      await expect(store.register(mockRegisterRequest)).rejects.toThrow('邮箱已存在')
      
      // 验证错误状态
      const state = useAuthStore.getState()
      expect(state.registerStatus).toBe('failed')
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe('邮箱已存在')
    })
  })

  describe('获取用户信息', () => {
    it('应该成功获取用户信息', async () => {
      // 设置已登录状态
      useAuthStore.setState({ isAuthenticated: true })
      
      // 模拟成功的获取用户信息响应
      vi.mocked(userService.getProfile).mockResolvedValue(mockUser)
      
      const store = useAuthStore.getState()
      
      // 执行获取用户信息
      await store.fetchUserProfile()
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.user).toEqual(mockUser)
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe(null)
    })

    it('应该在未登录时抛出错误', async () => {
      // 确保未登录状态
      useAuthStore.setState({ isAuthenticated: false })
      
      const store = useAuthStore.getState()
      
      // 执行获取用户信息并期望抛出错误
      await expect(store.fetchUserProfile()).rejects.toThrow('用户未登录')
    })

    it('应该处理获取用户信息失败', async () => {
      // 设置已登录状态
      useAuthStore.setState({ isAuthenticated: true })
      
      // 模拟获取用户信息失败
      vi.mocked(userService.getProfile).mockRejectedValue(mockAuthErrors.NETWORK_ERROR)
      
      const store = useAuthStore.getState()
      
      // 执行获取用户信息并期望抛出错误
      await expect(store.fetchUserProfile()).rejects.toThrow('网络连接失败')
      
      // 验证错误状态
      const state = useAuthStore.getState()
      expect(state.isLoading).toBe(false)
      expect(state.error).toBe('网络连接失败')
    })
  })

  describe('更新用户信息', () => {
    it('应该成功更新用户信息', async () => {
      const updatedUser = { ...mockUser, displayName: '更新用户' }
      
      // 模拟成功的更新用户信息响应
      vi.mocked(userService.updateProfile).mockResolvedValue(updatedUser)
      
      const store = useAuthStore.getState()
      
      // 执行更新用户信息
      await store.updateProfile(mockUpdateProfileRequest)
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.user).toEqual(updatedUser)
      expect(state.updateProfileStatus).toBe('success')
      expect(state.error).toBe(null)
    })

    it('应该处理更新用户信息失败', async () => {
      // 模拟更新用户信息失败
      vi.mocked(userService.updateProfile).mockRejectedValue(mockAuthErrors.SERVER_ERROR)
      
      const store = useAuthStore.getState()
      
      // 执行更新用户信息并期望抛出错误
      await expect(store.updateProfile(mockUpdateProfileRequest)).rejects.toThrow('服务器内部错误')
      
      // 验证错误状态
      const state = useAuthStore.getState()
      expect(state.updateProfileStatus).toBe('failed')
      expect(state.error).toBe('服务器内部错误')
    })
  })

  describe('修改密码', () => {
    it('应该成功修改密码', async () => {
      // 模拟成功的修改密码响应
      vi.mocked(userService.changePassword).mockResolvedValue(undefined)
      
      const store = useAuthStore.getState()
      
      // 执行修改密码
      await store.changePassword(mockChangePasswordRequest)
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.changePasswordStatus).toBe('success')
      expect(state.error).toBe(null)
    })

    it('应该处理修改密码失败', async () => {
      // 模拟修改密码失败
      vi.mocked(userService.changePassword).mockRejectedValue(mockAuthErrors.WEAK_PASSWORD)
      
      const store = useAuthStore.getState()
      
      // 执行修改密码并期望抛出错误
      await expect(store.changePassword(mockChangePasswordRequest)).rejects.toThrow('密码强度不足')
      
      // 验证错误状态
      const state = useAuthStore.getState()
      expect(state.changePasswordStatus).toBe('failed')
      expect(state.error).toBe('密码强度不足')
    })
  })

  describe('刷新令牌', () => {
    it('应该成功刷新令牌', async () => {
      // 设置有刷新令牌的状态
      useAuthStore.setState({ refreshToken: 'old-refresh-token' })
      
      const newTokenResponse = {
        token: 'new-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600
      }
      
      // 模拟成功的刷新令牌响应
      vi.mocked(userService.refreshToken).mockResolvedValue(newTokenResponse)
      
      const store = useAuthStore.getState()
      
      // 执行刷新令牌
      await store.refreshAuthToken()
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.token).toBe('new-token')
      expect(state.refreshToken).toBe('new-refresh-token')
      expect(state.error).toBe(null)
    })

    it('应该在没有刷新令牌时抛出错误', async () => {
      // 确保没有刷新令牌
      useAuthStore.setState({ refreshToken: null })
      
      const store = useAuthStore.getState()
      
      // 执行刷新令牌并期望抛出错误
      await expect(store.refreshAuthToken()).rejects.toThrow('无刷新令牌，请重新登录')
    })

    it('应该在刷新失败时清除认证状态', async () => {
      // 设置有刷新令牌的状态
      useAuthStore.setState({
        refreshToken: 'old-refresh-token',
        isAuthenticated: true,
        user: mockUser,
        token: 'old-token'
      })
      
      // 模拟刷新令牌失败
      vi.mocked(userService.refreshToken).mockRejectedValue(mockAuthErrors.TOKEN_EXPIRED)
      
      const store = useAuthStore.getState()
      
      // 执行刷新令牌并期望抛出错误
      await expect(store.refreshAuthToken()).rejects.toThrow('令牌已过期')
      
      // 验证认证状态被清除
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(false)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
      expect(state.refreshToken).toBe(null)
      expect(state.error).toBe('令牌刷新失败，请重新登录')
    })
  })

  describe('认证状态检查', () => {
    it('应该正确检查认证状态', () => {
      // 模拟service返回true
      vi.mocked(userService.isAuthenticated).mockReturnValue(true)
      
      // 设置有token的状态
      useAuthStore.setState({ token: 'valid-token' })
      
      const store = useAuthStore.getState()
      const isValid = store.checkAuthStatus()
      
      expect(isValid).toBe(true)
    })

    it('应该在无效状态时清除认证信息', () => {
      // 模拟service返回false
      vi.mocked(userService.isAuthenticated).mockReturnValue(false)
      
      // 设置已登录状态
      useAuthStore.setState({
        isAuthenticated: true,
        user: mockUser,
        token: 'invalid-token',
        refreshToken: 'invalid-refresh-token'
      })
      
      const store = useAuthStore.getState()
      const isValid = store.checkAuthStatus()
      
      expect(isValid).toBe(false)
      
      // 验证认证状态被清除
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(false)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
      expect(state.refreshToken).toBe(null)
    })
  })

  describe('初始化认证', () => {
    it('应该成功初始化认证', async () => {
      // 设置有token的状态
      useAuthStore.setState({ token: 'valid-token' })
      
      // 模拟checkAuthStatus返回true
      vi.mocked(userService.isAuthenticated).mockReturnValue(true)
      
      // 模拟成功获取用户信息
      vi.mocked(userService.getProfile).mockResolvedValue(mockUser)
      
      const store = useAuthStore.getState()
      
      // 执行初始化认证
      await store.initializeAuth()
      
      // 验证状态更新
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(true)
      expect(state.user).toEqual(mockUser)
      expect(state.error).toBe(null)
    })

    it('应该在初始化失败时清除认证状态', async () => {
      // 设置有token的状态
      useAuthStore.setState({
        token: 'invalid-token',
        isAuthenticated: true,
        user: mockUser
      })
      
      // 模拟checkAuthStatus返回true
      vi.mocked(userService.isAuthenticated).mockReturnValue(true)
      
      // 模拟获取用户信息失败
      vi.mocked(userService.getProfile).mockRejectedValue(mockAuthErrors.INVALID_TOKEN)
      
      const store = useAuthStore.getState()
      
      // 执行初始化认证，不应该抛出错误
      await expect(store.initializeAuth()).resolves.toBeUndefined()
      
      // 验证认证状态被清除
      const state = useAuthStore.getState()
      expect(state.isAuthenticated).toBe(false)
      expect(state.user).toBe(null)
      expect(state.token).toBe(null)
      expect(state.refreshToken).toBe(null)
      expect(state.error).toBe(null)
    })
  })

  describe('状态管理操作', () => {
    it('应该能够清除错误信息', () => {
      // 设置错误状态
      useAuthStore.setState({ error: '测试错误' })
      
      const store = useAuthStore.getState()
      store.clearError()
      
      // 验证错误被清除
      const state = useAuthStore.getState()
      expect(state.error).toBe(null)
    })

    it('应该能够重置操作状态', () => {
      // 设置各种操作状态
      useAuthStore.setState({
        loginStatus: 'success',
        registerStatus: 'failed',
        updateProfileStatus: 'updating',
        changePasswordStatus: 'changing'
      })
      
      const store = useAuthStore.getState()
      store.resetStatus()
      
      // 验证状态被重置
      const state = useAuthStore.getState()
      expect(state.loginStatus).toBe('idle')
      expect(state.registerStatus).toBe('idle')
      expect(state.updateProfileStatus).toBe('idle')
      expect(state.changePasswordStatus).toBe('idle')
    })

    it('应该能够设置用户信息', () => {
      const store = useAuthStore.getState()
      store.setUser(mockUser)
      
      // 验证用户信息被设置
      const state = useAuthStore.getState()
      expect(state.user).toEqual(mockUser)
    })

    it('应该能够清除用户信息', () => {
      // 先设置用户信息
      useAuthStore.setState({ user: mockUser })
      
      const store = useAuthStore.getState()
      store.setUser(null)
      
      // 验证用户信息被清除
      const state = useAuthStore.getState()
      expect(state.user).toBe(null)
    })
  })
})
