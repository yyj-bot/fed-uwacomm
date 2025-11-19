/**
 * 用户VM列表组件
 * 显示特定用户的水下机器人列表，支持批量分配和移除
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Select,
  Checkbox,
  Input,
  message,
  Alert,
  Spin
} from 'antd'
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useAdminVM } from '@/store/vm'
import type { VmPermission, VmStatus } from '@/services/admin/type'
import type { ColumnsType } from 'antd/es/table'

const { Option } = Select
const { TextArea } = Input

interface UserVmListProps {
  userId: string
  username: string
}

const UserVmList: React.FC<UserVmListProps> = ({ userId, username }) => {
  const [batchAssignModalVisible, setBatchAssignModalVisible] = useState(false)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [form] = Form.useForm()

  const {
    getUserVms,
    isUserVmListLoading,
    getUserVmListError,
    fetchUserVmList,
    batchAssignVmsToUser,
    batchRemoveUserVms,
    unassignedVms,
    fetchUnassignedVmList,
    isVmOperating
  } = useAdminVM()

  const userVmData = getUserVms(userId)
  const loading = isUserVmListLoading(userId)
  const error = getUserVmListError(userId)

  // 调试日志
  console.log('[UserVmList] userId:', userId)
  console.log('[UserVmList] userVmData:', userVmData)
  console.log('[UserVmList] VMs 数量:', userVmData?.vms?.length)

  useEffect(() => {
    console.log('[UserVmList] useEffect 触发，获取用户VM列表:', userId)
    fetchUserVmList(userId)
    fetchUnassignedVmList()
  }, [userId])

  // 权限选项
  const permissionOptions: { label: string; value: VmPermission }[] = [
    { label: '读取', value: 'READ' },
    { label: '写入', value: 'WRITE' },
    { label: '执行', value: 'EXECUTE' },
    { label: '管理', value: 'ADMIN' }
  ]

  // VM状态标签颜色
  const getStatusColor = (status: VmStatus): string => {
    const colorMap: Record<VmStatus, string> = {
      'RUNNING': 'success',
      'STOPPED': 'default',
      'ERROR': 'error',
      'STARTING': 'processing',
      'STOPPING': 'warning',
      'OFFLINE': 'default'
    }
    return colorMap[status]
  }

  // 权限标签颜色
  const getPermissionColor = (permission: VmPermission): string => {
    const colorMap: Record<VmPermission, string> = {
      'READ': 'blue',
      'WRITE': 'green',
      'EXECUTE': 'orange',
      'ADMIN': 'red'
    }
    return colorMap[permission]
  }

  // 处理批量分配
  const handleBatchAssign = async () => {
    try {
      const values = await form.validateFields()
      const result = await batchAssignVmsToUser(userId, values.vmIds, {
        permissions: values.permissions || ['READ'],
        notes: values.notes
      })

      if (result.success) {
        message.success('批量分配成功')
        setBatchAssignModalVisible(false)
        form.resetFields()
        fetchUserVmList(userId)
      } else {
        message.error(result.error || '批量分配失败')
      }
    } catch (error) {
      console.error('批量分配失败:', error)
    }
  }

  // 处理批量移除
  const handleBatchRemove = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要移除的水下机器人')
      return
    }

    Modal.confirm({
      title: '确认批量移除',
      content: `确定要移除 ${selectedRowKeys.length} 台水下机器人的分配吗？`,
      onOk: async () => {
        const result = await batchRemoveUserVms(userId, selectedRowKeys as string[])
        
        if (result.success) {
          message.success('批量移除成功')
          setSelectedRowKeys([])
          fetchUserVmList(userId)
        } else {
          message.error(result.error || '批量移除失败')
        }
      }
    })
  }

  // 表格列定义
  const columns: ColumnsType<any> = [
    {
      title: '水下机器人名称',
      dataIndex: 'vmName',
      key: 'vmName'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: VmStatus) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      )
    },
    {
      title: '权限',
      dataIndex: 'permissions',
      key: 'permissions',
      render: (permissions: VmPermission[]) => (
        <>
          {permissions.map(permission => (
            <Tag key={permission} color={getPermissionColor(permission)}>
              {permission}
            </Tag>
          ))}
        </>
      )
    },
    {
      title: '分配时间',
      dataIndex: 'assignedAt',
      key: 'assignedAt',
      render: (time: string) => new Date(time).toLocaleString()
    }
  ]

  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys: React.Key[]) => {
      setSelectedRowKeys(newSelectedRowKeys)
    }
  }

  if (loading && !userVmData) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin tip="加载中..." />
        </div>
      </Card>
    )
  }

  if (error) {
    return (
      <Card>
        <Alert
          message="加载失败"
          description={error}
          type="error"
          showIcon
          action={
            <Button size="small" onClick={() => fetchUserVmList(userId)}>
              重试
            </Button>
          }
        />
      </Card>
    )
  }

  return (
    <Card
      title={`${username} - 水下机器人列表`}
      extra={
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchUserVmList(userId)}
            loading={loading}
          >
            刷新
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setBatchAssignModalVisible(true)}
          >
            批量分配
          </Button>
          <Button
            icon={<DeleteOutlined />}
            onClick={handleBatchRemove}
            disabled={selectedRowKeys.length === 0}
            loading={isVmOperating('', `batch-remove-${userId}`)}
            style={{ color: '#ff4d4f', borderColor: '#ff4d4f' }}
          >
            批量移除
          </Button>
        </Space>
      }
    >
      <Table
        rowSelection={rowSelection}
        dataSource={userVmData?.vms || []}
        columns={columns}
        rowKey="vmId"
        pagination={{
          total: userVmData?.total || 0,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 台水下机器人`
        }}
        locale={{ emptyText: '该用户暂未分配任何水下机器人' }}
        loading={loading}
      />

      {/* 批量分配模态框 */}
      <Modal
        title={`批量分配水下机器人给 ${username}`}
        open={batchAssignModalVisible}
        onOk={handleBatchAssign}
        onCancel={() => {
          setBatchAssignModalVisible(false)
          form.resetFields()
        }}
        width={600}
        centered
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="选择水下机器人"
            name="vmIds"
            rules={[{ required: true, message: '请选择至少一台水下机器人' }]}
          >
            <Select
              mode="multiple"
              placeholder="请选择水下机器人"
              showSearch
              optionFilterProp="children"
              maxTagCount="responsive"
            >
              {unassignedVms.map(vm => (
                <Option key={vm.vmId} value={vm.vmId}>
                  {vm.name} - {vm.ipAddress} ({vm.status})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="权限"
            name="permissions"
            initialValue={['READ']}
            rules={[{ required: true, message: '请选择至少一个权限' }]}
          >
            <Checkbox.Group options={permissionOptions} />
          </Form.Item>

          <Form.Item
            label="备注"
            name="notes"
          >
            <TextArea
              placeholder="可选，添加批量分配备注"
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}

export default UserVmList

