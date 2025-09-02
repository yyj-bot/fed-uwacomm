/**
 * Admin Store 测试
 * 测试管理员功能状态管理的所有功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAdminStore } from '@/store/admin/adminStore'
import { adminService } from '@/services'
import {
  mockUserList,
  mockCreateUserRequest,
  mockUpdateUserRequest,
  mockUserListResponse,
  mockAdminErrors,
  mockInitialAdminState
} from '@/mocks/store/adminStoreMock'

// Mock adminService
vi.mock('@/services', () => ({
  adminService: {
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

describe('AdminStore', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 重置store状态
    useAdminStore.getState().resetState()
  })

  describe('初始状态', () => {
    it('应该有正确的初始状态', () => {
      const state = useAdminStore.getState()
      
      expect(state.userList).toEqual([])
      expect(state.userListTotal).toBe(0)
      expect(state.userListLoading).toBe(false)
      expect(state.userListError).toBe(null)
      expect(state.currentUser).toBe(null)
      expect(state.currentUserLoading).toBe(false)
      expect(state.currentUserError).toBe(null)
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.pagination).toEqual({
        page: 1,
        size: 20,
        total: 0
      })
      expect(state.queryParams).toEqual({})
      expect(state.userPermissions).toEqual({})
      expect(state.permissionsLoading).toEqual({})
      expect(state.createUserLoading).toBe(false)
      expect(state.createUserError).toBe(null)
      expect(state.userStatistics).toBe(null)
    })
  })

  describe('获取用户列表', () => {
    it('应该成功获取用户列表', async () => {
      // 模拟成功的API响应
      vi.mocked(adminService.getUserList).mockResolvedValue(mockUserListResponse)
      
      // 执行获取列表
      await useAdminStore.getState().fetchUserList()
      
      // 验证状态更新
      const state = useAdminStore.getState()
      expect(state.userList).toEqual(mockUserListResponse.list)
      expect(state.userListTotal).toBe(mockUserListResponse.total)
      expect(state.userListLoading).toBe(false)
      expect(state.userListError).toBe(null)
      expect(state.pagination).toEqual({
        page: mockUserListResponse.page,
        size: mockUserListResponse.size,
        total: mockUserListResponse.total
      })
      
      // 验证service被调用
      expect(adminService.getUserList).toHaveBeenCalledWith({
        page: 1,
        size: 20
      })
    })

    it('应该处理获取用户列表失败', async () => {
      // 模拟API失败
      vi.mocked(adminService.getUserList).mockRejectedValue(mockAdminErrors.NETWORK_ERROR)
      
      // 执行获取列表并期望抛出错误
      await expect(useAdminStore.getState().fetchUserList()).rejects.toThrow('网络连接失败')
      
      // 验证错误状态
      const state = useAdminStore.getState()
      expect(state.userListLoading).toBe(false)
      expect(state.userListError).toBe('网络连接失败')
    })

    it('应该使用自定义参数获取列表', async () => {
      const customParams = { page: 2, size: 10, role: 'RESEARCHER' as const }
      
      vi.mocked(adminService.getUserList).mockResolvedValue({
        ...mockUserListResponse,
        page: 2,
        size: 10
      })
      
      // 使用自定义参数获取列表
      await useAdminStore.getState().fetchUserList(customParams)
      
      // 验证service被正确调用
      expect(adminService.getUserList).toHaveBeenCalledWith(customParams)
    })
  })

  describe('获取用户详情', () => {
    it('应该成功获取用户详情', async () => {
      // 模拟成功的API响应
      vi.mocked(adminService.getUserDetail).mockResolvedValue(mockUserList[0])
      
      // 执行获取详情
      await useAdminStore.getState().fetchUserDetail('user-001')
      
      // 验证状态更新
      const state = useAdminStore.getState()
      expect(state.currentUser).toEqual(mockUserList[0])
      expect(state.currentUserLoading).toBe(false)
      expect(state.currentUserError).toBe(null)
      
      // 验证service被调用
      expect(adminService.getUserDetail).toHaveBeenCalledWith('user-001')
    })

    it('应该处理获取用户详情失败', async () => {
      // 模拟API失败
      vi.mocked(adminService.getUserDetail).mockRejectedValue(mockAdminErrors.USER_NOT_FOUND)
      
      // 执行获取详情并期望抛出错误
      await expect(useAdminStore.getState().fetchUserDetail('user-999')).rejects.toThrow('用户不存在')
      
      // 验证错误状态
      const state = useAdminStore.getState()
      expect(state.currentUserLoading).toBe(false)
      expect(state.currentUserError).toBe('用户不存在')
    })
  })

  describe('用户操作', () => {
    describe('创建用户', () => {
      it('应该成功创建用户', async () => {
        // 模拟成功的API响应
        const newUser = { ...mockUserList[0], userId: 'user-new' }
        vi.mocked(adminService.createUser).mockResolvedValue(newUser)
        
        // 模拟刷新列表的API响应，因为 createUser 成功后会调用 refreshUserList
        vi.mocked(adminService.getUserList).mockResolvedValue(mockUserListResponse)
        
        // 执行创建
        await useAdminStore.getState().createUser(mockCreateUserRequest)
        
        // 验证操作状态
        const state = useAdminStore.getState()
        expect(state.createUserLoading).toBe(false)
        
        // 验证service被调用
        expect(adminService.createUser).toHaveBeenCalledWith(mockCreateUserRequest)
        expect(adminService.getUserList).toHaveBeenCalled()
      })

      it('应该处理创建用户失败', async () => {
        // 模拟API失败
        vi.mocked(adminService.createUser).mockRejectedValue(mockAdminErrors.USERNAME_ALREADY_EXISTS)
        
        // 执行创建并期望抛出错误
        await expect(useAdminStore.getState().createUser(mockCreateUserRequest)).rejects.toThrow('用户名已存在')
        
        // 验证错误状态
        const state = useAdminStore.getState()
        expect(state.createUserLoading).toBe(false)
        expect(state.createUserError).toBe('用户名已存在')
      })
    })

    describe('更新用户', () => {
      it('应该成功更新用户', async () => {
        // 设置初始用户列表
        useAdminStore.setState({
          userList: [mockUserList[0]],
          currentUser: mockUserList[0]
        })
        
        // 模拟成功的API响应
        const updatedUser = { ...mockUserList[0], username: 'updated-user' }
        vi.mocked(adminService.updateUser).mockResolvedValue(updatedUser)
        
        // 执行更新
        await useAdminStore.getState().updateUser('user-001', mockUpdateUserRequest)
        
        // 验证操作状态
        const state = useAdminStore.getState()
        expect(state.operationLoading['update-user-001']).toBe(false)
        
        // 验证service被调用
        expect(adminService.updateUser).toHaveBeenCalledWith('user-001', mockUpdateUserRequest)
      })

      it('应该处理更新用户失败', async () => {
        // 模拟API失败
        vi.mocked(adminService.updateUser).mockRejectedValue(mockAdminErrors.INSUFFICIENT_PERMISSION)
        
        // 执行更新并期望抛出错误
        await expect(useAdminStore.getState().updateUser('user-001', mockUpdateUserRequest)).rejects.toThrow('权限不足')
        
        // 验证错误状态
        const state = useAdminStore.getState()
        expect(state.operationLoading['update-user-001']).toBe(false)
        expect(state.operationError['update-user-001']).toBe('权限不足')
      })
    })

    describe('删除用户', () => {
      it('应该成功删除用户', async () => {
        // 设置初始用户列表
        useAdminStore.setState({
          userList: mockUserList,
          userListTotal: mockUserList.length
        })
        
        // 模拟成功的API响应
        vi.mocked(adminService.deleteUser).mockResolvedValue(undefined)
        
        // 执行删除
        await useAdminStore.getState().deleteUser('user-001')
        
        // 验证用户被从列表中移除
        const state = useAdminStore.getState()
        expect(state.userList.find(user => user.userId === 'user-001')).toBeUndefined()
        expect(state.userListTotal).toBe(mockUserList.length - 1)
        expect(state.operationLoading['delete-user-001']).toBe(false)
        
        // 验证service被调用
        expect(adminService.deleteUser).toHaveBeenCalledWith('user-001')
      })

      it('应该处理删除用户失败', async () => {
        // 模拟API失败
        vi.mocked(adminService.deleteUser).mockRejectedValue(mockAdminErrors.CANNOT_DELETE_ADMIN)
        
        // 执行删除并期望抛出错误
        await expect(useAdminStore.getState().deleteUser('admin-001')).rejects.toThrow('无法删除管理员用户')
        
        // 验证错误状态
        const state = useAdminStore.getState()
        expect(state.operationLoading['delete-admin-001']).toBe(false)
        expect(state.operationError['delete-admin-001']).toBe('无法删除管理员用户')
      })
    })
  })

  describe('分页和查询参数管理', () => {
    it('应该正确设置分页参数', () => {
      // 设置分页
      useAdminStore.getState().setPagination(3, 15)
      
      // 验证分页状态
      const state = useAdminStore.getState()
      expect(state.pagination.page).toBe(3)
      expect(state.pagination.size).toBe(15)
    })

    it('应该正确设置查询参数', () => {
      const queryParams = { role: 'RESEARCHER' as const, status: 'ACTIVE' as const }
      
      useAdminStore.getState().setQueryParams(queryParams)
      
      // 验证查询参数
      const state = useAdminStore.getState()
      expect(state.queryParams).toEqual(queryParams)
    })
  })

  describe('错误处理', () => {
    it('应该能够清除所有错误', () => {
      // 设置各种错误状态
      useAdminStore.setState({
        userListError: '获取用户列表失败',
        currentUserError: '获取用户详情失败',
        operationError: {
          'create-user': '创建失败',
          'update-user-001': '更新失败'
        }
      })
      
      useAdminStore.getState().clearError()
      
      // 验证错误被清除
      const state = useAdminStore.getState()
      expect(state.userListError).toBe(null)
      expect(state.currentUserError).toBe(null)
      expect(state.operationError).toEqual({})
    })
  })

  describe('状态重置', () => {
    it('应该能够重置所有状态', () => {
      // 设置一些状态
      useAdminStore.setState({
        userList: mockUserList,
        userListTotal: 10,
        currentUser: mockUserList[0]
      })
      
      useAdminStore.getState().resetState()
      
      // 验证状态被重置为初始值
      const state = useAdminStore.getState()
      expect(state.userList).toEqual([])
      expect(state.userListTotal).toBe(0)
      expect(state.currentUser).toBe(null)
      expect(state.operationLoading).toEqual({})
      expect(state.operationError).toEqual({})
      expect(state.pagination.page).toBe(1)
      expect(state.pagination.size).toBe(20)
      expect(state.pagination.total).toBe(0)
    })
  })
})
