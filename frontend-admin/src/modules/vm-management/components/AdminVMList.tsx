/**
 * 管理员水下机器人列表组件
 * 使用管理员API查看所有水下机器人，不受权限限制
 * 
 * 功能：
 * - 查看水下机器人列表（管理员接口）
 * - 水下机器人更新（接口 4.3）
 * - 水下机器人删除（接口 4.4）
 * - 水下机器人控制：启动（接口 5.1）、停止（接口 5.2）、重启（接口 5.3）
 * - 水下机器人分配管理
 * 
 * 注意：查看水下机器人详情请使用"水下机器人列表"模块
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
  message,
  Popconfirm,
  Modal
} from 'antd'
import {
  SearchOutlined,
  TeamOutlined,
  ControlOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  PoweroffOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons'
import { vmService } from '@/services/vm/vmService'

import type { ColumnsType } from 'antd/es/table'
import './AdminVMList.css'

const { Option } = Select

interface AdminVMListProps {
  vmList: any[]
  loading: boolean
  onViewAssignment: (vm: any) => void
  onForceControl: (vm: any) => void
  onRefresh: () => void
  onEdit?: (vm: any) => void
}

const AdminVMList: React.FC<AdminVMListProps> = ({
  vmList,
  loading,
  onViewAssignment,
  onForceControl,
  onRefresh,
  onEdit
}) => {
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [assignedFilter, setAssignedFilter] = useState<boolean | undefined>(undefined)
  const [filteredVmList, setFilteredVmList] = useState<any[]>([])
  const [controlLoading, setControlLoading] = useState<string | null>(null)

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

  // 处理水下机器人启动 (接口 5.1)
  const handleStartVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.startVM(vmId)
      message.success('水下机器人启动命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 启动延迟 3s）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 3500)
    } catch (error: any) {
      message.error(error.message || '水下机器人启动失败')
      console.error('启动水下机器人失败:', error)
      setControlLoading(null)
    }
  }

  // 处理水下机器人停止 (接口 5.2)
  const handleStopVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.stopVM(vmId, { force: false })
      message.success('水下机器人停止命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 延迟 1000ms）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 1500)
    } catch (error: any) {
      message.error(error.message || '水下机器人停止失败')
      console.error('停止水下机器人失败:', error)
      setControlLoading(null)
    }
  }

  // 处理水下机器人重启 (接口 5.3)
  const handleRestartVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.restartVM(vmId)
      message.success('水下机器人重启命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 延迟 1000ms）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 1500)
    } catch (error: any) {
      message.error(error.message || '水下机器人重启失败')
      console.error('重启水下机器人失败:', error)
      setControlLoading(null)
    }
  }

  // 处理编辑水下机器人 (接口 4.3)
  const handleEditVM = (vm: any) => {
    if (onEdit) {
      onEdit(vm)
    } else {
      message.info('编辑功能开发中')
    }
  }

  // 处理删除水下机器人 (接口 4.4)
  const handleDeleteVM = async (vmId: string, vmName: string) => {
    try {
      await vmService.deleteVM(vmId, false)
      message.success(`水下机器人 "${vmName}" 删除成功`)
      onRefresh()
    } catch (error: any) {
      message.error(error.message || '水下机器人删除失败')
      console.error('删除水下机器人失败:', error)
    }
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
      title: '水下机器人名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      fixed: 'left',
      ellipsis: true
    },
    {
      title: '电量',
      dataIndex: 'batteryLevel',
      key: 'batteryLevel',
      width: 120,
      render: (battery: number | undefined) => (
        <Space size={6}>
          <Badge
            status={battery !== undefined ? (battery > 60 ? 'success' : battery > 30 ? 'warning' : 'error') : 'default'}
          />
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
      title: '分配状态',
      key: 'assignmentStatus',
      width: 160,
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
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 360,
      render: (_, record) => {
        const isRunning = record.status === 'RUNNING'
        const isStopped = record.status === 'STOPPED'
        const isConnected = record.connectionStatus === 'CONNECTED'
        const isLoading = controlLoading === record.vmId

        return (
          <Space size="small" wrap>
            {isStopped && (
              <Tooltip title="启动水下机器人">
                <Popconfirm
                  title="确认启动水下机器人？"
                  description="水下机器人将开始运行"
                  onConfirm={() => handleStartVM(record.vmId)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    size="small"
                    icon={<PlayCircleOutlined />}
                    loading={isLoading}
                    style={{
                      color: '#52c41a',
                      borderColor: '#b7eb8f',
                      backgroundColor: '#f6ffed'
                    }}
                  >
                    启动
                  </Button>
                </Popconfirm>
              </Tooltip>
            )}

            {isRunning && (
              <Tooltip title="停止水下机器人">
                <Popconfirm
                  title="确认停止水下机器人？"
                  description="水下机器人将被安全停止"
                  onConfirm={() => handleStopVM(record.vmId)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    size="small"
                    icon={<PoweroffOutlined />}
                    loading={isLoading}
                    disabled={!isConnected}
                    style={{
                      color: '#fa8c16',
                      borderColor: '#ffd591',
                      backgroundColor: '#fff7e6'
                    }}
                  >
                    停止
                  </Button>
                </Popconfirm>
              </Tooltip>
            )}

            {isRunning && (
              <Tooltip title="重启水下机器人">
                <Popconfirm
                  title="确认重启水下机器人？"
                  description="水下机器人将重新启动"
                  onConfirm={() => handleRestartVM(record.vmId)}
                  okText="确认"
                  cancelText="取消"
                >
                  <Button
                    size="small"
                    icon={<ReloadOutlined />}
                    loading={isLoading}
                    disabled={!isConnected}
                    style={{
                      color: '#1890ff',
                      borderColor: '#91d5ff',
                      backgroundColor: '#e6f7ff'
                    }}
                  >
                    重启
                  </Button>
                </Popconfirm>
              </Tooltip>
            )}

            <Tooltip title="编辑水下机器人">
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEditVM(record)}
              >
                编辑
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
                强控
              </Button>
            </Tooltip>

            <Tooltip title="删除水下机器人">
              <Popconfirm
                title={`确认删除水下机器人 "${record.name}"？`}
                description="此操作不可恢复！请确认删除。"
                onConfirm={() => handleDeleteVM(record.vmId, record.name)}
                okText="确认删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
              >
                <Button
                  size="small"
                  icon={<DeleteOutlined />}
                  style={{
                    color: '#ff4d4f',
                    borderColor: '#ffccc7',
                    backgroundColor: '#fff2f0'
                  }}
                >
                  删除
                </Button>
              </Popconfirm>
            </Tooltip>
          </Space>
        )
      }
    }
  ]

  return (
    <div className="vm-list-container">
      {/* 搜索和过滤器 */}
      <div className="vm-list-filters" style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索水下机器人名称"
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

      {/* 水下机器人列表 */}
      <Table
        columns={columns}
        dataSource={filteredVmList}
        rowKey="vmId"
        loading={loading}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 台水下机器人`,
          pageSizeOptions: ['10', '20', '50', '100']
        }}
      />
    </div>
  )
}

export default AdminVMList

