/**
 * VM Store Mock 数据
 * 用于测试虚拟机管理相关功能的模拟数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import type { VirtualMachine, VMStatus } from '@/api/vm'
import type { VMListParams, VMUpdateRequest, VMStartRequest, VMStopRequest } from '@/services'

// ==================== 模拟虚拟机数据 ====================

export const mockVM1: VirtualMachine = {
  vmId: 'vm-001',
  name: 'Ubuntu-Server-01',
  ipAddress: '192.168.1.100',
  port: 22,
  status: 'RUNNING',
  osType: 'Ubuntu 20.04',
  cpuCores: 4,
  memoryMb: 8192,
  diskGb: 100,
  connectionStatus: 'CONNECTED',
  lastHeartbeat: '2024-01-15T08:00:00Z',
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-15T08:00:00Z',
  systemInfo: {
    os: 'Ubuntu 20.04',
    kernel: '5.4.0',
    python: '3.8.10',
    gpu: 'NVIDIA GTX 1080',
    cuda: '11.2',
    cudnn: '8.1'
  },
  capabilities: {
    supportedAlgorithms: ['FedAvg', 'FedProx'],
    maxBatchSize: 128,
    maxMemoryUsage: 8000,
    gpuMemory: 8192,
    networkSpeed: 1000
  }
}

export const mockVM2: VirtualMachine = {
  vmId: 'vm-002',
  name: 'Windows-Dev-01',
  ipAddress: '192.168.1.101',
  port: 3389,
  status: 'STOPPED',
  osType: 'Windows 10',
  cpuCores: 2,
  memoryMb: 4096,
  diskGb: 50,
  connectionStatus: 'DISCONNECTED',
  createdAt: '2024-01-02T00:00:00Z',
  updatedAt: '2024-01-10T10:00:00Z',
  systemInfo: {
    os: 'Windows 10',
    kernel: 'NT 10.0',
    python: '3.9.7'
  }
}

export const mockVM3: VirtualMachine = {
  vmId: 'vm-003',
  name: 'CentOS-Web-01',
  ipAddress: '192.168.1.102',
  port: 80,
  status: 'ERROR',
  osType: 'CentOS 7',
  cpuCores: 2,
  memoryMb: 2048,
  diskGb: 30,
  connectionStatus: 'DISCONNECTED',
  createdAt: '2024-01-03T00:00:00Z',
  updatedAt: '2024-01-14T16:00:00Z',
  systemInfo: {
    os: 'CentOS 7',
    kernel: '3.10.0',
    python: '3.6.8'
  }
}

export const mockVMList = [mockVM1, mockVM2, mockVM3]

// ==================== 模拟虚拟机状态 ====================

export const mockVMStatus1: VMStatus = {
  vmId: 'vm-001',
  status: 'RUNNING',
  connectionStatus: 'CONNECTED',
  uptime: 3600000, // 1小时
  resourceUsage: {
    cpu: 45.5,
    memory: 60.2,
    disk: 35.8,
    gpu: 30.0
  },
  network: {
    ipAddress: '192.168.1.100',
    macAddress: '00:1B:44:11:3A:B7',
    port: 22,
    uploadSpeed: 500,
    downloadSpeed: 1000,
    latency: 10
  },
  processes: {
    total: 156,
    active: 25,
    system: 50,
    user: 106
  },
  lastHeartbeat: '2024-01-15T09:00:00Z',
  wsSessionId: 'session-001'
}

export const mockVMStatus2: VMStatus = {
  vmId: 'vm-002',
  status: 'STOPPED',
  connectionStatus: 'DISCONNECTED',
  uptime: 0,
  resourceUsage: {
    cpu: 0,
    memory: 0,
    disk: 25.3
  },
  network: {
    ipAddress: '192.168.1.101',
    macAddress: '00:1B:44:11:3A:B8',
    port: 3389,
    uploadSpeed: 0,
    downloadSpeed: 0,
    latency: 0
  },
  processes: {
    total: 0,
    active: 0,
    system: 0,
    user: 0
  },
  lastHeartbeat: '2024-01-15T08:00:00Z'
}

export const mockVMStatus3: VMStatus = {
  vmId: 'vm-003',
  status: 'ERROR',
  connectionStatus: 'DISCONNECTED',
  uptime: 0,
  resourceUsage: {
    cpu: 0,
    memory: 0,
    disk: 45.7
  },
  network: {
    ipAddress: '192.168.1.102',
    macAddress: '00:1B:44:11:3A:B9',
    port: 80,
    uploadSpeed: 0,
    downloadSpeed: 0,
    latency: 0
  },
  processes: {
    total: 0,
    active: 0,
    system: 0,
    user: 0
  },
  lastHeartbeat: '2024-01-15T07:00:00Z'
}

export const mockVMStatusMap = {
  'vm-001': mockVMStatus1,
  'vm-002': mockVMStatus2,
  'vm-003': mockVMStatus3
}

// ==================== 模拟请求数据 ====================

export const mockVMListParams: VMListParams = {
  page: 1,
  size: 20,
  status: undefined,
  osType: undefined,
  keyword: undefined
}

export const mockVMUpdateRequest: VMUpdateRequest = {
  name: '更新后的虚拟机名称',
  ipAddress: '192.168.1.200',
  port: 22,
  osType: 'Ubuntu 22.04',
  cpuCores: 8,
  memoryMb: 16384,
  diskGb: 200,
  metadata: {
    description: '更新后的描述',
    tags: ['更新', '测试']
  }
}

export const mockVMStartRequest: VMStartRequest = {
  timeout: 300,
  config: {
    memory: '8G',
    cpu: '4',
    disk: '100G',
    network: 'bridge'
  },
  environment: {
    variables: {
      'PYTHON_ENV': 'production',
      'CUDA_VISIBLE_DEVICES': '0'
    }
  }
}

export const mockVMStopRequest: VMStopRequest = {
  force: false,
  saveState: true
}

// ==================== 模拟响应数据 ====================

export const mockVMListResponse = {
  list: mockVMList,
  total: 3,
  page: 1,
  size: 20,
  pages: 1
}

// ==================== 模拟错误响应 ====================

export const mockVMErrors = {
  VM_NOT_FOUND: new Error('虚拟机不存在'),
  VM_ALREADY_RUNNING: new Error('虚拟机已在运行'),
  VM_ALREADY_STOPPED: new Error('虚拟机已停止'),
  INSUFFICIENT_RESOURCES: new Error('系统资源不足'),
  NETWORK_ERROR: new Error('网络连接失败'),
  PERMISSION_DENIED: new Error('权限不足'),
  VM_START_FAILED: new Error('虚拟机启动失败'),
  VM_STOP_FAILED: new Error('虚拟机停止失败'),
  VM_UPDATE_FAILED: new Error('更新虚拟机失败'),
  VM_DELETE_FAILED: new Error('删除虚拟机失败'),
  SERVER_ERROR: new Error('服务器内部错误')
}

// ==================== 模拟状态数据 ====================

export const mockInitialVMState = {
  vmList: [],
  vmListTotal: 0,
  vmListLoading: false,
  vmListError: null,
  currentVM: null,
  currentVMLoading: false,
  currentVMError: null,
  vmStatusMap: {},
  operationLoading: {},
  operationError: {},
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  queryParams: {}
}

export const mockLoadedVMState = {
  ...mockInitialVMState,
  vmList: mockVMList,
  vmListTotal: 3,
  vmStatusMap: mockVMStatusMap,
  pagination: {
    page: 1,
    size: 20,
    total: 3
  }
}

export const mockLoadingVMState = {
  ...mockInitialVMState,
  vmListLoading: true
}

export const mockErrorVMState = {
  ...mockInitialVMState,
  vmListError: '获取虚拟机列表失败'
}

export const mockCurrentVMState = {
  ...mockLoadedVMState,
  currentVM: mockVM1
}

export const mockOperationLoadingState = {
  ...mockLoadedVMState,
  operationLoading: {
    'start-vm-001': true
  }
}

export const mockOperationErrorState = {
  ...mockLoadedVMState,
  operationError: {
    'start-vm-001': '启动虚拟机失败'
  }
}

// ==================== 模拟工具函数 ====================

export const createMockVM = (overrides: Partial<VirtualMachine> = {}): VirtualMachine => ({
  ...mockVM1,
  ...overrides,
  vmId: overrides.vmId || `vm-${Date.now()}`
})

export const createMockVMStatus = (vmId: string, overrides: Partial<VMStatus> = {}): VMStatus => ({
  ...mockVMStatus1,
  vmId,
  ...overrides
})

export const createMockVMListResponse = (vms: VirtualMachine[] = mockVMList) => ({
  list: vms,
  total: vms.length,
  page: 1,
  size: 20,
  totalPages: Math.ceil(vms.length / 20)
})

// ==================== 导出默认 Mock ====================
export default {
  mockVM1,
  mockVM2,
  mockVM3,
  mockVMList,
  mockVMStatus1,
  mockVMStatus2,
  mockVMStatus3,
  mockVMStatusMap,
  mockVMListParams,
  mockVMUpdateRequest,
  mockVMStartRequest,
  mockVMStopRequest,
  mockVMListResponse,
  mockVMErrors,
  mockInitialVMState,
  mockLoadedVMState,
  mockLoadingVMState,
  mockErrorVMState,
  mockCurrentVMState,
  mockOperationLoadingState,
  mockOperationErrorState,
  createMockVM,
  createMockVMStatus,
  createMockVMListResponse
}
