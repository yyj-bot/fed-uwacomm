/**
 * 虚拟机API Mock数据
 * 提供与接口文档一致的Mock数据，用于开发和测试
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { ApiResponse } from '@/types'
import type { 
  VirtualMachine, 
  VMStatus
} from '@/api/vm'

// ==================== Mock数据生成工具 ====================

/**
 * 生成随机虚拟机ID（32位十六进制）
 */
const generateVMId = (): string => {
  return Array.from({ length: 32 }, () => 
    Math.floor(Math.random() * 16).toString(16)
  ).join('')
}

/**
 * 生成随机时间戳
 */
const generateTimestamp = (daysAgo: number = 0): string => {
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString()
}

/**
 * 生成随机IP地址
 */
const generateIPAddress = (): string => {
  return `192.168.1.${Math.floor(Math.random() * 254) + 1}`
}

/**
 * 生成随机MAC地址
 */
const generateMACAddress = (): string => {
  return Array.from({ length: 6 }, () => 
    Math.floor(Math.random() * 256).toString(16).padStart(2, '0')
  ).join(':')
}



// ==================== Mock虚拟机数据 ====================

/**
 * Mock虚拟机列表
 */
export const mockVMs: VirtualMachine[] = [
  {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    name: '水声联邦学习节点-001',
    ipAddress: '192.168.1.100',
    port: 22,
    status: 'RUNNING',
    osType: 'Ubuntu 20.04',
    cpuCores: 4,
    memoryMb: 8192,
    diskGb: 100,
    connectionStatus: 'CONNECTED',
    lastHeartbeat: generateTimestamp(0),
    wsSessionId: 'session-123456',
    createdAt: '2024-01-01T00:00:00.000Z',
    updatedAt: generateTimestamp(0),
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
      uploadSpeed: 100,
      downloadSpeed: 200,
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
    vmId: 'b2c3d4e5f6789012345678901234567a',
    name: '水声联邦学习节点-002',
    ipAddress: '192.168.1.101',
    port: 22,
    status: 'STOPPED',
    osType: 'Ubuntu 22.04',
    cpuCores: 8,
    memoryMb: 16384,
    diskGb: 200,
    connectionStatus: 'DISCONNECTED',
    lastHeartbeat: generateTimestamp(1),
    wsSessionId: 'session-123457',
    createdAt: '2024-01-02T00:00:00.000Z',
    updatedAt: generateTimestamp(1),
    systemInfo: {
      os: 'Ubuntu 22.04 LTS',
      kernel: '5.15.0-56-generic',
      python: '3.10.6',
      gpu: 'NVIDIA RTX 3090',
      cuda: '11.8',
      cudnn: '8.6.0'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG', 'FEDPROX', 'SCAFFOLD'],
      maxBatchSize: 256,
      maxMemoryUsage: 12288,
      gpuMemory: 24576,
      networkSpeed: 1000
    },
    networkConfig: {
      uploadSpeed: 200,
      downloadSpeed: 400,
      latency: 30,
      bandwidth: 1000
    },
    metadata: {
      description: '高性能GPU计算节点',
      location: '实验室B-机架02',
      owner: '李四',
      department: '水声工程学院',
      tags: ['水声', '联邦学习', 'GPU节点', '高性能']
    }
  },
  {
    vmId: 'c3d4e5f67890123456789012345678ab',
    name: '水声联邦学习节点-003',
    ipAddress: '192.168.1.102',
    port: 2222,
    status: 'ERROR',
    osType: 'CentOS 7',
    cpuCores: 2,
    memoryMb: 4096,
    diskGb: 50,
    connectionStatus: 'DISCONNECTED',
    lastHeartbeat: generateTimestamp(3),
    wsSessionId: undefined,
    createdAt: '2024-01-03T00:00:00.000Z',
    updatedAt: generateTimestamp(3),
    systemInfo: {
      os: 'CentOS Linux 7',
      kernel: '3.10.0-1160.el7.x86_64',
      python: '3.6.8'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG'],
      maxBatchSize: 64,
      maxMemoryUsage: 2048,
      networkSpeed: 100
    }
  }
]

// ==================== Mock API响应 ====================

/**
 * 创建成功响应
 */
export const createSuccessResponse = <T>(data: T, message: string = '操作成功'): ApiResponse<T> => ({
  code: 200,
  message,
  data
})

/**
 * 创建错误响应
 */
export const createErrorResponse = <T = null>(code: number, message: string, data?: T): ApiResponse<T> => ({
  code,
  message,
  data: data || null as T
})

// ==================== Mock服务方法 ====================

/**
 * Mock虚拟机API服务
 */
export const mockVMApi = {

  /**
   * Mock获取虚拟机列表
   */
  getVMList: (params: {
    page?: number
    size?: number
    status?: string
    osType?: string
    keyword?: string
  } = {}): ApiResponse<{
    total: number
    page: number
    size: number
    pages: number
    list: VirtualMachine[]
  }> => {
    const { page = 1, size = 20, status, osType, keyword } = params
    
    let filteredVMs = [...mockVMs]
    
    // 应用过滤条件
    if (status) {
      filteredVMs = filteredVMs.filter(vm => vm.status === status)
    }
    if (osType) {
      filteredVMs = filteredVMs.filter(vm => vm.osType.includes(osType))
    }
    if (keyword) {
      filteredVMs = filteredVMs.filter(vm => 
        vm.name.toLowerCase().includes(keyword.toLowerCase()) ||
        vm.ipAddress.includes(keyword)
      )
    }
    
    // 分页处理
    const total = filteredVMs.length
    const startIndex = (page - 1) * size
    const endIndex = startIndex + size
    const list = filteredVMs.slice(startIndex, endIndex)
    
    return createSuccessResponse({
      total,
      page,
      size,
      pages: Math.ceil(total / size),
      list
    }, '查询成功')
  },

  /**
   * Mock获取虚拟机详情
   */
  getVMDetail: (vmId: string): ApiResponse<VirtualMachine> => {
    const vm = mockVMs.find(vm => vm.vmId === vmId)
    if (!vm) {
      return createErrorResponse<VirtualMachine>(404, '虚拟机不存在')
    }
    return createSuccessResponse(vm, '查询成功')
  },

  /**
   * Mock更新虚拟机
   */
  updateVM: (vmId: string, vmData: any): ApiResponse<{
    vmId: string
    name: string
    updatedAt: string
  }> => {
    const vmIndex = mockVMs.findIndex(vm => vm.vmId === vmId)
    if (vmIndex === -1) {
      return createErrorResponse(404, '虚拟机不存在')
    }

    const updatedVM = {
      ...mockVMs[vmIndex],
      ...vmData,
      updatedAt: generateTimestamp()
    }

    mockVMs[vmIndex] = updatedVM

    return createSuccessResponse({
      vmId,
      name: updatedVM.name,
      updatedAt: updatedVM.updatedAt
    }, '虚拟机更新成功')
  },

  /**
   * Mock删除虚拟机
   */
  deleteVM: (vmId: string, force?: boolean): ApiResponse<{
    vmId: string
    deletedAt: string
  }> => {
    const vmIndex = mockVMs.findIndex(vm => vm.vmId === vmId)
    if (vmIndex === -1) {
      return createErrorResponse(404, '虚拟机不存在', null as any)
    }

    const vm = mockVMs[vmIndex]
    
    // 如果虚拟机正在运行且不是强制删除，返回错误
    if (vm.status === 'RUNNING' && !force) {
      return createErrorResponse(400, '无法删除正在运行的虚拟机，请先停止或使用强制删除', null as any)
    }

    mockVMs.splice(vmIndex, 1)

    return createSuccessResponse({
      vmId,
      deletedAt: generateTimestamp()
    }, '虚拟机删除成功')
  },

  /**
   * Mock启动虚拟机
   */
  startVM: (vmId: string, startData?: any): ApiResponse<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> => {
    const vmIndex = mockVMs.findIndex(vm => vm.vmId === vmId)
    if (vmIndex === -1) {
      return createErrorResponse(404, '虚拟机不存在')
    }

    const vm = mockVMs[vmIndex]
    if (vm.status === 'RUNNING') {
      return createErrorResponse(400, '虚拟机已在运行状态')
    }

    // 更新虚拟机状态
    mockVMs[vmIndex] = {
      ...vm,
      status: 'STARTING',
      updatedAt: generateTimestamp()
    }

    return createSuccessResponse({
      vmId,
      status: 'STARTING',
      commandId: `cmd-${Date.now()}`,
      estimatedTime: 60
    }, '虚拟机启动命令已发送')
  },

  /**
   * Mock停止虚拟机
   */
  stopVM: (vmId: string, stopData?: any): ApiResponse<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> => {
    const vmIndex = mockVMs.findIndex(vm => vm.vmId === vmId)
    if (vmIndex === -1) {
      return createErrorResponse(404, '虚拟机不存在')
    }

    const vm = mockVMs[vmIndex]
    if (vm.status === 'STOPPED') {
      return createErrorResponse(400, '虚拟机已在停止状态')
    }

    // 更新虚拟机状态
    mockVMs[vmIndex] = {
      ...vm,
      status: 'STOPPING',
      updatedAt: generateTimestamp()
    }

    return createSuccessResponse({
      vmId,
      status: 'STOPPING',
      commandId: `cmd-${Date.now()}`,
      estimatedTime: 30
    }, '虚拟机停止命令已发送')
  },

  /**
   * Mock重启虚拟机
   */
  restartVM: (vmId: string, restartData?: any): ApiResponse<{
    vmId: string
    status: string
    commandId: string
    estimatedTime: number
  }> => {
    const vmIndex = mockVMs.findIndex(vm => vm.vmId === vmId)
    if (vmIndex === -1) {
      return createErrorResponse(404, '虚拟机不存在')
    }

    const vm = mockVMs[vmIndex]

    // 更新虚拟机状态
    mockVMs[vmIndex] = {
      ...vm,
      status: 'STARTING',
      updatedAt: generateTimestamp()
    }

    return createSuccessResponse({
      vmId,
      status: 'STARTING',
      commandId: `cmd-${Date.now()}`,
      estimatedTime: 120
    }, '虚拟机重启命令已发送')
  },

  /**
   * Mock获取虚拟机状态
   */
  getVMStatus: (vmId: string): ApiResponse<VMStatus> => {
    const vm = mockVMs.find(vm => vm.vmId === vmId)
    if (!vm) {
      return createErrorResponse<VMStatus>(404, '虚拟机不存在')
    }

    const status: VMStatus = {
      vmId,
      status: vm.status,
      connectionStatus: vm.connectionStatus,
      uptime: vm.status === 'RUNNING' ? 3600 : undefined,
      resourceUsage: vm.status === 'RUNNING' ? {
        cpu: Math.random() * 100,
        memory: Math.random() * 100,
        disk: Math.random() * 100,
        gpu: Math.random() * 100
      } : undefined,
      network: {
        ipAddress: vm.ipAddress,
        macAddress: generateMACAddress(),
        port: vm.port,
        uploadSpeed: Math.floor(Math.random() * 2048),
        downloadSpeed: Math.floor(Math.random() * 4096),
        latency: Math.floor(Math.random() * 100)
      },
      processes: vm.status === 'RUNNING' ? {
        total: Math.floor(Math.random() * 200) + 50,
        active: Math.floor(Math.random() * 50) + 10,
        system: Math.floor(Math.random() * 20) + 5,
        user: Math.floor(Math.random() * 30) + 5
      } : undefined,
      lastHeartbeat: vm.lastHeartbeat,
      wsSessionId: vm.wsSessionId
    }

    return createSuccessResponse(status, '查询成功')
  }
}

// ==================== 导出Mock数据 ====================

export default mockVMApi
