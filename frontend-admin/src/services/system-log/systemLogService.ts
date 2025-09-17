/**
 * 系统日志服务实现
 * 提供日志查询、导出、清理、监控等业务逻辑处理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import { log } from '@/api/system-log'
import type {
  SystemLogPaginatedResponse,
  SystemLogListParams,
  RealtimeLogsParams,
  RealtimeLogsResponse,
  LogStatisticsParams,
  LogStatisticsResponse,
  LogExportData,
  LogExportResponse,
  ExportTask,
  ExportHistoryParams,
  LogCleanupData,
  LogCleanupResponse,
  CleanupTask,
  CleanupHistoryParams,
  SystemMonitor,
  LogMonitorParams,
  LogMonitor,
  PerformanceMonitorParams,
  PerformanceMonitor,
  AlertConfig,
  LogConfig,
  LogConfigUpdateData,
  LogConfigUpdateResponse,
  CleanupStrategy
} from './type'
import type { SystemLog, PaginatedResponse } from '@/types'

/**
 * 系统日志服务类
 * 封装所有日志管理相关的业务逻辑
 */
export class SystemLogService {
  // ==================== 日志查询接口 ====================

  /**
   * 获取日志列表
   * @param params 查询参数
   * @returns 分页日志列表
   */
  async getLogList(params: SystemLogListParams = {}): Promise<SystemLogPaginatedResponse<SystemLog>> {
    try {
      this.validateLogListParams(params)
      const result = await log.getLogList(params)
      return this.transformLogList(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取日志列表失败')
    }
  }

  /**
   * 获取日志详情
   * @param logId 日志ID
   * @returns 日志详情
   */
  async getLogDetail(logId: string): Promise<SystemLog> {
    try {
      this.validateLogId(logId)
      const detail = await log.getLogDetail(logId)
      return this.transformLogDetail(detail)
    } catch (error) {
      throw this.handleServiceError(error, `获取日志详情失败 (ID: ${logId})`)
    }
  }

  /**
   * 获取实时日志
   * @param params 查询参数
   * @returns 实时日志响应
   */
  async getRealtimeLogs(params: RealtimeLogsParams = {}): Promise<RealtimeLogsResponse> {
    try {
      this.validateRealtimeLogsParams(params)
      const result = await log.getRealtimeLogs(params)
      return this.transformRealtimeLogs(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取实时日志失败')
    }
  }

  /**
   * 获取日志统计
   * @param params 统计参数
   * @returns 日志统计数据
   */
  async getLogStatistics(params: LogStatisticsParams = {}): Promise<LogStatisticsResponse> {
    try {
      this.validateLogStatisticsParams(params)
      const result = await log.getLogStatistics(params)
      return this.transformLogStatistics(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取日志统计失败')
    }
  }

  // ==================== 日志导出接口 ====================

  /**
   * 同步下载日志文件
   * @param exportData 导出配置
   * @returns 文件Blob
   */
  async downloadLogs(exportData: LogExportData): Promise<Blob> {
    try {
      this.validateLogExportData(exportData)
      const blob = await log.downloadLogs(exportData)
      
      // 自动触发下载
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      
      // 生成文件名
      const timestamp = new Date().toISOString().slice(0, 19).replace(/[:-]/g, '')
      const format = exportData.format?.toLowerCase() || 'csv'
      let filename = `logs_${timestamp}.${format}`
      
      // 根据过滤条件生成更详细的文件名
      if (exportData.level) {
        filename = `logs_${exportData.level.toLowerCase()}_${timestamp}.${format}`
      }
      if (exportData.category) {
        filename = `logs_${exportData.level?.toLowerCase() || 'all'}_${exportData.category.toLowerCase()}_${timestamp}.${format}`
      }
      
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      return blob
    } catch (error) {
      throw this.handleServiceError(error, '下载日志文件失败')
    }
  }

  // ==================== 日志清理接口 ====================

  /**
   * 清理日志
   * @param cleanupData 清理配置
   * @returns 清理任务信息
   */
  async cleanupLogs(cleanupData: LogCleanupData): Promise<LogCleanupResponse> {
    try {
      this.validateLogCleanupData(cleanupData)
      const result = await log.cleanupLogs(cleanupData)
      return this.transformLogCleanupResponse(result)
    } catch (error) {
      throw this.handleServiceError(error, '创建日志清理任务失败')
    }
  }

  /**
   * 获取清理状态
   * @param cleanupId 清理任务ID
   * @returns 清理任务状态
   */
  async getCleanupStatus(cleanupId: string): Promise<CleanupTask> {
    try {
      this.validateCleanupId(cleanupId)
      const result = await log.getCleanupStatus(cleanupId)
      return this.transformCleanupTask(result)
    } catch (error) {
      throw this.handleServiceError(error, `获取清理状态失败 (CleanupID: ${cleanupId})`)
    }
  }

  /**
   * 获取清理历史
   * @param params 查询参数
   * @returns 清理历史列表
   */
  async getCleanupHistory(params: CleanupHistoryParams = {}): Promise<PaginatedResponse<CleanupTask>> {
    try {
      this.validateCleanupHistoryParams(params)
      const result = await log.getCleanupHistory(params)
      return this.transformCleanupHistory(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取清理历史失败')
    }
  }

  // ==================== 系统监控接口 ====================

  /**
   * 获取系统监控数据
   * @returns 系统监控信息
   */
  async getSystemMonitor(): Promise<SystemMonitor> {
    try {
      const result = await log.getSystemMonitor()
      return this.transformSystemMonitor(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取系统监控数据失败')
    }
  }

  /**
   * 获取日志监控数据
   * @param params 监控参数
   * @returns 日志监控信息
   */
  async getLogMonitor(params: LogMonitorParams = {}): Promise<LogMonitor> {
    try {
      this.validateLogMonitorParams(params)
      const result = await log.getLogMonitor(params)
      return this.transformLogMonitor(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取日志监控数据失败')
    }
  }

  /**
   * 获取性能监控数据
   * @param params 监控参数
   * @returns 性能监控信息
   */
  async getPerformanceMonitor(params: PerformanceMonitorParams = {}): Promise<PerformanceMonitor> {
    try {
      this.validatePerformanceMonitorParams(params)
      const result = await log.getPerformanceMonitor(params)
      return this.transformPerformanceMonitor(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取性能监控数据失败')
    }
  }

  /**
   * 获取告警配置
   * @returns 告警配置信息
   */
  async getAlertConfig(): Promise<AlertConfig> {
    try {
      const result = await log.getAlertConfig()
      return this.transformAlertConfig(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取告警配置失败')
    }
  }

  // ==================== 日志配置接口 ====================

  /**
   * 获取日志配置
   * @returns 日志配置信息
   */
  async getLogConfig(): Promise<LogConfig> {
    try {
      const result = await log.getLogConfig()
      return this.transformLogConfig(result)
    } catch (error) {
      throw this.handleServiceError(error, '获取日志配置失败')
    }
  }

  /**
   * 更新日志配置
   * @param configData 配置更新数据
   * @returns 配置更新响应
   */
  async updateLogConfig(configData: LogConfigUpdateData): Promise<LogConfigUpdateResponse> {
    try {
      this.validateLogConfigUpdateData(configData)
      const result = await log.updateLogConfig(configData)
      return this.transformLogConfigUpdateResponse(result)
    } catch (error) {
      throw this.handleServiceError(error, '更新日志配置失败')
    }
  }

  // ==================== 私有验证方法 ====================

  private validateLogId(logId: string): void {
    if (!logId || typeof logId !== 'string' || logId.trim().length === 0) {
      throw new Error('日志ID不能为空')
    }
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(logId)) {
      throw new Error('日志ID格式不正确，应为32位UUID格式')
    }
  }

  private validateTaskId(taskId: string): void {
    if (!taskId || typeof taskId !== 'string' || taskId.trim().length === 0) {
      throw new Error('任务ID不能为空')
    }
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(taskId)) {
      throw new Error('任务ID格式不正确，应为32位UUID格式')
    }
  }

  private validateVmId(vmId: string): void {
    if (!vmId || typeof vmId !== 'string' || vmId.trim().length === 0) {
      throw new Error('虚拟机ID不能为空')
    }
    const uuidRegex = /^[a-f0-9]{32}$/i
    if (!uuidRegex.test(vmId)) {
      throw new Error('虚拟机ID格式不正确，应为32位UUID格式')
    }
  }

  private validateLogLevel(level: string): void {
    const validLevels = ['DEBUG', 'INFO', 'WARN', 'ERROR']
    if (!validLevels.includes(level)) {
      throw new Error(`日志级别无效，必须为：${validLevels.join(', ')}`)
    }
  }

  private validateLogCategory(category: string): void {
    const validCategories = ['SYSTEM', 'USER', 'VM', 'TASK', 'DATA', 'MODEL', 'SECURITY', 'PERFORMANCE']
    if (!validCategories.includes(category)) {
      throw new Error(`日志类别无效，必须为：${validCategories.join(', ')}`)
    }
  }

  private validateTimeRange(timeRange: string): void {
    const validRanges = ['1h', '6h', '24h', '7d']
    if (!validRanges.includes(timeRange)) {
      throw new Error(`时间范围无效，必须为：${validRanges.join(', ')}`)
    }
  }

  private validateExportFormat(format: string): void {
    const validFormats = ['CSV', 'JSON', 'EXCEL']
    if (!validFormats.includes(format)) {
      throw new Error(`导出格式无效，必须为：${validFormats.join(', ')}`)
    }
  }

  private validateCleanupStrategy(strategy: CleanupStrategy): void {
    const validStrategies: CleanupStrategy[] = ['TIME_BASED', 'LEVEL_BASED', 'CATEGORY_BASED']
    if (!validStrategies.includes(strategy)) {
      throw new Error(`清理策略无效，必须为：${validStrategies.join(', ')}`)
    }
  }

  private validateExportId(exportId: string): void {
    if (!exportId || typeof exportId !== 'string' || exportId.trim().length === 0) {
      throw new Error('导出任务ID不能为空')
    }
  }

  private validateCleanupId(cleanupId: string): void {
    if (!cleanupId || typeof cleanupId !== 'string' || cleanupId.trim().length === 0) {
      throw new Error('清理任务ID不能为空')
    }
  }

  private validateLogListParams(params: SystemLogListParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
    if (params.level !== undefined) {
      this.validateLogLevel(params.level)
    }
    if (params.category !== undefined) {
      this.validateLogCategory(params.category)
    }
    if (params.vmId !== undefined) {
      this.validateVmId(params.vmId)
    }
    if (params.taskId !== undefined) {
      this.validateTaskId(params.taskId)
    }
    if (params.keyword !== undefined && params.keyword.length > 500) {
      throw new Error('关键词长度不能超过500字符')
    }
  }

  private validateRealtimeLogsParams(params: RealtimeLogsParams): void {
    if (params.level !== undefined) {
      this.validateLogLevel(params.level)
    }
    if (params.category !== undefined) {
      this.validateLogCategory(params.category)
    }
    if (params.vmId !== undefined) {
      this.validateVmId(params.vmId)
    }
    if (params.taskId !== undefined) {
      this.validateTaskId(params.taskId)
    }
    if (params.tail !== undefined && (params.tail < 1 || params.tail > 1000)) {
      throw new Error('tail参数必须在1-1000范围内')
    }
  }

  private validateLogStatisticsParams(params: LogStatisticsParams): void {
    if (params.category !== undefined) {
      this.validateLogCategory(params.category)
    }
    if (params.vmId !== undefined) {
      this.validateVmId(params.vmId)
    }
    if (params.taskId !== undefined) {
      this.validateTaskId(params.taskId)
    }
  }

  private validateLogExportData(exportData: LogExportData): void {
    if (exportData.level !== undefined) {
      this.validateLogLevel(exportData.level)
    }
    if (exportData.category !== undefined) {
      this.validateLogCategory(exportData.category)
    }
    if (exportData.vmId !== undefined) {
      this.validateVmId(exportData.vmId)
    }
    if (exportData.taskId !== undefined) {
      this.validateTaskId(exportData.taskId)
    }
    if (exportData.format !== undefined) {
      this.validateExportFormat(exportData.format)
    }
  }

  private validateExportHistoryParams(params: ExportHistoryParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
  }

  private validateLogCleanupData(cleanupData: LogCleanupData): void {
    this.validateCleanupStrategy(cleanupData.strategy)
    
    if (cleanupData.strategy === 'TIME_BASED' && cleanupData.retentionDays !== undefined) {
      if (cleanupData.retentionDays < 1 || cleanupData.retentionDays > 365) {
        throw new Error('保留天数必须在1-365范围内')
      }
    }
    
    if (cleanupData.strategy === 'LEVEL_BASED' && cleanupData.level !== undefined) {
      this.validateLogLevel(cleanupData.level)
    }
    

    if (cleanupData.category !== undefined) {
      this.validateLogCategory(cleanupData.category)
    }
    if (cleanupData.vmId !== undefined) {
      this.validateVmId(cleanupData.vmId)
    }
    if (cleanupData.taskId !== undefined) {
      this.validateTaskId(cleanupData.taskId)
    }
  }

  private validateCleanupHistoryParams(params: CleanupHistoryParams): void {
    if (params.page !== undefined && params.page < 1) {
      throw new Error('页码必须大于0')
    }
    if (params.size !== undefined && (params.size < 1 || params.size > 100)) {
      throw new Error('每页大小必须在1-100范围内')
    }
  }

  private validateLogMonitorParams(params: LogMonitorParams): void {
    if (params.timeRange !== undefined) {
      this.validateTimeRange(params.timeRange)
    }
    if (params.level !== undefined) {
      this.validateLogLevel(params.level)
    }
  }

  private validatePerformanceMonitorParams(params: PerformanceMonitorParams): void {
    if (params.timeRange !== undefined) {
      this.validateTimeRange(params.timeRange)
    }
  }

  private validateLogConfigUpdateData(configData: LogConfigUpdateData): void {
    if (configData.logLevel !== undefined) {
      this.validateLogLevel(configData.logLevel)
    }
    if (configData.retentionDays !== undefined && (configData.retentionDays < 1 || configData.retentionDays > 365)) {
      throw new Error('保留天数必须在1-365范围内')
    }
    if (configData.maxFileSize !== undefined && (configData.maxFileSize < 1024 || configData.maxFileSize > 1073741824)) {
      throw new Error('最大文件大小必须在1KB-1GB范围内')
    }
  }

  // ==================== 私有转换方法 ====================

  private transformLogList(result: any): SystemLogPaginatedResponse<SystemLog> {
    return {
      total: result.total,
      pages: result.pages,
      current: result.current,
      size: result.size,
      records: result.records.map((item: any) => this.transformLogDetail(item))
    }
  }

  private transformLogDetail(detail: any): SystemLog {
    return {
      logId: detail.logId,
      level: detail.level,
      category: detail.category,
      vmId: detail.vmId,
      taskId: detail.taskId,
      message: detail.message,
      details: detail.details,
      createdAt: detail.createdAt
    }
  }

  private transformRealtimeLogs(result: any): RealtimeLogsResponse {
    return {
      logs: result.logs.map((item: any) => this.transformLogDetail(item)),
      totalCount: result.totalCount,
      lastUpdateTime: result.lastUpdateTime
    }
  }

  private transformLogStatistics(result: any): LogStatisticsResponse {
    return {
      totalLogs: result.totalLogs,
      levelDistribution: result.levelDistribution,
      categoryDistribution: result.categoryDistribution,
      timeDistribution: result.timeDistribution,
      errorTrend: result.errorTrend
    }
  }

  private transformLogExportResponse(result: any): LogExportResponse {
    return {
      exportId: result.exportId,
      status: result.status,
      estimatedTime: result.estimatedTime,
      downloadUrl: result.downloadUrl
    }
  }

  private transformExportTask(result: any): ExportTask {
    return {
      exportId: result.exportId,
      status: result.status,
      estimatedTime: result.estimatedTime,
      downloadUrl: result.downloadUrl,
      expiresAt: result.expiresAt,
      progress: result.progress,
      totalRecords: result.totalRecords,
      processedRecords: result.processedRecords,
      fileSize: result.fileSize,
      createdAt: result.createdAt,
      completedAt: result.completedAt,
      format: result.format
    }
  }

  private transformExportHistory(result: any): PaginatedResponse<ExportTask> {
    return {
      total: result.total,
      page: result.page || result.current || 1,
      size: result.size,
      pages: result.pages,
      records: result.records.map((item: any) => this.transformExportTask(item))
    }
  }

  private transformLogCleanupResponse(result: any): LogCleanupResponse {
    return {
      cleanupId: result.cleanupId,
      status: result.status,
      estimatedRecords: result.estimatedRecords,
      estimatedSize: result.estimatedSize
    }
  }

  private transformCleanupTask(result: any): CleanupTask {
    return {
      cleanupId: result.cleanupId,
      status: result.status,
      estimatedRecords: result.estimatedRecords,
      estimatedSize: result.estimatedSize,
      progress: result.progress,
      deletedRecords: result.deletedRecords,
      freedSpace: result.freedSpace,
      createdAt: result.createdAt,
      completedAt: result.completedAt,
      strategy: result.strategy
    }
  }

  private transformCleanupHistory(result: any): PaginatedResponse<CleanupTask> {
    return {
      total: result.total,
      page: result.page || result.current || 1,
      size: result.size,
      pages: result.pages,
      records: result.records.map((item: any) => this.transformCleanupTask(item))
    }
  }

  private transformSystemMonitor(result: any): SystemMonitor {
    return {
      systemInfo: result.systemInfo,
      resourceUsage: result.resourceUsage,
      applicationMetrics: result.applicationMetrics,
      databaseMetrics: result.databaseMetrics
    }
  }

  private transformLogMonitor(result: any): LogMonitor {
    return {
      logMetrics: result.logMetrics,
      levelTrend: result.levelTrend,
      categoryTrend: result.categoryTrend,
      recentErrors: result.recentErrors
    }
  }

  private transformPerformanceMonitor(result: any): PerformanceMonitor {
    return {
      apiMetrics: result.apiMetrics,
      endpointMetrics: result.endpointMetrics,
      responseTimeTrend: result.responseTimeTrend
    }
  }

  private transformAlertConfig(result: any): AlertConfig {
    return {
      alerts: result.alerts,
      alertHistory: result.alertHistory
    }
  }

  private transformLogConfig(result: any): LogConfig {
    return {
      logLevel: result.logLevel,
      retentionDays: result.retentionDays,
      maxFileSize: result.maxFileSize,
      categories: result.categories,
      exportSettings: result.exportSettings
    }
  }

  private transformLogConfigUpdateResponse(result: any): LogConfigUpdateResponse {
    return {
      updatedAt: result.updatedAt
    }
  }

  // ==================== 私有错误处理方法 ====================

  private handleServiceError(error: any, message: string): Error {
    console.error(`[SystemLogService] ${message}:`, error)
    
    if (error.response?.data?.message) {
      return new Error(`${message}: ${error.response.data.message}`)
    }
    
    if (error.message) {
      return new Error(`${message}: ${error.message}`)
    }
    
    return new Error(message)
  }
}

export const systemLogService = new SystemLogService()
export default systemLogService
