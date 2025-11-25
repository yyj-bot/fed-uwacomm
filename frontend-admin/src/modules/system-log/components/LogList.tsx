/**
 * 日志列表组件
 * 显示系统日志列表，支持分页、筛选、排序等功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useCallback, useEffect } from 'react'
import { 
  Table, 
  Tag, 
  Typography, 
  Tooltip
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { SystemLog } from '@/types'
import { useSystem } from '@/store/system'
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
  height?: number | string
}

export const LogList: React.FC<LogListProps> = ({ height = '100%' }) => {
  const {
    logList,
    logListTotal,
    logListLoading,
    pagination,
    fetchLogList,
    setPagination
  } = useSystem()

  // 初始加载
  useEffect(() => {
    fetchLogList()
  }, [fetchLogList])

  // 分页变更
  const handleTableChange = (page: number, size?: number) => {
    setPagination(page, size)
    fetchLogList({ page, size })
  }

  // 表格列定义
  const columns: ColumnsType<SystemLog> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 220,
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
      width: 140,
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
      width: 160,
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
      width: 480,
      ellipsis: {
        showTitle: false
      },
      render: (message: string) => (
        <Tooltip title={message}>
          <Text>{message}</Text>
        </Tooltip>
      )
    },
  ]

  const resolvedHeight = typeof height === 'number' ? `${height}px` : height

  return (
    <div
      style={{
        height: resolvedHeight,
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        background: '#fff',
        borderRadius: 16,
        padding: 16,
        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.06)'
      }}
    >
      <Table<SystemLog>
        columns={columns}
        dataSource={logList}
        loading={logListLoading}
        rowKey="logId"
        scroll={{ x: 1100 }}
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
      />
    </div>
  )
}

export default LogList
