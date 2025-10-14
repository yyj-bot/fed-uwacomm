/**
 * 日志列表组件
 * 显示系统日志列表，支持分页、筛选、排序等功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Table, 
  Tag, 
  Button, 
  Space, 
  Modal, 
  Typography, 
  Tooltip,
  message,
  Select,
  Form
} from 'antd'
import { 
  EyeOutlined, 
  DownloadOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { SystemLog } from '@/types'
import { useSystem } from '@/store/system'
import { LogDetail } from './LogDetail'
import { formatDateTime } from '@/utils'

const { Text } = Typography

// 日志级别颜色映射
const LOG_LEVEL_COLORS = {
  DEBUG: 'default',
  INFO: 'blue', 
  WARN: 'orange',
  ERROR: 'red'
} as const

// 日志类别颜色映射
const LOG_CATEGORY_COLORS = {
  SYSTEM: 'blue',
  USER: 'green',
  VM: 'purple',
  TASK: 'orange',
  DATA: 'cyan',
  MODEL: 'magenta',
  SECURITY: 'red',
  PERFORMANCE: 'gold'
} as const

interface LogListProps {
  height?: number
}

export const LogList: React.FC<LogListProps> = ({ height = 600 }) => {
  const {
    logList,
    logListTotal,
    logListLoading,
    pagination,
    queryParams,
    fetchLogList,
    fetchLogDetail,
    fetchRealtimeLogs,
    realtimeLogs,
    setPagination
  } = useSystem()

  const [selectedLog, setSelectedLog] = useState<SystemLog | null>(null)
  const [detailVisible, setDetailVisible] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [exportVisible, setExportVisible] = useState(false)
  const [exportLoading, setExportLoading] = useState(false)
  const [exportForm] = Form.useForm()
  const [isRealtime, setIsRealtime] = useState(false)
  const [realtimeInterval, setRealtimeInterval] = useState<NodeJS.Timeout | null>(null)

  // 初始加载
  useEffect(() => {
    fetchLogList()
  }, [fetchLogList])

  // 实时日志功能
  const toggleRealtime = () => {
    if (isRealtime) {
      // 停止实时日志
      if (realtimeInterval) {
        clearInterval(realtimeInterval)
        setRealtimeInterval(null)
      }
      setIsRealtime(false)
      message.info('已停止实时日志')
    } else {
      // 开始实时日志
      setIsRealtime(true)
      message.info('已开启实时日志，每5秒自动刷新')
      
      // 立即获取一次
      fetchRealtimeLogs({
        ...queryParams,
        tail: 100
      })
      
      // 设置定时器
      const interval = setInterval(() => {
        fetchRealtimeLogs({
          ...queryParams,
          tail: 100
        })
      }, 5000)
      
      setRealtimeInterval(interval)
    }
  }

  // 清理定时器
  useEffect(() => {
    return () => {
      if (realtimeInterval) {
        clearInterval(realtimeInterval)
      }
    }
  }, [realtimeInterval])

  // 查看日志详情
  const handleViewDetail = async (record: SystemLog) => {
    setDetailLoading(true)
    setDetailVisible(true)
    
    try {
      const result = await fetchLogDetail(record.logId)
      if (result.success && result.data) {
        setSelectedLog(result.data)
      } else {
        let errorMessage = result.error || '获取日志详情失败'
        
        // 提供更友好的错误提示
        if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
          errorMessage = '您没有查看该日志的权限'
        } else if (errorMessage.includes('not found') || errorMessage.includes('不存在')) {
          errorMessage = '日志记录不存在或已被删除'
        } else if (errorMessage.includes('timeout') || errorMessage.includes('超时')) {
          errorMessage = '请求超时，请稍后重试'
        }
        
        message.error(errorMessage)
        setDetailVisible(false)
      }
    } catch (error) {
      console.error('获取日志详情失败:', error)
      
      let errorMessage = '获取日志详情失败'
      if (error instanceof Error) {
        errorMessage = error.message
      } else if (typeof error === 'string') {
        errorMessage = error
      } else if (error && typeof error === 'object') {
        const apiError = error as any
        if (apiError.response?.data?.message) {
          errorMessage = apiError.response.data.message
        } else if (apiError.message) {
          errorMessage = apiError.message
        }
      }
      
      // 提供友好的错误提示
      if (errorMessage.includes('network') || errorMessage.includes('网络')) {
        errorMessage = '网络连接失败，请检查网络状态'
      } else if (errorMessage.includes('server') || errorMessage.includes('服务器')) {
        errorMessage = '服务器暂时不可用，请稍后重试'
      }
      
      message.error(errorMessage)
      setDetailVisible(false)
    } finally {
      setDetailLoading(false)
    }
  }

  // 刷新列表
  const handleRefresh = () => {
    fetchLogList()
    message.success('日志列表已刷新')
  }

  // 分页变更
  const handleTableChange = (page: number, size?: number) => {
    setPagination(page, size)
    fetchLogList({ page, size })
  }

  // 导出日志
  const handleExport = () => {
    setExportVisible(true)
    exportForm.resetFields()
  }

  // 确认导出
  const handleExportConfirm = async () => {
    try {
      const values = await exportForm.validateFields()
      setExportLoading(true)
      
      // 调用导出API
      const response = await fetch('/api/log/download', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('access_token')}`
        },
        body: JSON.stringify({
          format: values.format || 'CSV',
          includeDetails: values.includeDetails !== false,
          ...queryParams // 使用当前的筛选条件
        })
      })

      if (response.ok) {
        // 获取文件名
        const contentDisposition = response.headers.get('Content-Disposition')
        const filename = contentDisposition 
          ? contentDisposition.split('filename=')[1]?.replace(/"/g, '') 
          : `logs_${new Date().getTime()}.${values.format?.toLowerCase() || 'csv'}`

        // 下载文件
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename
        document.body.appendChild(a)
        a.click()
        window.URL.revokeObjectURL(url)
        document.body.removeChild(a)

        message.success('日志导出成功')
        setExportVisible(false)
            } else {
              const errorResult = await response.json().catch(() => ({}))
              const errorMessage = errorResult.message || '导出失败，请重试'
              message.error(errorMessage)
            }
          } catch (error) {
            console.error('导出失败:', error)
            
            let errorMessage = '导出失败，请重试'
            if (error instanceof Error) {
              errorMessage = error.message
            } else if (typeof error === 'string') {
              errorMessage = error
            }
            
            // 提供友好的错误提示
            if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
              errorMessage = '您没有导出日志的权限'
            } else if (errorMessage.includes('too many') || errorMessage.includes('过多')) {
              errorMessage = '选择的日志数量过多，请缩小范围后重试'
            } else if (errorMessage.includes('network') || errorMessage.includes('网络')) {
              errorMessage = '网络连接失败，请检查网络状态'
            } else if (errorMessage.includes('timeout') || errorMessage.includes('超时')) {
              errorMessage = '导出超时，请稍后重试'
            }
            
            message.error(errorMessage)
          } finally {
            setExportLoading(false)
          }
  }

  // 表格列定义
  const columns: ColumnsType<SystemLog> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      fixed: 'left',
      render: (createdAt: string) => (
        <Text code>{formatDateTime(createdAt)}</Text>
      ),
      sorter: true
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 100,
      render: (level: keyof typeof LOG_LEVEL_COLORS) => (
        <Tag color={LOG_LEVEL_COLORS[level] || 'default'}>
          {level}
        </Tag>
      )
    },
    {
      title: '类别',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (category: keyof typeof LOG_CATEGORY_COLORS) => (
        <Tag color={LOG_CATEGORY_COLORS[category] || 'default'}>
          {category}
        </Tag>
      )
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: {
        showTitle: false
      },
      render: (message: string) => (
        <Tooltip title={message}>
          <Text>{message}</Text>
        </Tooltip>
      )
    },
    {
      title: 'VM ID',
      dataIndex: 'vmId',
      key: 'vmId',
      width: 140,
      render: (vmId: string) => vmId ? (
        <Text code>{vmId.substring(0, 8)}...</Text>
      ) : '-'
    },
    {
      title: '任务ID',
      dataIndex: 'taskId', 
      key: 'taskId',
      width: 140,
      render: (taskId: string) => taskId ? (
        <Text code>{taskId.substring(0, 8)}...</Text>
      ) : '-'
    },
    {
      title: '操作',
      key: 'action',
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="查看详情">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
              size="small"
            />
          </Tooltip>
        </Space>
      )
    }
  ]

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button 
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={logListLoading}
            disabled={isRealtime}
          >
            刷新
          </Button>
          <Button 
            icon={isRealtime ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            type={isRealtime ? 'primary' : 'default'}
            onClick={toggleRealtime}
            loading={logListLoading}
          >
            {isRealtime ? '停止实时' : '实时日志'}
          </Button>
          <Button 
            icon={<DownloadOutlined />}
            type="primary"
            onClick={handleExport}
          >
            导出日志
          </Button>
        </Space>
      </div>

      <Table<SystemLog>
        columns={columns}
        dataSource={logList}
        loading={logListLoading}
        rowKey="logId"
        scroll={{ x: 960, y: height - 120 }}
        pagination={{
          current: pagination.page,
          pageSize: pagination.size,
          total: logListTotal,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => 
            `第 ${range[0]}-${range[1]} 条，共 ${total} 条`,
          onChange: handleTableChange,
          onShowSizeChange: handleTableChange
        }}
        size="small"
        tableLayout="fixed"
      />

      {/* 日志详情模态框 */}
      <Modal
        title="日志详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={800}
        destroyOnHidden
      >
        <LogDetail 
          log={selectedLog} 
          loading={detailLoading}
        />
      </Modal>

      {/* 导出配置模态框 */}
      <Modal
        title="导出日志"
        open={exportVisible}
        onCancel={() => setExportVisible(false)}
        onOk={handleExportConfirm}
        confirmLoading={exportLoading}
        width={500}
      >
        <Form
          form={exportForm}
          layout="vertical"
          initialValues={{
            format: 'CSV',
            includeDetails: true
          }}
        >
          <Form.Item
            label="导出格式"
            name="format"
            rules={[{ required: true, message: '请选择导出格式' }]}
          >
            <Select>
              <Select.Option value="CSV">CSV</Select.Option>
              <Select.Option value="JSON">JSON</Select.Option>
              <Select.Option value="EXCEL">Excel</Select.Option>
            </Select>
          </Form.Item>
          
          <Form.Item
            label="包含详细信息"
            name="includeDetails"
            valuePropName="checked"
          >
            <Select>
              <Select.Option value={true}>是</Select.Option>
              <Select.Option value={false}>否</Select.Option>
            </Select>
          </Form.Item>

          <div style={{ color: '#666', fontSize: '12px' }}>
            注意：将导出当前筛选条件下的所有日志数据
          </div>
        </Form>
      </Modal>
    </>
  )
}

export default LogList
