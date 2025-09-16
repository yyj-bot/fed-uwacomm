import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'

// 在开发环境下启动 Mock Service Worker
async function enableMocking() {
  if (process.env.NODE_ENV !== 'development') {
    return
  }

  try {
    const { worker } = await import('./mocks/browser.ts')
    await worker.start({
      onUnhandledRequest: 'bypass',
    })
    console.log('✅ Mock Service Worker 启动成功')
  } catch (error) {
    console.warn('⚠️ Mock Service Worker 启动失败:', error)
  }
}

enableMocking().then(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
})