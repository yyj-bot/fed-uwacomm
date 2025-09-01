/**
 * 虚拟机服务单元测试
 * 使用Vitest测试所有虚拟机服务接口方法
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { VMService } from '@/services/vm/vmService'
import { vmApi } from '@/api/vm'
import { mockVMs, mockVMApi } from '../mocks/vmMock'
import type { 
  VirtualMachine
} from '@/api/vm'
import type { 
  VMListParams,
  VMUpdateRequest,
  VMStartRequest,
  VMStopRequest,
  VMRestartRequest
} from '@/services/vm/type'

// Mock vmApi
vi.mock('@/api/vm', () => ({
  vmApi: {
    getVMList: vi.fn(),
    getVMDetail: vi.fn(),
    updateVM: vi.fn(),
    deleteVM: vi.fn(),
    startVM: vi.fn(),
    stopVM: vi.fn(),
    restartVM: vi.fn(),
    getVMStatus: vi.fn()
  }
}))

describe('VMService', () => {
  let vmService: VMService
  
  beforeEach(() => {
    vmService = new VMService()
    vi.clearAllMocks()
  })
  
  afterEach(() => {
    vi.restoreAllMocks()
  })



  describe('getVMList', () => {
    it('应该成功获取虚拟机列表', async () => {
      const params: VMListParams = { page: 1, size: 10 }
      const mockResponse = mockVMApi.getVMList(params).data
      
      // Mock API调用
      vi.mocked(vmApi.getVMList).mockResolvedValue(mockResponse)
      
      // 执行测试
      const result = await vmService.getVMList(params)
      
      // 验证结果
      expect(vmApi.getVMList).toHaveBeenCalledWith(params)
      expect(result.total).toBeGreaterThanOrEqual(0)
      expect(result.page).toBe(1)
      expect(result.size).toBe(10)
      expect(Array.isArray(result.list)).toBe(true)
    })

    it('应该支持状态过滤', async () => {
      const params: VMListParams = { status: 'RUNNING' }
      const mockResponse = mockVMApi.getVMList(params).data
      
      vi.mocked(vmApi.getVMList).mockResolvedValue(mockResponse)
      
      const result = await vmService.getVMList(params)
      
      expect(vmApi.getVMList).toHaveBeenCalledWith(params)
      expect(result.list.every(vm => vm.status === 'RUNNING' || result.list.length === 0)).toBe(true)
    })

    it('应该支持关键词搜索', async () => {
      const params: VMListParams = { keyword: '节点-001' }
      const mockResponse = mockVMApi.getVMList(params).data
      
      vi.mocked(vmApi.getVMList).mockResolvedValue(mockResponse)
      
      const result = await vmService.getVMList(params)
      
      expect(vmApi.getVMList).toHaveBeenCalledWith(params)
      // 验证搜索结果
      expect(result.list.length).toBeGreaterThanOrEqual(0)
    })

    it('应该验证分页参数', async () => {
      const invalidParams: VMListParams = { page: 0 } // 无效页码
      
      await expect(vmService.getVMList(invalidParams)).rejects.toThrow('页码必须大于0')
    })

    it('应该验证每页大小', async () => {
      const invalidParams: VMListParams = { size: 101 } // 超出限制
      
      await expect(vmService.getVMList(invalidParams)).rejects.toThrow('每页大小必须在1-100范围内')
    })
  })

  describe('getVMDetail', () => {
    it('应该成功获取虚拟机详情', async () => {
      const vmId = mockVMs[0].vmId
      const mockVM = mockVMs[0]
      
      vi.mocked(vmApi.getVMDetail).mockResolvedValue(mockVM)
      
      const result = await vmService.getVMDetail(vmId)
      
      expect(vmApi.getVMDetail).toHaveBeenCalledWith(vmId)
      expect(result.vmId).toBe(vmId)
      expect(result.name).toBe(mockVM.name)
      expect(result.status).toBe(mockVM.status)
    })

    it('应该验证虚拟机ID', async () => {
      await expect(vmService.getVMDetail('')).rejects.toThrow('虚拟机ID不能为空')
    })

    it('应该处理虚拟机不存在', async () => {
      const error = new Error('虚拟机不存在')
      vi.mocked(vmApi.getVMDetail).mockRejectedValue(error)
      
      await expect(vmService.getVMDetail('nonexistent')).rejects.toThrow('获取虚拟机详情失败')
    })
  })

  describe('updateVM', () => {
    it('应该成功更新虚拟机', async () => {
      const vmId = mockVMs[0].vmId
      const updateData: VMUpdateRequest = {
        name: '更新后的虚拟机名称',
        cpuCores: 8
      }
      
      const mockResponse = mockVMApi.updateVM(vmId, updateData).data
      vi.mocked(vmApi.updateVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.updateVM(vmId, updateData)
      
      expect(vmApi.updateVM).toHaveBeenCalledWith(vmId, updateData)
      expect(result.vmId).toBe(vmId)
      expect(result.name).toBe(updateData.name)
      expect(result.updatedAt).toBeDefined()
    })

    it('应该验证更新数据', async () => {
      const vmId = mockVMs[0].vmId
      const invalidData: VMUpdateRequest = {
        name: '', // 空名称
        cpuCores: 8
      }
      
      await expect(vmService.updateVM(vmId, invalidData)).rejects.toThrow('虚拟机名称不能为空')
    })

    it('应该验证资源配置', async () => {
      const vmId = mockVMs[0].vmId
      const invalidData: VMUpdateRequest = {
        cpuCores: 0 // 无效CPU核心数
      }
      
      await expect(vmService.updateVM(vmId, invalidData)).rejects.toThrow('CPU核心数必须大于0')
    })
  })

  describe('deleteVM', () => {
    it('应该成功删除虚拟机', async () => {
      const vmId = mockVMs[1].vmId // 使用第二个VM，状态是STOPPED
      const mockResponse = mockVMApi.deleteVM(vmId, false).data
      
      vi.mocked(vmApi.deleteVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.deleteVM(vmId)
      
      expect(vmApi.deleteVM).toHaveBeenCalledWith(vmId, false)
      expect(result.vmId).toBe(vmId)
      expect(result.deletedAt).toBeDefined()
    })

    it('应该支持强制删除', async () => {
      const vmId = mockVMs[0].vmId
      const mockResponse = mockVMApi.deleteVM(vmId, true).data
      
      vi.mocked(vmApi.deleteVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.deleteVM(vmId, true)
      
      expect(vmApi.deleteVM).toHaveBeenCalledWith(vmId, true)
      expect(result.vmId).toBe(vmId)
    })

    it('应该验证虚拟机ID', async () => {
      await expect(vmService.deleteVM('')).rejects.toThrow('虚拟机ID不能为空')
    })
  })

  describe('startVM', () => {
    it('应该成功启动虚拟机', async () => {
      const vmId = mockVMs[0].vmId
      const startData: VMStartRequest = {
        timeout: 300,
        config: {
          memory: '8GB',
          cpu: '4cores'
        }
      }
      
      const mockResponse = mockVMApi.startVM(vmId, startData).data
      vi.mocked(vmApi.startVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.startVM(vmId, startData)
      
      expect(vmApi.startVM).toHaveBeenCalledWith(vmId, startData)
      expect(result.vmId).toBe(vmId)
      expect(result.status).toBe('STARTING')
      expect(result.commandId).toBeDefined()
      expect(result.estimatedTime).toBeGreaterThan(0)
    })

    it('应该支持无参数启动', async () => {
      const vmId = mockVMs[0].vmId
      const mockResponse = mockVMApi.startVM(vmId).data
      
      vi.mocked(vmApi.startVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.startVM(vmId)
      
      expect(vmApi.startVM).toHaveBeenCalledWith(vmId, undefined)
      expect(result.vmId).toBe(vmId)
    })

    it('应该验证启动参数', async () => {
      const vmId = mockVMs[0].vmId
      const invalidData: VMStartRequest = {
        timeout: -1 // 无效超时时间
      }
      
      await expect(vmService.startVM(vmId, invalidData)).rejects.toThrow('超时时间不能为负数')
    })
  })

  describe('stopVM', () => {
    it('应该成功停止虚拟机', async () => {
      const vmId = mockVMs[0].vmId
      const stopData: VMStopRequest = {
        force: false,
        timeout: 60,
        saveState: true
      }
      
      const mockResponse = mockVMApi.stopVM(vmId, stopData).data
      vi.mocked(vmApi.stopVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.stopVM(vmId, stopData)
      
      expect(vmApi.stopVM).toHaveBeenCalledWith(vmId, stopData)
      expect(result.vmId).toBe(vmId)
      expect(result.status).toBe('STOPPING')
      expect(result.commandId).toBeDefined()
    })

    it('应该支持强制停止', async () => {
      const vmId = mockVMs[0].vmId
      const stopData: VMStopRequest = {
        force: true
      }
      
      const mockResponse = mockVMApi.stopVM(vmId, stopData).data
      vi.mocked(vmApi.stopVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.stopVM(vmId, stopData)
      
      expect(vmApi.stopVM).toHaveBeenCalledWith(vmId, stopData)
      expect(result.vmId).toBe(vmId)
    })
  })

  describe('restartVM', () => {
    it('应该成功重启虚拟机', async () => {
      const vmId = mockVMs[0].vmId
      const restartData: VMRestartRequest = {
        timeout: 300,
        graceful: true,
        config: {
          memory: '8GB',
          cpu: '4cores'
        }
      }
      
      const mockResponse = mockVMApi.restartVM(vmId, restartData).data
      vi.mocked(vmApi.restartVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.restartVM(vmId, restartData)
      
      expect(vmApi.restartVM).toHaveBeenCalledWith(vmId, restartData)
      expect(result.vmId).toBe(vmId)
      expect(result.status).toBe('STARTING')
      expect(result.commandId).toBeDefined()
      expect(result.estimatedTime).toBeGreaterThan(0)
    })

    it('应该支持无参数重启', async () => {
      const vmId = mockVMs[0].vmId
      const mockResponse = mockVMApi.restartVM(vmId).data
      
      vi.mocked(vmApi.restartVM).mockResolvedValue(mockResponse)
      
      const result = await vmService.restartVM(vmId)
      
      expect(vmApi.restartVM).toHaveBeenCalledWith(vmId, undefined)
      expect(result.vmId).toBe(vmId)
    })
  })

  describe('getVMStatus', () => {
    it('应该成功获取虚拟机状态', async () => {
      const vmId = mockVMs[0].vmId
      const mockStatus = mockVMApi.getVMStatus(vmId).data
      
      vi.mocked(vmApi.getVMStatus).mockResolvedValue(mockStatus)
      
      const result = await vmService.getVMStatus(vmId)
      
      expect(vmApi.getVMStatus).toHaveBeenCalledWith(vmId)
      expect(result.vmId).toBe(vmId)
      expect(result.status).toBeDefined()
      expect(result.connectionStatus).toBeDefined()
    })

    it('应该验证虚拟机ID', async () => {
      await expect(vmService.getVMStatus('')).rejects.toThrow('虚拟机ID不能为空')
    })

    it('应该处理状态查询失败', async () => {
      const validVmId = 'a1b2c3d4e5f678901234567890123456'
      const error = new Error('连接失败')
      vi.mocked(vmApi.getVMStatus).mockRejectedValue(error)
      
      await expect(vmService.getVMStatus(validVmId)).rejects.toThrow('获取虚拟机状态失败')
    })
  })

  describe('错误处理', () => {
    it('应该正确处理API响应错误', async () => {
      const validVmId = 'a1b2c3d4e5f678901234567890123456'
      const error = {
        response: {
          data: {
            message: 'API错误信息'
          }
        }
      }
      
      vi.mocked(vmApi.getVMDetail).mockRejectedValue(error)
      
      await expect(vmService.getVMDetail(validVmId)).rejects.toThrow('获取虚拟机详情失败 (ID: a1b2c3d4e5f678901234567890123456): API错误信息')
    })

    it('应该正确处理通用错误', async () => {
      const validVmId = 'a1b2c3d4e5f678901234567890123456'
      const error = new Error('网络连接失败')
      
      vi.mocked(vmApi.getVMDetail).mockRejectedValue(error)
      
      await expect(vmService.getVMDetail(validVmId)).rejects.toThrow('获取虚拟机详情失败 (ID: a1b2c3d4e5f678901234567890123456): 网络连接失败')
    })

    it('应该处理未知错误', async () => {
      const validVmId = 'a1b2c3d4e5f678901234567890123456'
      vi.mocked(vmApi.getVMDetail).mockRejectedValue('未知错误')
      
      await expect(vmService.getVMDetail(validVmId)).rejects.toThrow('获取虚拟机详情失败')
    })
  })

  describe('数据转换', () => {
    it('应该正确转换虚拟机数据', async () => {
      const mockVM = mockVMs[0]
      vi.mocked(vmApi.getVMDetail).mockResolvedValue(mockVM)
      
      const result = await vmService.getVMDetail(mockVM.vmId)
      
      expect(result).toEqual(mockVM)
      expect(result.vmId).toBe(mockVM.vmId)
      expect(result.name).toBe(mockVM.name)
      expect(result.status).toBe(mockVM.status)
      expect(result.connectionStatus).toBe(mockVM.connectionStatus)
      expect(result.systemInfo).toEqual(mockVM.systemInfo)
      expect(result.capabilities).toEqual(mockVM.capabilities)
    })

    it('应该正确转换虚拟机状态数据', async () => {
      const vmId = mockVMs[0].vmId
      const mockStatus = mockVMApi.getVMStatus(vmId).data
      
      vi.mocked(vmApi.getVMStatus).mockResolvedValue(mockStatus)
      
      const result = await vmService.getVMStatus(vmId)
      
      expect(result.vmId).toBe(mockStatus.vmId)
      expect(result.status).toBe(mockStatus.status)
      expect(result.connectionStatus).toBe(mockStatus.connectionStatus)
      expect(result.resourceUsage).toEqual(mockStatus.resourceUsage)
      expect(result.network).toEqual(mockStatus.network)
      expect(result.processes).toEqual(mockStatus.processes)
    })
  })

  describe('参数验证', () => {
    it('应该验证IP地址格式 - IPv4', () => {
      const validIPs = ['192.168.1.1', '10.0.0.1', '172.16.0.1', '127.0.0.1']
      const invalidIPs = ['999.999.999.999', '192.168.1', '192.168.1.1.1', 'invalid-ip']
      
      validIPs.forEach(ip => {
        expect(vmService['isValidIPAddress'](ip)).toBe(true)
      })
      
      invalidIPs.forEach(ip => {
        expect(vmService['isValidIPAddress'](ip)).toBe(false)
      })
    })

    it('应该验证虚拟机名称长度限制', async () => {
      const longName = 'a'.repeat(101) // 超过100字符
      const vmId = mockVMs[0].vmId
      const updateData: VMUpdateRequest = {
        name: longName
      }
      
      await expect(vmService.updateVM(vmId, updateData)).rejects.toThrow('虚拟机名称不能超过100个字符')
    })

    it('应该验证内存最小值', async () => {
      const vmId = mockVMs[0].vmId
      const updateData: VMUpdateRequest = {
        memoryMb: 512 // 小于1024MB
      }
      
      await expect(vmService.updateVM(vmId, updateData)).rejects.toThrow('内存必须大于等于1024MB')
    })

    it('应该验证磁盘最小值', async () => {
      const vmId = mockVMs[0].vmId
      const updateData: VMUpdateRequest = {
        diskGb: 10 // 小于20GB
      }
      
      await expect(vmService.updateVM(vmId, updateData)).rejects.toThrow('磁盘必须大于等于20GB')
    })
  })
})
