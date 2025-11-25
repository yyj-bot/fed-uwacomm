/**
 * 水下声呐目标识别任务详情页
 * 固定显示水下声呐目标识别任务的概览信息
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
  Timeline,
  Tooltip,
  Image
} from 'antd'
import { 
  ArrowLeftOutlined,
  LineChartOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  LoadingOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import './TaskDetailPage.css'

const { TabPane } = Tabs

const SonarRecognitionDetailPage: React.FC = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState<string>('overview')

  // 硬编码的任务数据
  const taskData = {
    taskId: 'e5f67890123456789012345678901234',
    taskName: '水下声呐目标识别',
    taskType: 'CLASSIFICATION',
    status: 'COMPLETED',
    progress: 100,
    algorithm: 'FEDERATED_AVERAGING',
    currentRound: 5,
    totalRounds: 15,
    participants: 3,
    createTime: '2024/1/1 17:00:00',
    startTime: '2024/1/1 17:05:00',
    description: '基于水下机器人的水下声呐目标识别任务'
  }

  const recognitionStats = {
    accuracy: 100.0,
    latency: 2.4
  }

  // 参与者数据
  const participantsData = [
    {
      key: '1',
      vmId: 'AUV-01',
      vmName: 'AUV-01',
      status: '运行中',
      currentRound: 5,
      accuracy: 0.85,
      dataSize: 1000,
      recognitionResult: '铁管'
    },
    {
      key: '2',
      vmId: 'AUV-02',
      vmName: 'AUV-02',
      status: '运行中',
      currentRound: 5,
      accuracy: 0.82,
      dataSize: 950,
      recognitionResult: '螺旋桨'
    },
    {
      key: '3',
      vmId: 'ROV-02',
      vmName: 'ROV-02',
      status: '运行中',
      currentRound: 5,
      accuracy: 0.84,
      dataSize: 1050,
      recognitionResult: '轮胎'
    }
  ]

  // 日志数据
  const logsData = [
    { time: '2024/1/1 17:05:00', level: 'INFO', message: 'AUV-01连接成功' },
    { time: '2024/1/1 17:05:15', level: 'INFO', message: 'AUV-02连接成功' },
    { time: '2024/1/1 17:07:15', level: 'INFO', message: 'ROV-02连接成功' },
    { time: '2024/1/1 17:07:20', level: 'INFO', message: '任务开始执行' },
    { time: '2024/1/1 17:09:15', level: 'INFO', message: '目标识别成功' }
  ]

  const participantsColumns = [
    { title: '节点ID', dataIndex: 'vmId', key: 'vmId' },
    { title: '节点名称', dataIndex: 'vmName', key: 'vmName' },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <Tag color={status === '运行中' ? 'green' : 'default'}>{status}</Tag>
      )
    },
    {
      title: '声呐图像',
      key: 'sonarImage',
      render: (record: any) => (
        <Image
          src={`/sonar-images/${record.vmId}.jpg`}
          alt={`${record.vmName}声呐图像`}
          width={100}
          height={60}
          style={{ objectFit: 'cover', borderRadius: '4px' }}
          preview={{
            src: `/sonar-images/${record.vmId}.jpg`
          }}
        />
      )
    },
    {
      title: '识别结果',
      dataIndex: 'recognitionResult',
      key: 'recognitionResult',
      render: (result: string) => (
        <Tag color="geekblue" style={{ fontSize: 13, padding: '2px 10px' }}>
          {result}
        </Tag>
      )
    }
  ]

  const progressColumns = [
    { title: '轮次', dataIndex: 'round', key: 'round' },
    { 
      title: '准确率', 
      dataIndex: 'accuracy', 
      key: 'accuracy',
      render: (acc: number) => `${(acc * 100).toFixed(2)}%`
    },
    { 
      title: '损失', 
      dataIndex: 'loss', 
      key: 'loss',
      render: (loss: number) => loss.toFixed(4)
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
              title="识别准确率" 
              value={recognitionStats.accuracy} 
              suffix="%"
              prefix={<LineChartOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="平均识别耗时" 
              value={recognitionStats.latency}
              suffix="秒"
              prefix={<ClockCircleOutlined />}
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
              <Descriptions.Item label="创建时间">{taskData.createTime}</Descriptions.Item>
              <Descriptions.Item label="启动时间">{taskData.startTime}</Descriptions.Item>
              <Descriptions.Item label="任务类型">
                <Tag color="green">目标识别</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="任务描述" span={2}>
                {taskData.description}
              </Descriptions.Item>
              <Descriptions.Item label="任务进度" span={2}>
                <Progress percent={taskData.progress} status="active" />
              </Descriptions.Item>
            </Descriptions>
          </TabPane>

          <TabPane tab="结果" key="participants">
            <Table 
              columns={participantsColumns} 
              dataSource={participantsData}
              pagination={false}
            />
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

export default SonarRecognitionDetailPage
