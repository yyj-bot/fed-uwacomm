/**
 * 联邦学习流程编排列表页面
 * 显示所有工作流编排任务，支持搜索、筛选、分页等功能
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
  Progress,
  message,
  Modal,
  Tooltip,
  Statistic,
  Row,
  Col,
  Badge
} from 'antd'
import { 
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  EyeOutlined,
  ReloadOutlined,
  PlusOutlined,
  SearchOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  LoadingOutlined,
  LineChartOutlined,
  BarChartOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useNavigate } from 'react-router-dom'
import { useOrchestration } from '@/store/federated-task/useFederatedTaskStore'
import './OrchestrationListPage.css'

const { Option } = Select

// 编排状态配置
const ORCHESTRATION_STATUS_CONFIG = {
  CREATED: { color: 'blue', text: '已创建', icon: <ClockCircleOutlined /> },
  STARTED: { color: 'cyan', text: '已启动', icon: <PlayCircleOutlined /> },
  IN_PROGRESS: { color: 'green', text: '执行中', icon: <LoadingOutlined spin /> },
  PAUSED: { color: 'orange', text: '已暂停', icon: <PauseCircleOutlined /> },
  COMPLETED: { color: 'success', text: '已完成', icon: <CheckCircleOutlined /> },
  FAILED: { color: 'error', text: '执行失败', icon: <ExclamationCircleOutlined /> },
  TERMINATED: { color: 'default', text: '已终止', icon: <StopOutlined /> }
}

// 工作流阶段配置
const WORKFLOW_STAGE_CONFIG = {
  INITIAL_MODEL_GENERATION: '初始模型生成',
  DATA_DISTRIBUTION: '数据分发',
  MODEL_DISTRIBUTION: '模型分发',
  FEDERATED_TRAINING: '联邦训练',
  FINAL_AGGREGATION: '最终聚合'
}

const OrchestrationListPage: React.FC = () => {
  const navigate = useNavigate()
  
  // 使用Hook获取状态和操作
  const {
    orchestrationList,
    orchestrationListTotal,
    orchestrationListLoading,
    orchestrationListError,
    orchestrationPagination,
    orchestrationQueryParams,
    fetchOrchestrationList,
    refreshOrchestrationList,
    pauseOrchestration,
    resumeOrchestration,
    terminateOrchestration,
    setOrchestrationQueryParams,
    setOrchestrationPagination,
    clearOrchestrationError,
    canPauseOrchestration,
    canResumeOrchestration,
    canTerminateOrchestration,
    isOrchestrationOperating,
    runningOrchestrationCount,
    completedOrchestrationCount,
    failedOrchestrationCount
  } = useOrchestration()

  // 本地状态
  const [searchKeyword, setSearchKeyword] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [taskIdFilter, setTaskIdFilter] = useState<string>('')

  // 初始化加载编排列表
  useEffect(() => {
    fetchOrchestrationList()
  }, [fetchOrchestrationList])

  // 处理搜索
  const handleSearch = useCallback(() => {
    const params: any = {}
    
    if (searchKeyword.trim()) {
      params.keyword = searchKeyword.trim()
    }
    
    if (statusFilter) {
      params.status = statusFilter
    }
    
    if (taskIdFilter.trim()) {
      params.taskId = taskIdFilter.trim()
    }
    
    setOrchestrationQueryParams(params)
    setOrchestrationPagination(1) // 重置到第一页
    fetchOrchestrationList(params)
  }, [searchKeyword, statusFilter, taskIdFilter, setOrchestrationQueryParams, setOrchestrationPagination, fetchOrchestrationList])

  // 重置搜索条件
  const handleReset = useCallback(() => {
    setSearchKeyword('')
    setStatusFilter('')
    setTaskIdFilter('')
    setOrchestrationQueryParams({})
    setOrchestrationPagination(1)
    fetchOrchestrationList()
  }, [setOrchestrationQueryParams, setOrchestrationPagination, fetchOrchestrationList])

  // 处理分页变化
  const handleTableChange = useCallback((page: number, size: number) => {
    setOrchestrationPagination(page, size)
    fetchOrchestrationList({ ...orchestrationQueryParams, page, size })
  }, [orchestrationQueryParams, setOrchestrationPagination, fetchOrchestrationList])

  // 编排操作处理函数
  const handlePauseOrchestration = useCallback(async (orchestration: any) => {
    try {
      const result = await pauseOrchestration(orchestration.orchestrationId, {
        reason: '用户主动暂停',
        pauseMode: 'GRACEFUL',
        waitForCurrentRound: true,
        preserveState: true,
        notifyParticipants: true
      })
      if (result.success) {
        message.success('编排暂停成功')
        refreshOrchestrationList()
      } else {
        message.error(result.error || '编排暂停失败')
      }
    } catch (error) {
      message.error('编排暂停失败')
    }
  }, [pauseOrchestration, refreshOrchestrationList])

  const handleResumeOrchestration = useCallback(async (orchestration: any) => {
    try {
      const result = await resumeOrchestration(orchestration.orchestrationId, {
        resumeFromSnapshot: true,
        validateState: true,
        notifyParticipants: true
      })
      if (result.success) {
        message.success('编排恢复成功')
        refreshOrchestrationList()
      } else {
        message.error(result.error || '编排恢复失败')
      }
    } catch (error) {
      message.error('编排恢复失败')
    }
  }, [resumeOrchestration, refreshOrchestrationList])

  const handleTerminateOrchestration = useCallback(async (orchestration: any) => {
    Modal.confirm({
      title: '确认终止编排',
      icon: <ExclamationCircleOutlined />,
      content: `确定要终止编排"${orchestration.orchestrationId}"吗？终止后可以查看部分结果。`,
      okText: '确认终止',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await terminateOrchestration(orchestration.orchestrationId, {
            force: false,
            cleanup: true,
            saveResults: true
          })
          if (result.success) {
            message.success('编排终止成功')
            refreshOrchestrationList()
          } else {
            message.error(result.error || '编排终止失败')
          }
        } catch (error) {
          message.error('编排终止失败')
        }
      }
    })
  }, [terminateOrchestration, refreshOrchestrationList])

  // 表格列定义
  const columns: ColumnsType<any> = [
    {
      title: '编排ID',
      dataIndex: 'orchestrationId',
      key: 'orchestrationId',
      width: 200,
      ellipsis: true,
      render: (text: string, record: any) => (
        <Tooltip title={text}>
          <Button 
            type="link" 
            onClick={() => navigate(`/federated-learning/orchestrations/${record.orchestrationId}`)}
            style={{ padding: 0, height: 'auto', fontSize: '12px' }}
          >
            {text.slice(0, 16)}...
          </Button>
        </Tooltip>
      )
    },
    {
      title: '关联任务',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true,
      render: (taskId: string) => (
        <Tooltip title={taskId}>
          <Button 
            type="link" 
            onClick={() => navigate(`/federated-learning/tasks/${taskId}`)}
            style={{ padding: 0, height: 'auto', fontSize: '12px' }}
          >
            {taskId.slice(0, 16)}...
          </Button>
        </Tooltip>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: keyof typeof ORCHESTRATION_STATUS_CONFIG) => {
        const config = ORCHESTRATION_STATUS_CONFIG[status] || { color: 'default', text: status, icon: null }
        return (
          <Tag color={config.color}>
            {config.icon} {config.text}
          </Tag>
        )
      }
    },
    {
      title: '当前阶段',
      dataIndex: 'currentStage',
      key: 'currentStage',
      width: 150,
      render: (stage: keyof typeof WORKFLOW_STAGE_CONFIG) => {
        const stageText = WORKFLOW_STAGE_CONFIG[stage] || stage
        return <Tag color="blue">{stageText}</Tag>
      }
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      width: 120,
      render: (progress: number) => (
        <Progress 
          percent={progress} 
          size="small" 
          status={progress === 100 ? 'success' : 'active'}
          format={(percent) => `${percent}%`}
        />
      )
    },
    {
      title: '轮次进度',
      key: 'rounds',
      width: 100,
      render: (_, record: any) => (
        <div style={{ fontSize: '12px', textAlign: 'center' }}>
          <div>{record.completedRounds}/{record.totalRounds}</div>
          <Progress 
            percent={record.totalRounds > 0 ? (record.completedRounds / record.totalRounds) * 100 : 0}
            size="small"
            showInfo={false}
          />
        </div>
      )
    },
    {
      title: '参与者',
      dataIndex: 'participatingVms',
      key: 'participatingVms',
      width: 80,
      render: (count: number) => (
        <Badge count={count} color="blue" />
      )
    },
    {
      title: '执行时长',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (duration: string) => (
        <span style={{ fontSize: '12px' }}>{duration}</span>
      )
    },
    {
      title: '最终准确率',
      dataIndex: 'finalAccuracy',
      key: 'finalAccuracy',
      width: 100,
      render: (accuracy: number) => 
        accuracy ? `${(accuracy * 100).toFixed(2)}%` : '-'
    },
    {
      title: '开始时间',
      dataIndex: 'startedAt',
      key: 'startedAt',
      width: 150,
      render: (time: string) => new Date(time).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      fixed: 'right',
      render: (_, record: any) => {
        const operating = isOrchestrationOperating(record.orchestrationId)
        
        return (
          <Space size="small">
            <Tooltip title="查看详情">
              <Button
                type="text"
                icon={<EyeOutlined />}
                onClick={() => navigate(`/federated-learning/orchestrations/${record.orchestrationId}`)}
              />
            </Tooltip>
            
            {canPauseOrchestration(record) && (
              <Tooltip title="暂停编排">
                <Button
                  type="text"
                  icon={<PauseCircleOutlined />}
                  onClick={() => handlePauseOrchestration(record)}
                  loading={operating}
                  style={{ color: '#faad14' }}
                />
              </Tooltip>
            )}
            
            {canResumeOrchestration(record) && (
              <Tooltip title="恢复编排">
                <Button
                  type="text"
                  icon={<PlayCircleOutlined />}
                  onClick={() => handleResumeOrchestration(record)}
                  loading={operating}
                  style={{ color: '#52c41a' }}
                />
              </Tooltip>
            )}
            
            {canTerminateOrchestration(record) && (
              <Tooltip title="终止编排">
                <Button
                  type="text"
                  icon={<StopOutlined />}
                  onClick={() => handleTerminateOrchestration(record)}
                  loading={operating}
                  style={{ color: '#ff4d4f' }}
                />
              </Tooltip>
            )}
            
            <Tooltip title="查看时间线">
              <Button
                type="text"
                icon={<LineChartOutlined />}
                onClick={() => navigate(`/federated-learning/orchestrations/${record.orchestrationId}/timeline`)}
              />
            </Tooltip>
            
            <Tooltip title="性能分析">
              <Button
                type="text"
                icon={<BarChartOutlined />}
                onClick={() => navigate(`/federated-learning/orchestrations/${record.orchestrationId}/analytics`)}
              />
            </Tooltip>
          </Space>
        )
      }
    }
  ]

  return (
    <div className="orchestration-list-page">
      {/* 统计概览 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={8} md={6}>
          <Card>
            <Statistic
              title="运行中编排"
              value={runningOrchestrationCount()}
              prefix={<LoadingOutlined spin style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8} md={6}>
          <Card>
            <Statistic
              title="已完成编排"
              value={completedOrchestrationCount()}
              prefix={<CheckCircleOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8} md={6}>
          <Card>
            <Statistic
              title="失败编排"
              value={failedOrchestrationCount()}
              prefix={<ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8} md={6}>
          <Card>
            <Statistic
              title="总编排数量"
              value={orchestrationListTotal}
              prefix={<BarChartOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        {/* 页面标题和操作 */}
        <div className="page-header">
          <div className="page-title">
            <h2>联邦学习流程编排</h2>
            <span className="page-description">管理和监控联邦学习工作流编排的执行状态</span>
          </div>
          <div className="page-actions">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => navigate('/federated-learning/orchestrations/create')}
            >
              创建编排
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => refreshOrchestrationList()}
              loading={orchestrationListLoading}
            >
              刷新
            </Button>
          </div>
        </div>

        {/* 搜索和筛选 */}
        <div className="search-section">
          <Space wrap size="middle">
            <Input
              placeholder="搜索编排ID"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 200 }}
              prefix={<SearchOutlined />}
            />
            
            <Input
              placeholder="关联任务ID"
              value={taskIdFilter}
              onChange={(e) => setTaskIdFilter(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 200 }}
            />
            
            <Select
              placeholder="编排状态"
              value={statusFilter}
              onChange={setStatusFilter}
              allowClear
              style={{ width: 120 }}
            >
              {Object.entries(ORCHESTRATION_STATUS_CONFIG).map(([value, config]) => (
                <Option key={value} value={value}>
                  {config.icon} {config.text}
                </Option>
              ))}
            </Select>
            
            <Button type="primary" onClick={handleSearch} loading={orchestrationListLoading}>
              搜索
            </Button>
            
            <Button onClick={handleReset}>
              重置
            </Button>
          </Space>
        </div>

        {/* 编排列表表格 */}
        <Table
          columns={columns}
          dataSource={orchestrationList}
          rowKey="orchestrationId"
          loading={orchestrationListLoading}
          pagination={{
            current: orchestrationPagination.page,
            pageSize: orchestrationPagination.size,
            total: orchestrationListTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: handleTableChange,
            onShowSizeChange: handleTableChange
          }}
          scroll={{ x: 1400 }}
          size="middle"
        />
      </Card>
    </div>
  )
}

export default OrchestrationListPage



