/**
 * 系统监控组件
 * 显示系统状态、性能指标、告警信息等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { 
  Row, 
  Col, 
  Card, 
  Statistic, 
  Progress, 
  Tag, 
  Typography, 
  Space, 
  Button,
  Divider,
  Alert,
  List,
  Select
} from 'antd'
import { 
  DesktopOutlined,
  DatabaseOutlined,
  CloudOutlined,
  BugOutlined,
  ReloadOutlined,
  WarningOutlined,
  CheckCircleOutlined
} from '@ant-design/icons'
import { useSystem } from '@/store/system'
import { formatDateTime } from '@/utils'

const { Title, Text } = Typography

export const SystemMonitor: React.FC = () => {
  const {
    systemMonitor,
    systemMonitorLoading,
    logMonitor,
    performanceMonitor,
    alertConfig,
    fetchSystemMonitor,
    fetchLogMonitor,
    fetchPerformanceMonitor,
    fetchAlertConfig
  } = useSystem()

  const [autoRefresh, setAutoRefresh] = useState(false)
  const [timeRange, setTimeRange] = useState<'1h' | '6h' | '24h' | '7d'>('1h')
  const [selectedEndpoint, setSelectedEndpoint] = useState<string>()

  // 初始加载 - 调用所有监控接口
  useEffect(() => {
    fetchSystemMonitor()
    fetchLogMonitor({ timeRange })
    fetchPerformanceMonitor({ timeRange, endpoint: selectedEndpoint })
    fetchAlertConfig()
  }, [fetchSystemMonitor, fetchLogMonitor, fetchPerformanceMonitor, fetchAlertConfig])

  // 自动刷新
  useEffect(() => {
    if (!autoRefresh) return

    const interval = setInterval(() => {
      fetchSystemMonitor()
      fetchLogMonitor({ timeRange })
      fetchPerformanceMonitor({ timeRange, endpoint: selectedEndpoint })
      fetchAlertConfig()
    }, 30000) // 30秒刷新一次

    return () => clearInterval(interval)
  }, [autoRefresh, fetchSystemMonitor, fetchLogMonitor, fetchPerformanceMonitor, fetchAlertConfig, timeRange, selectedEndpoint])

  // 手动刷新
  const handleRefresh = () => {
    fetchSystemMonitor()
    fetchLogMonitor({ timeRange })
    fetchPerformanceMonitor({ timeRange, endpoint: selectedEndpoint })
    fetchAlertConfig()
  }

  // 时间范围变化时重新获取数据
  const handleTimeRangeChange = (newTimeRange: '1h' | '6h' | '24h' | '7d') => {
    setTimeRange(newTimeRange)
    fetchLogMonitor({ timeRange: newTimeRange })
    fetchPerformanceMonitor({ timeRange: newTimeRange, endpoint: selectedEndpoint })
  }

  // 端点变化时重新获取性能数据
  const handleEndpointChange = (endpoint: string) => {
    setSelectedEndpoint(endpoint)
    fetchPerformanceMonitor({ timeRange, endpoint })
  }

  // 获取状态颜色
  const getStatusColor = (value: number, thresholds = { warning: 70, danger: 90 }) => {
    if (value >= thresholds.danger) return '#ff4d4f'
    if (value >= thresholds.warning) return '#faad14'
    return '#52c41a'
  }

  return (
    <div>
      {/* 操作栏 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button 
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={systemMonitorLoading}
          >
            刷新数据
          </Button>
          <Button 
            type={autoRefresh ? 'primary' : 'default'}
            onClick={() => setAutoRefresh(!autoRefresh)}
          >
            {autoRefresh ? '停止自动刷新' : '开启自动刷新'}
          </Button>
          <Select
            value={timeRange}
            onChange={handleTimeRangeChange}
            style={{ width: 100 }}
          >
            <Select.Option value="1h">1小时</Select.Option>
            <Select.Option value="6h">6小时</Select.Option>
            <Select.Option value="24h">24小时</Select.Option>
            <Select.Option value="7d">7天</Select.Option>
          </Select>
          <Select
            placeholder="选择端点"
            value={selectedEndpoint}
            onChange={handleEndpointChange}
            allowClear
            style={{ width: 200 }}
          >
            <Select.Option value="/api/user/login">用户登录</Select.Option>
            <Select.Option value="/api/vm/connect">虚拟机连接</Select.Option>
            <Select.Option value="/api/task/create">任务创建</Select.Option>
            <Select.Option value="/api/log/list">日志查询</Select.Option>
          </Select>
        </Space>
      </div>

      {/* 系统信息 */}
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card 
            title={
              <Space>
                <DesktopOutlined />
                系统信息
              </Space>
            }
            loading={systemMonitorLoading}
          >
            {systemMonitor?.systemInfo ? (
              <Row gutter={16}>
                <Col span={6}>
                  <Statistic 
                    title="系统版本" 
                    value={systemMonitor.systemInfo.version || 'N/A'}
                  />
                </Col>
                <Col span={6}>
                  <Statistic 
                    title="运行时间" 
                    value={systemMonitor.systemInfo.uptime ? 
                      (systemMonitor.systemInfo.uptime > 86400 ? 
                        `${Math.floor(systemMonitor.systemInfo.uptime / 86400)}天${Math.floor((systemMonitor.systemInfo.uptime % 86400) / 3600)}时` :
                        `${Math.floor(systemMonitor.systemInfo.uptime / 3600)}小时`
                      ) : '未知'
                    }
                    suffix=""
                  />
                </Col>
                <Col span={6}>
                  <Statistic 
                    title="Java版本" 
                    value={systemMonitor.systemInfo.javaVersion || '未提供'}
                  />
                </Col>
                <Col span={6}>
                  <Text type="secondary">
                    启动时间: {systemMonitor.systemInfo.startTime ? formatDateTime(systemMonitor.systemInfo.startTime) : '未提供'}
                  </Text>
                </Col>
              </Row>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                暂无系统信息
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 资源使用情况 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="CPU使用率"
              value={systemMonitor?.resourceUsage?.cpuUsage || 0}
              suffix="%"
              valueStyle={{ 
                color: getStatusColor(systemMonitor?.resourceUsage?.cpuUsage || 0) 
              }}
            />
            <Progress 
              percent={systemMonitor?.resourceUsage?.cpuUsage || 0}
              strokeColor={getStatusColor(systemMonitor?.resourceUsage?.cpuUsage || 0)}
              size="small"
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        
        <Col span={6}>
          <Card>
            <Statistic
              title="内存使用率"
              value={systemMonitor?.resourceUsage?.memoryUsage || 0}
              suffix="%"
              valueStyle={{ 
                color: getStatusColor(systemMonitor?.resourceUsage?.memoryUsage || 0) 
              }}
            />
            <Progress 
              percent={systemMonitor?.resourceUsage?.memoryUsage || 0}
              strokeColor={getStatusColor(systemMonitor?.resourceUsage?.memoryUsage || 0)}
              size="small"
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        
        <Col span={6}>
          <Card>
            <Statistic
              title="磁盘使用率"
              value={systemMonitor?.resourceUsage?.diskUsage || 0}
              suffix="%"
              valueStyle={{ 
                color: getStatusColor(systemMonitor?.resourceUsage?.diskUsage || 0) 
              }}
            />
            <Progress 
              percent={systemMonitor?.resourceUsage?.diskUsage || 0}
              strokeColor={getStatusColor(systemMonitor?.resourceUsage?.diskUsage || 0)}
              size="small"
              style={{ marginTop: 8 }}
            />
          </Card>
        </Col>
        
        <Col span={6}>
          <Card>
            <Statistic
              title="网络IO"
              value={systemMonitor?.resourceUsage?.networkIO?.bytesIn ? 
                ((systemMonitor.resourceUsage.networkIO.bytesIn) / 1024 / 1024).toFixed(2) : 0}
              suffix="MB/s"
              precision={2}
            />
            <Text type="secondary" style={{ fontSize: '12px' }}>
              出: {systemMonitor?.resourceUsage?.networkIO?.bytesOut ? 
                ((systemMonitor.resourceUsage.networkIO.bytesOut) / 1024 / 1024).toFixed(2) : 0} MB/s
            </Text>
          </Card>
        </Col>
      </Row>

      {/* 应用指标 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card 
            title={
              <Space>
                <CloudOutlined />
                应用指标
              </Space>
            }
            loading={systemMonitorLoading}
          >
            {systemMonitor && (
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="活跃连接"
                    value={systemMonitor.applicationMetrics?.activeConnections || 0}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="请求/秒"
                    value={systemMonitor.applicationMetrics?.requestPerSecond || 0}
                    precision={1}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="平均响应时间"
                    value={systemMonitor.applicationMetrics?.averageResponseTime || 0}
                    suffix="ms"
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="错误率"
                    value={systemMonitor.applicationMetrics?.errorRate || 0}
                    suffix="%"
                    precision={2}
                    valueStyle={{ 
                      color: getStatusColor(systemMonitor.applicationMetrics?.errorRate || 0, { warning: 1, danger: 5 }) 
                    }}
                  />
                </Col>
              </Row>
            )}
          </Card>
        </Col>
        
        <Col span={12}>
          <Card 
            title={
              <Space>
                <DatabaseOutlined />
                数据库指标
              </Space>
            }
            loading={systemMonitorLoading}
          >
            {systemMonitor && (
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="活跃连接"
                    value={systemMonitor.databaseMetrics?.activeConnections || 0}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="查询/秒"
                    value={systemMonitor.databaseMetrics?.queryPerSecond || 0}
                    precision={1}
                  />
                </Col>
                <Col span={24}>
                  <Statistic
                    title="平均查询时间"
                    value={systemMonitor.databaseMetrics?.averageQueryTime || 0}
                    suffix="ms"
                  />
                </Col>
              </Row>
            )}
          </Card>
        </Col>
      </Row>

      {/* 日志监控 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card 
            title={
              <Space>
                <BugOutlined />
                日志监控
              </Space>
            }
            loading={systemMonitorLoading}
          >
            {systemMonitor?.logMetrics ? (
              <>
                <Row gutter={16}>
                  <Col span={6}>
                    <Statistic
                      title="总日志数"
                      value={systemMonitor.logMetrics.totalLogs || 0}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="错误日志"
                      value={systemMonitor.logMetrics.errorCount || 0}
                      valueStyle={{ color: '#ff4d4f' }}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="警告日志"
                      value={systemMonitor.logMetrics.warningCount || 0}
                      valueStyle={{ color: '#faad14' }}
                    />
                  </Col>
                  <Col span={6}>
                    <Statistic
                      title="错误率"
                      value={systemMonitor.logMetrics.errorRate || 0}
                      suffix="%"
                      precision={2}
                      valueStyle={{ 
                        color: getStatusColor(systemMonitor.logMetrics.errorRate || 0, { warning: 1, danger: 5 })
                      }}
                    />
                  </Col>
                </Row>

                {/* 最近错误 */}
                {systemMonitor.recentErrors && systemMonitor.recentErrors.length > 0 && (
                  <>
                    <Divider>最近错误</Divider>
                    <List
                      size="small"
                      dataSource={systemMonitor.recentErrors.slice(0, 5)}
                      renderItem={(error) => (
                        <List.Item>
                          <List.Item.Meta
                            title={
                              <Space>
                                <Tag color="red">{error.level}</Tag>
                                <Tag color="blue">{error.category}</Tag>
                                <Text type="secondary">{formatDateTime(error.createdAt)}</Text>
                              </Space>
                            }
                            description={error.message}
                          />
                        </List.Item>
                      )}
                    />
                  </>
                )}
              </>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                暂无日志监控数据
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 告警信息 */}
      {systemMonitor?.alerts && systemMonitor.alerts.length > 0 && (
        <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
          <Col span={24}>
            <Card 
              title={
                <Space>
                  <WarningOutlined />
                  系统告警
                </Space>
              }
              loading={systemMonitorLoading}
            >
              <List
                size="small"
                dataSource={systemMonitor.alerts.filter(alert => alert.status === 'ACTIVE')}
                renderItem={(alert) => (
                  <List.Item>
                    <Alert
                      message={alert.name}
                      description={`条件: ${alert.condition} | 最后触发: ${alert.lastTriggered ? formatDateTime(alert.lastTriggered) : 'N/A'}`}
                      type="warning"
                      showIcon
                      icon={<WarningOutlined />}
                      style={{ width: '100%' }}
                    />
                  </List.Item>
                )}
                locale={{ emptyText: 
                  <div style={{ textAlign: 'center', padding: '20px' }}>
                    <CheckCircleOutlined style={{ color: '#52c41a', fontSize: '24px' }} />
                    <div style={{ marginTop: '8px' }}>暂无活跃告警</div>
                  </div>
                }}
              />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  )
}

export default SystemMonitor
