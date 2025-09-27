/**
 * API连接测试工具
 * 用于验证前后端连接是否正常
 */

import { createApiInstance } from '@/api/base'

/**
 * 测试API连接
 */
export async function testApiConnection() {
  console.log('🔍 开始测试API连接...')
  
  const tests = [
    {
      name: '管理员接口',
      endpoint: 'ADMIN' as const,
      path: '/user/list',
      method: 'GET'
    },
    {
      name: '系统日志接口', 
      endpoint: 'LOG' as const,
      path: '/list',
      method: 'GET'
    },
    {
      name: '联邦学习任务接口',
      endpoint: 'FEDERATED' as const,
      path: '/tasks',
      method: 'GET'
    }
  ]

  const results = []

  for (const test of tests) {
    try {
      console.log(`🧪 测试 ${test.name}...`)
      const api = createApiInstance(test.endpoint)
      
      const response = await api.get(test.path, {
        params: { page: 1, size: 10 },
        timeout: 5000
      })
      
      console.log(`✅ ${test.name} 连接成功`)
      results.push({ name: test.name, status: 'success', statusCode: response.status })
      
    } catch (error: any) {
      console.error(`❌ ${test.name} 连接失败:`, error.message)
      results.push({ 
        name: test.name, 
        status: 'failed', 
        error: error.message,
        statusCode: error.response?.status || 'Network Error'
      })
    }
  }

  console.log('📊 API连接测试结果:', results)
  return results
}

/**
 * 在浏览器控制台中调用此函数来测试API连接
 * 使用方法: 
 * import { testApiConnection } from '@/utils/api-test'
 * testApiConnection()
 */
if (typeof window !== 'undefined') {
  ;(window as any).testApiConnection = testApiConnection
}
