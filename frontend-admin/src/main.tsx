import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// Mock Service Worker 已禁用，使用真实后端API
// async function enableMocking() {
//   if (process.env.NODE_ENV !== 'development') {
//     return
//   }

//   try {
//     const { worker } = await import('./mocks/browser.ts')
//     await worker.start({
//       onUnhandledRequest: 'bypass',
//     })
//     console.log('✅ Mock Service Worker 启动成功')
//   } catch (error) {
//     console.warn('⚠️ Mock Service Worker 启动失败:', error)
//   }
// }

// 直接启动React应用，不使用Mock数据
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)