/**
 * 性能图表组件
 * 显示系统性能指标的图表数据
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import { Card, Row, Col, Select, DatePicker, Space, Skeleton } from 'antd'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts'

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
  const [timeRange, setTimeRange] = useState<string>('24h')
  const [metric, setMetric] = useState<string>('all')

  // 模拟图表数据
  const generateMockData = (hours: number) => {
    const data = []
    const now = new Date()
    for (let i = hours; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60 * 60 * 1000)
      data.push({
        time: time.toISOString().substr(11, 5),
        timestamp: time.getTime(),
        cpu: Math.floor(Math.random() * 30) + 20,
        memory: Math.floor(Math.random() * 25) + 40,
        network: Math.floor(Math.random() * 40) + 10,
        storage: Math.floor(Math.random() * 20) + 30,
        tasks: Math.floor(Math.random() * 5) + 2,
        participants: Math.floor(Math.random() * 10) + 15
      })
    }
    return data
  }

  const data = generateMockData(24) // 使用模拟数据

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
            <div className="fed-chart-container">
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={data}>
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
            </div>
          </Col>
        )}

        {/* 业务指标 */}
        {(metric === 'all' || metric === 'business') && (
          <Col span={24}>
            <div style={{ marginBottom: '16px' }}>
              <strong>业务指标</strong>
            </div>
            <div className="fed-chart-container">
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={data}>
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
            </div>
          </Col>
        )}
      </Row>
    </Card>
  )
}

export default PerformanceCharts
