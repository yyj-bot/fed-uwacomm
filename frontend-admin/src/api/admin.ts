import { createApiInstance } from './base'
import type { 
  ApiResponse, 
  User,
  PaginatedResponse,
  PaginationParams
} from '@/types'
import type { UserStatistics } from '@/services/admin/type'

// 创建管理员API实例
const adminApiInstance = createApiInstance('ADMIN')

// 权限信息类型定义
interface Permission {
  permissionId: string
  permissionName: string
  description: string
  grantedAt: string
}

// ==================== 虚拟机管理相关类型定义 ====================

// VM权限类型
type VmPermission = 'READ' | 'WRITE' | 'EXECUTE' | 'ADMIN'

// VM状态类型
type VmStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING'

// VM连接状态类型
type VmConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'

// VM控制操作类型
type VmControlAction = 'START' | 'STOP' | 'RESTART' | 'FORCE_STOP'

// 虚拟机分配信息
interface VmAssignment {
  vmId: string
  userId: string
  permissions: VmPermission[]
  assignedAt: string
  assignedBy: string
  notes?: string
}

// 虚拟机分配详情（包含用户信息）
interface VmAssignmentDetail {
  userId: string
  username: string
  email: string
  permissions: VmPermission[]
  assignedAt: string
  assignedBy: string
}

// 虚拟机基本信息
interface VirtualMachine {
  vmId: string
  name: string
  ipAddress: string
  status: VmStatus
  connectionStatus: VmConnectionStatus
  isAssigned?: boolean
  assignedUserCount?: number
  createdAt: string
  lastHeartbeat?: string
}

// 用户虚拟机信息
interface UserVm {
  vmId: string
  vmName: string
  ipAddress: string
  status: VmStatus
  permissions: VmPermission[]
  assignedAt: string
}

// 批量操作结果
interface BatchOperationResult {
  vmId: string
  status: 'SUCCESS' | 'FAILED'
  assignedAt?: string
  unassignedAt?: string
  error?: string
}

// 虚拟机分配概况统计
interface VmAssignmentOverview {
  summary: {
    totalVms: number
    assignedVms: number
    unassignedVms: number
    totalUsers: number
    usersWithVms: number
  }
  statusDistribution: Record<VmStatus, number>
  topAssignedVms: Array<{
    vmId: string
    vmName: string
    assignedUserCount: number
  }>
  recentAssignments: Array<{
    vmId: string
    vmName: string
    userId: string
    username: string
    assignedAt: string
  }>
}

// VM控制命令响应
interface VmControlResponse {
  vmId: string
  action: VmControlAction
  commandId: string
  reason?: string
  executedBy: string
  executedAt: string
  estimatedTime?: number
}

// ==================== 管理员用户管理API ====================
export const admin = {
  // 1.1 获取用户列表
  async getUserList(params: PaginationParams & {
    username?: string
    email?: string
    role?: string
    status?: string
  } = {}): Promise<PaginatedResponse<User>> {
    const response = await adminApiInstance.get<ApiResponse<PaginatedResponse<User>>>('/user/list', { params })
    return response.data.data
  },

  // 1.2 获取用户详情
  async getUserDetail(userId: string): Promise<User> {
    const response = await adminApiInstance.get<ApiResponse<User>>(`/user/${userId}`)
    return response.data.data
  },

  // 1.3 创建用户
  async createUser(userData: {
    username: string
    email: string
    password: string
    role: string
    status?: string
  }): Promise<User> {
    const response = await adminApiInstance.post<ApiResponse<User>>('/user/create', userData)
    return response.data.data
  },

  // 1.4 更新用户
  async updateUser(userId: string, userData: {
    username?: string
    email?: string
    role?: string
    status?: string
    password?: string
  }): Promise<User> {
    const response = await adminApiInstance.put<ApiResponse<User>>(`/user/${userId}`, userData)
    return response.data.data
  },

  // 1.5 删除用户
  async deleteUser(userId: string): Promise<void> {
    await adminApiInstance.delete<ApiResponse<null>>(`/user/${userId}`)
  },

  // 1.6 锁定用户
  async lockUser(userId: string, duration?: number): Promise<{ userId: string; lockedUntil: string }> {
    const response = await adminApiInstance.post<ApiResponse<{ userId: string; lockedUntil: string }>>(
      `/user/${userId}/lock`,
      { duration }
    )
    return response.data.data
  },

  // 1.7 解锁用户
  async unlockUser(userId: string): Promise<{ userId: string; status: string }> {
    const response = await adminApiInstance.post<ApiResponse<{ userId: string; status: string }>>(
      `/user/${userId}/unlock`
    )
    return response.data.data
  },

  // 1.8 重置用户密码
  async resetUserPassword(userId: string, newPassword: string): Promise<void> {
    await adminApiInstance.post<ApiResponse<null>>(`/user/${userId}/reset-password`, { newPassword })
  },

  // 1.9 获取用户统计信息
  async getUserStatistics(): Promise<UserStatistics> {
    const response = await adminApiInstance.get<ApiResponse<UserStatistics>>('/user/statistics')
    return response.data.data
  },

  // ==================== 虚拟机分配管理API ====================
  
  // 1.1 分配虚拟机给用户
  async assignVmToUser(vmId: string, userId: string, params: {
    permissions?: VmPermission[]
    notes?: string
  } = {}): Promise<VmAssignment> {
    const response = await adminApiInstance.post<ApiResponse<VmAssignment>>(
      `/vm/${vmId}/assign/${userId}`,
      params
    )
    return response.data.data
  },

  // 1.2 取消虚拟机分配
  async unassignVmFromUser(vmId: string, userId: string): Promise<{
    vmId: string
    userId: string
    unassignedAt: string
  }> {
    const response = await adminApiInstance.delete<ApiResponse<{
      vmId: string
      userId: string
      unassignedAt: string
    }>>(`/vm/${vmId}/assign/${userId}`)
    return response.data.data
  },

  // 1.3 查看虚拟机分配情况
  async getVmAssignments(vmId: string): Promise<{
    vmId: string
    vmName: string
    assignments: VmAssignmentDetail[]
    totalAssignments: number
  }> {
    const response = await adminApiInstance.get<ApiResponse<{
      vmId: string
      vmName: string
      assignments: VmAssignmentDetail[]
      totalAssignments: number
    }>>(`/vm/${vmId}/assignments`)
    return response.data.data
  },

  // 1.4 修改用户虚拟机权限
  async updateUserVmPermissions(vmId: string, userId: string, permissions: VmPermission[]): Promise<{
    vmId: string
    userId: string
    oldPermissions: VmPermission[]
    newPermissions: VmPermission[]
    updatedAt: string
  }> {
    const response = await adminApiInstance.put<ApiResponse<{
      vmId: string
      userId: string
      oldPermissions: VmPermission[]
      newPermissions: VmPermission[]
      updatedAt: string
    }>>(`/vm/${vmId}/assign/${userId}/permissions`, { permissions })
    return response.data.data
  },

  // ==================== 用户虚拟机管理API ====================
  
  // 2.1 查看用户的虚拟机列表
  async getUserVmList(userId: string, params: PaginationParams & {
    status?: VmStatus
  } = {}): Promise<PaginatedResponse<UserVm> & {
    userId: string
    username: string
  }> {
    const response = await adminApiInstance.get<ApiResponse<PaginatedResponse<UserVm> & {
      userId: string
      username: string
    }>>(`/user/${userId}/vms`, { params })
    return response.data.data
  },

  // 2.2 批量分配虚拟机给用户
  async batchAssignVmsToUser(userId: string, params: {
    vmIds: string[]
    permissions?: VmPermission[]
    notes?: string
  }): Promise<{
    userId: string
    successCount: number
    failedCount: number
    results: BatchOperationResult[]
  }> {
    const response = await adminApiInstance.post<ApiResponse<{
      userId: string
      successCount: number
      failedCount: number
      results: BatchOperationResult[]
    }>>(`/user/${userId}/vm/batch-assign`, params)
    return response.data.data
  },

  // 2.3 批量移除用户虚拟机权限
  async batchRemoveUserVms(userId: string, params: {
    vmIds: string[]
  }): Promise<{
    userId: string
    successCount: number
    failedCount: number
    results: BatchOperationResult[]
  }> {
    const response = await adminApiInstance.delete<ApiResponse<{
      userId: string
      successCount: number
      failedCount: number
      results: BatchOperationResult[]
    }>>(`/user/${userId}/vm/batch-remove`, { data: params })
    return response.data.data
  },

  // ==================== 管理员虚拟机管理视图API ====================
  
  // 3.1 管理员查看所有虚拟机
  async getAdminVmList(params: PaginationParams & {
    status?: VmStatus
    assigned?: boolean
    keyword?: string
  } = {}): Promise<PaginatedResponse<VirtualMachine>> {
    const response = await adminApiInstance.get<ApiResponse<PaginatedResponse<VirtualMachine>>>('/vm/list', { params })
    return response.data.data
  },

  // 3.2 查看未分配虚拟机列表
  async getUnassignedVmList(params: {
    status?: VmStatus
  } = {}): Promise<{
    unassignedVms: VirtualMachine[]
    total: number
  }> {
    const response = await adminApiInstance.get<ApiResponse<{
      unassignedVms: VirtualMachine[]
      total: number
    }>>('/vm/unassigned', { params })
    return response.data.data
  },

  // 3.3 虚拟机分配概况
  async getVmAssignmentOverview(): Promise<VmAssignmentOverview> {
    const response = await adminApiInstance.get<ApiResponse<VmAssignmentOverview>>('/vm/assignments/overview')
    return response.data.data
  },

  // 3.4 管理员强制控制虚拟机
  async forceControlVm(vmId: string, params: {
    action: VmControlAction
    reason: string
    timeout?: number
  }): Promise<VmControlResponse> {
    const response = await adminApiInstance.post<ApiResponse<VmControlResponse>>(
      `/vm/${vmId}/force-control`,
      params
    )
    return response.data.data
  },
} as const

// 使用命名导出以保持一致性
