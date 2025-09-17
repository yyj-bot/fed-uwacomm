// 格式化文件大小
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

// 格式化时间
export const formatTime = (timestamp: string | number): string => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN')
}

// 格式化日期时间 (用于系统日志)
export const formatDateTime = (timestamp: string | number): string => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  })
}

// 格式化持续时间
export const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

// 深拷贝对象
export const deepClone = <T>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj))
}

// 防抖函数
export const debounce = <T extends (...args: unknown[]) => void>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

// 节流函数
export const throttle = <T extends (...args: unknown[]) => void>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args)
      inThrottle = true
      setTimeout(() => (inThrottle = false), limit)
    }
  }
}

// 随机ID生成
export const generateId = (): string => {
  return Math.random().toString(36).substr(2, 9)
}

// 获取状态颜色
export const getStatusColor = (status: string): string => {
  const statusColors: Record<string, string> = {
    ACTIVE: '#52c41a',
    INACTIVE: '#d9d9d9',
    LOCKED: '#faad14',
    RUNNING: '#52c41a',
    STOPPED: '#d9d9d9',
    ERROR: '#ff4d4f',
    SUCCESS: '#52c41a',
    PENDING: '#1890ff',
    FAILED: '#ff4d4f',
  }
  return statusColors[status] || '#d9d9d9'
} 