/**
 * 管理员虚拟机管理 Mock 数据
 * 基于 admin-vm-api-reference.md 文档
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

// ==================== 类型定义 ====================

type VmPermission = 'READ' | 'WRITE' | 'EXECUTE' | 'ADMIN'
type VmStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING'
type VmConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'
type VmControlAction = 'START' | 'STOP' | 'RESTART' | 'FORCE_STOP'

// ==================== Mock 数据 ====================

/**
 * VM分配概况数据
 */
export const mockVmAssignmentOverview = {
  summary: {
    totalVms: 3,
    assignedVms: 1,
    unassignedVms: 2,
    totalUsers: 12,
    usersWithVms: 4
  },
  statusDistribution: {
    RUNNING: 1,
    STOPPED: 2,
    ERROR: 0,
    STARTING: 0,
    STOPPING: 0
  },
  topAssignedVms: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: '水声联邦学习节点-001',
      assignedUserCount: 4
    }
  ],
  recentAssignments: [
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: '水声联邦学习节点-001',
      userId: 'operator-001',
      username: 'operator01',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString()
    },
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: '水声联邦学习节点-001',
      userId: 'researcher-002',
      username: 'researcher02',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString()
    },
    {
      vmId: 'a1b2c3d4e5f678901234567890123456',
      vmName: '水声联邦学习节点-001',
      userId: 'researcher-001',
      username: 'researcher01',
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3).toISOString()
    }
  ]
}

/**
 * VM分配详情数据（按vmId索引）
 * 使用实际的 vmId: a1b2c3d4e5f678901234567890123456
 */
export const mockVmAssignments: Record<string, any> = {
  'a1b2c3d4e5f678901234567890123456': {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    vmName: '水声联邦学习节点-001',
    assignments: [
      {
        userId: 'a1b2c3d4e5f678901234567890123456',
        username: 'admin',
        email: 'admin@feduwacomm.com',
        permissions: ['READ', 'WRITE', 'EXECUTE', 'ADMIN'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 5).toISOString(),
        assignedBy: 'system'
      },
      {
        userId: 'researcher-001',
        username: 'researcher01',
        email: 'researcher01@example.com',
        permissions: ['READ', 'WRITE', 'EXECUTE'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 3).toISOString(),
        assignedBy: 'admin'
      },
      {
        userId: 'researcher-002',
        username: 'researcher02',
        email: 'researcher02@example.com',
        permissions: ['READ', 'WRITE'],
        assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(),
        assignedBy: 'admin'
      },
      {
        userId: 'operator-001',
        username: 'operator01',
        email: 'operator01@example.com',
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
    vmName: '水声联邦学习节点-002',
    assignments: [],
    totalAssignments: 0
  },
  'c3d4e5f67890123456789012345678901': {
    vmId: 'c3d4e5f67890123456789012345678901',
    vmName: '水声联邦学习节点-003',
    assignments: [],
    totalAssignments: 0
  }
}

/**
 * 未分配的VM列表
 * 严格按照 admin-vm-api-reference.md 3.2 接口定义
 * 注意：为了支持详情页展示，额外添加了一些字段（port, cpuCores, memoryMb, diskGb, osType, updatedAt等）
 */
export const mockUnassignedVms = [
  {
    vmId: 'b2c3d4e5f67890123456789012345678',
    name: '水声联邦学习节点-002',
    ipAddress: '192.168.1.101',
    port: 22,
    status: 'RUNNING' as VmStatus,
    connectionStatus: 'CONNECTED' as VmConnectionStatus,
    osType: 'Ubuntu 22.04 LTS',
    cpuCores: 6,
    memoryMb: 12288,
    diskGb: 300,
    createdAt: '2025-10-10T09:15:00.000Z',
    updatedAt: new Date().toISOString(),
    lastHeartbeat: new Date().toISOString(),
    systemInfo: {
      os: 'Ubuntu 22.04 LTS',
      kernel: '5.15.0-56-generic',
      python: '3.10.6',
      gpu: 'NVIDIA RTX 3090',
      cuda: '11.7',
      cudnn: '8.4.1'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG', 'FEDPROX'],
      maxBatchSize: 64,
      maxMemoryUsage: 4096,
      gpuMemory: 24576,
      networkSpeed: 500
    },
    networkConfig: {
      uploadSpeed: 512,
      downloadSpeed: 1024,
      latency: 60,
      bandwidth: 500
    },
    metadata: {
      description: '水声联邦学习备用节点',
      location: '实验室B-机架02',
      owner: '李四',
      department: '水声工程学院',
      tags: ['水声', '联邦学习', '备用节点']
    }
  },
  {
    vmId: 'c3d4e5f67890123456789012345678901',
    name: '水声联邦学习节点-003',
    ipAddress: '192.168.1.102',
    port: 22,
    status: 'STOPPED' as VmStatus,
    connectionStatus: 'DISCONNECTED' as VmConnectionStatus,
    osType: 'CentOS 7.9',
    cpuCores: 4,
    memoryMb: 8192,
    diskGb: 200,
    createdAt: '2025-10-10T07:45:00.000Z',
    updatedAt: '2025-10-10T18:20:00.000Z',
    lastHeartbeat: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
    systemInfo: {
      os: 'CentOS 7.9',
      kernel: '3.10.0-1160.el7.x86_64',
      python: '3.6.8',
      gpu: undefined,
      cuda: undefined,
      cudnn: undefined
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG'],
      maxBatchSize: 32,
      maxMemoryUsage: 2048,
      gpuMemory: undefined,
      networkSpeed: 200
    },
    networkConfig: {
      uploadSpeed: 256,
      downloadSpeed: 512,
      latency: 80,
      bandwidth: 200
    },
    metadata: {
      description: 'CPU训练节点',
      location: '实验室C-机架03',
      owner: '王五',
      department: '水声工程学院',
      tags: ['水声', 'CPU节点']
    }
  }
]

/**
 * 所有VM列表（管理员视图）
 * 严格按照 admin-vm-api-reference.md 3.1 接口定义
 * 注意：为了支持详情页展示，额外添加了一些字段（port, cpuCores, memoryMb, diskGb, osType, updatedAt等）
 */
export const mockAllAdminVms = [
  {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    name: '水声联邦学习节点-001',
    ipAddress: '192.168.1.100',
    port: 22,
    status: 'RUNNING' as VmStatus,
    connectionStatus: 'CONNECTED' as VmConnectionStatus,
    isAssigned: true,
    assignedUserCount: 4,
    osType: 'Ubuntu 20.04 LTS',
    cpuCores: 8,
    memoryMb: 16384,
    diskGb: 500,
    createdAt: '2025-10-10T08:30:00.000Z',
    updatedAt: new Date().toISOString(),
    lastHeartbeat: new Date().toISOString(),
    systemInfo: {
      os: 'Ubuntu 20.04 LTS',
      kernel: '5.4.0-42-generic',
      python: '3.8.10',
      gpu: 'NVIDIA Tesla V100',
      cuda: '11.0',
      cudnn: '8.0.5'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG', 'FEDPROX', 'FEDNOVA', 'SCAFFOLD'],
      maxBatchSize: 128,
      maxMemoryUsage: 6144,
      gpuMemory: 16384,
      networkSpeed: 1000
    },
    networkConfig: {
      uploadSpeed: 1024,
      downloadSpeed: 2048,
      latency: 50,
      bandwidth: 1000
    },
    metadata: {
      description: '水声联邦学习专用虚拟机节点',
      location: '实验室A-机架01',
      owner: '张三',
      department: '水声工程学院',
      tags: ['水声', '联邦学习', 'GPU节点']
    }
  },
  {
    vmId: 'b2c3d4e5f67890123456789012345678',
    name: '水声联邦学习节点-002',
    ipAddress: '192.168.1.101',
    port: 22,
    status: 'RUNNING' as VmStatus,
    connectionStatus: 'CONNECTED' as VmConnectionStatus,
    isAssigned: false,
    assignedUserCount: 0,
    osType: 'Ubuntu 22.04 LTS',
    cpuCores: 6,
    memoryMb: 12288,
    diskGb: 300,
    createdAt: '2025-10-10T09:15:00.000Z',
    updatedAt: new Date().toISOString(),
    lastHeartbeat: new Date().toISOString(),
    systemInfo: {
      os: 'Ubuntu 22.04 LTS',
      kernel: '5.15.0-56-generic',
      python: '3.10.6',
      gpu: 'NVIDIA RTX 3090',
      cuda: '11.7',
      cudnn: '8.4.1'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG', 'FEDPROX'],
      maxBatchSize: 64,
      maxMemoryUsage: 4096,
      gpuMemory: 24576,
      networkSpeed: 500
    },
    networkConfig: {
      uploadSpeed: 512,
      downloadSpeed: 1024,
      latency: 60,
      bandwidth: 500
    },
    metadata: {
      description: '水声联邦学习备用节点',
      location: '实验室B-机架02',
      owner: '李四',
      department: '水声工程学院',
      tags: ['水声', '联邦学习', '备用节点']
    }
  },
  {
    vmId: 'c3d4e5f67890123456789012345678901',
    name: '水声联邦学习节点-003',
    ipAddress: '192.168.1.102',
    port: 22,
    status: 'STOPPED' as VmStatus,
    connectionStatus: 'DISCONNECTED' as VmConnectionStatus,
    isAssigned: false,
    assignedUserCount: 0,
    osType: 'CentOS 7.9',
    cpuCores: 4,
    memoryMb: 8192,
    diskGb: 200,
    createdAt: '2025-10-10T07:45:00.000Z',
    updatedAt: '2025-10-10T18:20:00.000Z',
    lastHeartbeat: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
    systemInfo: {
      os: 'CentOS 7.9',
      kernel: '3.10.0-1160.el7.x86_64',
      python: '3.6.8',
      gpu: undefined,
      cuda: undefined,
      cudnn: undefined
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG'],
      maxBatchSize: 32,
      maxMemoryUsage: 2048,
      gpuMemory: undefined,
      networkSpeed: 200
    },
    networkConfig: {
      uploadSpeed: 256,
      downloadSpeed: 512,
      latency: 80,
      bandwidth: 200
    },
    metadata: {
      description: 'CPU训练节点',
      location: '实验室C-机架03',
      owner: '王五',
      department: '水声工程学院',
      tags: ['水声', 'CPU节点']
    }
  }
]

/**
 * 用户VM列表数据（按userId索引）
 */
export const mockUserVms: Record<string, any[]> = {
  'user-001': [
    {
      vmId: 'vm-001',
      vmName: '水声联邦学习节点-001',
      ipAddress: '192.168.1.101',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE', 'EXECUTE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString()
    },
    {
      vmId: 'vm-002',
      vmName: '水声联邦学习节点-002',
      ipAddress: '192.168.1.102',
      status: 'RUNNING' as VmStatus,
      permissions: ['READ', 'WRITE'] as VmPermission[],
      assignedAt: new Date(Date.now() - 1000 * 60 * 60 * 12).toISOString()
    }
  ]
}

// ==================== API Mock 响应 ====================

export const adminVmApiMock = {
  // 3.3 虚拟机分配概况
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

  // 1.1 分配虚拟机给用户
  assignVm: {
    success: {
      code: 200,
      message: '虚拟机分配成功',
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
      message: '虚拟机或用户不存在',
      data: null
    },
    error409: {
      code: 409,
      message: '虚拟机已分配给该用户',
      data: {
        vmId: 'vm-001',
        userId: 'user-001',
        existingAssignment: true
      }
    }
  },

  // 1.2 取消虚拟机分配
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

  // 1.4 修改用户虚拟机权限
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

  // 2.2 批量分配虚拟机给用户
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
            error: '虚拟机已分配给其他用户'
          }
        ]
      }
    }
  },

  // 3.4 管理员强制控制虚拟机
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
      message: '虚拟机不存在',
      data: null
    }
  }
}

