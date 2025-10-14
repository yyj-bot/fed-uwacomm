/**
 * 系统状态监控组件
 * 显示系统各组件的运行状态，包括聚合引擎状态
 * 
 * @author FedUWAComm Team
 * @version 1.4.0 - 新增聚合引擎监控
 */

import React, { useEffect, useState } from 'react'
import { Card, Row, Col, Progress, Typography, Space, Tag, Divider, Alert } from 'antd'
import { 
  DesktopOutlined, 
  HddOutlined, 
  WifiOutlined,
  DatabaseOutlined,
  ThunderboltOutlined,
  ClusterOutlined
} from '@ant-design/icons'

import { StatusIndicator } from '@/components'
import { federatedTask } from '@/api/federated-task'

const { Text, Title } = Typography

const SystemStatus: React.FC = () => {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [engineStatus, setEngineStatus] = useState<any>(null)
  const [engineError, setEngineError] = useState<string | null>(null)

  // 系统指标数据 - 从聚合引擎状态获取
  const systemMetrics = {
    cpu: engineStatus?.systemMetrics?.cpuUsage || 0,
    memory: engineStatus?.systemMetrics?.memoryUsage || 0,
    network: 0, // 聚合引擎状态不包含网络指标，需要从系统监控API获取
    storage: engineStatus?.systemMetrics?.diskUsage || 0
  }

  // 获取聚合引擎状态 - v1.4 新增
  const fetchEngineStatus = async () => {
    try {
      setLoading(true)
      setEngineError(null)
      const status = await federatedTask.getAggregationEngineStatus()
      setEngineStatus(status)
    } catch (error) {
      console.error('获取聚合引擎状态失败:', error)
      setEngineError('无法获取聚合引擎状态，请检查后端服务')
      setEngineStatus(null)
    } finally {
      setLoading(false)
    }
  }

  // 组件挂载时获取数据
  useEffect(() => {
    fetchEngineStatus()
    // 每30秒刷新一次
    const interval = setInterval(fetchEngineStatus, 30000)
    return () => clearInterval(interval)
  }, [])

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
      {/* 错误提示 */}
      {engineError && (
        <Alert
          message="聚合引擎连接异常"
          description={engineError}
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 系统基础指标 */}
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

      {/* v1.4 新增：聚合引擎状态 */}
      {engineStatus && (
        <>
          <Divider>聚合引擎状态</Divider>
          <Row gutter={[24, 16]}>
            <Col xs={24} sm={12} md={8}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', marginBottom: '8px', color: '#1890ff' }}>
                  <ThunderboltOutlined />
                </div>
                <Title level={5}>引擎状态</Title>
                <Tag color={engineStatus.engineStatus === 'RUNNING' ? 'green' : 'red'}>
                  {engineStatus.engineStatus === 'RUNNING' ? '运行中' : engineStatus.engineStatus}
                </Tag>
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    当前任务: {engineStatus.currentTasks?.length || 0}个
                  </Text>
                </div>
              </div>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', marginBottom: '8px', color: '#52c41a' }}>
                  <ClusterOutlined />
                </div>
                <Title level={5}>聚合成功率</Title>
                <Progress
                  type="circle"
                  percent={Math.round((engineStatus.aggregationMetrics?.successRate || 0) * 100)}
                  size={60}
                  strokeColor="#52c41a"
                />
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    平均耗时: {engineStatus.aggregationMetrics?.averageAggregationTime?.toFixed(1)}s
                  </Text>
                </div>
              </div>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <div style={{ textAlign: 'center' }}>
                <Title level={5}>支持的算法</Title>
                <Space wrap>
                  {engineStatus.supportedAlgorithms?.map((algorithm: string) => (
                    <Tag key={algorithm} color="blue">
                      {algorithm.replace('FEDERATED_', '').replace('_', ' ')}
                    </Tag>
                  ))}
                </Space>
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    总聚合次数: {engineStatus.aggregationMetrics?.totalAggregations || 0}
                  </Text>
                </div>
              </div>
            </Col>
          </Row>
        </>
      )}
    </Card>
  )
}

export default SystemStatus
