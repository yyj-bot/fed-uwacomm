/**
 * 性能图表组件
 * 显示系统性能指标的图表数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import { Card, Row, Col, Select, DatePicker, Space, Skeleton, Alert } from 'antd'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts'
import { ChartErrorBoundary } from '@/components'

import type { DashboardChartData } from '@/store'

const { RangePicker } = DatePicker
const { Option } = Select

interface PerformanceChartsProps {
  chartData?: DashboardChartData
  loading?: boolean
}

const PerformanceCharts: React.FC<PerformanceChartsProps> = ({ 
  chartData, 
  loading = false 
}) => {
  const [chartError, setChartError] = useState<string | null>(null)
  const [timeRange, setTimeRange] = useState<string>('24h')
  const [metric, setMetric] = useState<string>('all')

  // 生成安全的静态数据
  const generateSafeData = React.useCallback(() => {
    const baseData = [
      { time: '00:00', cpu: 25, memory: 45, network: 15, storage: 30, tasks: 3, participants: 18 },
      { time: '01:00', cpu: 22, memory: 42, network: 12, storage: 32, tasks: 2, participants: 16 },
      { time: '02:00', cpu: 20, memory: 40, network: 10, storage: 35, tasks: 4, participants: 15 },
      { time: '03:00', cpu: 28, memory: 48, network: 18, storage: 33, tasks: 3, participants: 19 },
      { time: '04:00', cpu: 35, memory: 52, network: 25, storage: 38, tasks: 5, participants: 22 },
      { time: '05:00', cpu: 42, memory: 58, network: 32, storage: 42, tasks: 6, participants: 25 },
      { time: '06:00', cpu: 38, memory: 55, network: 28, storage: 40, tasks: 4, participants: 23 },
      { time: '07:00', cpu: 45, memory: 62, network: 35, storage: 45, tasks: 7, participants: 28 },
      { time: '08:00', cpu: 50, memory: 65, network: 40, storage: 48, tasks: 8, participants: 30 },
      { time: '09:00', cpu: 48, memory: 63, network: 38, storage: 46, tasks: 7, participants: 29 },
      { time: '10:00', cpu: 52, memory: 67, network: 42, storage: 50, tasks: 9, participants: 32 },
      { time: '11:00', cpu: 46, memory: 61, network: 36, storage: 44, tasks: 6, participants: 27 },
      { time: '12:00', cpu: 40, memory: 56, network: 30, storage: 38, tasks: 5, participants: 24 },
      { time: '13:00', cpu: 44, memory: 59, network: 34, storage: 41, tasks: 6, participants: 26 },
      { time: '14:00', cpu: 48, memory: 63, network: 38, storage: 45, tasks: 7, participants: 28 },
      { time: '15:00', cpu: 45, memory: 60, network: 35, storage: 43, tasks: 6, participants: 27 },
      { time: '16:00', cpu: 42, memory: 57, network: 32, storage: 40, tasks: 5, participants: 25 },
      { time: '17:00', cpu: 38, memory: 54, network: 28, storage: 37, tasks: 4, participants: 23 },
      { time: '18:00', cpu: 35, memory: 51, network: 25, storage: 35, tasks: 4, participants: 21 },
      { time: '19:00', cpu: 32, memory: 48, network: 22, storage: 33, tasks: 3, participants: 19 },
      { time: '20:00', cpu: 30, memory: 46, network: 20, storage: 31, tasks: 3, participants: 18 },
      { time: '21:00', cpu: 28, memory: 44, network: 18, storage: 29, tasks: 2, participants: 17 },
      { time: '22:00', cpu: 26, memory: 42, network: 16, storage: 28, tasks: 2, participants: 16 },
      { time: '23:00', cpu: 24, memory: 41, network: 14, storage: 27, tasks: 2, participants: 15 }
    ]
    
    // 确保数据格式完全正确
    return baseData.map((item, index) => ({
      ...item,
      timestamp: Date.now() - (baseData.length - index) * 60 * 60 * 1000,
      // 确保所有数值都是有效数字
      cpu: Math.max(0, Math.min(100, item.cpu)),
      memory: Math.max(0, Math.min(100, item.memory)),
      network: Math.max(0, Math.min(100, item.network)),
      storage: Math.max(0, Math.min(100, item.storage)),
      tasks: Math.max(0, item.tasks),
      participants: Math.max(0, item.participants)
    }))
  }, [])

  // 使用静态安全数据
  const data = React.useMemo(() => {
    return generateSafeData()
  }, [generateSafeData])

  if (loading) {
    return (
      <Card 
        title="性能监控" 
        className="fed-dashboard-card"
      >
        <Skeleton active paragraph={{ rows: 8 }} />
      </Card>
    )
  }

  return (
    <Card 
      title="性能监控" 
      className="fed-dashboard-card"
      extra={
        <Space>
          <Select
            value={metric}
            onChange={setMetric}
            size="small"
            style={{ width: 120 }}
          >
            <Option value="all">全部指标</Option>
            <Option value="system">系统资源</Option>
            <Option value="business">业务指标</Option>
          </Select>
          <Select
            value={timeRange}
            onChange={setTimeRange}
            size="small"
            style={{ width: 100 }}
          >
            <Option value="1h">1小时</Option>
            <Option value="6h">6小时</Option>
            <Option value="24h">24小时</Option>
            <Option value="7d">7天</Option>
          </Select>
        </Space>
      }
    >
      <Row gutter={[0, 24]}>
        {/* 系统资源使用率 */}
        {(metric === 'all' || metric === 'system') && (
          <Col span={24}>
            <div style={{ marginBottom: '16px' }}>
              <strong>系统资源使用率 (%)</strong>
            </div>
            {chartError ? (
              <Alert
                message="图表加载失败"
                description={chartError}
                type="error"
                showIcon
                closable
                onClose={() => setChartError(null)}
              />
            ) : (
              <div className="fed-chart-container">
                <ChartErrorBoundary>
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={data || []}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="time" 
                    tick={{ fontSize: 12 }}
                  />
                  <YAxis 
                    domain={[0, 100]}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip 
                    labelFormatter={(label) => `时间: ${label}`}
                    formatter={(value: number, name: string) => [
                      `${value}%`,
                      name === 'cpu' ? 'CPU' :
                      name === 'memory' ? '内存' :
                      name === 'network' ? '网络' :
                      name === 'storage' ? '存储' : name
                    ]}
                  />
                  <Legend 
                    formatter={(value: string) => 
                      value === 'cpu' ? 'CPU' :
                      value === 'memory' ? '内存' :
                      value === 'network' ? '网络' :
                      value === 'storage' ? '存储' : value
                    }
                  />
                  <Area
                    type="monotone"
                    dataKey="cpu"
                    stackId="1"
                    stroke="#1890ff"
                    fill="#1890ff"
                    fillOpacity={0.6}
                  />
                  <Area
                    type="monotone"
                    dataKey="memory"
                    stackId="2"
                    stroke="#52c41a"
                    fill="#52c41a"
                    fillOpacity={0.6}
                  />
                  <Area
                    type="monotone"
                    dataKey="network"
                    stackId="3"
                    stroke="#faad14"
                    fill="#faad14"
                    fillOpacity={0.6}
                  />
                  <Area
                    type="monotone"
                    dataKey="storage"
                    stackId="4"
                    stroke="#722ed1"
                    fill="#722ed1"
                    fillOpacity={0.6}
                  />
                    </AreaChart>
                  </ResponsiveContainer>
                </ChartErrorBoundary>
              </div>
            )}
          </Col>
        )}

        {/* 业务指标 */}
        {(metric === 'all' || metric === 'business') && (
          <Col span={24}>
            <div style={{ marginBottom: '16px' }}>
              <strong>业务指标</strong>
            </div>
            <div className="fed-chart-container">
              <ChartErrorBoundary>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={data || []}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="time" 
                    tick={{ fontSize: 12 }}
                  />
                  <YAxis 
                    yAxisId="left"
                    tick={{ fontSize: 12 }}
                    orientation="left"
                  />
                  <YAxis 
                    yAxisId="right"
                    tick={{ fontSize: 12 }}
                    orientation="right"
                  />
                  <Tooltip 
                    labelFormatter={(label) => `时间: ${label}`}
                    formatter={(value: number, name: string) => [
                      value,
                      name === 'tasks' ? '活跃任务' :
                      name === 'participants' ? '在线参与者' : name
                    ]}
                  />
                  <Legend 
                    formatter={(value: string) => 
                      value === 'tasks' ? '活跃任务' :
                      value === 'participants' ? '在线参与者' : value
                    }
                  />
                  <Line
                    yAxisId="left"
                    type="monotone"
                    dataKey="tasks"
                    stroke="#1890ff"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                  <Line
                    yAxisId="right"
                    type="monotone"
                    dataKey="participants"
                    stroke="#52c41a"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                  </LineChart>
                </ResponsiveContainer>
              </ChartErrorBoundary>
            </div>
          </Col>
        )}
      </Row>
    </Card>
  )
}

export default PerformanceCharts
