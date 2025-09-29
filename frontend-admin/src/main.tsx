console.log('🔥 main.tsx 文件最开始执行')

import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

console.log('🚀 main.tsx 文件开始加载')
console.log('🚀 所有导入完成，main.tsx 开始执行')

// 立即输出环境变量信息
console.log('=== 环境变量检查 ===')
console.log('VITE_ENABLE_MOCK:', import.meta.env.VITE_ENABLE_MOCK)
console.log('环境变量类型:', typeof import.meta.env.VITE_ENABLE_MOCK)
console.log('所有VITE环境变量:', Object.keys(import.meta.env).filter(key => key.startsWith('VITE_')))
console.log('完整环境变量对象:', import.meta.env)

// ==================== Mock Service Worker 配置 ====================

/**
 * 检查是否启用Mock数据
 * 可以通过以下方式控制：
 * 1. 环境变量 VITE_ENABLE_MOCK=true/false
 * 2. localStorage.setItem('enableMock', 'true'/'false')
 * 3. URL参数 ?mock=true/false
 */
function shouldEnableMocking(): boolean {
  // 生产环境不启用Mock
  if (import.meta.env.PROD) {
    return false
  }
  
  // 临时强制启用Mock进行调试
  console.warn('🔧 强制启用Mock进行调试')
  return true
  
  // 原有的检查逻辑（暂时注释）
  /*
  // 优先检查环境变量
  if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
    return true
  }
  if (import.meta.env.VITE_ENABLE_MOCK === 'false') {
    return false
  }
  
  // 检查localStorage
  if (localStorage.getItem('enableMock') === 'true') {
    return true
  }
  if (localStorage.getItem('enableMock') === 'false') {
    return false
  }
  
  // 检查URL参数
  const urlParams = new URLSearchParams(window.location.search)
  if (urlParams.get('mock') === 'true') {
    return true
  }
  if (urlParams.get('mock') === 'false') {
    return false
  }
  
  // 默认不启用Mock
  return false
  */
}

/**
 * 启用Mock Service Worker
 */
async function enableMocking() {
  console.warn('🚀 enableMocking() 函数开始执行')
  const shouldEnable = shouldEnableMocking()
  console.warn('🔍 shouldEnableMocking() 返回:', shouldEnable)
  
  if (!shouldEnable) {
    console.log('🔄 使用真实后端API')
    return
  }

  try {
    const { worker } = await import('./mocks/browser')
    await worker.start({
      onUnhandledRequest: 'bypass',
    })
    console.log('✅ Mock Service Worker 启动成功')
    console.log('📝 Mock数据已启用，包含完整的联邦学习任务管理API')
    
    // 在控制台提供便捷的切换方法
    ;(window as any).toggleMock = (enable: boolean) => {
      localStorage.setItem('enableMock', enable.toString())
      console.log(`🔄 Mock数据已${enable ? '启用' : '禁用'}，请刷新页面生效`)
    }
    console.log('💡 使用 toggleMock(true/false) 切换Mock数据')
    console.log('💡 或修改 .env.local 中的 VITE_ENABLE_MOCK=true/false')
    
  } catch (error) {
    console.warn('⚠️ Mock Service Worker 启动失败:', error)
  }
}

// ==================== 应用启动 ====================

async function startApp() {
  console.warn('🚀 startApp() 函数开始执行')
  // 先启用Mock（如果需要）
  await enableMocking()
  
  // 然后启动React应用
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
}

// 立即设置全局调试函数
console.log('🔧 准备设置 checkMockStatus 函数')
;(window as any).checkMockStatus = function() {
  console.log('=== Mock启用状态检查 ===')
  console.log('生产环境:', import.meta.env.PROD)
  console.log('环境变量 VITE_ENABLE_MOCK:', import.meta.env.VITE_ENABLE_MOCK)
  console.log('环境变量类型:', typeof import.meta.env.VITE_ENABLE_MOCK)
  console.log('所有VITE环境变量:', Object.keys(import.meta.env).filter(key => key.startsWith('VITE_')))
  console.log('localStorage enableMock:', localStorage.getItem('enableMock'))
  console.log('URL参数 mock:', new URLSearchParams(location.search).get('mock'))
  console.log('shouldEnableMocking():', shouldEnableMocking())
  
  // 测试环境变量读取
  if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
    console.log('✅ 环境变量正确读取为 true')
  } else if (import.meta.env.VITE_ENABLE_MOCK === 'false') {
    console.log('✅ 环境变量正确读取为 false') 
  } else {
    console.log('⚠️ 环境变量读取异常，值为:', import.meta.env.VITE_ENABLE_MOCK)
  }
}

// 确保代码执行到这里
console.log('🔧 调试函数已设置，使用 checkMockStatus() 检查Mock状态')

// 验证函数是否被正确设置
setTimeout(() => {
  if (typeof (window as any).checkMockStatus === 'function') {
    console.log('✅ checkMockStatus 函数已正确设置')
  } else {
    console.error('❌ checkMockStatus 函数设置失败')
  }
}, 100)

console.log('🚀 准备启动应用')
console.log('📍 即将调用 startApp()')
startApp()
console.log('📍 startApp() 调用完成')