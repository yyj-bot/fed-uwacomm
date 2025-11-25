/**
 * 海底地形声学分析任务详情页
 * 固定显示海底地形声学分析任务的概览信息
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
  PauseCircleOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import './TaskDetailPage.css'

const { TabPane } = Tabs

const SeabedTerrainDetailPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<string>('overview')

  // 硬编码的任务数据
  const taskData = {
    taskId: 'd4e5f678901234567890123456789012',
    taskName: '水声通信质量评估',
    taskType: 'PERFORMANCE_ANALYSIS',
    status: 'COMPLETED',
    progress: 100,
    algorithm: 'FEDNOVA',
    currentRound: 10,
    totalRounds: 10,
    participants: 4,
    createTime: '2024/1/3 14:00:00',
    startTime: '2024/1/3 14:05:00',
    pauseTime: '2024/1/3 15:30:00',
    description: '基于ofdm的水声通信效果质量评估与性能'
  }

  // 参与者数据
  const participantsData = [
    {
      key: '1',
      vmId: 'AUV-01',
      vmName: 'AUV-01',
      status: '已完成',
      currentRound: 10,
      BER: 0.0002,
      dataSize: 800
    },
    {
      key: '2',
      vmId: 'AUV-03',
      vmName: 'AUV-03',
      status: '已完成',
      currentRound: 10,
      BER: 0.0003,
      dataSize: 850
    },
    {
      key: '3',
      vmId: 'ROV-01',
      vmName: 'ROV-01',
      status: '已完成',
      currentRound: 10,
      BER: 0.0001,
      dataSize: 900
    },
    {
      key: '4',
      vmId: 'ROV-02',
      vmName: 'ROV-02',
      status: '已完成',
      currentRound: 10,
      BER: 0.0002,
      dataSize: 820
    }
  ]

  // 训练进度数据
  const trainingProgressData = [
    { key: '1', round: 1, snr: 5, BER: 0.4991, SER: 0.9881, EVM: 0.21, duration: '165s' },
    { key: '2', round: 2, snr: 5, BER: 0.215, SER: 0.4231, EVM: 0.15, duration: '158s' },
    { key: '3', round: 3, snr: 5, BER: 0.0023, SER: 0.0045, EVM: 0.14, duration: '152s' },
    { key: '4', round: 4, snr: 5, BER: 0.0005, SER: 0.0011, EVM: 0.1, duration: '150s' },
    { key: '5', round: 5, snr: 5, BER: 0.0003, SER: 0.0005, EVM: 0.06, duration: '148s' },
    { key: '6', round: 6, snr: 10, BER: 0.4975, SER: 0.991, EVM: 0.2, duration: '160s' },
    { key: '7', round: 7, snr: 10, BER: 0.2041, SER: 0.4015, EVM: 0.13, duration: '155s' },
    { key: '8', round: 8, snr: 10, BER: 0.0015, SER: 0.0031, EVM: 0.13, duration: '150s' },
    { key: '9', round: 9, snr: 10, BER: 0.0004, SER: 0.0007, EVM: 0.11, duration: '148s' },
    { key: '10', round: 10, snr: 10, BER: 0.0002, SER: 0.0004, EVM: 0.05, duration: '145s' }
  ]

  // 日志数据
  const logsData = [
    { time: '2024/1/3 14:05:00', level: 'INFO', message: '任务开始执行' },
    { time: '2024/1/3 14:05:15', level: 'INFO', message: '第1轮训练开始' },
    { time: '2024/1/3 14:08:00', level: 'INFO', message: '第1轮训练完成' },
    { time: '2024/1/3 14:08:05', level: 'INFO', message: '第2轮训练开始' },
    { time: '2024/1/3 14:10:43', level: 'INFO', message: '第2轮训练完成' },
    { time: '2024/1/3 15:28:20', level: 'INFO', message: '第10轮训练完成' },
    { time: '2024/1/3 15:30:00', level: 'INFO', message: '任务已完成' }
  ]

  const participantsColumns = [
    { title: '节点ID', dataIndex: 'vmId', key: 'vmId' },
    { title: '节点名称', dataIndex: 'vmName', key: 'vmName' },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '已完成' ? 'green' : 'default'}>{status}</Tag>
      )
    },
    { title: '当前轮次', dataIndex: 'currentRound', key: 'currentRound' },
    { 
      title: 'BER', 
      dataIndex: 'BER', 
      key: 'BER',
      render: (score: number) => score.toFixed(4)
    },
    { title: '数据量', dataIndex: 'dataSize', key: 'dataSize' }
  ]

  const progressColumns = [
    { title: '轮次', dataIndex: 'round', key: 'round' },
    { title: 'SNR', dataIndex: 'snr', key: 'snr' },
    { 
      title: 'BER', 
      dataIndex: 'BER', 
      key: 'BER',
      render: (val: number) => val.toFixed(4)
    },
    { 
      title: 'SER', 
      dataIndex: 'SER', 
      key: 'SER',
      render: (val: number) => val.toFixed(4)
    },
    { 
      title: 'EVM', 
      dataIndex: 'EVM', 
      key: 'EVM',
      render: (val: number) => val.toFixed(2)
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
                color="green" 
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
              title="当前轮次" 
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
              title="当前误码率" 
              value={0.0002} 
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
                <Tag color="blue">性能分析</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="算法">
                <Tag color="purple">FedNova算法</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{taskData.createTime}</Descriptions.Item>
              <Descriptions.Item label="启动时间">{taskData.startTime}</Descriptions.Item>
              <Descriptions.Item label="暂停时间">{taskData.pauseTime}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color="green">已完成</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="任务描述" span={2}>
                {taskData.description}
              </Descriptions.Item>
              <Descriptions.Item label="训练进度" span={2}>
                <Progress percent={taskData.progress} status="active" />
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
            <Card title="结果图" bodyStyle={{ textAlign: 'center' }}>
              <img
                src="/results/OFDM-results.png"
                alt="水声通信质量评估 OFDM 结果"
                style={{ maxWidth: '70%', borderRadius: 8 }}
              />
            </Card>
          </TabPane>

          <TabPane tab="日志" key="logs">
            <Timeline>
              {logsData.map((log, index) => (
                <Timeline.Item 
                  key={index}
                  color={log.level === 'INFO' ? 'blue' : log.level === 'WARNING' ? 'orange' : 'red'}
                >
                  <p style={{ marginBottom: 4, color: '#8c8c8c', fontSize: 12 }}>
                    {log.time}
                  </p>
                  <p style={{ margin: 0 }}>
                    <Tag color={log.level === 'INFO' ? 'blue' : log.level === 'WARNING' ? 'orange' : 'red'}>
                      {log.level}
                    </Tag>
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

export default SeabedTerrainDetailPage
