/**
 * 联邦学习任务列表页面
 * 显示所有联邦学习任务，支持搜索、筛选、分页等功能
 * 
 * @author FedUWAComm Team
 * @version 1.4.0
 */

import React, { useEffect, useState, useCallback } from 'react'
import { 
  Card, 
  Table, 
  Button, 
  Tag, 
  Space, 
  Input, 
  Select, 
  DatePicker, 
  Tooltip,
  Progress,
  message,
  Modal,
  Popconfirm
} from 'antd'
import { 
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  DeleteOutlined,
  EyeOutlined,
  EditOutlined,
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { useTask } from '@/store/federated-task/useFederatedTaskStore'
import type { FederatedTask } from '@/types'
import './TaskListPage.css'

const { RangePicker } = DatePicker
const { Option } = Select

// 任务状态配置
const TASK_STATUS_CONFIG = {
  CREATED: { color: 'blue', text: '已创建' },
  CONFIGURED: { color: 'cyan', text: '已配置' },
  RUNNING: { color: 'green', text: '运行中' },
  PAUSED: { color: 'orange', text: '已暂停' },
  STOPPED: { color: 'red', text: '已停止' },
  COMPLETED: { color: 'success', text: '已完成' },
  FAILED: { color: 'error', text: '执行失败' },
  CANCELLED: { color: 'default', text: '已取消' }
}

// 任务类型配置
const TASK_TYPE_CONFIG = {
  CLASSIFICATION: '分类任务',
  REGRESSION: '回归任务',
  CLUSTERING: '聚类任务',
  ANOMALY_DETECTION: '异常检测'
}

const TaskListPage: React.FC = () => {
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    taskList,
    taskListTotal,
    taskListLoading,
    taskListError,
    pagination,
    queryParams,
    fetchTaskList,
    refreshTaskList,
    startTask,
    pauseTask,
    resumeTask,
    stopTask,
    cancelTask,
    deleteTask,
    setPagination,
    setQueryParams,
    clearError,
    canStartTask,
    canPauseTask,
    canResumeTask,
    canStopTask,
    canCancelTask,
    canDeleteTask,
    getTaskProgress,
    isTaskOperating
  } = useTask()

  // 本地状态
  const [searchKeyword, setSearchKeyword] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [typeFilter, setTypeFilter] = useState<string>('')
  const [dateRange, setDateRange] = useState<[any, any] | null>(null)

  // 初始化加载任务列表
  useEffect(() => {
    fetchTaskList()
  }, [fetchTaskList])

  // 处理搜索
  const handleSearch = useCallback(() => {
    const params: any = {}
    
    if (searchKeyword.trim()) {
      params.keyword = searchKeyword.trim()
    }
    
    if (statusFilter) {
      params.status = statusFilter
    }
    
    if (typeFilter) {
      params.type = typeFilter
    }
    
    if (dateRange && dateRange[0] && dateRange[1]) {
      params.startDate = dateRange[0].format('YYYY-MM-DD')
      params.endDate = dateRange[1].format('YYYY-MM-DD')
    }
    
    setQueryParams(params)
    setPagination(1) // 重置到第一页
    fetchTaskList(params)
  }, [searchKeyword, statusFilter, typeFilter, dateRange, setQueryParams, setPagination, fetchTaskList])

  // 重置搜索条件
  const handleReset = useCallback(() => {
    setSearchKeyword('')
    setStatusFilter('')
    setTypeFilter('')
    setDateRange(null)
    setQueryParams({})
    setPagination(1)
    fetchTaskList()
  }, [setQueryParams, setPagination, fetchTaskList])

  // 处理分页变化
  const handleTableChange = useCallback((page: number, size: number) => {
    setPagination(page, size)
    fetchTaskList({ ...queryParams, page, size })
  }, [queryParams, setPagination, fetchTaskList])

  // 任务操作处理函数
  const handleStartTask = useCallback(async (task: FederatedTask) => {
    try {
      const result = await startTask(task.taskId)
      if (result.success) {
        message.success('任务启动成功')
        refreshTaskList()
      } else {
        message.error(result.error || '任务启动失败')
      }
    } catch (error) {
      message.error('任务启动失败')
    }
  }, [startTask, refreshTaskList])

  const handlePauseTask = useCallback(async (task: FederatedTask) => {
    try {
      const result = await pauseTask(task.taskId)
      if (result.success) {
        message.success('任务暂停成功')
        refreshTaskList()
      } else {
        message.error(result.error || '任务暂停失败')
      }
    } catch (error) {
      message.error('任务暂停失败')
    }
  }, [pauseTask, refreshTaskList])

  const handleResumeTask = useCallback(async (task: FederatedTask) => {
    try {
      const result = await resumeTask(task.taskId)
      if (result.success) {
        message.success('任务恢复成功')
        refreshTaskList()
      } else {
        message.error(result.error || '任务恢复失败')
      }
    } catch (error) {
      message.error('任务恢复失败')
    }
  }, [resumeTask, refreshTaskList])

  const handleStopTask = useCallback(async (task: FederatedTask) => {
    Modal.confirm({
      title: '确认停止任务',
      icon: <ExclamationCircleOutlined />,
      content: `确定要停止任务"${task.taskName}"吗？停止后可以查看部分结果。`,
      okText: '确认停止',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await stopTask(task.taskId, {
            reason: '用户主动停止',
            saveCheckpoint: true
          })
          if (result.success) {
            message.success('任务停止成功')
            refreshTaskList()
          } else {
            message.error(result.error || '任务停止失败')
          }
        } catch (error) {
          message.error('任务停止失败')
        }
      }
    })
  }, [stopTask, refreshTaskList])

  const handleCancelTask = useCallback(async (task: FederatedTask) => {
    Modal.confirm({
      title: '确认取消任务',
      icon: <ExclamationCircleOutlined />,
      content: `确定要取消任务"${task.taskName}"吗？取消后无法恢复。`,
      okText: '确认取消',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await cancelTask(task.taskId, {
            reason: '用户主动取消'
          })
          if (result.success) {
            message.success('任务取消成功')
            refreshTaskList()
          } else {
            message.error(result.error || '任务取消失败')
          }
        } catch (error) {
          message.error('任务取消失败')
        }
      }
    })
  }, [cancelTask, refreshTaskList])

  const handleDeleteTask = useCallback(async (task: FederatedTask) => {
    Modal.confirm({
      title: '确认删除任务',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除任务"${task.taskName}"吗？删除后无法恢复，建议保留模型数据。`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await deleteTask(task.taskId, {
            deleteData: false, // 保留数据
            deleteModel: false // 保留模型
          })
          if (result.success) {
            message.success('任务删除成功')
            refreshTaskList()
          } else {
            message.error(result.error || '任务删除失败')
          }
        } catch (error) {
          message.error('任务删除失败')
        }
      }
    })
  }, [deleteTask, refreshTaskList])

  // 表格列定义
  const columns: ColumnsType<FederatedTask> = [
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 200,
      ellipsis: true,
      render: (text: string, record: FederatedTask) => (
        <Tooltip title={text}>
          <Button 
            type="link" 
            onClick={() => navigate(`/federated-learning/tasks/${record.taskId}`)}
            style={{ padding: 0, height: 'auto' }}
          >
            {text}
          </Button>
        </Tooltip>
      )
    },
    {
      title: '任务类型',
      dataIndex: 'taskType',
      key: 'taskType',
      width: 120,
      render: (type: keyof typeof TASK_TYPE_CONFIG) => (
        <Tag color="blue">{TASK_TYPE_CONFIG[type]}</Tag>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: keyof typeof TASK_STATUS_CONFIG) => {
        const config = TASK_STATUS_CONFIG[status]
        return <Tag color={config.color}>{config.text}</Tag>
      }
    },
    {
      title: '进度',
      key: 'progress',
      width: 150,
      render: (_, record: FederatedTask) => {
        const progress = getTaskProgress(record)
        const isRunning = record.status === 'RUNNING'
        
        return (
          <div>
            <Progress 
              percent={progress} 
              size="small" 
              status={isRunning ? 'active' : 'normal'}
              format={(percent) => `${percent}%`}
            />
            {record.currentRound && record.totalRounds && (
              <div style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>
                轮次: {record.currentRound}/{record.totalRounds}
              </div>
            )}
          </div>
        )
      }
    },
    {
      title: '参与者',
      dataIndex: 'participantCount',
      key: 'participantCount',
      width: 80,
      render: (count: number) => (
        <Tag color="geekblue">{count}个</Tag>
      )
    },
    {
      title: '算法',
      dataIndex: 'algorithm',
      key: 'algorithm',
      width: 120,
      ellipsis: true
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (time: string) => new Date(time).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      fixed: 'right',
      render: (_, record: FederatedTask) => {
        const operating = isTaskOperating(record.taskId)
        
        return (
          <Space size="small">
            <Tooltip title="查看详情">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => navigate(`/federated-learning/tasks/${record.taskId}`)}
              />
            </Tooltip>
            
            {canStartTask(record) && (
              <Tooltip title="启动任务">
                <Button
                  type="text"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleStartTask(record)}
                  loading={operating}
                  style={{ color: '#52c41a' }}
                />
              </Tooltip>
            )}
            
            {canPauseTask(record) && (
              <Tooltip title="暂停任务">
                <Button
                  type="text"
                  icon={<PauseCircleOutlined />}
                  onClick={() => handlePauseTask(record)}
                  loading={operating}
                  style={{ color: '#faad14' }}
                />
              </Tooltip>
            )}
            
            {canResumeTask(record) && (
              <Tooltip title="恢复任务">
                <Button
                  type="text"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleResumeTask(record)}
                  loading={operating}
                  style={{ color: '#52c41a' }}
                />
              </Tooltip>
            )}
            
            {canStopTask(record) && (
              <Tooltip title="停止任务">
                <Button
                  type="text"
                  icon={<StopOutlined />}
                  onClick={() => handleStopTask(record)}
                  loading={operating}
                  style={{ color: '#ff4d4f' }}
                />
              </Tooltip>
            )}
            
            {canCancelTask(record) && (
              <Tooltip title="取消任务">
                <Popconfirm
                  title="确定要取消这个任务吗？"
                  onConfirm={() => handleCancelTask(record)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    icon={<DeleteOutlined />}
                    loading={operating}
                    style={{ color: '#ff7875' }}
                  />
                </Popconfirm>
              </Tooltip>
            )}
            
            {canDeleteTask(record) && (
              <Tooltip title="删除任务">
                <Button
                  type="text"
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteTask(record)}
                  style={{ color: '#ff4d4f' }}
                />
              </Tooltip>
            )}
          </Space>
        )
      }
    }
  ]

  return (
    <div className="task-list-page">
      <Card>
        {/* 页面标题和操作 */}
        <div className="page-header">
          <div className="page-title">
            <h2>联邦学习任务</h2>
            <span className="page-description">管理和监控联邦学习任务的执行状态</span>
          </div>
          <div className="page-actions">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/federated-learning/tasks/create')}
            >
              创建任务
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => refreshTaskList()}
              loading={taskListLoading}
            >
              刷新
            </Button>
          </div>
        </div>

        {/* 搜索和筛选 */}
        <div className="search-section">
          <Space wrap size="middle">
            <Input
              placeholder="搜索任务名称或描述"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 250 }}
              prefix={<SearchOutlined />}
            />
            
            <Select
              placeholder="任务状态"
              value={statusFilter}
              onChange={setStatusFilter}
              allowClear
              style={{ width: 120 }}
            >
              {Object.entries(TASK_STATUS_CONFIG).map(([value, config]) => (
                <Option key={value} value={value}>{config.text}</Option>
              ))}
            </Select>
            
            <Select
              placeholder="任务类型"
              value={typeFilter}
              onChange={setTypeFilter}
              allowClear
              style={{ width: 120 }}
            >
              {Object.entries(TASK_TYPE_CONFIG).map(([value, text]) => (
                <Option key={value} value={value}>{text}</Option>
              ))}
            </Select>
            
            <RangePicker
              value={dateRange}
              onChange={setDateRange}
              placeholder={['开始日期', '结束日期']}
            />
            
            <Button type="primary" onClick={handleSearch} loading={taskListLoading}>
              搜索
            </Button>
            
            <Button onClick={handleReset}>
              重置
            </Button>
          </Space>
        </div>

        {/* 任务列表表格 */}
        <Table
          columns={columns}
          dataSource={taskList}
          rowKey="taskId"
          loading={taskListLoading}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: taskListTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: handleTableChange,
            onShowSizeChange: handleTableChange
          }}
          scroll={{ x: 1200 }}
          size="middle"
        />
      </Card>
    </div>
  )
}

export default TaskListPage



