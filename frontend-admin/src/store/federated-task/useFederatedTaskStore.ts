/**
 * 联邦学习任务 Hook - 封装任务状态和操作
 * 为组件层提供简洁的联邦学习任务管理功能访问接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { useCallback } from 'react'
import { useTaskStore } from './federatedTaskStore'
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

// ==================== Hook 实现 ====================

export const useTask = () => {
  // 获取状态
  const taskList = useTaskStore((state) => state.taskList)
  const taskListTotal = useTaskStore((state) => state.taskListTotal)
  const taskListLoading = useTaskStore((state) => state.taskListLoading)
  const taskListError = useTaskStore((state) => state.taskListError)
  
  const currentTask = useTaskStore((state) => state.currentTask)
  const currentTaskLoading = useTaskStore((state) => state.currentTaskLoading)
  const currentTaskError = useTaskStore((state) => state.currentTaskError)
  
  const taskResults = useTaskStore((state) => state.taskResults)
  const taskLogs = useTaskStore((state) => state.taskLogs)
  const taskLogsLoading = useTaskStore((state) => state.taskLogsLoading)
  
  const operationLoading = useTaskStore((state) => state.operationLoading)
  const operationError = useTaskStore((state) => state.operationError)
  
  const createTaskLoading = useTaskStore((state) => state.createTaskLoading)
  const createTaskError = useTaskStore((state) => state.createTaskError)
  
  const pagination = useTaskStore((state) => state.pagination)
  const queryParams = useTaskStore((state) => state.queryParams)
  const realtimeData = useTaskStore((state) => state.realtimeData)

  // 获取操作方法
  const fetchTaskListAction = useTaskStore((state) => state.fetchTaskList)
  const refreshTaskListAction = useTaskStore((state) => state.refreshTaskList)
  const fetchTaskDetailAction = useTaskStore((state) => state.fetchTaskDetail)
  const setCurrentTaskAction = useTaskStore((state) => state.setCurrentTask)
  const createTaskAction = useTaskStore((state) => state.createTask)
  const configTaskAction = useTaskStore((state) => state.configTask)
  const startTaskAction = useTaskStore((state) => state.startTask)
  const pauseTaskAction = useTaskStore((state) => state.pauseTask)
  const resumeTaskAction = useTaskStore((state) => state.resumeTask)
  const stopTaskAction = useTaskStore((state) => state.stopTask)
  const cancelTaskAction = useTaskStore((state) => state.cancelTask)
  const deleteTaskAction = useTaskStore((state) => state.deleteTask)
  const fetchTaskResultsAction = useTaskStore((state) => state.fetchTaskResults)
  const fetchTaskLogsAction = useTaskStore((state) => state.fetchTaskLogs)
  const setPaginationAction = useTaskStore((state) => state.setPagination)
  const setQueryParamsAction = useTaskStore((state) => state.setQueryParams)
  const resetQueryParamsAction = useTaskStore((state) => state.resetQueryParams)
  const updateRealtimeDataAction = useTaskStore((state) => state.updateRealtimeData)
  const clearErrorAction = useTaskStore((state) => state.clearError)
  const clearTaskErrorAction = useTaskStore((state) => state.clearTaskError)
  const resetStateAction = useTaskStore((state) => state.resetState)

  // ==================== 封装操作方法 ====================

  /**
   * 获取任务列表
   */
  const fetchTaskList = useCallback(async (params?: TaskListParams) => {
    try {
      await fetchTaskListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskListAction])

  /**
   * 刷新任务列表
   */
  const refreshTaskList = useCallback(async () => {
    try {
      await refreshTaskListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新任务列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshTaskListAction])

  /**
   * 获取任务详情
   */
  const fetchTaskDetail = useCallback(async (taskId: string) => {
    try {
      await fetchTaskDetailAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务详情失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskDetailAction])

  /**
   * 设置当前任务
   */
  const setCurrentTask = useCallback((task: FederatedTaskDetails | null) => {
    setCurrentTaskAction(task)
  }, [setCurrentTaskAction])

  /**
   * 创建任务
   */
  const createTask = useCallback(async (taskData: CreateTaskRequest) => {
    try {
      const taskId = await createTaskAction(taskData)
      return { success: true, error: null, data: taskId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '创建任务失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [createTaskAction])

  /**
   * 配置任务
   */
  const configTask = useCallback(async (taskId: string, config: ConfigTaskRequest) => {
    try {
      await configTaskAction(taskId, config)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '配置任务失败'
      return { success: false, error: errorMessage }
    }
  }, [configTaskAction])

  /**
   * 启动任务
   */
  const startTask = useCallback(async (taskId: string) => {
    try {
      await startTaskAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '启动任务失败'
      return { success: false, error: errorMessage }
    }
  }, [startTaskAction])

  /**
   * 暂停任务
   */
  const pauseTask = useCallback(async (taskId: string) => {
    try {
      await pauseTaskAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '暂停任务失败'
      return { success: false, error: errorMessage }
    }
  }, [pauseTaskAction])

  /**
   * 恢复任务
   */
  const resumeTask = useCallback(async (taskId: string) => {
    try {
      await resumeTaskAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '恢复任务失败'
      return { success: false, error: errorMessage }
    }
  }, [resumeTaskAction])

  /**
   * 停止任务
   */
  const stopTask = useCallback(async (taskId: string, stopData?: StopTaskRequest) => {
    try {
      await stopTaskAction(taskId, stopData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '停止任务失败'
      return { success: false, error: errorMessage }
    }
  }, [stopTaskAction])

  /**
   * 取消任务
   */
  const cancelTask = useCallback(async (taskId: string, cancelData?: CancelTaskRequest) => {
    try {
      await cancelTaskAction(taskId, cancelData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '取消任务失败'
      return { success: false, error: errorMessage }
    }
  }, [cancelTaskAction])

  /**
   * 删除任务
   */
  const deleteTask = useCallback(async (taskId: string, deleteOptions?: DeleteTaskRequest) => {
    try {
      await deleteTaskAction(taskId, deleteOptions)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '删除任务失败'
      return { success: false, error: errorMessage }
    }
  }, [deleteTaskAction])

  /**
   * 获取任务结果
   */
  const fetchTaskResults = useCallback(async (taskId: string) => {
    try {
      await fetchTaskResultsAction(taskId)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务结果失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskResultsAction])

  /**
   * 获取任务日志
   */
  const fetchTaskLogs = useCallback(async (taskId: string, params?: TaskLogsParams) => {
    try {
      await fetchTaskLogsAction(taskId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取任务日志失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchTaskLogsAction])

  /**
   * 设置分页参数
   */
  const setPagination = useCallback((page: number, size?: number) => {
    setPaginationAction(page, size)
  }, [setPaginationAction])

  /**
   * 设置查询参数
   */
  const setQueryParams = useCallback((params: TaskListParams) => {
    setQueryParamsAction(params)
  }, [setQueryParamsAction])

  /**
   * 重置查询参数
   */
  const resetQueryParams = useCallback(() => {
    resetQueryParamsAction()
  }, [resetQueryParamsAction])

  /**
   * 更新实时数据
   */
  const updateRealtimeData = useCallback((taskId: string, data: any) => {
    updateRealtimeDataAction(taskId, data)
  }, [updateRealtimeDataAction])

  /**
   * 清除错误信息
   */
  const clearError = useCallback(() => {
    clearErrorAction()
  }, [clearErrorAction])

  /**
   * 清除特定任务的错误信息
   */
  const clearTaskError = useCallback((taskId: string) => {
    clearTaskErrorAction(taskId)
  }, [clearTaskErrorAction])

  /**
   * 重置状态
   */
  const resetState = useCallback(() => {
    resetStateAction()
  }, [resetStateAction])

  // ==================== 计算属性 ====================

  /**
   * 获取任务结果
   */
  const getTaskResults = useCallback((taskId: string): TaskResults | null => {
    return taskResults[taskId] || null
  }, [taskResults])

  /**
   * 获取任务日志
   */
  const getTaskLogs = useCallback((taskId: string): any[] => {
    return taskLogs[taskId] || []
  }, [taskLogs])

  /**
   * 检查任务日志是否正在加载
   */
  const isTaskLogsLoading = useCallback((taskId: string): boolean => {
    return !!taskLogsLoading[taskId]
  }, [taskLogsLoading])

  /**
   * 检查任务是否正在执行操作
   */
  const isTaskOperating = useCallback((taskId: string, operation?: string): boolean => {
    if (operation) {
      return !!operationLoading[`${operation}-${taskId}`]
    }
    
    // 检查是否有任何操作正在进行
    const operations = ['config', 'start', 'pause', 'resume', 'stop', 'cancel', 'delete']
    return operations.some(op => !!operationLoading[`${op}-${taskId}`])
  }, [operationLoading])

  /**
   * 获取任务操作错误
   */
  const getTaskOperationError = useCallback((taskId: string, operation: string): string | null => {
    return operationError[`${operation}-${taskId}`] || null
  }, [operationError])

  /**
   * 获取任务实时数据
   */
  const getTaskRealtimeData = useCallback((taskId: string) => {
    return realtimeData[taskId] || null
  }, [realtimeData])

  /**
   * 检查任务是否可以启动
   */
  const canStartTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return ['CREATED', 'CONFIGURED', 'PAUSED'].includes(task.status) && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 检查任务是否可以暂停
   */
  const canPauseTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return task.status === 'RUNNING' && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 检查任务是否可以恢复
   */
  const canResumeTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return task.status === 'PAUSED' && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 检查任务是否可以停止
   */
  const canStopTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return ['RUNNING', 'PAUSED'].includes(task.status) && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 检查任务是否可以取消
   */
  const canCancelTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return !['COMPLETED', 'FAILED', 'CANCELLED'].includes(task.status) && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 检查任务是否可以删除
   */
  const canDeleteTask = useCallback((task: FederatedTask | FederatedTaskDetails): boolean => {
    if (!task) return false
    return ['COMPLETED', 'FAILED', 'CANCELLED', 'STOPPED'].includes(task.status) && !isTaskOperating(task.taskId)
  }, [isTaskOperating])

  /**
   * 获取运行中的任务数量
   */
  const runningTaskCount = useCallback((): number => {
    return taskList.filter(task => task.status === 'RUNNING').length
  }, [taskList])

  /**
   * 获取已完成的任务数量
   */
  const completedTaskCount = useCallback((): number => {
    return taskList.filter(task => task.status === 'COMPLETED').length
  }, [taskList])

  /**
   * 获取失败的任务数量
   */
  const failedTaskCount = useCallback((): number => {
    return taskList.filter(task => task.status === 'FAILED').length
  }, [taskList])

  /**
   * 获取任务进度百分比
   */
  const getTaskProgress = useCallback((task: FederatedTask | FederatedTaskDetails): number => {
    if (!task || !task.currentRound || !task.totalRounds) return 0
    return Math.round((task.currentRound / task.totalRounds) * 100)
  }, [])

  // ==================== 返回接口 ====================

  return {
    // 状态
    taskList,
    taskListTotal,
    taskListLoading,
    taskListError,
    currentTask,
    currentTaskLoading,
    currentTaskError,
    taskResults,
    taskLogs,
    taskLogsLoading,
    operationLoading,
    operationError,
    createTaskLoading,
    createTaskError,
    pagination,
    queryParams,
    realtimeData,
    
    // 操作方法
    fetchTaskList,
    refreshTaskList,
    fetchTaskDetail,
    setCurrentTask,
    createTask,
    configTask,
    startTask,
    pauseTask,
    resumeTask,
    stopTask,
    cancelTask,
    deleteTask,
    fetchTaskResults,
    fetchTaskLogs,
    setPagination,
    setQueryParams,
    resetQueryParams,
    updateRealtimeData,
    clearError,
    clearTaskError,
    resetState,
    
    // 计算属性和工具方法
    getTaskResults,
    getTaskLogs,
    isTaskLogsLoading,
    isTaskOperating,
    getTaskOperationError,
    getTaskRealtimeData,
    canStartTask,
    canPauseTask,
    canResumeTask,
    canStopTask,
    canCancelTask,
    canDeleteTask,
    runningTaskCount,
    completedTaskCount,
    failedTaskCount,
    getTaskProgress
  }
}

// ==================== v1.4 新增：联邦学习流程编排 Hook ====================

export const useOrchestration = () => {
  // 获取编排状态
  const orchestrationList = useTaskStore((state) => state.orchestrationList)
  const orchestrationListTotal = useTaskStore((state) => state.orchestrationListTotal)
  const orchestrationListLoading = useTaskStore((state) => state.orchestrationListLoading)
  const orchestrationListError = useTaskStore((state) => state.orchestrationListError)
  
  const currentOrchestration = useTaskStore((state) => state.currentOrchestration)
  const currentOrchestrationLoading = useTaskStore((state) => state.currentOrchestrationLoading)
  const currentOrchestrationError = useTaskStore((state) => state.currentOrchestrationError)
  
  const orchestrationTimelines = useTaskStore((state) => state.orchestrationTimelines)
  const orchestrationTimelinesLoading = useTaskStore((state) => state.orchestrationTimelinesLoading)
  const orchestrationTimelinesError = useTaskStore((state) => state.orchestrationTimelinesError)
  
  const orchestrationAnalytics = useTaskStore((state) => state.orchestrationAnalytics)
  const orchestrationAnalyticsLoading = useTaskStore((state) => state.orchestrationAnalyticsLoading)
  const orchestrationAnalyticsError = useTaskStore((state) => state.orchestrationAnalyticsError)
  
  const orchestrationOperationLoading = useTaskStore((state) => state.orchestrationOperationLoading)
  const orchestrationOperationError = useTaskStore((state) => state.orchestrationOperationError)
  
  const createOrchestrationLoading = useTaskStore((state) => state.createOrchestrationLoading)
  const createOrchestrationError = useTaskStore((state) => state.createOrchestrationError)
  
  const orchestrationQueryParams = useTaskStore((state) => state.orchestrationQueryParams)
  const orchestrationPagination = useTaskStore((state) => state.orchestrationPagination)

  // 获取编排操作方法
  const fetchOrchestrationListAction = useTaskStore((state) => state.fetchOrchestrationList)
  const refreshOrchestrationListAction = useTaskStore((state) => state.refreshOrchestrationList)
  const fetchOrchestrationStatusAction = useTaskStore((state) => state.fetchOrchestrationStatus)
  const setCurrentOrchestrationAction = useTaskStore((state) => state.setCurrentOrchestration)
  const startOrchestrationAction = useTaskStore((state) => state.startOrchestration)
  const pauseOrchestrationAction = useTaskStore((state) => state.pauseOrchestration)
  const resumeOrchestrationAction = useTaskStore((state) => state.resumeOrchestration)
  const terminateOrchestrationAction = useTaskStore((state) => state.terminateOrchestration)
  const fetchOrchestrationTimelineAction = useTaskStore((state) => state.fetchOrchestrationTimeline)
  const fetchOrchestrationAnalyticsAction = useTaskStore((state) => state.fetchOrchestrationAnalytics)
  const setOrchestrationQueryParamsAction = useTaskStore((state) => state.setOrchestrationQueryParams)
  const resetOrchestrationQueryParamsAction = useTaskStore((state) => state.resetOrchestrationQueryParams)
  const setOrchestrationPaginationAction = useTaskStore((state) => state.setOrchestrationPagination)
  const clearOrchestrationErrorAction = useTaskStore((state) => state.clearOrchestrationError)
  const clearOrchestrationOperationErrorAction = useTaskStore((state) => state.clearOrchestrationOperationError)

  // ==================== 封装编排操作方法 ====================

  /**
   * 获取编排列表
   */
  const fetchOrchestrationList = useCallback(async (params?: {
    taskId?: string
    status?: string
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }) => {
    try {
      await fetchOrchestrationListAction(params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取编排列表失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchOrchestrationListAction])

  /**
   * 刷新编排列表
   */
  const refreshOrchestrationList = useCallback(async () => {
    try {
      await refreshOrchestrationListAction()
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '刷新编排列表失败'
      return { success: false, error: errorMessage }
    }
  }, [refreshOrchestrationListAction])

  /**
   * 获取编排状态
   */
  const fetchOrchestrationStatus = useCallback(async (orchestrationId: string, params?: {
    includeDetails?: boolean
    includeMetrics?: boolean
    refresh?: boolean
  }) => {
    try {
      await fetchOrchestrationStatusAction(orchestrationId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取编排状态失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchOrchestrationStatusAction])

  /**
   * 设置当前编排
   */
  const setCurrentOrchestration = useCallback((orchestration: any) => {
    setCurrentOrchestrationAction(orchestration)
  }, [setCurrentOrchestrationAction])

  /**
   * 启动编排
   */
  const startOrchestration = useCallback(async (orchestrationData: {
    taskId: string
    workflowConfig: any
    schedulingOptions?: any
  }) => {
    try {
      const orchestrationId = await startOrchestrationAction(orchestrationData)
      return { success: true, error: null, data: orchestrationId }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '启动编排失败'
      return { success: false, error: errorMessage, data: null }
    }
  }, [startOrchestrationAction])

  /**
   * 暂停编排
   */
  const pauseOrchestration = useCallback(async (orchestrationId: string, pauseData?: {
    reason?: string
    pauseMode?: 'GRACEFUL' | 'IMMEDIATE'
    waitForCurrentRound?: boolean
    preserveState?: boolean
    notifyParticipants?: boolean
  }) => {
    try {
      await pauseOrchestrationAction(orchestrationId, pauseData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '暂停编排失败'
      return { success: false, error: errorMessage }
    }
  }, [pauseOrchestrationAction])

  /**
   * 恢复编排
   */
  const resumeOrchestration = useCallback(async (orchestrationId: string, resumeData?: {
    resumeFromSnapshot?: boolean
    snapshotId?: string
    validateState?: boolean
    notifyParticipants?: boolean
  }) => {
    try {
      await resumeOrchestrationAction(orchestrationId, resumeData)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '恢复编排失败'
      return { success: false, error: errorMessage }
    }
  }, [resumeOrchestrationAction])

  /**
   * 终止编排
   */
  const terminateOrchestration = useCallback(async (orchestrationId: string, params?: {
    force?: boolean
    cleanup?: boolean
    saveResults?: boolean
  }) => {
    try {
      await terminateOrchestrationAction(orchestrationId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '终止编排失败'
      return { success: false, error: errorMessage }
    }
  }, [terminateOrchestrationAction])

  /**
   * 获取编排时间线
   */
  const fetchOrchestrationTimeline = useCallback(async (orchestrationId: string, params?: {
    includeEvents?: boolean
    eventLevel?: 'ALL' | 'MAJOR' | 'ERROR'
    timeRange?: string
  }) => {
    try {
      await fetchOrchestrationTimelineAction(orchestrationId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取编排时间线失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchOrchestrationTimelineAction])

  /**
   * 获取编排性能分析
   */
  const fetchOrchestrationAnalytics = useCallback(async (orchestrationId: string, params?: {
    includeRecommendations?: boolean
    metricsLevel?: 'BASIC' | 'DETAILED' | 'FULL'
  }) => {
    try {
      await fetchOrchestrationAnalyticsAction(orchestrationId, params)
      return { success: true, error: null }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '获取编排性能分析失败'
      return { success: false, error: errorMessage }
    }
  }, [fetchOrchestrationAnalyticsAction])

  /**
   * 设置编排查询参数
   */
  const setOrchestrationQueryParams = useCallback((params: {
    taskId?: string
    status?: string
    page?: number
    size?: number
    sortBy?: string
    sortOrder?: 'asc' | 'desc'
  }) => {
    setOrchestrationQueryParamsAction(params)
  }, [setOrchestrationQueryParamsAction])

  /**
   * 重置编排查询参数
   */
  const resetOrchestrationQueryParams = useCallback(() => {
    resetOrchestrationQueryParamsAction()
  }, [resetOrchestrationQueryParamsAction])

  /**
   * 设置编排分页
   */
  const setOrchestrationPagination = useCallback((page: number, size?: number) => {
    setOrchestrationPaginationAction(page, size)
  }, [setOrchestrationPaginationAction])

  /**
   * 清除编排错误
   */
  const clearOrchestrationError = useCallback(() => {
    clearOrchestrationErrorAction()
  }, [clearOrchestrationErrorAction])

  /**
   * 清除编排操作错误
   */
  const clearOrchestrationOperationError = useCallback((orchestrationId: string) => {
    clearOrchestrationOperationErrorAction(orchestrationId)
  }, [clearOrchestrationOperationErrorAction])

  // ==================== 编排计算属性 ====================

  /**
   * 获取编排时间线
   */
  const getOrchestrationTimeline = useCallback((orchestrationId: string) => {
    return orchestrationTimelines[orchestrationId] || null
  }, [orchestrationTimelines])

  /**
   * 检查编排时间线是否正在加载
   */
  const isOrchestrationTimelineLoading = useCallback((orchestrationId: string): boolean => {
    return !!orchestrationTimelinesLoading[orchestrationId]
  }, [orchestrationTimelinesLoading])

  /**
   * 获取编排性能分析
   */
  const getOrchestrationAnalytics = useCallback((orchestrationId: string) => {
    return orchestrationAnalytics[orchestrationId] || null
  }, [orchestrationAnalytics])

  /**
   * 检查编排性能分析是否正在加载
   */
  const isOrchestrationAnalyticsLoading = useCallback((orchestrationId: string): boolean => {
    return !!orchestrationAnalyticsLoading[orchestrationId]
  }, [orchestrationAnalyticsLoading])

  /**
   * 检查编排是否正在执行操作
   */
  const isOrchestrationOperating = useCallback((orchestrationId: string): boolean => {
    return !!orchestrationOperationLoading[orchestrationId]
  }, [orchestrationOperationLoading])

  /**
   * 获取编排操作错误
   */
  const getOrchestrationOperationError = useCallback((orchestrationId: string): string | null => {
    return orchestrationOperationError[orchestrationId] || null
  }, [orchestrationOperationError])

  /**
   * 检查编排是否可以暂停
   */
  const canPauseOrchestration = useCallback((orchestration: any): boolean => {
    if (!orchestration) return false
    return orchestration.status === 'RUNNING' && !isOrchestrationOperating(orchestration.orchestrationId)
  }, [isOrchestrationOperating])

  /**
   * 检查编排是否可以恢复
   */
  const canResumeOrchestration = useCallback((orchestration: any): boolean => {
    if (!orchestration) return false
    return orchestration.status === 'PAUSED' && !isOrchestrationOperating(orchestration.orchestrationId)
  }, [isOrchestrationOperating])

  /**
   * 检查编排是否可以终止
   */
  const canTerminateOrchestration = useCallback((orchestration: any): boolean => {
    if (!orchestration) return false
    return ['RUNNING', 'PAUSED'].includes(orchestration.status) && !isOrchestrationOperating(orchestration.orchestrationId)
  }, [isOrchestrationOperating])

  /**
   * 获取运行中的编排数量
   */
  const runningOrchestrationCount = useCallback((): number => {
    return orchestrationList.filter(orchestration => orchestration.status === 'RUNNING').length
  }, [orchestrationList])

  /**
   * 获取已完成的编排数量
   */
  const completedOrchestrationCount = useCallback((): number => {
    return orchestrationList.filter(orchestration => orchestration.status === 'COMPLETED').length
  }, [orchestrationList])

  /**
   * 获取失败的编排数量
   */
  const failedOrchestrationCount = useCallback((): number => {
    return orchestrationList.filter(orchestration => orchestration.status === 'FAILED').length
  }, [orchestrationList])

  return {
    // 编排状态
    orchestrationList,
    orchestrationListTotal,
    orchestrationListLoading,
    orchestrationListError,
    currentOrchestration,
    currentOrchestrationLoading,
    currentOrchestrationError,
    orchestrationTimelines,
    orchestrationTimelinesLoading,
    orchestrationTimelinesError,
    orchestrationAnalytics,
    orchestrationAnalyticsLoading,
    orchestrationAnalyticsError,
    orchestrationOperationLoading,
    orchestrationOperationError,
    createOrchestrationLoading,
    createOrchestrationError,
    orchestrationQueryParams,
    orchestrationPagination,
    
    // 编排操作方法
    fetchOrchestrationList,
    refreshOrchestrationList,
    fetchOrchestrationStatus,
    setCurrentOrchestration,
    startOrchestration,
    pauseOrchestration,
    resumeOrchestration,
    terminateOrchestration,
    fetchOrchestrationTimeline,
    fetchOrchestrationAnalytics,
    setOrchestrationQueryParams,
    resetOrchestrationQueryParams,
    setOrchestrationPagination,
    clearOrchestrationError,
    clearOrchestrationOperationError,
    
    // 编排计算属性和工具方法
    getOrchestrationTimeline,
    isOrchestrationTimelineLoading,
    getOrchestrationAnalytics,
    isOrchestrationAnalyticsLoading,
    isOrchestrationOperating,
    getOrchestrationOperationError,
    canPauseOrchestration,
    canResumeOrchestration,
    canTerminateOrchestration,
    runningOrchestrationCount,
    completedOrchestrationCount,
    failedOrchestrationCount
  }
}

// ==================== 导出默认 Hook ====================
export default useTask
