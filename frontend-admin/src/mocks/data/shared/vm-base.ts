/**
 * VM基础数据 - 单一数据源
 * 所有VM相关的mock数据都应该引用这里的基础数据
 * 
 * 用途：
 * - vmApiMockData.ts (GET /api/vm/list, GET /api/vm/{id})
 * - adminVmMockData.ts (GET /api/admin/vm/list, GET /api/admin/vm/unassigned)
 * - federatedTaskMockData.ts (mockAvailableVMs)
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

export type VmStatus = 'RUNNING' | 'STOPPED' | 'ERROR' | 'STARTING' | 'STOPPING' | 'OFFLINE'
export type VmConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING'

export interface BaseVM {
  vmId: string
  name: string
  ipAddress: string
  port: number
  status: VmStatus
  connectionStatus: VmConnectionStatus
  osType: string
  cpuCores: number
  memoryMb: number
  diskGb: number
  createdAt: string
  updatedAt: string
  lastHeartbeat: string
  systemInfo?: {
    os: string
    kernel: string
    python: string
    gpu?: string
    cuda?: string
    cudnn?: string
  }
  capabilities?: {
    supportedAlgorithms: string[]
    maxBatchSize: number
    maxMemoryUsage: number
    gpuMemory?: number
    networkSpeed: number
  }
  networkConfig?: {
    uploadSpeed: number
    downloadSpeed: number
    latency: number
    bandwidth: number
  }
  metadata?: {
    description: string
    location: string
    owner: string
    department: string
    tags: string[]
  }
}

/**
 * 基础VM列表 - 系统中所有虚拟机的单一数据源
 * 
 * 注意：修改这里的数据会影响所有使用VM数据的地方
 */
export const baseVmList: BaseVM[] = [
  {
    vmId: 'a1b2c3d4e5f678901234567890123456',
    name: '水声联邦学习节点-001',
    ipAddress: '192.168.1.100',
    port: 22,
    status: 'RUNNING',
    connectionStatus: 'CONNECTED',
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
    status: 'RUNNING',
    connectionStatus: 'CONNECTED',
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
    status: 'STOPPED',
    connectionStatus: 'DISCONNECTED',
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
      python: '3.6.8'
    },
    capabilities: {
      supportedAlgorithms: ['FEDAVG'],
      maxBatchSize: 32,
      maxMemoryUsage: 2048,
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
 * 通过vmId获取VM数据
 */
export const getVmById = (vmId: string): BaseVM | undefined => {
  return baseVmList.find(vm => vm.vmId === vmId)
}

/**
 * 获取所有运行中的VM
 */
export const getRunningVms = (): BaseVM[] => {
  return baseVmList.filter(vm => vm.status === 'RUNNING')
}

/**
 * 获取所有停止的VM
 */
export const getStoppedVms = (): BaseVM[] => {
  return baseVmList.filter(vm => vm.status === 'STOPPED')
}

