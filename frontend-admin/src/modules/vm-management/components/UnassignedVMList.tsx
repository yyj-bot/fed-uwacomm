/**
 * 未分配水下机器人列表组件
 * 显示系统中尚未分配给任何用户的水下机器人
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import {
  Table,
  Tag,
  Space,
  Button,
  Select,
  Badge,
  Tooltip,
  Empty,
  message
} from 'antd'
import {
  TeamOutlined,
  ReloadOutlined
} from '@ant-design/icons'

import type { ColumnsType } from 'antd/es/table'

const { Option } = Select

interface UnassignedVMListProps {
  vmList: any[]
  loading: boolean
  onViewAssignment: (vm: any) => void
  onRefresh: () => void
}

const UnassignedVMList: React.FC<UnassignedVMListProps> = ({
  vmList,
  loading,
  onViewAssignment,
  onRefresh
}) => {
  const [statusFilter, setStatusFilter] = useState<string>('')

  // 过滤后的VM列表
  const filteredVmList = statusFilter 
    ? vmList.filter(vm => vm.status === statusFilter)
    : vmList

  // 获取状态标签
  const getStatusTag = (status: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      'RUNNING': { color: 'success', text: '运行中' },
      'STOPPED': { color: 'default', text: '已停止' },
      'STARTING': { color: 'processing', text: '启动中' },
      'STOPPING': { color: 'warning', text: '停止中' },
      'ERROR': { color: 'error', text: '异常' },
      'OFFLINE': { color: 'default', text: '离线' }
    }
    const config = statusMap[status] || { color: 'default', text: status }
    return <Tag color={config.color}>{config.text}</Tag>
  }

  // 获取连接状态标签
  const getConnectionStatusTag = (status: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      'CONNECTED': { color: 'success', text: '已连接' },
      'DISCONNECTED': { color: 'default', text: '未连接' },
      'CONNECTING': { color: 'processing', text: '连接中' }
    }
    const config = statusMap[status] || { color: 'default', text: status }
    return <Badge status={config.color as any} text={config.text} />
  }

  // 表格列定义 - 严格按照接口文档 admin-vm-api-reference.md 3.2
  const columns: ColumnsType<any> = [
    {
      title: '水下机器人名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
      ellipsis: true
    },
    {
      title: '电量',
      dataIndex: 'batteryLevel',
      key: 'batteryLevel',
      width: 120,
      render: (battery: number | undefined) => (
        <Space size={6}>
          <Badge status={battery !== undefined ? (battery > 60 ? 'success' : battery > 30 ? 'warning' : 'error') : 'default'} />
          <span>{battery !== undefined ? `${battery}%` : '—'}</span>
        </Space>
      )
    },
    {
      title: '运行状态',
      key: 'status',
      width: 120,
      render: (_, record) => getStatusTag(record.status)
    },
    {
      title: '连接状态',
      key: 'connectionStatus',
      width: 140,
      render: (_, record) => getConnectionStatusTag(record.connectionStatus)
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 180,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="分配给用户">
            <Button
              type="primary"
              size="small"
              icon={<TeamOutlined style={{ color: '#ffffff' }} />}
              onClick={() => onViewAssignment(record)}
              style={{ color: '#ffffff' }}
            >
              分配
            </Button>
          </Tooltip>
        </Space>
      )
    }
  ]

  return (
    <div className="unassigned-vm-list-container">
      {/* 过滤器 */}
      <div className="vm-list-filters" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="运行状态"
            value={statusFilter || undefined}
            onChange={setStatusFilter}
            style={{ width: 140 }}
            allowClear
          >
            <Option value="RUNNING">运行中</Option>
            <Option value="STOPPED">已停止</Option>
            <Option value="ERROR">异常</Option>
            <Option value="OFFLINE">离线</Option>
          </Select>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={onRefresh}
            loading={loading}
          >
            刷新
          </Button>
        </Space>
      </div>

      {/* 水下机器人列表 */}
      {filteredVmList.length === 0 && !loading ? (
        <Empty
          description="暂无未分配的水下机器人"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      ) : (
        <Table
          columns={columns}
          dataSource={filteredVmList}
          rowKey="vmId"
          loading={loading}
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 台未分配水下机器人`,
            pageSizeOptions: ['10', '20', '50']
          }}
        />
      )}
    </div>
  )
}

export default UnassignedVMList

