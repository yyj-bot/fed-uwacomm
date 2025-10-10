/**
 * VM分配管理组件
 * 管理虚拟机的用户分配、权限管理
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
  Input,
  message,
  Popconfirm,
  Spin,
  Alert,
  Checkbox,
  Typography
} from 'antd'
import {
  UserAddOutlined,
  DeleteOutlined,
  EditOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useAdminVM } from '@/store/vm'
import { useAdmin } from '@/store/admin'
import type { VmPermission } from '@/services/admin/type'
import type { ColumnsType } from 'antd/es/table'

const { Option } = Select
const { TextArea } = Input
const { Text } = Typography

interface VMAssignmentManagerProps {
  vmId: string
  vmName: string
}

const VMAssignmentManager: React.FC<VMAssignmentManagerProps> = ({ vmId, vmName }) => {
  const [assignModalVisible, setAssignModalVisible] = useState(false)
  const [editPermissionModalVisible, setEditPermissionModalVisible] = useState(false)
  const [selectedUserId, setSelectedUserId] = useState<string>('')
  const [selectedUsername, setSelectedUsername] = useState<string>('')
  const [form] = Form.useForm()
  const [permissionForm] = Form.useForm()

  const {
    getVmAssignment,
    isVmAssignmentLoading,
    getVmAssignmentError,
    fetchVmAssignments,
    assignVmToUser,
    unassignVmFromUser,
    updateUserVmPermissions,
    isVmOperating
  } = useAdminVM()

  const { userList, fetchUserList } = useAdmin()

  const vmAssignment = getVmAssignment(vmId)
  const loading = isVmAssignmentLoading(vmId)
  const error = getVmAssignmentError(vmId)

  useEffect(() => {
    fetchVmAssignments(vmId)
    fetchUserList({ page: 1, size: 100 })
  }, [vmId])

  // 权限选项
  const permissionOptions: { label: string; value: VmPermission }[] = [
    { label: '读取 (READ)', value: 'READ' },
    { label: '写入 (WRITE)', value: 'WRITE' },
    { label: '执行 (EXECUTE)', value: 'EXECUTE' },
    { label: '管理 (ADMIN)', value: 'ADMIN' }
  ]

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

  // 处理分配
  const handleAssign = async () => {
    try {
      const values = await form.validateFields()
      const result = await assignVmToUser(vmId, values.userId, {
        permissions: values.permissions || ['READ'],
        notes: values.notes
      })

      if (result.success) {
        message.success('分配成功')
        setAssignModalVisible(false)
        form.resetFields()
        fetchVmAssignments(vmId)
      } else {
        message.error(result.error || '分配失败')
      }
    } catch (error) {
      console.error('分配失败:', error)
    }
  }

  // 处理取消分配
  const handleUnassign = async (userId: string, username: string) => {
    const result = await unassignVmFromUser(vmId, userId)
    
    if (result.success) {
      message.success(`已取消 ${username} 的分配`)
      fetchVmAssignments(vmId)
    } else {
      message.error(result.error || '取消分配失败')
    }
  }

  // 处理编辑权限
  const handleEditPermission = (userId: string, username: string, currentPermissions: VmPermission[]) => {
    setSelectedUserId(userId)
    setSelectedUsername(username)
    permissionForm.setFieldsValue({
      permissions: currentPermissions
    })
    setEditPermissionModalVisible(true)
  }

  // 处理更新权限
  const handleUpdatePermissions = async () => {
    try {
      const values = await permissionForm.validateFields()
      const result = await updateUserVmPermissions(vmId, selectedUserId, values.permissions)

      if (result.success) {
        message.success('权限更新成功')
        setEditPermissionModalVisible(false)
        permissionForm.resetFields()
        fetchVmAssignments(vmId)
      } else {
        message.error(result.error || '权限更新失败')
      }
    } catch (error) {
      console.error('权限更新失败:', error)
    }
  }

  // 表格列定义
  const columns: ColumnsType<any> = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (text: string) => <Text strong>{text}</Text>
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
      ellipsis: true
    },
    {
      title: '权限',
      dataIndex: 'permissions',
      key: 'permissions',
      width: 200,
      render: (permissions: VmPermission[]) => (
        <Space size={[0, 4]} wrap>
          {permissions.map(permission => (
            <Tag key={permission} color={getPermissionColor(permission)}>
              {permission}
            </Tag>
          ))}
        </Space>
      )
    },
    {
      title: '分配时间',
      dataIndex: 'assignedAt',
      key: 'assignedAt',
      width: 170,
      render: (time: string) => new Date(time).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    {
      title: '分配者',
      dataIndex: 'assignedBy',
      key: 'assignedBy',
      width: 100
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_: any, record: any) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditPermission(record.userId, record.username, record.permissions)}
            loading={isVmOperating(vmId, `update-permissions-${vmId}-${record.userId}`)}
          >
            编辑权限
          </Button>
          <Popconfirm
            title="确定取消分配吗?"
            description={`将取消 ${record.username} 对此虚拟机的访问权限`}
            onConfirm={() => handleUnassign(record.userId, record.username)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              size="small"
              icon={<DeleteOutlined />}
              loading={isVmOperating(vmId, `unassign-${vmId}-${record.userId}`)}
            >
              取消分配
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 过滤掉已分配的用户
  const availableUsers = userList.filter(
    user => !vmAssignment?.assignments.some(a => a.userId === user.userId)
  )

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 0' }}>
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (error) {
    return (
      <Alert
        message="加载失败"
        description={error}
        type="error"
        showIcon
        action={
          <Button size="small" onClick={() => fetchVmAssignments(vmId)}>
            重试
          </Button>
        }
      />
    )
  }

  return (
    <div>
      {/* 操作栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text type="secondary">
          当前已分配 <Text strong>{vmAssignment?.assignments?.length || 0}</Text> 个用户
        </Text>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => fetchVmAssignments(vmId)}
          >
            刷新
          </Button>
          <Button
            type="primary"
            icon={<UserAddOutlined />}
            onClick={() => setAssignModalVisible(true)}
          >
            分配给用户
          </Button>
        </Space>
      </div>

      {/* 分配列表 */}
      <Table
        dataSource={vmAssignment?.assignments || []}
        columns={columns}
        rowKey="userId"
        pagination={false}
        locale={{ emptyText: '暂无分配记录' }}
        scroll={{ x: 1000 }}
        size="middle"
      />

      {/* 分配模态框 */}
      <Modal
        title="分配虚拟机给用户"
        open={assignModalVisible}
        onOk={handleAssign}
        onCancel={() => {
          setAssignModalVisible(false)
          form.resetFields()
        }}
        okText="确定"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="选择用户"
            name="userId"
            rules={[{ required: true, message: '请选择用户' }]}
          >
            <Select
              placeholder="请选择用户"
              showSearch
              optionFilterProp="children"
            >
              {availableUsers.map(user => (
                <Option key={user.userId} value={user.userId}>
                  {user.username} ({user.email})
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
              placeholder="可选，添加分配备注"
              rows={3}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 编辑权限模态框 */}
      <Modal
        title={`编辑权限 - ${selectedUsername}`}
        open={editPermissionModalVisible}
        onOk={handleUpdatePermissions}
        onCancel={() => {
          setEditPermissionModalVisible(false)
          permissionForm.resetFields()
        }}
        okText="确定"
        cancelText="取消"
      >
        <Form form={permissionForm} layout="vertical">
          <Form.Item
            label="权限"
            name="permissions"
            rules={[{ required: true, message: '请选择至少一个权限' }]}
          >
            <Checkbox.Group options={permissionOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default VMAssignmentManager

