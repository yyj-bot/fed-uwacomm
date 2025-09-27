/**
 * 错误处理工具函数
 * 提供统一的错误信息提取和友好提示转换
 */

export interface ErrorInfo {
  message: string
  code?: string | number
  type?: 'validation' | 'permission' | 'network' | 'server' | 'business' | 'unknown'
}

/**
 * 从错误对象中提取错误信息
 */
export function extractErrorMessage(error: any): string {
  if (!error) return '未知错误'
  
  // 字符串错误
  if (typeof error === 'string') {
    return error
  }
  
  // Error实例
  if (error instanceof Error) {
    return error.message
  }
  
  // API响应错误
  if (error && typeof error === 'object') {
    // Axios响应错误
    if (error.response?.data) {
      const { data } = error.response
      if (data.message) return data.message
      if (data.error) return data.error
      if (data.msg) return data.msg
    }
    
    // 其他对象错误
    if (error.message) return error.message
    if (error.error) return error.error
    if (error.msg) return error.msg
  }
  
  return '操作失败，请重试'
}

/**
 * 将技术错误信息转换为用户友好的提示
 */
export function getFriendlyErrorMessage(error: any, context?: string): string {
  const rawMessage = extractErrorMessage(error)
  const lowerMessage = rawMessage.toLowerCase()
  
  // 权限相关错误
  if (lowerMessage.includes('permission') || lowerMessage.includes('权限') || 
      lowerMessage.includes('unauthorized') || lowerMessage.includes('forbidden')) {
    return `您没有执行此操作的权限${context ? `（${context}）` : ''}`
  }
  
  // 重复/冲突错误 - 优先检查具体字段
  if (lowerMessage.includes('already exists') || lowerMessage.includes('已存在') || 
      lowerMessage.includes('duplicate') || lowerMessage.includes('重复') ||
      lowerMessage.includes('conflict') || lowerMessage.includes('冲突')) {
    
    // 邮箱重复
    if (lowerMessage.includes('email') || lowerMessage.includes('邮箱') || 
        lowerMessage.includes('mail') || context === 'email') {
      return '该邮箱地址已被使用，请使用其他邮箱'
    }
    
    // 用户名重复
    if (lowerMessage.includes('username') || lowerMessage.includes('用户名') || 
        lowerMessage.includes('user name') || context === 'username') {
      return '该用户名已被使用，请使用其他用户名'
    }
    
    // 用户相关的重复错误
    if (context === 'user' || lowerMessage.includes('user')) {
      // 尝试从错误信息中判断是邮箱还是用户名
      if (lowerMessage.includes('email') || lowerMessage.includes('邮箱')) {
        return '该邮箱地址已被使用，请使用其他邮箱'
      } else if (lowerMessage.includes('username') || lowerMessage.includes('用户名')) {
        return '该用户名已被使用，请使用其他用户名'
      } else {
        return '用户信息已存在，请检查邮箱和用户名'
      }
    }
    
    return '数据已存在，请检查后重试'
  }
  
  // 验证错误
  if (lowerMessage.includes('validation') || lowerMessage.includes('格式') || 
      lowerMessage.includes('invalid') || lowerMessage.includes('不合法')) {
    return '输入信息格式不正确，请检查后重试'
  }
  
  // 网络错误
  if (lowerMessage.includes('network') || lowerMessage.includes('网络') || 
      lowerMessage.includes('connection') || lowerMessage.includes('连接')) {
    return '网络连接失败，请检查网络状态'
  }
  
  // 超时错误
  if (lowerMessage.includes('timeout') || lowerMessage.includes('超时')) {
    return '请求超时，请稍后重试'
  }
  
  // 服务器错误
  if (lowerMessage.includes('server') || lowerMessage.includes('服务器') || 
      lowerMessage.includes('internal error') || lowerMessage.includes('500')) {
    return '服务器暂时不可用，请稍后重试'
  }
  
  // 资源不存在
  if (lowerMessage.includes('not found') || lowerMessage.includes('不存在') || 
      lowerMessage.includes('404')) {
    return '请求的资源不存在或已被删除'
  }
  
  // 数据库错误
  if (lowerMessage.includes('database') || lowerMessage.includes('数据库') || 
      lowerMessage.includes('sql')) {
    return '数据库操作失败，请稍后重试'
  }
  
  // 磁盘空间
  if (lowerMessage.includes('disk space') || lowerMessage.includes('磁盘空间') || 
      lowerMessage.includes('storage')) {
    return '存储空间不足，请清理后重试'
  }
  
  // 任务进行中
  if (lowerMessage.includes('in progress') || lowerMessage.includes('进行中') || 
      lowerMessage.includes('running')) {
    return '已有任务在进行中，请等待完成后再试'
  }
  
  // 用户相关特殊错误
  if (context === 'user') {
    if (lowerMessage.includes('locked') || lowerMessage.includes('锁定')) {
      return '用户已被锁定'
    }
    if (lowerMessage.includes('admin') || lowerMessage.includes('管理员')) {
      return '无法对管理员用户执行此操作'
    }
    if (lowerMessage.includes('in use') || lowerMessage.includes('使用中')) {
      return '该用户正在使用中，无法执行此操作'
    }
  }
  
  // 密码相关错误
  if (context === 'password') {
    if (lowerMessage.includes('policy') || lowerMessage.includes('策略')) {
      return '新密码不符合安全策略要求'
    }
    if (lowerMessage.includes('same') || lowerMessage.includes('相同')) {
      return '新密码不能与当前密码相同'
    }
  }
  
  // 日志相关错误
  if (context === 'log') {
    if (lowerMessage.includes('too many') || lowerMessage.includes('过多')) {
      return '选择的日志数量过多，请缩小范围后重试'
    }
  }
  
  // 如果没有匹配到特定模式，返回原始错误信息（如果比较友好）或默认消息
  if (rawMessage && rawMessage.length < 100 && !rawMessage.includes('Error:') && 
      !rawMessage.includes('Exception') && !rawMessage.includes('Stack trace')) {
    return rawMessage
  }
  
  return '操作失败，请重试'
}

/**
 * 错误处理类型判断
 */
export function getErrorType(error: any): ErrorInfo['type'] {
  const message = extractErrorMessage(error)
  const lowerMessage = message.toLowerCase()
  
  if (lowerMessage.includes('permission') || lowerMessage.includes('权限') || 
      lowerMessage.includes('unauthorized') || lowerMessage.includes('forbidden')) {
    return 'permission'
  }
  
  if (lowerMessage.includes('validation') || lowerMessage.includes('格式') || 
      lowerMessage.includes('invalid')) {
    return 'validation'
  }
  
  if (lowerMessage.includes('network') || lowerMessage.includes('网络') || 
      lowerMessage.includes('connection') || lowerMessage.includes('timeout')) {
    return 'network'
  }
  
  if (lowerMessage.includes('server') || lowerMessage.includes('服务器') || 
      lowerMessage.includes('internal error') || lowerMessage.includes('500')) {
    return 'server'
  }
  
  return 'business'
}

/**
 * 创建标准化的错误信息对象
 */
export function createErrorInfo(error: any, context?: string): ErrorInfo {
  return {
    message: getFriendlyErrorMessage(error, context),
    type: getErrorType(error),
    code: error?.response?.status || error?.code
  }
}

/**
 * 处理异步操作的错误，返回统一格式
 */
export async function handleAsyncError<T>(
  operation: () => Promise<T>,
  context?: string
): Promise<{ success: boolean; data?: T; error?: ErrorInfo }> {
  try {
    const data = await operation()
    return { success: true, data }
  } catch (error) {
    console.error(`异步操作失败${context ? ` (${context})` : ''}:`, error)
    return { 
      success: false, 
      error: createErrorInfo(error, context) 
    }
  }
}
