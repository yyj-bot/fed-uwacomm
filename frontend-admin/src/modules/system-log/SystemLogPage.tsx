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
const { TabPane } = Tabs

const SystemPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('logs')
  const { 
    logList,
    systemMonitor
  } = useSystem()

  // 计算错误日志数量用于显示徽标
  const errorLogCount = logList?.filter(log => log.level === 'ERROR').length || 0
  const warningLogCount = logList?.filter(log => log.level === 'WARN').length || 0
  const totalIssues = errorLogCount + warningLogCount

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
      >
        <TabPane
          tab={
            <Space>
              <FileTextOutlined />
              系统日志
              {totalIssues > 0 && (
                <Badge 
                  count={totalIssues} 
                  size="small"
                  style={{ backgroundColor: errorLogCount > 0 ? '#ff4d4f' : '#faad14' }}
                />
              )}
            </Space>
          }
          key="logs"
        >
          <Tabs 
            type="card" 
            size="small"
            defaultActiveKey="list"
          >
            <TabPane tab="日志查询" key="list">
              <Space direction="vertical" style={{ width: '100%' }} size="middle">
                <LogFilters />
                <LogList height={500} />
              </Space>
            </TabPane>
            <TabPane tab="日志统计" key="statistics">
              <LogStatistics />
            </TabPane>
            <TabPane tab="日志清理" key="cleanup">
              <LogCleanup />
            </TabPane>
          </Tabs>
        </TabPane>

        <TabPane
          tab={
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
          }
          key="monitor"
        >
          <SystemMonitor />
        </TabPane>

        <TabPane
          tab={
            <Space>
              <SettingOutlined />
              日志配置
            </Space>
          }
          key="config"
        >
          <LogConfig />
        </TabPane>
      </Tabs>
    </div>
  )
}

export default SystemPage
