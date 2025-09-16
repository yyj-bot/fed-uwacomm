/**
 * 系统状态监控组件
 * 显示系统各组件的运行状态
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React from 'react'
import { Card, Row, Col, Progress, Typography, Space } from 'antd'
import { 
  DesktopOutlined, 
  HddOutlined, 
  WifiOutlined,
  DatabaseOutlined 
} from '@ant-design/icons'

import { StatusIndicator } from '@/components'

const { Text, Title } = Typography

const SystemStatus: React.FC = () => {
  // 模拟系统指标数据
  const systemMetrics = {
    cpu: Math.floor(Math.random() * 30) + 20,
    memory: Math.floor(Math.random() * 25) + 40,
    network: Math.floor(Math.random() * 40) + 10,
    storage: Math.floor(Math.random() * 20) + 30
  }
  const loading = false

  const statusItems = [
    {
      title: 'CPU 使用率',
      value: systemMetrics?.cpu || 0,
      icon: <DesktopOutlined />,
      color: systemMetrics?.cpu > 80 ? '#ff4d4f' : systemMetrics?.cpu > 60 ? '#faad14' : '#52c41a'
    },
    {
      title: '内存使用率',
      value: systemMetrics?.memory || 0,
      icon: <HddOutlined />,
      color: systemMetrics?.memory > 80 ? '#ff4d4f' : systemMetrics?.memory > 60 ? '#faad14' : '#52c41a'
    },
    {
      title: '网络连接',
      value: systemMetrics?.network || 0,
      icon: <WifiOutlined />,
      color: systemMetrics?.network > 80 ? '#ff4d4f' : systemMetrics?.network > 60 ? '#faad14' : '#52c41a'
    },
    {
      title: '存储使用率',
      value: systemMetrics?.storage || 0,
      icon: <DatabaseOutlined />,
      color: systemMetrics?.storage > 80 ? '#ff4d4f' : systemMetrics?.storage > 60 ? '#faad14' : '#52c41a'
    }
  ]

  return (
    <Card 
      title="系统状态监控" 
      className="fed-dashboard-card"
      loading={loading}
      extra={
        <StatusIndicator
          status="running"
          text="运行正常"
          variant="badge"
          size="small"
        />
      }
    >
      <Row gutter={[24, 24]}>
        {statusItems.map((item, index) => (
          <Col xs={12} sm={6} key={index}>
            <div className="fed-status-item">
              <div style={{ color: item.color, fontSize: '24px', marginBottom: '8px' }}>
                {item.icon}
              </div>
              <Title level={5} className="fed-status-title">
                {item.title}
              </Title>
              <Progress
                type="circle"
                percent={item.value}
                size={80}
                strokeColor={item.color}
                format={(percent) => (
                  <Text strong style={{ fontSize: '14px' }}>
                    {percent}%
                  </Text>
                )}
              />
              <div style={{ marginTop: '8px' }}>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {item.value > 80 ? '使用率过高' : item.value > 60 ? '使用率较高' : '运行正常'}
                </Text>
              </div>
            </div>
          </Col>
        ))}
      </Row>
    </Card>
  )
}

export default SystemStatus
