import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import Layout from '@/components/Layout'
import Dashboard from '@/pages/Dashboard'
import EnvironmentAnalysis from '@/pages/EnvironmentAnalysis'
import ModelPerformance from '@/pages/ModelPerformance'
import FederatedLearning from '@/pages/FederatedLearning'
import UnderwaterOptimization from '@/pages/UnderwaterOptimization'
import SystemMonitor from '@/pages/SystemMonitor'

const App: React.FC = () => {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
        },
      }}
    >
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/environment" element={<EnvironmentAnalysis />} />
            <Route path="/models" element={<ModelPerformance />} />
            <Route path="/federated" element={<FederatedLearning />} />
            <Route path="/optimization" element={<UnderwaterOptimization />} />
            <Route path="/monitor" element={<SystemMonitor />} />
          </Routes>
        </Layout>
      </Router>
    </ConfigProvider>
  )
}

export default App 