import React, { useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { ConfigProvider, theme, App as AntdApp } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import { MainLayout } from '@/layouts'
import {
  LoginPage,
  DashboardPage,
  SystemManagementPage,
  FederatedLearningPage,
  SystemLogsPage,
  UnderwaterOptimizationPage,
  EnvironmentAnalysisPage,
  UserProfilePage,
  AccountSettingsPage
} from '@/modules'

// 模型管理相关页面
import {
  InitialModelPage,
  VersionManagementPage,
  DeploymentPage
} from '@/modules/model-management'

// 联邦学习相关页面
import TaskListPage from '@/modules/federated-learning/TaskListPage'
import TaskDetailPage from '@/modules/federated-learning/TaskDetailPage'
import TaskCreatePage from '@/modules/federated-learning/TaskCreatePage'
import OrchestrationListPage from '@/modules/federated-learning/OrchestrationListPage'
import OrchestrationDetailPage from '@/modules/federated-learning/OrchestrationDetailPage'
import OrchestrationTimelinePage from '@/modules/federated-learning/OrchestrationTimelinePage'
import OrchestrationAnalyticsPage from '@/modules/federated-learning/OrchestrationAnalyticsPage'

import { userService, websocketService } from '@/services'
import { isTokenValid, clearAllTokens } from '@/utils/auth-helper'
import { useAuthStore } from '@/store/auth/authStore'

// 路由保护组件
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const token = userService.getAccessToken()
  
  console.log('🛡️ ProtectedRoute检查:', {
    hasToken: !!token,
    tokenPreview: token ? '***' + token.slice(-10) : 'null',
    pathname: window.location.pathname
  })
  
  // 简化逻辑：只检查token存在，不验证有效性
  if (!token) {
    //console.log('❌ ProtectedRoute无token，重定向到login')
    return <Navigate to="/login" replace />
  }
  
  //console.log('✅ ProtectedRoute有token，允许访问')
  return <>{children}</>
}

const App: React.FC = () => {
  const authStore = useAuthStore()
  
  useEffect(() => {
    // 初始化应用
    //console.log('🚀 App应用初始化开始')
    
    // 同步localStorage和store的认证状态
    const localToken = localStorage.getItem('access_token')
    const localRefresh = localStorage.getItem('refresh_token')
    const storeToken = authStore.token
    const storeIsAuthenticated = authStore.isAuthenticated
    const localTokenValid = localToken ? isTokenValid(localToken) : false
    
    console.log('🔍 App认证状态检查:', { 
      localToken: !!localToken, 
      localRefresh: !!localRefresh,
      storeToken: !!storeToken, 
      storeIsAuthenticated,
      localTokenValid,
      localTokenPreview: localToken ? '***' + localToken.slice(-10) : 'null',
      storeTokenPreview: storeToken ? '***' + storeToken.slice(-10) : 'null'
    })
    
    // 如果localStorage有token，但store没有认证状态，同步到store
    if (localToken && (!storeToken || !storeIsAuthenticated)) {
      //console.log('✅ 检测到localStorage有token，同步到store')
      authStore.initializeAuth()
    }
    // 如果localStorage没有token，但store认为已认证，清理store（但不调用API logout）
    else if (!localToken && storeIsAuthenticated) {
      //console.log('🧹 localStorage无token，直接清理store状态（不调用API）')
      authStore.clearAuthState()
    }
    // 如果状态一致，无需操作
    else {
      //console.log('✅ 认证状态一致，无需操作')
    }
    
    //console.log('🎯 App初始化完成')
  }, [])
  
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
                (() => {
                  const token = userService.getAccessToken()
                  console.log('🔍 根路径处理，token检查:', {
                    hasToken: !!token,
                    tokenPreview: token ? '***' + token.slice(-10) : 'null',
                    pathname: window.location.pathname
                  })
                  
                  // 如果有token就跳转到dashboard，不验证token有效性（避免清理）
                  if (token) {
                    console.log('✅ 根路径有token，跳转到dashboard')
                    return <Navigate to="/dashboard" replace />
                  } else {
                    console.log('❌ 根路径无token，跳转到login')
                    return <Navigate to="/login" replace />
                  }
                })()
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
                      <Route path="/admin" element={<SystemManagementPage />} />
                      
                      {/* 联邦学习路由 */}
                      <Route path="/federated-learning" element={<Navigate to="/federated-learning/tasks" replace />} />
                      <Route path="/federated-learning/tasks" element={<TaskListPage />} />
                      <Route path="/federated-learning/tasks/create" element={<TaskCreatePage />} />
                      <Route path="/federated-learning/tasks/:taskId" element={<TaskDetailPage />} />
                      
                      {/* 工作流编排路由 */}
                      <Route path="/federated-learning/orchestrations" element={<OrchestrationListPage />} />
                      <Route path="/federated-learning/orchestrations/:orchestrationId" element={<OrchestrationDetailPage />} />
                      <Route path="/federated-learning/orchestrations/:orchestrationId/timeline" element={<OrchestrationTimelinePage />} />
                      <Route path="/federated-learning/orchestrations/:orchestrationId/analytics" element={<OrchestrationAnalyticsPage />} />
                      
                      {/* 模型管理路由 */}
                      <Route path="/models" element={<Navigate to="/models/initial" replace />} />
                      <Route path="/models/initial" element={<InitialModelPage />} />
                      <Route path="/models/versions" element={<VersionManagementPage />} />
                      <Route path="/models/deployment" element={<DeploymentPage />} />
                      
                      <Route path="/logs" element={<SystemLogsPage />} />
                      <Route path="/underwater-optimization" element={<UnderwaterOptimizationPage />} />
                      <Route path="/environment-analysis" element={<EnvironmentAnalysisPage />} />
                      
                      {/* 用户相关路由 */}
                      <Route path="/user/profile" element={<UserProfilePage />} />
                      <Route path="/user/settings" element={<AccountSettingsPage />} />
                      
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