/**
 * 联邦学习任务状态管理 Store
 * 管理联邦学习任务的创建、配置、控制、监控等相关状态和操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { create } from 'zustand'
import { federatedTaskService } from '@/services'
import type { FederatedTask, FederatedTaskDetails, TaskResults } from '@/types'
import type { 
  CreateTaskRequest,
  ConfigTaskRequest,
  TaskListParams,
  TaskLogsParams,
  StopTaskRequest,
  CancelTaskRequest,
  DeleteTaskRequest
} from '@/services'

// ==================== 状态类型定义 ====================

interface TaskState {
  // 任务列表状态
  taskList: FederatedTask[]
  taskListTotal: number
  taskListLoading: boolean
  taskListError: string | null
  
  // 当前选中的任务
  currentTask: FederatedTaskDetails | null
  currentTaskLoading: boolean
  currentTaskError: string | null
  
  // 任务结果
  taskResults: Record<string, TaskResults>
  
  // 任务日志
  taskLogs: Record<string, any[]>
  taskLogsLoading: Record<string, boolean>
  
  // 操作状态
  operationLoading: Record<string, boolean>
  operationError: Record<string, string | null>
  
  // 创建任务状态
  createTaskLoading: boolean
  createTaskError: string | null
  
  // 分页参数
  pagination: {
    page: number
    size: number
    total: number
  }
  
  // 查询参数
  queryParams: TaskListParams
  
  // 实时状态
  realtimeData: Record<string, any>
}

interface TaskActions {
  // 任务列表操作
  fetchTaskList: (params?: TaskListParams) => Promise<void>
  refreshTaskList: () => Promise<void>
  
  // 任务详情操作
  fetchTaskDetail: (taskId: string) => Promise<void>
  setCurrentTask: (task: FederatedTaskDetails | null) => void
  
  // 任务创建和配置
  createTask: (taskData: CreateTaskRequest) => Promise<string>
  configTask: (taskId: string, config: ConfigTaskRequest) => Promise<void>
  
  // 任务控制操作
  startTask: (taskId: string) => Promise<void>
  pauseTask: (taskId: string) => Promise<void>
  resumeTask: (taskId: string) => Promise<void>
  stopTask: (taskId: string, stopData?: StopTaskRequest) => Promise<void>
  cancelTask: (taskId: string, cancelData?: CancelTaskRequest) => Promise<void>
  deleteTask: (taskId: string, deleteOptions?: DeleteTaskRequest) => Promise<void>
  
  // 任务结果和日志
  fetchTaskResults: (taskId: string) => Promise<void>
  fetchTaskLogs: (taskId: string, params?: TaskLogsParams) => Promise<void>
  
  // 分页操作
  setPagination: (page: number, size?: number) => void
  
  // 查询参数操作
  setQueryParams: (params: TaskListParams) => void
  resetQueryParams: () => void
  
  // 实时数据更新
  updateRealtimeData: (taskId: string, data: any) => void
  
  // 错误处理
  clearError: () => void
  clearTaskError: (taskId: string) => void
  
  // 状态重置
  resetState: () => void
}

type TaskStore = TaskState & TaskActions

// ==================== 初始状态 ====================

const initialState: TaskState = {
  taskList: [],
  taskListTotal: 0,
  taskListLoading: false,
  taskListError: null,
  
  currentTask: null,
  currentTaskLoading: false,
  currentTaskError: null,
  
  taskResults: {},
  taskLogs: {},
  taskLogsLoading: {},
  
  operationLoading: {},
  operationError: {},
  
  createTaskLoading: false,
  createTaskError: null,
  
  pagination: {
    page: 1,
    size: 20,
    total: 0
  },
  
  queryParams: {},
  realtimeData: {}
}

// ==================== Store 实现 ====================

export const useTaskStore = create<TaskStore>((set, get) => ({
  ...initialState,

  // ==================== 任务列表操作 ====================
  
  /**
   * 获取任务列表
   */
  fetchTaskList: async (params?: TaskListParams) => {
    const { pagination, queryParams } = get()
    const finalParams = {
      ...queryParams,
      ...params,
      page: params?.page || pagination.page,
      size: params?.size || pagination.size
    }
    
    set({ taskListLoading: true, taskListError: null })
    
    try {
      const response = await federatedTaskService.getTaskList(finalParams)
      
      set({
        taskList: response.tasks,
        taskListTotal: response.total,
        taskListLoading: false,
        taskListError: null,
        pagination: {
          page: response.page,
          size: response.size,
          total: response.total
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务列表失败'
      set({
        taskListLoading: false,
        taskListError: errorMessage
      })
      throw error
    }
  },

  /**
   * 刷新任务列表
   */
  refreshTaskList: async () => {
    const { fetchTaskList, queryParams, pagination } = get()
    await fetchTaskList({
      ...queryParams,
      page: pagination.page,
      size: pagination.size
    })
  },

  // ==================== 任务详情操作 ====================
  
  /**
   * 获取任务详情
   */
  fetchTaskDetail: async (taskId: string) => {
    set({ currentTaskLoading: true, currentTaskError: null })
    
    try {
      const task = await federatedTaskService.getTaskDetail(taskId)
      
      set({
        currentTask: task,
        currentTaskLoading: false,
        currentTaskError: null
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务详情失败'
      set({
        currentTaskLoading: false,
        currentTaskError: errorMessage
      })
      throw error
    }
  },

  /**
   * 设置当前任务
   */
  setCurrentTask: (task: FederatedTaskDetails | null) => {
    set({ currentTask: task })
  },

  // ==================== 任务创建和配置 ====================
  
  /**
   * 创建任务
   */
  createTask: async (taskData: CreateTaskRequest) => {
    set({ createTaskLoading: true, createTaskError: null })
    
    try {
      const response = await federatedTaskService.createTask(taskData)
      
      set({
        createTaskLoading: false,
        createTaskError: null
      })
      
      // 刷新任务列表
      await get().refreshTaskList()
      
      return response.taskId
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '创建任务失败'
      set({
        createTaskLoading: false,
        createTaskError: errorMessage
      })
      throw error
    }
  },

  /**
   * 配置任务
   */
  configTask: async (taskId: string, config: ConfigTaskRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`config-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`config-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.configTask(taskId, config)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`config-${taskId}`]: false
        }
      }))
      
      // 重新获取任务详情
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '配置任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`config-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`config-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 任务控制操作 ====================
  
  /**
   * 启动任务
   */
  startTask: async (taskId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`start-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`start-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.startTask(taskId)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`start-${taskId}`]: false
        }
      }))
      
      // 更新任务状态
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '启动任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`start-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`start-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 暂停任务
   */
  pauseTask: async (taskId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`pause-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`pause-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.pauseTask(taskId)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`pause-${taskId}`]: false
        }
      }))
      
      // 更新任务状态
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '暂停任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`pause-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`pause-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 恢复任务
   */
  resumeTask: async (taskId: string) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`resume-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`resume-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.resumeTask(taskId)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`resume-${taskId}`]: false
        }
      }))
      
      // 更新任务状态
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '恢复任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`resume-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`resume-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 停止任务
   */
  stopTask: async (taskId: string, stopData?: StopTaskRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`stop-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`stop-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.stopTask(taskId, stopData)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`stop-${taskId}`]: false
        }
      }))
      
      // 更新任务状态
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '停止任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`stop-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`stop-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 取消任务
   */
  cancelTask: async (taskId: string, cancelData?: CancelTaskRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`cancel-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`cancel-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.cancelTask(taskId, cancelData)
      
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`cancel-${taskId}`]: false
        }
      }))
      
      // 更新任务状态
      await get().fetchTaskDetail(taskId)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '取消任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`cancel-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`cancel-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  /**
   * 删除任务
   */
  deleteTask: async (taskId: string, deleteOptions?: DeleteTaskRequest) => {
    set((state) => ({
      operationLoading: {
        ...state.operationLoading,
        [`delete-${taskId}`]: true
      },
      operationError: {
        ...state.operationError,
        [`delete-${taskId}`]: null
      }
    }))
    
    try {
      await federatedTaskService.deleteTask(taskId, deleteOptions)
      
      // 从列表中移除
      set((state) => ({
        taskList: state.taskList.filter(task => task.taskId !== taskId),
        taskListTotal: state.taskListTotal - 1,
        currentTask: state.currentTask?.taskId === taskId ? null : state.currentTask,
        operationLoading: {
          ...state.operationLoading,
          [`delete-${taskId}`]: false
        }
      }))
      
      // 清理相关数据
      set((state) => {
        const newTaskResults = { ...state.taskResults }
        const newTaskLogs = { ...state.taskLogs }
        const newTaskLogsLoading = { ...state.taskLogsLoading }
        const newRealtimeData = { ...state.realtimeData }
        
        delete newTaskResults[taskId]
        delete newTaskLogs[taskId]
        delete newTaskLogsLoading[taskId]
        delete newRealtimeData[taskId]
        
        return {
          taskResults: newTaskResults,
          taskLogs: newTaskLogs,
          taskLogsLoading: newTaskLogsLoading,
          realtimeData: newRealtimeData
        }
      })
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除任务失败'
      set((state) => ({
        operationLoading: {
          ...state.operationLoading,
          [`delete-${taskId}`]: false
        },
        operationError: {
          ...state.operationError,
          [`delete-${taskId}`]: errorMessage
        }
      }))
      throw error
    }
  },

  // ==================== 任务结果和日志 ====================
  
  /**
   * 获取任务结果
   */
  fetchTaskResults: async (taskId: string) => {
    try {
      const results = await federatedTaskService.getTaskResults(taskId)
      
      set((state) => ({
        taskResults: {
          ...state.taskResults,
          [taskId]: results
        }
      }))
    } catch (error) {
      console.error(`获取任务结果失败 (${taskId}):`, error)
      throw error
    }
  },

  /**
   * 获取任务日志
   */
  fetchTaskLogs: async (taskId: string, params?: TaskLogsParams) => {
    set((state) => ({
      taskLogsLoading: {
        ...state.taskLogsLoading,
        [taskId]: true
      }
    }))
    
    try {
      const response = await federatedTaskService.getTaskLogs(taskId, params)
      
      set((state) => ({
        taskLogs: {
          ...state.taskLogs,
          [taskId]: response.logs
        },
        taskLogsLoading: {
          ...state.taskLogsLoading,
          [taskId]: false
        }
      }))
    } catch (error) {
      set((state) => ({
        taskLogsLoading: {
          ...state.taskLogsLoading,
          [taskId]: false
        }
      }))
      console.error(`获取任务日志失败 (${taskId}):`, error)
      throw error
    }
  },

  // ==================== 分页操作 ====================
  
  /**
   * 设置分页参数
   */
  setPagination: (page: number, size?: number) => {
    set((state) => ({
      pagination: {
        ...state.pagination,
        page,
        size: size || state.pagination.size
      }
    }))
  },

  // ==================== 查询参数操作 ====================
  
  /**
   * 设置查询参数
   */
  setQueryParams: (params: TaskListParams) => {
    set({ queryParams: params })
  },

  /**
   * 重置查询参数
   */
  resetQueryParams: () => {
    set({ queryParams: {} })
  },

  // ==================== 实时数据更新 ====================
  
  /**
   * 更新实时数据
   */
  updateRealtimeData: (taskId: string, data: any) => {
    set((state) => ({
      realtimeData: {
        ...state.realtimeData,
        [taskId]: {
          ...state.realtimeData[taskId],
          ...data,
          lastUpdated: Date.now()
        }
      }
    }))
  },

  // ==================== 错误处理 ====================
  
  /**
   * 清除错误信息
   */
  clearError: () => {
    set({
      taskListError: null,
      currentTaskError: null,
      createTaskError: null,
      operationError: {}
    })
  },

  /**
   * 清除特定任务的错误信息
   */
  clearTaskError: (taskId: string) => {
    set((state) => {
      const newOperationError = { ...state.operationError }
      Object.keys(newOperationError).forEach(key => {
        if (key.includes(taskId)) {
          delete newOperationError[key]
        }
      })
      return { operationError: newOperationError }
    })
  },

  // ==================== 状态重置 ====================
  
  /**
   * 重置状态
   */
  resetState: () => {
    set(initialState)
  }
}))

// ==================== 导出类型 ====================
export type { TaskState, TaskActions, TaskStore }
