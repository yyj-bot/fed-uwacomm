/**
 * useAdmin Hook 测试
 * 测试管理员hook的错误处理封装和组件接口
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useAdmin } from '@/store/admin/useAdminStore'
import { useAdminStore } from '@/store/admin/adminStore'
import {
  mockUserList,
  mockCreateUserRequest,
  mockUpdateUserRequest
} from '@/mocks/store/adminStoreMock'

// Mock the store
vi.mock('@/store/admin/adminStore')

const mockStore = {
  userList: [],
  userListTotal: 0,
  userListLoading: false,
  userListError: null,
  currentUser: null,
  currentUserLoading: false,
  currentUserError: null,
  operationLoading: {},
  operationError: {},
  pagination: { page: 1, size: 20, total: 0 },
  queryParams: {},
  fetchUserList: vi.fn(),
  refreshUserList: vi.fn(),
  fetchUserDetail: vi.fn(),
  setCurrentUser: vi.fn(),
  createUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn(),
  resetUserPassword: vi.fn(),
  setPagination: vi.fn(),
  setQueryParams: vi.fn(),
  resetQueryParams: vi.fn(),
  clearError: vi.fn(),
  clearUserError: vi.fn(),
  resetState: vi.fn()
}

describe('useAdmin', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock the store selector function
    const mockUseAdminStore = vi.mocked(useAdminStore)
    mockUseAdminStore.mockImplementation((selector: any) => {
      if (typeof selector === 'function') {
        return selector(mockStore)
      }
      return mockStore
    })
  })

  describe('状态访问', () => {
    it('应该正确暴露管理员状态', () => {
      const { result } = renderHook(() => useAdmin())
      
      expect(result.current.userList).toEqual([])
      expect(result.current.userListTotal).toBe(0)
      expect(result.current.userListLoading).toBe(false)
      expect(result.current.userListError).toBe(null)
      expect(result.current.currentUser).toBe(null)
      expect(result.current.currentUserLoading).toBe(false)
      expect(result.current.currentUserError).toBe(null)
      expect(result.current.operationLoading).toEqual({})
      expect(result.current.operationError).toEqual({})
      expect(result.current.pagination).toEqual({ page: 1, size: 20, total: 0 })
      expect(result.current.queryParams).toEqual({})
    })
  })

  describe('用户列表操作', () => {
    it('应该成功获取用户列表', async () => {
      mockStore.fetchUserList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserList()
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchUserList).toHaveBeenCalled()
    })

    it('应该处理获取用户列表失败', async () => {
      const error = new Error('获取用户列表失败')
      mockStore.fetchUserList.mockRejectedValue(error)
      
      const { result } = renderHook(() => useAdmin())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取用户列表失败' })
    })

    it('应该成功刷新用户列表', async () => {
      mockStore.refreshUserList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let refreshResult
      await act(async () => {
        refreshResult = await result.current.refreshUserList()
      })
      
      expect(refreshResult).toEqual({ success: true, error: null })
      expect(mockStore.refreshUserList).toHaveBeenCalled()
    })

    it('应该支持带参数的用户列表获取', async () => {
      mockStore.fetchUserList.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      const params = {
        role: 'ADMIN' as const,
        status: 'ACTIVE' as const,
        page: 2,
        size: 10
      }
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserList(params)
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchUserList).toHaveBeenCalledWith(params)
    })
  })

  describe('用户详情操作', () => {
    it('应该成功获取用户详情', async () => {
      mockStore.fetchUserDetail.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserDetail('user-001')
      })
      
      expect(fetchResult).toEqual({ success: true, error: null })
      expect(mockStore.fetchUserDetail).toHaveBeenCalledWith('user-001')
    })

    it('应该处理获取用户详情失败', async () => {
      const error = new Error('获取用户详情失败')
      mockStore.fetchUserDetail.mockRejectedValue(error)
      
      const { result } = renderHook(() => useAdmin())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserDetail('user-001')
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取用户详情失败' })
    })

    it('应该设置当前用户', () => {
      const { result } = renderHook(() => useAdmin())
      
      const testUser = {
        userId: 'user-001',
        username: 'testuser',
        email: 'test@example.com',
        role: 'RESEARCHER' as const,
        status: 'ACTIVE' as const,
        permissions: [],
        createdAt: '2024-01-15T10:00:00Z',
        updatedAt: '2024-01-15T10:00:00Z'
      }
      
      act(() => {
        result.current.setCurrentUser(testUser)
      })
      
      expect(mockStore.setCurrentUser).toHaveBeenCalledWith(testUser)
    })
  })

  describe('用户管理操作', () => {
    it('应该成功创建用户', async () => {
      const mockUserId = 'user-new-001'
      mockStore.createUser.mockResolvedValue(mockUserId)
      
      const { result } = renderHook(() => useAdmin())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createUser(mockCreateUserRequest)
      })
      
      expect(createResult).toEqual({ success: true, error: null, data: mockUserId })
      expect(mockStore.createUser).toHaveBeenCalledWith(mockCreateUserRequest)
    })

    it('应该处理创建用户失败', async () => {
      const error = new Error('创建用户失败')
      mockStore.createUser.mockRejectedValue(error)
      
      const { result } = renderHook(() => useAdmin())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createUser(mockCreateUserRequest)
      })
      
      expect(createResult).toEqual({ success: false, error: '创建用户失败', data: null })
    })

    it('应该成功更新用户', async () => {
      mockStore.updateUser.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let updateResult
      await act(async () => {
        updateResult = await result.current.updateUser('user-001', mockUpdateUserRequest)
      })
      
      expect(updateResult).toEqual({ success: true, error: null })
      expect(mockStore.updateUser).toHaveBeenCalledWith('user-001', mockUpdateUserRequest)
    })

    it('应该成功删除用户', async () => {
      mockStore.deleteUser.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let deleteResult
      await act(async () => {
        deleteResult = await result.current.deleteUser('user-001')
      })
      
      expect(deleteResult).toEqual({ success: true, error: null })
      expect(mockStore.deleteUser).toHaveBeenCalledWith('user-001')
    })


  })

  describe('用户状态操作', () => {
    it('应该成功重置密码', async () => {
      mockStore.resetUserPassword.mockResolvedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      const resetRequest = {
        newPassword: 'newPassword123',
        reason: '管理员重置',
        sendNotification: true
      }
      
      let resetResult
      await act(async () => {
        resetResult = await result.current.resetUserPassword('user-001', resetRequest)
      })
      
      expect(resetResult).toEqual({ success: true, error: null })
      expect(mockStore.resetUserPassword).toHaveBeenCalledWith('user-001', resetRequest)
    })


  })

  describe('状态管理操作', () => {
    it('应该成功设置分页参数', () => {
      const { result } = renderHook(() => useAdmin())
      
      act(() => {
        result.current.setPagination(2, 50)
      })
      
      expect(mockStore.setPagination).toHaveBeenCalledWith(2, 50)
    })

    it('应该成功设置查询参数', () => {
      const { result } = renderHook(() => useAdmin())
      
      const params = { role: 'ADMIN' as const, status: 'ACTIVE' as const }
      
      act(() => {
        result.current.setQueryParams(params)
      })
      
      expect(mockStore.setQueryParams).toHaveBeenCalledWith(params)
    })

    it('应该成功重置查询参数', () => {
      const { result } = renderHook(() => useAdmin())
      
      act(() => {
        result.current.resetQueryParams()
      })
      
      expect(mockStore.resetQueryParams).toHaveBeenCalled()
    })

    it('应该成功清除错误', () => {
      const { result } = renderHook(() => useAdmin())
      
      act(() => {
        result.current.clearError()
      })
      
      expect(mockStore.clearError).toHaveBeenCalled()
    })

    it('应该成功重置状态', () => {
      const { result } = renderHook(() => useAdmin())
      
      act(() => {
        result.current.resetState()
      })
      
      expect(mockStore.resetState).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该处理非Error类型的异常', async () => {
      mockStore.fetchUserList.mockRejectedValue('string error')
      
      const { result } = renderHook(() => useAdmin())
      
      let fetchResult
      await act(async () => {
        fetchResult = await result.current.fetchUserList()
      })
      
      expect(fetchResult).toEqual({ success: false, error: '获取用户列表失败' })
    })

    it('应该处理undefined异常', async () => {
      mockStore.createUser.mockRejectedValue(undefined)
      
      const { result } = renderHook(() => useAdmin())
      
      let createResult
      await act(async () => {
        createResult = await result.current.createUser(mockCreateUserRequest)
      })
      
      expect(createResult).toEqual({ success: false, error: '创建用户失败', data: null })
    })
  })
})
