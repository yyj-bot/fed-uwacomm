import React from 'react'
import ReactDOM from 'react-dom/client'
import './index.css'

console.log('🚀 main.tsx: Starting application...')

// 最简单的测试应用，用于检查基础是否正常
const TestApp: React.FC = () => {
  const [status, setStatus] = React.useState('检查中...')
  const [components, setComponents] = React.useState<string[]>([])

  React.useEffect(() => {
    const checkComponents = async () => {
      const results: string[] = []
      
      try {
        console.log('🔍 检查基础组件...')
        
        // 检查路由
        try {
          await import('react-router-dom')
          results.push('✅ React Router DOM - 正常')
        } catch (e) {
          results.push('❌ React Router DOM - 失败: ' + e)
        }

        // 检查Ant Design
        try {
          await import('antd')
          results.push('✅ Ant Design - 正常')
        } catch (e) {
          results.push('❌ Ant Design - 失败: ' + e)
        }

        // 检查App组件
        try {
          await import('./App.tsx')
          results.push('✅ App.tsx - 正常')
        } catch (e) {
          results.push('❌ App.tsx - 失败: ' + e)
        }

        // 检查服务层
        try {
          await import('./services/api')
          results.push('✅ API Services - 正常')
        } catch (e) {
          results.push('❌ API Services - 失败: ' + e)
        }

        // 检查页面组件
        try {
          await import('./pages/Login')
          results.push('✅ Login Page - 正常')
        } catch (e) {
          results.push('❌ Login Page - 失败: ' + e)
        }

        try {
          await import('./pages/Dashboard')
          results.push('✅ Dashboard Page - 正常')
        } catch (e) {
          results.push('❌ Dashboard Page - 失败: ' + e)
        }

        // 检查MSW
        try {
          await import('./mocks/browser')
          results.push('✅ MSW Browser - 正常')
        } catch (e) {
          results.push('❌ MSW Browser - 失败: ' + e)
        }

        setComponents(results)
        setStatus('检查完成')
        
      } catch (error) {
        setStatus('检查失败: ' + error)
        console.error('检查组件时出错:', error)
      }
    }

    checkComponents()
  }, [])

  const handleStartApp = async () => {
    try {
      setStatus('正在启动MSW...')
      console.log('🔄 启动Mock Service Worker...')
      
      // 先启动MSW
      const { startMockServiceWorker } = await import('./mocks/browser')
      await startMockServiceWorker()
      console.log('✅ MSW启动成功')
      
      setStatus('正在启动完整应用...')
      console.log('🔄 启动完整应用...')
      
      // 动态导入并渲染完整应用
      const { default: App } = await import('./App.tsx')
      
      const root = ReactDOM.createRoot(document.getElementById('root')!)
      root.render(
        <React.StrictMode>
          <App />
        </React.StrictMode>
      )
      
      console.log('🎉 完整应用启动成功!')
      
    } catch (error) {
      console.error('启动完整应用失败:', error)
      setStatus('启动失败: ' + error)
    }
  }

  return (
    <div style={{ 
      padding: '20px', 
      fontFamily: 'Arial, sans-serif',
      background: '#f0f2f5',
      minHeight: '100vh'
    }}>
      <div style={{
        background: 'white',
        padding: '24px',
        borderRadius: '8px',
        marginBottom: '16px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <h1 style={{ color: '#1890ff', margin: '0 0 16px 0' }}>
          🔧 FedUWAComm 诊断工具
        </h1>
        <p style={{ margin: '0 0 16px 0' }}>
          状态: <strong>{status}</strong>
        </p>
      </div>
      
      <div style={{
        background: 'white',
        padding: '24px',
        borderRadius: '8px',
        marginBottom: '16px',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <h3 style={{ margin: '0 0 16px 0' }}>📦 组件检查结果</h3>
        {components.length === 0 ? (
          <p>正在检查组件...</p>
        ) : (
          <div style={{ fontFamily: 'monospace', fontSize: '14px' }}>
            {components.map((result, index) => (
              <div key={index} style={{ 
                marginBottom: '8px',
                color: result.includes('✅') ? '#52c41a' : '#ff4d4f'
              }}>
                {result}
              </div>
            ))}
          </div>
        )}
      </div>

      {status === '检查完成' && (
        <div style={{
          background: 'white',
          padding: '24px',
          borderRadius: '8px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}>
          <h3 style={{ margin: '0 0 16px 0' }}>🚀 启动选项</h3>
          <button 
            onClick={handleStartApp}
            style={{
              padding: '12px 24px',
              background: '#1890ff',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              fontSize: '16px',
              cursor: 'pointer',
              marginRight: '12px'
            }}
          >
            启动完整应用
          </button>
          <p style={{ margin: '16px 0 0 0', fontSize: '14px', color: '#666' }}>
            只有当所有组件都显示 ✅ 时，才建议启动完整应用
          </p>
          
          <div style={{ 
            marginTop: '16px', 
            padding: '12px', 
            background: '#f6ffed', 
            border: '1px solid #b7eb8f',
            borderRadius: '6px',
            fontSize: '14px'
          }}>
            <strong>💡 提示：</strong> 启动后如果登录遇到Network Error，请确保：
            <ul style={{ margin: '8px 0 0 0', paddingLeft: '20px' }}>
              <li>Mock Service Worker正常运行</li>
              <li>使用任意用户名和密码登录（开发环境）</li>
              <li>检查浏览器控制台是否有MSW日志</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}

try {
  console.log('📦 获取根元素...')
  const rootElement = document.getElementById('root')
  
  if (!rootElement) {
    throw new Error('找不到根元素!')
  }
  
  console.log('✅ 根元素找到')
  console.log('🔨 创建React根...')
  
  const root = ReactDOM.createRoot(rootElement)
  console.log('✅ React根创建成功')
  
  console.log('🎨 渲染诊断应用...')
  root.render(<TestApp />)
  
  console.log('🎉 诊断应用渲染成功!')
  
} catch (error) {
  console.error('💥 渲染过程中出错:', error)
  
  // 应急HTML渲染
  const rootElement = document.getElementById('root')
  if (rootElement) {
    rootElement.innerHTML = `
      <div style="padding: 20px; font-family: Arial, sans-serif; background: #fff2f0; color: #a8071a; border: 1px solid #ffccc7; border-radius: 6px; margin: 20px;">
        <h2>💥 严重错误</h2>
        <p><strong>错误:</strong> ${error}</p>
        <p>连基础渲染都失败了，请检查:</p>
        <ul>
          <li>Node.js和npm是否正常安装</li>
          <li>依赖包是否正确安装</li>
          <li>是否有语法错误</li>
        </ul>
        <button onclick="window.location.reload()" style="padding: 8px 16px; background: #ff4d4f; color: white; border: none; border-radius: 4px; cursor: pointer;">
          🔄 刷新页面
        </button>
      </div>
    `
  }
} 