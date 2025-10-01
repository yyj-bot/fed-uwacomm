import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// 立即输出环境变量信息
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
  //console.log('🔍 shouldEnableMocking 开始检查...')
  
  // 生产环境不启用Mock
  if (import.meta.env.PROD) {
    console.log('🏭 生产环境，禁用Mock')
    return false
  }
  
  // 详细的环境变量调试信息
  const envMock = import.meta.env.VITE_ENABLE_MOCK
  // console.log('🔍 环境变量详细信息:', {
  //   value: envMock,
  //   type: typeof envMock,
  //   isString: typeof envMock === 'string',
  //   isTrue: envMock === 'true',
  //   isFalse: envMock === 'false',
  //   isUndefined: envMock === undefined,
  //   allViteEnvs: Object.keys(import.meta.env).filter(key => key.startsWith('VITE_'))
  // })
  
  // 优先检查环境变量 - 严格字符串比较
  if (envMock === 'true') {
    //console.log('✅ 环境变量启用Mock: VITE_ENABLE_MOCK=true')
    return true
  }
  if (envMock === 'false') {
    //console.log('❌ 环境变量禁用Mock: VITE_ENABLE_MOCK=false')
    return false
  }
  
  // 检查localStorage作为备选 - 这可能会覆盖环境变量！
  const localMock = localStorage.getItem('enableMock')
  // console.log('🔍 localStorage检查:', localMock)
  //console.log('⚠️ 注意: localStorage会覆盖环境变量设置!')
  if (localMock === 'true') {
    //console.log('✅ localStorage启用Mock (覆盖环境变量)')
    return true
  }
  if (localMock === 'false') {
    //console.log('❌ localStorage禁用Mock (覆盖环境变量)')
    return false
  }
  
  // 检查URL参数作为备选
  const urlParams = new URLSearchParams(window.location.search)
  const urlMock = urlParams.get('mock')
  //console.log('🔍 URL参数检查:', urlMock)
  if (urlMock === 'true') {
    //console.log('✅ URL参数启用Mock')
    return true
  }
  if (urlMock === 'false') {
    //console.log('❌ URL参数禁用Mock')
    return false
  }
  
  // 如果环境变量未定义，默认在开发环境启用Mock
  if (envMock === undefined) {
    console.log('⚠️ 环境变量未定义，开发环境默认启用Mock')
    console.log('💡 在 .env.local 中设置 VITE_ENABLE_MOCK=false 来禁用')
    return true
  }
  
  // 兜底：禁用Mock
  console.log('🔧 使用默认配置：禁用Mock')
  return false
}

/**
 * 启用Mock Service Worker
 */
async function enableMocking() {
  //console.warn('🚀 enableMocking() 函数开始执行')
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
      serviceWorker: {
        url: '/mockServiceWorker.js'
      }
    })
    //console.log('✅ Mock Service Worker 启动成功')
    //console.log('📝 Mock数据已启用，包含完整的联邦学习任务管理API')
    //console.log('🛡️ Mock将拦截所有/api请求，绕过Vite代理')
    
    // 验证MSW是否正确拦截
    setTimeout(() => {
      console.log('🧪 Mock拦截验证：如果看到[MSW]标记，说明Mock正常工作')
    }, 1000)
    
    // 在控制台提供便捷的切换方法
    ;(window as any).toggleMock = (enable: boolean) => {
      localStorage.setItem('enableMock', enable.toString())
      console.log(`🔄 Mock数据已${enable ? '启用' : '禁用'}，请刷新页面生效`)
    }
    //console.log('💡 使用 toggleMock(true/false) 切换Mock数据')
    //console.log('💡 或修改 .env.local 中的 VITE_ENABLE_MOCK=true/false')
    
  } catch (error) {
    console.warn('⚠️ Mock Service Worker 启动失败:', error)
    console.warn('💡 请检查 public/mockServiceWorker.js 文件是否存在')
  }
}

// ==================== 应用启动 ====================

async function startApp() {
  //console.warn('🚀 startApp() 函数开始执行')
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
  //console.log('生产环境:', import.meta.env.PROD)
  //console.log('环境变量 VITE_ENABLE_MOCK:', import.meta.env.VITE_ENABLE_MOCK)
  //console.log('环境变量类型:', typeof import.meta.env.VITE_ENABLE_MOCK)
  //console.log('所有VITE环境变量:', Object.keys(import.meta.env).filter(key => key.startsWith('VITE_')))
  //console.log('localStorage enableMock:', localStorage.getItem('enableMock'))
  //console.log('URL参数 mock:', new URLSearchParams(location.search).get('mock'))
  //console.log('shouldEnableMocking():', shouldEnableMocking())
  
  // 测试环境变量读取
  if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
    console.log('✅ 环境变量正确读取为 true')
  } else if (import.meta.env.VITE_ENABLE_MOCK === 'false') {
    console.log('✅ 环境变量正确读取为 false') 
  } else {
    console.log('⚠️ 环境变量读取异常，值为:', import.meta.env.VITE_ENABLE_MOCK)
  }
}

// 🧹 添加清理localStorage的调试函数
;(window as any).clearMockCache = function() {
  //console.log('🧹 清理Mock相关缓存...')
  //console.log('清理前 localStorage.enableMock:', localStorage.getItem('enableMock'))
  localStorage.removeItem('enableMock')
  //console.log('清理后 localStorage.enableMock:', localStorage.getItem('enableMock'))
  console.log('✅ 缓存已清理，请刷新页面生效')
}

// 确保代码执行到这里
//console.log('🔧 调试函数已设置，使用 checkMockStatus() 检查Mock状态')

// 验证函数是否被正确设置
setTimeout(() => {
  if (typeof (window as any).checkMockStatus === 'function') {
    //console.log('✅ checkMockStatus 函数已正确设置')
  } else {
    //console.error('❌ checkMockStatus 函数设置失败')
  }
}, 100)

//console.log('🚀 准备启动应用')
//console.log('📍 即将调用 startApp()')
startApp()
//console.log('📍 startApp() 调用完成')