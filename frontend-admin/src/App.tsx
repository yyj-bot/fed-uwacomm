import React, { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider, theme, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import { MainLayout } from '@/layouts'
import {
  LoginPage,
  DashboardPage,
  FederatedLearningPage,
  ModelManagementPage,
  SystemLogsPage,
  UnderwaterOptimizationPage,
  EnvironmentAnalysisPage
} from '@/modules'

import { authService, websocketService } from '@/api'
import useAppStore from '@/store'

// 路由保护组件
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // 直接检查认证服务，而不是依赖store状态
  if (!authService.isAuthenticated()) {
    return <Navigate to="/login" replace />
  }
  
  return <>{children}</>
}

const App: React.FC = () => {
  const { setUser, setAuthenticated, setWsState } = useAppStore()

  useEffect(() => {
    // 初始化认证状态
    const currentUser = authService.getCurrentUser()
    if (currentUser && authService.isAuthenticated()) {
      setUser(currentUser)
      setAuthenticated(true)
      
      // 建立WebSocket连接
      websocketService.connect()
    }

    // 监听WebSocket状态变化
    const unsubscribeWs = websocketService.onStateChange((state) => {
      setWsState(state)
    })

    // 清理
    return () => {
      unsubscribeWs()
    }
  }, [setUser, setAuthenticated, setWsState])
  
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
          colorBgContainer: '#ffffff',
        },
      }}
    >
      <AntdApp>
        <Router>
          <Routes>
            {/* 登录页面 */}
            <Route path="/login" element={<LoginPage />} />
            
            {/* 根路径处理 */}
            <Route 
              path="/" 
              element={
                authService.isAuthenticated() ? 
                  <Navigate to="/dashboard" replace /> : 
                  <Navigate to="/login" replace />
              } 
            />
            
            {/* 受保护的路由 */}
            <Route
              path="/*"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <Routes>
                      {/* 主页面路由 */}
                      <Route path="/dashboard" element={<DashboardPage />} />
                      <Route path="/federated-learning" element={<FederatedLearningPage />} />
                      <Route path="/models" element={<ModelManagementPage />} />
                      <Route path="/logs" element={<SystemLogsPage />} />
                      <Route path="/underwater-optimization" element={<UnderwaterOptimizationPage />} />
                      <Route path="/environment-analysis" element={<EnvironmentAnalysisPage />} />
                      
                      {/* 未匹配路径重定向到仪表板 */}
                      <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                  </MainLayout>
                </ProtectedRoute>
              }
            />
          </Routes>
        </Router>
      </AntdApp>
    </ConfigProvider>
  )
}

export default App 