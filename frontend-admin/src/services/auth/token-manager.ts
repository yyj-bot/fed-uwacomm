/**
 * Token管理器 - 负责Token的存储、刷新和验证
 */

import type { TokenInfo } from './types'

export class TokenManager {
  private static readonly ACCESS_TOKEN_KEY = 'access_token'
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token'
  private static readonly USER_INFO_KEY = 'user_info'
  private static readonly TOKEN_EXPIRES_AT_KEY = 'token_expires_at'

  /**
   * 存储Token信息
   */
  static setTokenInfo(tokenInfo: TokenInfo): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, tokenInfo.accessToken)
    localStorage.setItem(this.REFRESH_TOKEN_KEY, tokenInfo.refreshToken)
    localStorage.setItem(this.TOKEN_EXPIRES_AT_KEY, tokenInfo.expiresAt.toString())
  }

  /**
   * 获取访问Token
   */
  static getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY)
  }

  /**
   * 获取刷新Token
   */
  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY)
  }

  /**
   * 获取Token过期时间
   */
  static getTokenExpiresAt(): number | null {
    const expiresAtStr = localStorage.getItem(this.TOKEN_EXPIRES_AT_KEY)
    return expiresAtStr ? parseInt(expiresAtStr, 10) : null
  }

  /**
   * 检查Token是否有效
   */
  static isTokenValid(): boolean {
    const accessToken = this.getAccessToken()
    const expiresAt = this.getTokenExpiresAt()
    
    if (!accessToken || !expiresAt) {
      return false
    }

    // 检查是否过期（提前5分钟判断为过期）
    const now = Date.now()
    const bufferTime = 5 * 60 * 1000 // 5分钟缓冲时间
    
    return expiresAt > now + bufferTime
  }

  /**
   * 检查Token是否需要刷新
   */
  static shouldRefreshToken(): boolean {
    const expiresAt = this.getTokenExpiresAt()
    
    if (!expiresAt) {
      return false
    }

    // 提前5分钟刷新Token
    const now = Date.now()
    const refreshThreshold = 5 * 60 * 1000 // 5分钟
    
    return expiresAt - now <= refreshThreshold
  }

  /**
   * 清除所有Token信息
   */
  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY)
    localStorage.removeItem(this.REFRESH_TOKEN_KEY)
    localStorage.removeItem(this.USER_INFO_KEY)
    localStorage.removeItem(this.TOKEN_EXPIRES_AT_KEY)
  }

  /**
   * 存储用户信息
   */
  static setUserInfo(user: any): void {
    localStorage.setItem(this.USER_INFO_KEY, JSON.stringify(user))
  }

  /**
   * 获取用户信息
   */
  static getUserInfo(): any | null {
    const userStr = localStorage.getItem(this.USER_INFO_KEY)
    if (!userStr) {
      return null
    }

    try {
      return JSON.parse(userStr)
    } catch (error) {
      console.error('解析用户信息失败:', error)
      return null
    }
  }

  /**
   * 清除用户信息
   */
  static clearUserInfo(): void {
    localStorage.removeItem(this.USER_INFO_KEY)
  }
}
