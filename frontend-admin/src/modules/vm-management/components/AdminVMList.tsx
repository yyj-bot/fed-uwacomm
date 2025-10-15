/**
 * 管理员虚拟机列表组件
 * 使用管理员API查看所有虚拟机，不受权限限制
 * 
 * 功能：
 * - 查看虚拟机列表（管理员接口）
 * - 虚拟机更新（接口 4.3）
 * - 虚拟机删除（接口 4.4）
 * - 虚拟机控制：启动（接口 5.1）、停止（接口 5.2）、重启（接口 5.3）
 * - 虚拟机分配管理
 * 
 * 注意：查看虚拟机详情请使用"虚拟机列表"模块
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

  // 处理虚拟机启动 (接口 5.1)
  const handleStartVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.startVM(vmId)
      message.success('虚拟机启动命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 启动延迟 3s）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 3500)
    } catch (error: any) {
      message.error(error.message || '虚拟机启动失败')
      console.error('启动虚拟机失败:', error)
      setControlLoading(null)
    }
  }

  // 处理虚拟机停止 (接口 5.2)
  const handleStopVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.stopVM(vmId, { force: false })
      message.success('虚拟机停止命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 延迟 1000ms）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 1500)
    } catch (error: any) {
      message.error(error.message || '虚拟机停止失败')
      console.error('停止虚拟机失败:', error)
      setControlLoading(null)
    }
  }

  // 处理虚拟机重启 (接口 5.3)
  const handleRestartVM = async (vmId: string) => {
    setControlLoading(vmId)
    try {
      await vmService.restartVM(vmId)
      message.success('虚拟机重启命令已发送')
      // 延迟刷新，给状态变更一点时间（mock 延迟 1000ms）
      setTimeout(() => {
        onRefresh()
        setControlLoading(null)
      }, 1500)
    } catch (error: any) {
      message.error(error.message || '虚拟机重启失败')
      console.error('重启虚拟机失败:', error)
      setControlLoading(null)
    }
  }

  // 处理编辑虚拟机 (接口 4.3)
  const handleEditVM = (vm: any) => {
    if (onEdit) {
      onEdit(vm)
    } else {
      message.info('编辑功能开发中')
    }
  }

  // 处理删除虚拟机 (接口 4.4)
  const handleDeleteVM = async (vmId: string, vmName: string) => {
    try {
      await vmService.deleteVM(vmId, false)
      message.success(`虚拟机 "${vmName}" 删除成功`)
      onRefresh()
    } catch (error: any) {
      message.error(error.message || '虚拟机删除失败')
      console.error('删除虚拟机失败:', error)
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
      width: 450,
      render: (_, record) => {
        const isRunning = record.status === 'RUNNING'
        const isStopped = record.status === 'STOPPED'
        const isConnected = record.connectionStatus === 'CONNECTED'
        const isLoading = controlLoading === record.vmId

        return (
          <Space size="small" wrap>
            {isStopped && (
              <Tooltip title="启动虚拟机">
                <Popconfirm
                  title="确认启动虚拟机？"
                  description="虚拟机将开始运行"
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
              <Tooltip title="停止虚拟机">
                <Popconfirm
                  title="确认停止虚拟机？"
                  description="虚拟机将被安全停止"
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
              <Tooltip title="重启虚拟机">
                <Popconfirm
                  title="确认重启虚拟机？"
                  description="虚拟机将重新启动"
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

            <Tooltip title="编辑虚拟机">
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

            <Tooltip title="删除虚拟机">
              <Popconfirm
                title={`确认删除虚拟机 "${record.name}"？`}
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

