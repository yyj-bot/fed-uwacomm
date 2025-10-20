/**
 * 系统管理页面
 * 提供系统日志查看、监控面板、配置管理等功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import { 
  Typography, 
  Tabs, 
  Card, 
  Space,
  Badge
} from 'antd'
import { 
  FileTextOutlined,
  DashboardOutlined,
  SettingOutlined
} from '@ant-design/icons'

import { useSystem } from '@/store/system'
import { LogList, LogFilters, SystemMonitor, LogCleanup, LogStatistics, LogConfig } from './components'

const { Title } = Typography

const SystemPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('logs')
  const { 
    systemMonitor
  } = useSystem()

  // 子Tabs的items配置
  const logTabItems = [
    {
      key: 'list',
      label: '日志查询',
      children: (
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <LogFilters />
          <LogList height={500} />
        </Space>
      )
    },
    {
      key: 'statistics',
      label: '日志统计',
      children: <LogStatistics />
    },
    {
      key: 'cleanup',
      label: '日志清理',
      children: <LogCleanup />
    }
  ]

  // 主Tabs的items配置
  const mainTabItems = [
    {
      key: 'logs',
      label: (
        <Space>
          <FileTextOutlined />
          系统日志
        </Space>
      ),
      children: (
        <Tabs 
          type="card" 
          size="small"
          defaultActiveKey="list"
          items={logTabItems}
        />
      )
    },
    {
      key: 'monitor',
      label: (
        <Space>
          <DashboardOutlined />
          系统监控
          {systemMonitor?.logMetrics?.errorRate > 1 && (
            <Badge 
              status="error" 
              title={`错误率: ${systemMonitor.logMetrics.errorRate.toFixed(2)}%`}
            />
          )}
        </Space>
      ),
      children: <SystemMonitor />
    },
    {
      key: 'config',
      label: (
        <Space>
          <SettingOutlined />
          日志配置
        </Space>
      ),
      children: <LogConfig />
    }
  ]

  return (
    <div style={{ padding: '0 24px' }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2}>
          <Space>
            <DashboardOutlined />
            系统管理
          </Space>
        </Title>
      </div>

      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        type="card"
        size="large"
        items={mainTabItems}
      />
    </div>
  )
}

export default SystemPage
