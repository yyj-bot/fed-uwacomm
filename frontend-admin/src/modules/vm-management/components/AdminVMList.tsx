/**
 * 管理员虚拟机列表组件
 * 使用管理员API查看所有虚拟机，不受权限限制
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import {
  Table,
  Tag,
  Space,
  Button,
  Input,
  Select,
  Tooltip,
  Badge,
  Spin,
  Alert,
  message
} from 'antd'
import {
  SearchOutlined,
  TeamOutlined,
  ControlOutlined,
  ReloadOutlined,
  EyeOutlined
} from '@ant-design/icons'

import type { ColumnsType } from 'antd/es/table'

const { Option } = Select

interface AdminVMListProps {
  vmList: any[]
  loading: boolean
  onViewDetail?: (vm: any) => void
  onViewAssignment: (vm: any) => void
  onForceControl: (vm: any) => void
  onRefresh: () => void
}

const AdminVMList: React.FC<AdminVMListProps> = ({
  vmList,
  loading,
  onViewDetail,
  onViewAssignment,
  onForceControl,
  onRefresh
}) => {
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [assignedFilter, setAssignedFilter] = useState<boolean | undefined>(undefined)
  const [filteredVmList, setFilteredVmList] = useState<any[]>([])

  // 应用过滤器
  useEffect(() => {
    let filtered = [...vmList]

    // 关键词搜索
    if (searchKeyword) {
      filtered = filtered.filter(vm => 
        vm.name.toLowerCase().includes(searchKeyword.toLowerCase()) ||
        vm.ipAddress.toLowerCase().includes(searchKeyword.toLowerCase())
      )
    }

    // 状态过滤
    if (statusFilter) {
      filtered = filtered.filter(vm => vm.status === statusFilter)
    }

    // 分配状态过滤
    if (assignedFilter !== undefined) {
      filtered = filtered.filter(vm => {
        const isAssigned = (vm as any).isAssigned || false
        return assignedFilter ? isAssigned : !isAssigned
      })
    }

    setFilteredVmList(filtered)
  }, [vmList, searchKeyword, statusFilter, assignedFilter])

  // 处理搜索
  const handleSearch = () => {
    // 过滤逻辑已在 useEffect 中处理
  }

  // 重置过滤器
  const handleResetFilters = () => {
    setSearchKeyword('')
    setStatusFilter('')
    setAssignedFilter(undefined)
  }

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

  // 表格列定义 - 严格按照接口文档 admin-vm-api-reference.md 3.1
  const columns: ColumnsType<any> = [
    {
      title: '虚拟机名称',
      dataIndex: 'name',
      key: 'name',
      width: 220,
      fixed: 'left',
      ellipsis: true
    },
    {
      title: 'IP地址',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 150
    },
    {
      title: '运行状态',
      key: 'status',
      width: 110,
      render: (_, record) => getStatusTag(record.status)
    },
    {
      title: '连接状态',
      key: 'connectionStatus',
      width: 130,
      render: (_, record) => getConnectionStatusTag(record.connectionStatus)
    },
    {
      title: '分配状态',
      key: 'assignmentStatus',
      width: 140,
      render: (_, record) => {
        const vm = record as any
        if (vm.isAssigned) {
          return (
            <Tooltip title={`已分配给 ${vm.assignedUserCount || 0} 个用户`}>
              <Tag color="blue">
                <TeamOutlined /> 已分配 ({vm.assignedUserCount || 0})
              </Tag>
            </Tooltip>
          )
        }
        return <Tag>未分配</Tag>
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    },
    {
      title: '最后心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      width: 180,
      render: (time: string) => time ? new Date(time).toLocaleString('zh-CN') : '-'
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 240,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => onViewDetail ? onViewDetail(record) : message.info('虚拟机详情功能开发中')}
            >
              详情
            </Button>
          </Tooltip>
          <Tooltip title="分配管理">
            <Button
              type="link"
              size="small"
              icon={<TeamOutlined />}
              onClick={() => onViewAssignment(record)}
            >
              分配
            </Button>
          </Tooltip>
          <Tooltip title="强制控制">
            <Button
              type="link"
              size="small"
              icon={<ControlOutlined />}
              onClick={() => onForceControl(record)}
            >
              控制
            </Button>
          </Tooltip>
        </Space>
      )
    }
  ]

  return (
    <div className="vm-list-container">
      {/* 搜索和过滤器 */}
      <div className="vm-list-filters" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索虚拟机名称或IP地址"
            prefix={<SearchOutlined />}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 280 }}
            allowClear
          />
          <Select
            placeholder="运行状态"
            value={statusFilter || undefined}
            onChange={setStatusFilter}
            style={{ width: 120 }}
            allowClear
          >
            <Option value="RUNNING">运行中</Option>
            <Option value="STOPPED">已停止</Option>
            <Option value="STARTING">启动中</Option>
            <Option value="STOPPING">停止中</Option>
            <Option value="ERROR">异常</Option>
            <Option value="OFFLINE">离线</Option>
          </Select>
          <Select
            placeholder="分配状态"
            value={assignedFilter}
            onChange={setAssignedFilter}
            style={{ width: 120 }}
            allowClear
          >
            <Option value={true}>已分配</Option>
            <Option value={false}>未分配</Option>
          </Select>
          <Button onClick={handleResetFilters}>重置</Button>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={onRefresh}
            loading={loading}
          >
            刷新
          </Button>
        </Space>
      </div>

      {/* 虚拟机列表 */}
      <Table
        columns={columns}
        dataSource={filteredVmList}
        rowKey="vmId"
        loading={loading}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 台虚拟机`,
          pageSizeOptions: ['10', '20', '50', '100']
        }}
        scroll={{ x: 1400 }}
      />
    </div>
  )
}

export default AdminVMList

