/**
 * 声学传播回归分析任务详情页
 * 固定显示声学传播回归分析任务的概览信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import { 
  Card, 
  Row, 
  Col, 
  Descriptions, 
  Tag, 
  Progress, 
  Button, 
  Space, 
  Tabs, 
  Table,
  Statistic,
  Timeline
} from 'antd'
import { 
  ArrowLeftOutlined,
  LineChartOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { 
  ResponsiveContainer,
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend
} from 'recharts'
import './TaskDetailPage.css'

const { TabPane } = Tabs

const AcousticPropagationDetailPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<string>('overview')

  // 硬编码的任务数据
  const taskData = {
    taskId: 'c3d4e5f6789012345678901234567890',
    taskName: '声学传播回归分析',
    taskType: 'REGRESSION',
    status: 'COMPLETED',
    progress: 100,
    algorithm: 'FEDPROX',
    currentRound: 10,
    totalRounds: 10,
    participants: 4,
    createTime: '2024/1/2 10:00:00',
    startTime: '2024/1/2 10:05:00',
    endTime: '2024/1/2 11:20:00',
    description: '基于联邦学习的声学传播路径损失回归预测'
  }

  // 参与者数据
  const participantsData = [
    {
      key: '1',
      vmId: 'AUV-02',
      vmName: 'AUV-02',
      status: '已完成',
      currentRound: 10,
      mse: 0.024,
      dataSize: 1200
    },
    {
      key: '2',
      vmId: 'AUV-03',
      vmName: 'AUV-03',
      status: '已完成',
      currentRound: 10,
      mse: 0.028,
      dataSize: 1150
    },
    {
      key: '3',
      vmId: 'ROV-01',
      vmName: 'ROV-01',
      status: '已完成',
      currentRound: 10,
      mse: 0.022,
      dataSize: 1300
    },
    {
      key: '4',
      vmId: 'ROV-02',
      vmName: 'ROV-02',
      status: '已完成',
      currentRound: 10,
      mse: 0.0245,
      dataSize: 1200
    }
  ]

  // 训练进度数据
  const trainingProgressData = [
    { key: '1', round: 1, mse: 0.185, r2: 0.52, duration: '145s' },
    { key: '2', round: 2, mse: 0.142, r2: 0.63, duration: '138s' },
    { key: '3', round: 3, mse: 0.108, r2: 0.72, duration: '142s' },
    { key: '4', round: 4, mse: 0.085, r2: 0.78, duration: '140s' },
    { key: '5', round: 5, mse: 0.068, r2: 0.83, duration: '139s' },
    { key: '6', round: 6, mse: 0.052, r2: 0.87, duration: '141s' },
    { key: '7', round: 7, mse: 0.041, r2: 0.90, duration: '143s' },
    { key: '8', round: 8, mse: 0.033, r2: 0.92, duration: '138s' },
    { key: '9', round: 9, mse: 0.028, r2: 0.94, duration: '140s' },
    { key: '10', round: 10, mse: 0.025, r2: 0.95, duration: '142s' }
  ]

  const chartHeight = 400
  const chartMargin = { top: 10, right: 36, left: 24, bottom: 0 }

  // 日志数据
  const logsData = [
    { time: '2024/1/2 10:05:00', level: 'INFO', message: '任务开始执行' },
    { time: '2024/1/2 10:05:15', level: 'INFO', message: '第1轮训练开始' },
    { time: '2024/1/2 10:07:40', level: 'INFO', message: '第1轮训练完成，MSE: 0.185' },
    { time: '2024/1/2 10:07:45', level: 'INFO', message: '第2轮训练开始' },
    { time: '2024/1/2 10:10:03', level: 'INFO', message: '第2轮训练完成，MSE: 0.142' },
    { time: '2024/1/2 11:19:50', level: 'INFO', message: '第10轮训练完成，MSE: 0.025' },
    { time: '2024/1/2 11:20:00', level: 'INFO', message: '任务执行完成' }
  ]

  const participantsColumns = [
    { title: '节点ID', dataIndex: 'vmId', key: 'vmId' },
    { title: '节点名称', dataIndex: 'vmName', key: 'vmName' },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '已完成' ? 'success' : 'default'}>{status}</Tag>
      )
    },
    { title: '完成轮次', dataIndex: 'currentRound', key: 'currentRound' },
    { 
      title: 'MSE', 
      dataIndex: 'mse', 
      key: 'mse',
      render: (mse: number) => mse.toFixed(4)
    },
    { title: '数据量', dataIndex: 'dataSize', key: 'dataSize' }
  ]

  const progressColumns = [
    { title: '轮次', dataIndex: 'round', key: 'round' },
    { 
      title: 'MSE', 
      dataIndex: 'mse', 
      key: 'mse',
      render: (mse: number) => mse.toFixed(4)
    },
    { 
      title: 'R² Score', 
      dataIndex: 'r2', 
      key: 'r2',
      render: (r2: number) => r2.toFixed(4)
    },
    { title: '用时', dataIndex: 'duration', key: 'duration' }
  ]

  return (
    <div className="task-detail-page">
      {/* 页面头部 */}
      <Card className="header-card" bordered={false}>
        <div className="page-header">
          <div className="header-left">
            <div>
              <h2 className="page-title">{taskData.taskName}</h2>
              <p className="page-description">任务ID: {taskData.taskId}</p>
            </div>
          </div>
          <div className="header-actions">
            <Space>
              <Tag 
                color="success" 
                icon={<CheckCircleOutlined />}
              >
                已完成
              </Tag>
            </Space>
          </div>
        </div>
      </Card>

      {/* 进度统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic 
              title="总体进度" 
              value={taskData.progress} 
              suffix="%" 
              prefix={<LineChartOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="完成轮次" 
              value={taskData.currentRound} 
              suffix={`/ ${taskData.totalRounds}`}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="参与节点" 
              value={taskData.participants} 
              suffix="个"
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="最终MSE" 
              value={0.025} 
              precision={4}
              prefix={<LineChartOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 详情内容标签页 */}
      <Card>
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="概览" key="overview">
            <Descriptions bordered column={2}>
              <Descriptions.Item label="任务名称">{taskData.taskName}</Descriptions.Item>
              <Descriptions.Item label="任务ID">{taskData.taskId}</Descriptions.Item>
              <Descriptions.Item label="任务类型">
                <Tag color="green">回归任务</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="算法">
                <Tag color="purple">FedAVG算法</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{taskData.createTime}</Descriptions.Item>
              <Descriptions.Item label="启动时间">{taskData.startTime}</Descriptions.Item>
              <Descriptions.Item label="完成时间">{taskData.endTime}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color="success">已完成</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="任务描述" span={2}>
                {taskData.description}
              </Descriptions.Item>
              <Descriptions.Item label="训练进度" span={2}>
                <Progress percent={taskData.progress} status="success" />
              </Descriptions.Item>
            </Descriptions>
          </TabPane>

          <TabPane tab="参与者" key="participants">
            <Table 
              columns={participantsColumns} 
              dataSource={participantsData}
              pagination={false}
            />
          </TabPane>

          <TabPane tab="训练进度" key="progress">
            <Table 
              columns={progressColumns} 
              dataSource={trainingProgressData}
              pagination={false}
            />
          </TabPane>

          <TabPane tab="结果" key="results">
            <Card title="训练结果概览" style={{ marginBottom: 16 }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic title="最终MSE" value={0.025} precision={4} />
                </Col>
                <Col span={12}>
                  <Statistic title="R² Score" value={0.95} precision={4} />
                </Col>
              </Row>
            </Card>
            <Row gutter={16}>
              <Col span={12}>
                <Card title="MSE 曲线">
                  <div className="chart-container">
                    <ResponsiveContainer width="100%" height={chartHeight}>
                      <LineChart data={trainingProgressData} margin={chartMargin}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis 
                          dataKey="round" 
                          tickFormatter={(value) => `第${value}轮`}
                          interval={0}
                          padding={{ left: 0, right: 30 }}
                        />
                        <YAxis tickFormatter={(value) => value.toFixed(3)} />
                        <Tooltip
                          formatter={(value: number) => value.toFixed(4)}
                          labelFormatter={(round) => `第 ${round} 轮`}
                        />
                        <Legend />
                        <Line
                          type="monotone"
                          dataKey="mse"
                          name="MSE"
                          stroke="#1890ff"
                          strokeWidth={2}
                          dot={{ r: 4 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </Card>
              </Col>
              <Col span={12}>
                <Card title="R² 曲线">
                  <div className="chart-container">
                    <ResponsiveContainer width="100%" height={chartHeight}>
                      <LineChart data={trainingProgressData} margin={chartMargin}>
                        <CartesianGrid strokeDasharray="3 3" />
                        <XAxis 
                          dataKey="round" 
                          tickFormatter={(value) => `第${value}轮`}
                          interval={0}
                          padding={{ left: 0, right: 30 }}
                        />
                        <YAxis domain={[0, 1]} tickFormatter={(value) => value.toFixed(2)} />
                        <Tooltip
                          formatter={(value: number) => value.toFixed(4)}
                          labelFormatter={(round) => `第 ${round} 轮`}
                        />
                        <Legend />
                        <Line
                          type="monotone"
                          dataKey="r2"
                          name="R²"
                          stroke="#52c41a"
                          strokeWidth={2}
                          dot={{ r: 4 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </Card>
              </Col>
            </Row>
          </TabPane>

          <TabPane tab="日志" key="logs">
            <Timeline>
              {logsData.map((log, index) => (
                <Timeline.Item 
                  key={index}
                  color={log.level === 'INFO' ? 'blue' : 'red'}
                >
                  <p style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>
                    {log.time}
                  </p>
                  <p style={{ margin: 0 }}>
                    <Tag color={log.level === 'INFO' ? 'blue' : 'red'}>{log.level}</Tag>
                    {log.message}
                  </p>
                </Timeline.Item>
              ))}
            </Timeline>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default AcousticPropagationDetailPage
