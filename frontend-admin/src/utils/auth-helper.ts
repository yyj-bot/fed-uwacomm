/**
 * 认证辅助工具
 * 用于处理token相关操作
 */

/**
 * 清理损坏的token
 */
export function cleanupCorruptedTokens() {
  try {
    const token = localStorage.getItem('access_token')
    if (token) {
      // 检查token是否包含非法字符
      const cleanToken = token.trim().replace(/\s+/g, '')
      if (cleanToken !== token || !isValidJWTFormat(cleanToken)) {
        console.warn('检测到损坏的token，正在清理...')
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        return false
      }
    }
    return true
  } catch (error) {
    console.error('清理token时出错:', error)
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    return false
  }
}

/**
 * 检查JWT格式是否有效
 */
function isValidJWTFormat(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return false
    }
    
    // 检查每个部分是否是有效的base64url
    for (const part of parts) {
      if (!/^[A-Za-z0-9_-]+$/.test(part)) {
        return false
      }
    }
    
    return true
  } catch {
    return false
  }
}

/**
 * 安全获取token
 */
export function getCleanToken(): string | null {
  try {
    const token = localStorage.getItem('access_token')
    if (!token) return null
    
    const cleanToken = token.trim().replace(/\s+/g, '')
    if (!isValidJWTFormat(cleanToken)) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      return null
    }
    
    return cleanToken
  } catch {
    return null
  }
}

/**
 * 生成临时访客token用于开发测试
 */
export function generateGuestToken(): string {
  const header = btoa(JSON.stringify({ "alg": "HS256", "typ": "JWT" }))
  const payload = btoa(JSON.stringify({ 
    "sub": "guest", 
    "name": "Guest User", 
    "role": "VIEWER",
    "exp": Math.floor(Date.now() / 1000) + (60 * 60) // 1小时过期
  }))
  const signature = btoa("guest-signature")
  
  return `${header}.${payload}.${signature}`
}

/**
 * 设置访客模式（用于开发测试）
 */
export function enableGuestMode() {
  console.warn('🔧 启用访客模式 - 仅用于开发测试')
  const guestToken = generateGuestToken()
  localStorage.setItem('access_token', guestToken)
  localStorage.setItem('refresh_token', 'fake-refresh-token')
  console.log('✅ 访客模式已启用，token已设置')
}

/**
 * 检查token有效性（包括过期检查）
 */
export function isTokenValid(token: string | null): boolean {
  if (!token) return false
  
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false
    
    const payload = JSON.parse(atob(parts[1]))
    if (!payload.exp) return false
    
    const currentTime = Math.floor(Date.now() / 1000)
    return payload.exp > currentTime
  } catch {
    return false
  }
}

/**
 * 安全清理所有token
 */
export function clearAllTokens(): void {
  localStorage.removeItem('access_token')
  localStorage.removeItem('refresh_token')
  console.log('已清理所有认证token')
  console.trace('clearAllTokens调用堆栈:')
}
