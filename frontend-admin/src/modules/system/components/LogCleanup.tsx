/**
 * 日志清理组件
 * 提供日志清理功能，包括清理任务创建、状态查询、历史记录等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Form, 
  Select, 
  InputNumber, 
  Button, 
  Table, 
  Tag, 
  Progress, 
  Space, 
  Modal, 
  Typography, 
  message,
  Divider,
  Row,
  Col,
  Statistic
} from 'antd'
import { 
  DeleteOutlined, 
  ReloadOutlined,
  HistoryOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useSystem } from '@/store/system'
import { formatDateTime } from '@/utils'

const { Title, Text } = Typography
const { Option } = Select
const { confirm } = Modal

// 清理策略选项
const CLEANUP_STRATEGIES = [
  { value: 'TIME_BASED', label: '基于时间清理' },
  { value: 'LEVEL_BASED', label: '基于级别清理' },
  { value: 'CATEGORY_BASED', label: '基于类别清理' }
]

// 日志级别选项
const LOG_LEVELS = [
  { value: 'DEBUG', label: 'DEBUG' },
  { value: 'INFO', label: 'INFO' },
  { value: 'WARN', label: 'WARN' },
  { value: 'ERROR', label: 'ERROR' }
]

// 日志类别选项
const LOG_CATEGORIES = [
  { value: 'SYSTEM', label: '系统日志' },
  { value: 'USER', label: '用户操作' },
  { value: 'VM', label: '虚拟机' },
  { value: 'TASK', label: '任务执行' },
  { value: 'DATA', label: '数据管理' },
  { value: 'MODEL', label: '模型管理' },
  { value: 'SECURITY', label: '安全审计' },
  { value: 'PERFORMANCE', label: '性能监控' }
]

export const LogCleanup: React.FC = () => {
  const {
    cleanupTasks,
    operationLoading
  } = useSystem()

  const [form] = Form.useForm()
  const [cleanupLoading, setCleanupLoading] = useState(false)
  const [historyVisible, setHistoryVisible] = useState(false)
  const [cleanupHistory, setCleanupHistory] = useState<any[]>([])
  const [statusCheckInterval, setStatusCheckInterval] = useState<NodeJS.Timeout | null>(null)
  const [activeCleanupIds, setActiveCleanupIds] = useState<Set<string>>(new Set())

  // 获取清理状态
  const fetchCleanupStatus = async (cleanupId: string) => {
    try {
      const response = await fetch(`/api/log/cleanup/status/${cleanupId}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('access_token')}`
        }
      })
      
      if (response.ok) {
        const result = await response.json()
        return result.data
      }
    } catch (error) {
      console.error('获取清理状态失败:', error)
    }
    return null
  }

  // 开始状态检查
  const startStatusCheck = (cleanupId: string) => {
    const checkStatus = async () => {
      const status = await fetchCleanupStatus(cleanupId)
      if (status) {
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          // 任务完成，移除活跃列表
          setActiveCleanupIds(prev => {
            const newSet = new Set(prev)
            newSet.delete(cleanupId)
            return newSet
          })
          
          // 刷新历史记录
          fetchCleanupHistory()
          
          if (status.status === 'COMPLETED') {
            message.success(`清理任务 ${cleanupId} 已完成，清理了 ${status.deletedRecords} 条记录`)
          } else {
            message.error(`清理任务 ${cleanupId} 执行失败`)
          }
          
          return false // 停止检查
        }
      }
      return true // 继续检查
    }
    
    // 立即检查一次
    checkStatus().then(shouldContinue => {
      if (shouldContinue) {
        // 设置定时检查
        const interval = setInterval(async () => {
          const shouldContinue = await checkStatus()
          if (!shouldContinue) {
            clearInterval(interval)
          }
        }, 3000) // 每3秒检查一次
        
        setStatusCheckInterval(interval)
      }
    })
  }

  // 获取清理历史
  const fetchCleanupHistory = async () => {
    try {
      const response = await fetch('/api/log/cleanup/history', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('access_token')}`
        }
      })
      
      if (response.ok) {
        const result = await response.json()
        setCleanupHistory(result.data.records || [])
      }
    } catch (error) {
      console.error('获取清理历史失败:', error)
    }
  }

  // 初始加载
  useEffect(() => {
    fetchCleanupHistory()
  }, [])

  // 组件卸载时清理定时器
  useEffect(() => {
    return () => {
      if (statusCheckInterval) {
        clearInterval(statusCheckInterval)
      }
    }
  }, [statusCheckInterval])

  // 执行清理
  const handleCleanup = async () => {
    try {
      const values = await form.validateFields()
      
      confirm({
        title: '确认清理日志',
        icon: <ExclamationCircleOutlined />,
        content: (
          <div>
            <p>确定要执行日志清理操作吗？</p>
            <p><Text type="warning">注意：此操作不可逆，请谨慎操作！</Text></p>
          </div>
        ),
        onOk: async () => {
          setCleanupLoading(true)
          
          try {
            // 调用清理API
            const response = await fetch('/api/log/cleanup', {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('access_token')}`
              },
              body: JSON.stringify(values)
            })

            if (response.ok) {
              const result = await response.json()
              const cleanupId = result.data.cleanupId
              message.success(`清理任务已创建，任务ID: ${cleanupId}`)
              
              // 添加到活跃清理任务列表
              setActiveCleanupIds(prev => new Set([...prev, cleanupId]))
              
              // 开始状态检查
              startStatusCheck(cleanupId)
              
              form.resetFields()
              fetchCleanupHistory() // 刷新历史记录
            } else {
              const error = await response.json()
              message.error(`清理失败: ${error.message || '未知错误'}`)
            }
          } catch (error) {
            message.error('清理失败：' + error.message)
          } finally {
            setCleanupLoading(false)
          }
        }
      })
    } catch (error) {
      // 表单验证失败
    }
  }

  // 查看清理历史
  const handleViewHistory = () => {
    setHistoryVisible(true)
    fetchCleanupHistory()
  }

  // 清理历史表格列
  const historyColumns: ColumnsType<any> = [
    {
      title: '清理ID',
      dataIndex: 'cleanupId',
      key: 'cleanupId',
      width: 120,
      render: (cleanupId: string) => (
        <Text code>{cleanupId.substring(0, 8)}...</Text>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const colors = {
          'PROCESSING': 'blue',
          'COMPLETED': 'green',
          'FAILED': 'red',
          'CANCELLED': 'orange'
        }
        return <Tag color={colors[status] || 'default'}>{status}</Tag>
      }
    },
    {
      title: '策略',
      dataIndex: 'strategy',
      key: 'strategy',
      width: 120,
      render: (strategy: string) => {
        const strategyMap = {
          'TIME_BASED': '时间',
          'LEVEL_BASED': '级别',
          'CATEGORY_BASED': '类别'
        }
        return strategyMap[strategy] || strategy
      }
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      width: 150,
      render: (progress: number) => (
        <Progress percent={progress || 0} size="small" />
      )
    },
    {
      title: '删除记录数',
      dataIndex: 'deletedRecords',
      key: 'deletedRecords',
      width: 120,
      render: (deletedRecords: number) => deletedRecords || 0
    },
    {
      title: '释放空间',
      dataIndex: 'freedSpace',
      key: 'freedSpace',
      width: 120,
      render: (freedSpace: number) => 
        freedSpace ? `${(freedSpace / 1024 / 1024).toFixed(2)} MB` : '-'
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (createdAt: string) => formatDateTime(createdAt)
    }
  ]

  return (
    <div>
      <Row gutter={[16, 16]}>
        {/* 清理配置 */}
        <Col span={12}>
          <Card 
            title={
              <Space>
                <DeleteOutlined />
                日志清理配置
              </Space>
            }
          >
            <Form
              form={form}
              layout="vertical"
              initialValues={{
                strategy: 'TIME_BASED',
                retentionDays: 30
              }}
            >
              <Form.Item
                label="清理策略"
                name="strategy"
                rules={[{ required: true, message: '请选择清理策略' }]}
              >
                <Select>
                  {CLEANUP_STRATEGIES.map(strategy => (
                    <Option key={strategy.value} value={strategy.value}>
                      {strategy.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                noStyle
                shouldUpdate={(prevValues, currentValues) => 
                  prevValues.strategy !== currentValues.strategy
                }
              >
                {({ getFieldValue }) => {
                  const strategy = getFieldValue('strategy')
                  
                  if (strategy === 'TIME_BASED') {
                    return (
                      <Form.Item
                        label="保留天数"
                        name="retentionDays"
                        rules={[{ required: true, message: '请输入保留天数' }]}
                      >
                        <InputNumber 
                          min={1} 
                          max={365} 
                          placeholder="保留最近N天的日志"
                          style={{ width: '100%' }}
                        />
                      </Form.Item>
                    )
                  }
                  
                  if (strategy === 'LEVEL_BASED') {
                    return (
                      <Form.Item
                        label="清理级别"
                        name="level"
                        rules={[{ required: true, message: '请选择要清理的日志级别' }]}
                      >
                        <Select placeholder="选择要清理的日志级别">
                          {LOG_LEVELS.map(level => (
                            <Option key={level.value} value={level.value}>
                              {level.label}
                            </Option>
                          ))}
                        </Select>
                      </Form.Item>
                    )
                  }
                  
                  if (strategy === 'CATEGORY_BASED') {
                    return (
                      <Form.Item
                        label="清理类别"
                        name="category"
                        rules={[{ required: true, message: '请选择要清理的日志类别' }]}
                      >
                        <Select placeholder="选择要清理的日志类别">
                          {LOG_CATEGORIES.map(category => (
                            <Option key={category.value} value={category.value}>
                              {category.label}
                            </Option>
                          ))}
                        </Select>
                      </Form.Item>
                    )
                  }
                  
                  return null
                }}
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button 
                    type="primary" 
                    danger
                    icon={<DeleteOutlined />}
                    onClick={handleCleanup}
                    loading={cleanupLoading}
                  >
                    执行清理
                  </Button>
                  <Button 
                    icon={<HistoryOutlined />}
                    onClick={handleViewHistory}
                  >
                    查看历史
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* 最近清理任务 */}
        <Col span={12}>
          <Card 
            title={
              <Space>
                <HistoryOutlined />
                最近清理任务
              </Space>
            }
            extra={
              <Button 
                icon={<ReloadOutlined />}
                onClick={() => fetchCleanupHistory()}
                loading={operationLoading}
                size="small"
              >
                刷新
              </Button>
            }
          >
            {cleanupHistory && cleanupHistory.length > 0 ? (
              <Table
                dataSource={cleanupHistory.slice(0, 5)}
                columns={historyColumns.filter(col => 
                  ['status', 'strategy', 'progress', 'createdAt'].includes(col.key as string)
                )}
                pagination={false}
                size="small"
                rowKey="cleanupId"
              />
            ) : (
              <div style={{ textAlign: 'center', padding: '20px', color: '#999' }}>
                暂无清理任务记录
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 清理历史模态框 */}
      <Modal
        title="日志清理历史"
        open={historyVisible}
        onCancel={() => setHistoryVisible(false)}
        footer={null}
        width={1000}
      >
        <Table
          dataSource={cleanupHistory}
          columns={historyColumns}
          rowKey="cleanupId"
          loading={cleanupLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
          size="small"
        />
      </Modal>
    </div>
  )
}

export default LogCleanup
