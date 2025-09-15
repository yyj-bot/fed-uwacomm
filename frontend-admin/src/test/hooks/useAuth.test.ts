/**
 * useAuth Hook 测试
 * 测试认证Hook的封装逻辑和计算属性
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useAuth } from '@/store/auth/useAuthStore'
import { useAuthStore } from '@/store/auth/authStore'
import { userService } from '@/services'
import {
  mockUser,
  mockAdminUser,
  mockOperatorUser,
  mockLoginResponse,
  mockLoginRequest,
  mockRegisterRequest,
  mockUpdateProfileRequest,
  mockChangePasswordRequest,
  mockAuthErrors
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

describe('useAuth Hook', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    const store = useAuthStore.getState()
    store.logout()
  })

  describe('基本状态访问', () => {
    it('应该返回正确的初始状态', () => {
      const { result } = renderHook(() => useAuth())
      
      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.isLoading).toBe(false)
      expect(result.current.error).toBe(null)
      expect(result.current.user).toBe(null)
      expect(result.current.userRole).toBe(null)
      expect(result.current.loginStatus).toBe('idle')
      expect(result.current.registerStatus).toBe('idle')
      expect(result.current.updateProfileStatus).toBe('idle')
      expect(result.current.changePasswordStatus).toBe('idle')
    })

    it('应该正确反映已认证状态', () => {
      // 设置已认证状态
      act(() => {
        useAuthStore.setState({
          isAuthenticated: true,
          user: mockUser,
          token: 'test-token',
          loginStatus: 'success'
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.user).toEqual(mockUser)
      expect(result.current.userRole).toBe('RESEARCHER')
      expect(result.current.isLoginSuccess).toBe(true)
    })
  })

  describe('登录功能', () => {
    it('应该成功登录并返回成功结果', async () => {
      // 模拟成功的登录
      vi.mocked(userService.login).mockResolvedValue(mockLoginResponse)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行登录
      let loginResult: any
      await act(async () => {
        loginResult = await result.current.login(mockLoginRequest)
      })
      
      // 验证返回结果
      expect(loginResult.success).toBe(true)
      expect(loginResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.user).toEqual(mockLoginResponse.user)
    })

    it('应该处理登录失败并返回错误结果', async () => {
      // 模拟登录失败
      vi.mocked(userService.login).mockRejectedValue(mockAuthErrors.LOGIN_FAILED)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行登录
      let loginResult: any
      await act(async () => {
        loginResult = await result.current.login(mockLoginRequest)
      })
      
      // 验证返回结果
      expect(loginResult.success).toBe(false)
      expect(loginResult.error).toBe('用户名或密码错误')
      
      // 验证状态保持未认证
      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.error).toBe('用户名或密码错误')
    })
  })

  describe('登出功能', () => {
    it('应该成功登出', async () => {
      // 先设置已登录状态
      act(() => {
        useAuthStore.setState({
          isAuthenticated: true,
          user: mockUser,
          token: 'test-token'
        })
      })
      
      // 模拟成功的登出
      vi.mocked(userService.logout).mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行登出
      let logoutResult: any
      await act(async () => {
        logoutResult = await result.current.logout()
      })
      
      // 验证返回结果
      expect(logoutResult.success).toBe(true)
      expect(logoutResult.error).toBe(null)
      
      // 验证状态被清除
      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.user).toBe(null)
    })

    it('应该处理登出失败', async () => {
      // 模拟登出失败
      vi.mocked(userService.logout).mockRejectedValue(mockAuthErrors.NETWORK_ERROR)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行登出
      let logoutResult: any
      await act(async () => {
        logoutResult = await result.current.logout()
      })
      
      // 验证返回结果 - authStore.logout 设计为即使API失败也要成功清除本地状态
      expect(logoutResult.success).toBe(true)
      expect(logoutResult.error).toBe(null)
      
      // 验证状态被清除（这是更重要的验证）
      expect(result.current.isAuthenticated).toBe(false)
      expect(result.current.user).toBe(null)
    })
  })

  describe('注册功能', () => {
    it('应该成功注册', async () => {
      // 模拟成功的注册
      vi.mocked(userService.register).mockResolvedValue(mockUser)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行注册
      let registerResult: any
      await act(async () => {
        registerResult = await result.current.register(mockRegisterRequest)
      })
      
      // 验证返回结果
      expect(registerResult.success).toBe(true)
      expect(registerResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.isRegisterSuccess).toBe(true)
    })

    it('应该处理注册失败', async () => {
      // 模拟注册失败
      vi.mocked(userService.register).mockRejectedValue(mockAuthErrors.EMAIL_ALREADY_EXISTS)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行注册
      let registerResult: any
      await act(async () => {
        registerResult = await result.current.register(mockRegisterRequest)
      })
      
      // 验证返回结果
      expect(registerResult.success).toBe(false)
      expect(registerResult.error).toBe('邮箱已存在')
    })
  })

  describe('用户信息管理', () => {
    it('应该成功获取用户信息', async () => {
      // 设置已登录状态
      act(() => {
        useAuthStore.setState({ isAuthenticated: true })
      })
      
      // 模拟成功的获取用户信息
      vi.mocked(userService.getProfile).mockResolvedValue(mockUser)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行获取用户信息
      let fetchResult: any
      await act(async () => {
        fetchResult = await result.current.fetchUserProfile()
      })
      
      // 验证返回结果
      expect(fetchResult.success).toBe(true)
      expect(fetchResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.user).toEqual(mockUser)
    })

    it('应该成功更新用户信息', async () => {
      const updatedUser = { ...mockUser, displayName: '更新用户' }
      
      // 模拟成功的更新用户信息
      vi.mocked(userService.updateProfile).mockResolvedValue(updatedUser)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行更新用户信息
      let updateResult: any
      await act(async () => {
        updateResult = await result.current.updateProfile(mockUpdateProfileRequest)
      })
      
      // 验证返回结果
      expect(updateResult.success).toBe(true)
      expect(updateResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.isUpdateProfileSuccess).toBe(true)
    })

    it('应该成功修改密码', async () => {
      // 模拟成功的修改密码
      vi.mocked(userService.changePassword).mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行修改密码
      let changeResult: any
      await act(async () => {
        changeResult = await result.current.changePassword(mockChangePasswordRequest)
      })
      
      // 验证返回结果
      expect(changeResult.success).toBe(true)
      expect(changeResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.isChangePasswordSuccess).toBe(true)
    })
  })

  describe('令牌管理', () => {
    it('应该成功刷新令牌', async () => {
      // 设置有刷新令牌的状态
      act(() => {
        useAuthStore.setState({ refreshToken: 'old-refresh-token' })
      })
      
      const newTokenResponse = {
        token: 'new-token',
        refreshToken: 'new-refresh-token',
        expiresIn: 3600
      }
      
      // 模拟成功的刷新令牌
      vi.mocked(userService.refreshToken).mockResolvedValue(newTokenResponse)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行刷新令牌
      let refreshResult: any
      await act(async () => {
        refreshResult = await result.current.refreshToken()
      })
      
      // 验证返回结果
      expect(refreshResult.success).toBe(true)
      expect(refreshResult.error).toBe(null)
    })

    it('应该处理刷新令牌失败', async () => {
      // 设置有刷新令牌的状态
      act(() => {
        useAuthStore.setState({ refreshToken: 'invalid-refresh-token' })
      })
      
      // 模拟刷新令牌失败
      vi.mocked(userService.refreshToken).mockRejectedValue(mockAuthErrors.TOKEN_EXPIRED)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行刷新令牌
      let refreshResult: any
      await act(async () => {
        refreshResult = await result.current.refreshToken()
      })
      
      // 验证返回结果
      expect(refreshResult.success).toBe(false)
      expect(refreshResult.error).toBe('令牌已过期')
    })
  })

  describe('计算属性', () => {
    it('应该正确计算loading状态', () => {
      const { result, rerender } = renderHook(() => useAuth())
      
      // 初始状态不应该loading
      expect(result.current.isLoading).toBe(false)
      expect(result.current.isLoading).toBe(false)
      
      // 设置各种loading状态
      act(() => {
        useAuthStore.setState({ 
          isLoading: true,
          loginStatus: 'logging'
        })
      })
      
      rerender()
      expect(result.current.isLoading).toBe(true)
      expect(result.current.isLoggingIn).toBe(true)
      expect(result.current.isLoading).toBe(true)
    })

    it('应该正确计算操作状态', () => {
      const { result, rerender } = renderHook(() => useAuth())
      
      // 设置不同的操作状态
      act(() => {
        useAuthStore.setState({
          registerStatus: 'registering',
          updateProfileStatus: 'updating',
          changePasswordStatus: 'changing'
        })
      })
      
      rerender()
      expect(result.current.isRegistering).toBe(true)
      expect(result.current.isUpdatingProfile).toBe(true)
      expect(result.current.isChangingPassword).toBe(true)
    })

    it('应该正确计算成功状态', () => {
      const { result, rerender } = renderHook(() => useAuth())
      
      // 设置成功状态
      act(() => {
        useAuthStore.setState({
          loginStatus: 'success',
          registerStatus: 'success',
          updateProfileStatus: 'success',
          changePasswordStatus: 'success'
        })
      })
      
      rerender()
      expect(result.current.isLoginSuccess).toBe(true)
      expect(result.current.isRegisterSuccess).toBe(true)
      expect(result.current.isUpdateProfileSuccess).toBe(true)
      expect(result.current.isChangePasswordSuccess).toBe(true)
    })
  })

  describe('权限检查', () => {
    it('应该正确检查管理员权限', () => {
      // 设置管理员用户
      act(() => {
        useAuthStore.setState({
          isAuthenticated: true,
          user: mockAdminUser
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 管理员应该拥有所有权限
      expect(result.current.hasPermission('vm:read')).toBe(true)
      expect(result.current.hasPermission('vm:write')).toBe(true)
      expect(result.current.hasPermission('vm:delete')).toBe(true)
      expect(result.current.hasPermission('user:write')).toBe(true)
      expect(result.current.hasPermission('any:permission')).toBe(true)
    })

    it('应该正确检查研究员权限', () => {
      // 设置研究员用户
      act(() => {
        useAuthStore.setState({
          isAuthenticated: true,
          user: mockUser // RESEARCHER role
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 研究员应该有特定权限
      expect(result.current.hasPermission('vm:read')).toBe(true)
      expect(result.current.hasPermission('task:create')).toBe(true)
      expect(result.current.hasPermission('data:upload')).toBe(true)
      
      // 研究员不应该有管理权限
      expect(result.current.hasPermission('vm:delete')).toBe(false)
      expect(result.current.hasPermission('user:write')).toBe(false)
    })

    it('应该正确检查操作员权限', () => {
      // 设置操作员用户
      act(() => {
        useAuthStore.setState({
          isAuthenticated: true,
          user: mockOperatorUser
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 操作员应该有特定权限
      expect(result.current.hasPermission('vm:read')).toBe(true)
      expect(result.current.hasPermission('vm:operate')).toBe(true)
      expect(result.current.hasPermission('task:read')).toBe(true)
      
      // 操作员不应该有创建权限
      expect(result.current.hasPermission('task:create')).toBe(false)
      expect(result.current.hasPermission('data:upload')).toBe(false)
    })

    it('应该在未认证时拒绝所有权限', () => {
      // 确保未认证状态
      act(() => {
        useAuthStore.setState({
          isAuthenticated: false,
          user: null
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 未认证用户不应该有任何权限
      expect(result.current.hasPermission('vm:read')).toBe(false)
      expect(result.current.hasPermission('task:read')).toBe(false)
      expect(result.current.hasPermission('any:permission')).toBe(false)
    })
  })

  describe('状态管理操作', () => {
    it('应该能够清除错误', () => {
      // 设置错误状态
      act(() => {
        useAuthStore.setState({ error: '测试错误' })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 清除错误
      act(() => {
        result.current.clearError()
      })
      
      // 验证错误被清除
      expect(result.current.error).toBe(null)
    })

    it('应该能够重置状态', () => {
      // 设置各种状态
      act(() => {
        useAuthStore.setState({
          loginStatus: 'success',
          registerStatus: 'failed',
          updateProfileStatus: 'updating',
          changePasswordStatus: 'changing'
        })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 重置状态
      act(() => {
        result.current.resetStatus()
      })
      
      // 验证状态被重置
      expect(result.current.loginStatus).toBe('idle')
      expect(result.current.registerStatus).toBe('idle')
      expect(result.current.updateProfileStatus).toBe('idle')
      expect(result.current.changePasswordStatus).toBe('idle')
    })

    it('应该能够检查认证状态', () => {
      // 模拟service返回true
      vi.mocked(userService.isAuthenticated).mockReturnValue(true)
      
      // 设置有token的状态
      act(() => {
        useAuthStore.setState({ token: 'valid-token' })
      })
      
      const { result } = renderHook(() => useAuth())
      
      // 检查认证状态
      let isValid: boolean
      act(() => {
        isValid = result.current.checkAuthStatus()
      })
      
      expect(isValid).toBe(true)
    })

    it('应该能够初始化认证', async () => {
      // 设置有token的状态
      act(() => {
        useAuthStore.setState({ token: 'valid-token' })
      })
      
      // 模拟成功的初始化
      vi.mocked(userService.isAuthenticated).mockReturnValue(true)
      vi.mocked(userService.getProfile).mockResolvedValue(mockUser)
      
      const { result } = renderHook(() => useAuth())
      
      // 执行初始化认证
      let initResult: any
      await act(async () => {
        initResult = await result.current.initializeAuth()
      })
      
      // 验证返回结果
      expect(initResult.success).toBe(true)
      expect(initResult.error).toBe(null)
      
      // 验证状态更新
      expect(result.current.isAuthenticated).toBe(true)
      expect(result.current.user).toEqual(mockUser)
    })
  })
})
