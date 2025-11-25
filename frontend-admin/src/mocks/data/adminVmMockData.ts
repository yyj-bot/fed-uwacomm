/**
 * 管理员水下机器人管理 Mock 数据
 * 基于 admin-vm-api-reference.md 文档
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { baseVmList, type BaseVM } from './shared/vm-base'
import { baseUserList, getUserById } from './shared/user-base'

// ==================== 类型定义 ====================

type VmPermission = 'READ' | 'WRITE' | 'EXECUTE' | 'ADMIN'
type VmStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING'
type VmConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'
type VmControlAction = 'START' | 'STOP' | 'RESTART' | 'FORCE_STOP'

// ==================== Mock 数据 ====================

/**
 * VM分配概况数据
 * 注意：部分数据（如 summary）会在 handlers 中动态计算
 * 这里只提供静态的 topAssignedVms 和 recentAssignments
 */
export const mockVmAssignmentOverview = {
  summary: {
    totalVms: 5,
    assignedVms: 1,
    unassignedVms: 4,
    totalUsers: baseUserList.length, // 从用户数据源动态获取
    usersWithVms: 4
  },
  statusDistribution: {
    RUNNING: 4,
    STOPPED: 1,
    ERROR: 0,
    STARTING: 0,
    STOPPING: 0
  },
  topAssignedVms: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      assignedUserCount: 4
    }
  ],
  recentAssignments: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      userId: 'operator-001',
      username: getUserById('operator-001')?.username || 'operator01',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString()
    },
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      userId: 'researcher-002',
      username: getUserById('researcher-002')?.username || 'researcher02',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString()
    },
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      userId: 'researcher-001',
      username: getUserById('researcher-001')?.username || 'researcher01',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3).toISOString()
    }
  ]
}

/**
 * VM分配详情数据（按vmId索引）
 * 数据来源：用户信息从 baseUserList 动态获取
 */
export const mockVmAssignments: Record<string, any> = {
  'a1b2c3d4e5f678901234567890123456': {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    vmName: 'AUV-01',
    assignments: [
      {
        userId: 'a1b2c3d4e5f678901234567890123456',
        username: getUserById('a1b2c3d4e5f678901234567890123456')?.username || 'admin',
        email: getUserById('a1b2c3d4e5f678901234567890123456')?.email || 'admin@feduwacomm.com',
        permissions: ['READ', 'WRITE', 'EXECUTE', 'ADMIN'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 5).toISOString(),
        assignedBy: 'system'
      },
      {
        userId: 'researcher-001',
        username: getUserById('researcher-001')?.username || 'researcher01',
        email: getUserById('researcher-001')?.email || 'researcher01@example.com',
        permissions: ['READ', 'WRITE', 'EXECUTE'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3).toISOString(),
        assignedBy: 'admin'
      },
      {
        userId: 'researcher-002',
        username: getUserById('researcher-002')?.username || 'researcher02',
        email: getUserById('researcher-002')?.email || 'researcher02@example.com',
        permissions: ['READ', 'WRITE'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(),
        assignedBy: 'admin'
      },
      {
        userId: 'operator-001',
        username: getUserById('operator-001')?.username || 'operator01',
        email: getUserById('operator-001')?.email || 'operator01@example.com',
        permissions: ['READ', 'EXECUTE'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
        assignedBy: 'admin'
      }
    ],
    totalAssignments: 4
  },
  // 其他VM的分配记录（使用真实的vmId）
  'b2c3d4e5f67890123456789012345678': {
    vmId: 'b2c3d4e5f67890123456789012345678',
    vmName: 'AUV-02',
    assignments: [],
    totalAssignments: 0
  },
  'c3d4e5f678901234567890123456789a': {
    vmId: 'c3d4e5f678901234567890123456789a',
    vmName: 'AUV-03',
    assignments: [],
    totalAssignments: 0
  },
  'd4e5f6789012345678901234567890ab': {
    vmId: 'd4e5f6789012345678901234567890ab',
    vmName: 'ROV-01',
    assignments: [],
    totalAssignments: 0
  },
  'e5f67890123456789012345678901bc': {
    vmId: 'e5f67890123456789012345678901bc',
    vmName: 'ROV-02',
    assignments: [],
    totalAssignments: 0
  }
}

/**
 * 未分配的VM列表
 * 严格按照 admin-vm-api-reference.md 3.2 接口定义
 * 
 * 数据来源：baseVmList（单一数据源）
 * 过滤条件：未分配的VM
 */
export const mockUnassignedVms = baseVmList.filter(vm => {
  const unassignedVmIds = ['b2c3d4e5f67890123456789012345678', 'c3d4e5f678901234567890123456789a', 'd4e5f6789012345678901234567890ab', 'e5f67890123456789012345678901bc']
  return unassignedVmIds.includes(vm.vmId)
})

/**
 * 所有VM列表（管理员视图）
 * 严格按照 admin-vm-api-reference.md 3.1 接口定义
 * 
 * 数据来源：baseVmList（单一数据源）
 * 管理员特有字段：isAssigned, assignedUserCount
 */
export const mockAllAdminVms = baseVmList.map(vm => {
  // 根据vmId确定分配状态
  const assignmentInfo = {
    'a1b2c3d4e5f678901234567890123456': { isAssigned: true, assignedUserCount: 4 },
    'b2c3d4e5f67890123456789012345678': { isAssigned: false, assignedUserCount: 0 },
    'c3d4e5f678901234567890123456789a': { isAssigned: false, assignedUserCount: 0 },
    'd4e5f6789012345678901234567890ab': { isAssigned: false, assignedUserCount: 0 },
    'e5f67890123456789012345678901bc': { isAssigned: false, assignedUserCount: 0 }
  }

  const info = assignmentInfo[vm.vmId as keyof typeof assignmentInfo] || { isAssigned: false, assignedUserCount: 0 }

  return {
    ...vm,
    isAssigned: info.isAssigned,
    assignedUserCount: info.assignedUserCount,
    batteryLevel: vm.batteryLevel
  }
})

/**
 * 用户VM列表数据（按userId索引）
 */
export const mockUserVms: Record<string, any[]> = {
  'user-001': [
    {
      vmId: 'vm-001',
      vmName: 'AUV-01',
      ipAddress: '192.168.1.101',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE', 'EXECUTE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(),
      batteryLevel: 92
    },
    {
      vmId: 'vm-002',
      vmName: 'AUV-02',
      ipAddress: '192.168.1.102',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString(),
      batteryLevel: 86
    }
  ],
  // admin用户的VM列表（userId: a1b2c3d4e5f678901234567890123456）
  'a1b2c3d4e5f678901234567890123456': [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      ipAddress: '192.168.1.100',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE', 'EXECUTE', 'ADMIN'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 5).toISOString(),
      batteryLevel: 92
    }
  ],
  // researcher01用户的VM列表（userId: researcher-001）
  'researcher-001': [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      ipAddress: '192.168.1.100',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE', 'EXECUTE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3).toISOString()
    }
  ],
  // researcher02用户的VM列表（userId: researcher-002）
  'researcher-002': [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      ipAddress: '192.168.1.100',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString()
    }
  ],
  // operator01用户的VM列表（userId: operator-001）
  'operator-001': [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: 'AUV-01',
      ipAddress: '192.168.1.100',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'EXECUTE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString()
    }
  ]
}

// ==================== API Mock 响应 ====================

export const adminVmApiMock = {
  // 3.3 水下机器人分配概况
  assignmentOverview: {
    success: {
      code: 200,
      message: '获取成功',
      data: mockVmAssignmentOverview
    },
    error401: {
      code: 401,
      message: '认证失败',
      data: null
    },
    error403: {
      code: 403,
      message: '权限不足，需要管理员权限',
      data: null
    },
    error500: {
      code: 500,
      message: '服务器内部错误',
      data: {
        error: '数据库查询失败',
        requestId: 'req-overview-123456'
      }
    }
  },

  // 1.1 分配水下机器人给用户
  assignVm: {
    success: {
      code: 200,
      message: '水下机器人分配成功',
      data: {
        vmId: 'vm-001',
        userId: 'user-001',
        permissions: ['READ', 'WRITE'],
        assignedAt: new Date().toISOString(),
        assignedBy: 'admin',
        notes: '分配给研究人员'
      }
    },
    error400: {
      code: 400,
      message: '请求参数错误',
      data: {
        errors: [
          { field: 'permissions', message: '权限列表不能为空' }
        ]
      }
    },
    error404: {
      code: 404,
      message: '水下机器人或用户不存在',
      data: null
    },
    error409: {
      code: 409,
      message: '水下机器人已分配给该用户',
      data: {
        vmId: 'vm-001',
        userId: 'user-001',
        existingAssignment: true
      }
    }
  },

  // 1.2 取消水下机器人分配
  unassignVm: {
    success: {
      code: 200,
      message: '取消分配成功',
      data: {
        vmId: 'vm-001',
        userId: 'user-001',
        unassignedAt: new Date().toISOString()
      }
    },
    error404: {
      code: 404,
      message: '分配记录不存在',
      data: null
    }
  },

  // 1.4 修改用户水下机器人权限
  updatePermissions: {
    success: {
      code: 200,
      message: '权限更新成功',
      data: {
        vmId: 'vm-001',
        userId: 'user-001',
        oldPermissions: ['READ'],
        newPermissions: ['READ', 'WRITE', 'EXECUTE'],
        updatedAt: new Date().toISOString()
      }
    },
    error400: {
      code: 400,
      message: '权限参数无效',
      data: {
        errors: [
          { field: 'permissions', message: '权限列表不能为空' }
        ]
      }
    }
  },

  // 2.2 批量分配水下机器人给用户
  batchAssign: {
    success: {
      code: 200,
      message: '批量分配成功',
      data: {
        userId: 'user-001',
        successCount: 2,
        failedCount: 0,
        results: [
          {
            vmId: 'vm-001',
            status: 'SUCCESS',
            assignedAt: new Date().toISOString()
          },
          {
            vmId: 'vm-002',
            status: 'SUCCESS',
            assignedAt: new Date().toISOString()
          }
        ]
      }
    },
    partialSuccess: {
      code: 200,
      message: '批量分配部分成功',
      data: {
        userId: 'user-001',
        successCount: 1,
        failedCount: 1,
        results: [
          {
            vmId: 'vm-001',
            status: 'SUCCESS',
            assignedAt: new Date().toISOString()
          },
          {
            vmId: 'vm-002',
            status: 'FAILED',
            error: '水下机器人已分配给其他用户'
          }
        ]
      }
    }
  },

  // 3.4 管理员强制控制水下机器人
  forceControl: {
    success: {
      code: 200,
      message: '强制控制命令已发送',
      data: {
        vmId: 'vm-001',
        action: 'START' as VmControlAction,
        commandId: `cmd-admin-${Date.now()}`,
        reason: '系统维护需要',
        executedBy: 'admin',
        executedAt: new Date().toISOString(),
        estimatedTime: 60
      }
    },
    error400: {
      code: 400,
      message: '操作原因不能为空',
      data: {
        errors: [
          { field: 'reason', message: '必须提供强制控制的原因' }
        ]
      }
    },
    error404: {
      code: 404,
      message: '水下机器人不存在',
      data: null
    }
  }
}

