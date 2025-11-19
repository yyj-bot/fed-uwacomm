/**
 * 水下机器人列表组件（普通用户）
 * 显示水下机器人列表，支持搜索、筛选、分页
 * 
 * 功能：
 * - 查看水下机器人列表（接口 4.1）
 * - 查看水下机器人详情（接口 4.2）
 * - 查看本地模型
 * 
 * 注意：不包含水下机器人控制操作（启动/停止/重启），这些功能仅管理员可用
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Table, 
  Button, 
  Space, 
  Tag, 
  Input, 
  Select, 
  Card, 
  Tooltip,
  Modal,
  message,
  Popconfirm,
  Badge,
  Typography
} from 'antd'
import { 
  SearchOutlined, 
  ReloadOutlined, 
  EyeOutlined,
  BarChartOutlined,
  CloudServerOutlined,
  ThunderboltOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons'
import { useVM } from '@/store/vm'
import type { VirtualMachine } from '@/api/vm'
import type { ColumnsType } from 'antd/es/table'

const { Text } = Typography

const { Search } = Input
const { Option } = Select

interface VMListProps {
  onSelectVM: (vm: VirtualMachine) => void
  onViewModels: (vm: VirtualMachine) => void
}

const VMList: React.FC<VMListProps> = ({ onSelectVM, onViewModels }) => {
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [osTypeFilter, setOsTypeFilter] = useState<string>('')

  const {
    vmList,
    vmListLoading,
    vmListError,
    pagination,
    fetchVMList,
    setPagination,
    setQueryParams,
    getVMStatus
  } = useVM()

  // 初始化数据
  useEffect(() => {
    fetchVMList()
  }, [])

  // 处理搜索
  const handleSearch = (value: string) => {
    setSearchText(value)
    setQueryParams({
      keyword: value,
      status: statusFilter as any || undefined,
      osType: osTypeFilter || undefined
    })
    fetchVMList({ page: 1 })
  }

  // 处理状态筛选
  const handleStatusFilter = (value: string) => {
    setStatusFilter(value)
    setQueryParams({
      keyword: searchText || undefined,
      status: value as any || undefined,
      osType: osTypeFilter || undefined
    })
    fetchVMList({ page: 1 })
  }

  // 处理操作系统筛选
  const handleOsTypeFilter = (value: string) => {
    setOsTypeFilter(value)
    setQueryParams({
      keyword: searchText || undefined,
      status: statusFilter as any || undefined,
      osType: value || undefined
    })
    fetchVMList({ page: 1 })
  }

  // 处理分页
  const handleTableChange = (page: number, pageSize: number) => {
    setPagination(page, pageSize)
    fetchVMList({ page, size: pageSize })
  }

  // 渲染状态标签
  const renderStatus = (vm: VirtualMachine) => {
    const status = getVMStatus(vm.vmId)
    const currentStatus = status?.status || vm.status
    
    const statusConfig = {
      RUNNING: { color: 'green', text: '运行中' },
      STOPPED: { color: 'default', text: '已停止' },
      STARTING: { color: 'blue', text: '启动中' },
      STOPPING: { color: 'orange', text: '停止中' },
      ERROR: { color: 'red', text: '异常' },
      OFFLINE: { color: 'default', text: '离线' }
    }

    const config = statusConfig[currentStatus] || { color: 'default', text: currentStatus }
    
    return (
      <Badge 
        status={config.color as any} 
        text={config.text}
      />
    )
  }

  // 渲染连接状态
  const renderConnectionStatus = (vm: VirtualMachine) => {
    const status = getVMStatus(vm.vmId)
    const connectionStatus = status?.connectionStatus || vm.connectionStatus
    
    return (
      <Tag color={connectionStatus === 'CONNECTED' ? 'green' : 'red'}>
        {connectionStatus === 'CONNECTED' ? '已连接' : '未连接'}
      </Tag>
    )
  }

  // 表格列定义
  const columns: ColumnsType<VirtualMachine> = [
    {
      title: '水下机器人名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true,
      render: (text: string, record: VirtualMachine) => (
        <Space>
          <CloudServerOutlined style={{ fontSize: '18px', color: '#1890ff' }} />
          <Button 
            type="link" 
            onClick={() => onSelectVM(record)}
            style={{ 
              padding: 0, 
              height: 'auto',
              fontWeight: 600
            }}
          >
            {text}
          </Button>
        </Space>
      )
    },
    {
      title: '关键参数',
      key: 'keySpecs',
      width: 240,
      render: (_, record: VirtualMachine) => (
        <Space direction="vertical" size={4}>
          <Text style={{ fontSize: 13 }}>
            <ThunderboltOutlined style={{ color: '#faad14', marginRight: 4 }} />
            尺寸: {record.specs?.dimensions || '—'}
          </Text>
          <Text style={{ fontSize: 13 }}>
            <SafetyCertificateOutlined style={{ color: '#52c41a', marginRight: 4 }} />
            重量: {record.specs?.airWeight || '—'}
          </Text>
          <Text style={{ fontSize: 13 }}>
            <CloudServerOutlined style={{ color: '#1890ff', marginRight: 4 }} />
            深度: {record.specs?.depthRating || '—'}
          </Text>
        </Space>
      )
    },
    {
      title: '能力概览',
      key: 'capabilitySummary',
      width: 240,
      render: (_, record: VirtualMachine) => (
        <Space direction="vertical" size={4}>
          <Text style={{ fontSize: 13 }}>
            推进: {record.specs?.thrusters || '—'}
          </Text>
          <Text style={{ fontSize: 13 }}>
            摄像: {record.specs?.camera || '—'}
          </Text>
          <Text style={{ fontSize: 13 }}>
            传感: {record.specs?.sensors || '—'}
          </Text>
        </Space>
      )
    },
    {
      title: '运行状态',
      key: 'status',
      width: 100,
      render: (_, record: VirtualMachine) => renderStatus(record)
    },
    {
      title: '连接状态',
      key: 'connectionStatus',
      width: 100,
      render: (_, record: VirtualMachine) => renderConnectionStatus(record)
    },
    {
      title: '最后心跳',
      dataIndex: 'lastHeartbeat',
      key: 'lastHeartbeat',
      width: 150,
      render: (text: string) => text ? new Date(text).toLocaleString() : '-'
    },
    {
      title: '操作',
      key: 'actions',
      width: 120,
      fixed: 'right',
      render: (_, record: VirtualMachine) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="text" 
              icon={<EyeOutlined />} 
              onClick={() => onSelectVM(record)}
            />
          </Tooltip>
          
          <Tooltip title="本地模型">
            <Button 
              type="text" 
              icon={<BarChartOutlined />} 
              onClick={() => onViewModels(record)}
            />
          </Tooltip>
        </Space>
      )
    }
  ]

  // 获取唯一的操作系统类型
  const osTypes = Array.from(new Set(vmList.map(vm => vm.osType))).filter(Boolean)

  return (
    <div className="vm-list">
      {/* 页面标题 */}
      <div className="vm-page-header">
        <div className="vm-page-title">
          <h2>水下机器人列表</h2>
          <span className="vm-page-description">查看和管理所有可用的水下机器人节点</span>
        </div>
        <div className="vm-page-actions">
          <Button 
            type="primary"
            icon={<ReloadOutlined />} 
            onClick={() => fetchVMList()}
            loading={vmListLoading}
            size="large"
          >
            刷新数据
          </Button>
        </div>
      </div>

      {/* 搜索和筛选 */}
      <Card 
        className="vm-list-filters" 
        style={{ 
          marginBottom: 20,
          marginLeft: 24,
          marginRight: 24
        }}
      >
        <Space size="middle" wrap style={{ width: '100%' }}>
          <Input.Search
            placeholder="搜索水下机器人名称"
            enterButton
            allowClear
            onSearch={handleSearch}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ 
              width: 320,
            }}
            size="large"
          />
          
          <Select
            placeholder="筛选状态"
            allowClear
            style={{ width: 140 }}
            value={statusFilter}
            onChange={handleStatusFilter}
            size="large"
          >
            <Option value="RUNNING">🟢 运行中</Option>
            <Option value="STOPPED">⚪ 已停止</Option>
            <Option value="STARTING">🔵 启动中</Option>
            <Option value="STOPPING">🟠 停止中</Option>
            <Option value="ERROR">🔴 异常</Option>
            <Option value="OFFLINE">⚫ 离线</Option>
          </Select>
          
          <Button 
            icon={<ReloadOutlined />} 
            onClick={() => fetchVMList()}
            loading={vmListLoading}
            size="large"
            style={{
              borderRadius: '8px',
              fontWeight: 500
            }}
          >
            刷新
          </Button>
        </Space>
      </Card>

      {/* 水下机器人列表表格 */}
      <Card 
        style={{ 
          borderRadius: '12px',
          overflow: 'hidden',
          marginLeft: 24,
          marginRight: 24,
          marginBottom: 24
        }}
        bodyStyle={{ padding: 0 }}
      >
        <Table
          columns={columns}
          dataSource={vmList}
          rowKey="vmId"
          loading={vmListLoading}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `📊 第 ${range[0]}-${range[1]} 条，共 ${total} 条记录`,
            onChange: handleTableChange,
            onShowSizeChange: handleTableChange,
            pageSizeOptions: ['10', '20', '50', '100']
          }}
          scroll={{ x: 1200 }}
          size="middle"
          style={{
            background: 'transparent'
          }}
        />
      </Card>
    </div>
  )
}

export default VMList










