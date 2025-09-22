/**
 * 用户管理组件
 * 提供用户的增删改查功能
 */

import React, { useState, useCallback } from 'react'
import { 
  Space, 
  Button, 
  Input, 
  Select, 
  Tag, 
  Popconfirm, 
  message, 
  Dropdown,
  Tooltip 
} from 'antd'
import { 
  PlusOutlined, 
  SearchOutlined, 
  EditOutlined, 
  DeleteOutlined,
  EyeOutlined,
  LockOutlined,
  UnlockOutlined,
  KeyOutlined,
  MoreOutlined
} from '@ant-design/icons'
import { Table, StatusIndicator } from '@/components'
import { useAdmin } from '@/store'
import { UserFormModal, UserDetailModal, ResetPasswordModal } from './'
import type { User } from '@/types'

const { Search } = Input
const { Option } = Select

const UserManagement: React.FC = () => {
  const { 
    userList, 
    userListLoading, 
    fetchUserList,
    deleteUser,
    lockUser,
    unlockUser,
    operationLoading,
    canDeleteUser,
    canEditUser,
    canLockUser,
    canUnlockUser
  } = useAdmin()
  
  // 搜索和过滤状态
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  
  // 模态框状态
  const [userFormVisible, setUserFormVisible] = useState(false)
  const [userDetailVisible, setUserDetailVisible] = useState(false)
  const [resetPasswordVisible, setResetPasswordVisible] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create')

  // 处理用户操作的回调函数
  const handleViewUser = useCallback((user: User) => {
    setSelectedUser(user)
    setUserDetailVisible(true)
  }, [])

  const handleEditUser = useCallback((user: User) => {
    setEditingUser(user)
    setFormMode('edit')
    setUserFormVisible(true)
  }, [])

  const handleDeleteUser = useCallback(async (user: User) => {
    try {
      const result = await deleteUser(user.userId)
      
      if (result.success) {
        message.success('用户删除成功')
      } else {
        throw new Error(result.error || '删除用户失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      
      let errorMessage = '删除用户失败'
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
      if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
        errorMessage = '您没有删除该用户的权限'
      } else if (errorMessage.includes('in use') || errorMessage.includes('使用中')) {
        errorMessage = '该用户正在使用中，无法删除'
      } else if (errorMessage.includes('admin') || errorMessage.includes('管理员')) {
        errorMessage = '无法删除管理员用户'
      } else if (errorMessage.includes('not found') || errorMessage.includes('不存在')) {
        errorMessage = '用户不存在或已被删除'
      }
      
      message.error(errorMessage)
    }
  }, [deleteUser])

  const handleLockUser = useCallback(async (user: User) => {
    try {
      const result = await lockUser(user.userId)
      
      if (result.success) {
        message.success('用户已锁定')
      } else {
        throw new Error(result.error || '锁定用户失败')
      }
    } catch (error) {
      console.error('锁定用户失败:', error)
      
      let errorMessage = '锁定用户失败'
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
      if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
        errorMessage = '您没有锁定该用户的权限'
      } else if (errorMessage.includes('already locked') || errorMessage.includes('已锁定')) {
        errorMessage = '该用户已经被锁定'
      } else if (errorMessage.includes('admin') || errorMessage.includes('管理员')) {
        errorMessage = '无法锁定管理员用户'
      } else if (errorMessage.includes('not found') || errorMessage.includes('不存在')) {
        errorMessage = '用户不存在'
      }
      
      message.error(errorMessage)
    }
  }, [lockUser])

  const handleUnlockUser = useCallback(async (user: User) => {
    try {
      const result = await unlockUser(user.userId)
      
      if (result.success) {
        message.success('用户已解锁')
      } else {
        throw new Error(result.error || '解锁用户失败')
      }
    } catch (error) {
      console.error('解锁用户失败:', error)
      
      let errorMessage = '解锁用户失败'
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
      if (errorMessage.includes('permission') || errorMessage.includes('权限')) {
        errorMessage = '您没有解锁该用户的权限'
      } else if (errorMessage.includes('not locked') || errorMessage.includes('未锁定')) {
        errorMessage = '该用户未被锁定'
      } else if (errorMessage.includes('not found') || errorMessage.includes('不存在')) {
        errorMessage = '用户不存在'
      }
      
      message.error(errorMessage)
    }
  }, [unlockUser])

  const handleResetPassword = useCallback((user: User) => {
    setSelectedUser(user)
    setResetPasswordVisible(true)
  }, [])

  const handleAddUser = useCallback(() => {
    setEditingUser(null)
    setFormMode('create')
    setUserFormVisible(true)
  }, [])

  // 获取用户操作菜单
  const getUserActionMenu = useCallback((user: User) => {
    const items = [
      {
        key: 'view',
        label: '查看详情',
        icon: <EyeOutlined />
      }
    ]

    if (canEditUser(user)) {
      items.push({
        key: 'edit',
        label: '编辑',
        icon: <EditOutlined />
      })
    }

    if (canLockUser(user)) {
      items.push({
        key: 'lock',
        label: '锁定',
        icon: <LockOutlined />
      })
    }

    if (canUnlockUser(user)) {
      items.push({
        key: 'unlock',
        label: '解锁',
        icon: <UnlockOutlined />
      })
    }

    items.push({
      key: 'resetPassword',
      label: '重置密码',
      icon: <KeyOutlined />
    })

    if (canDeleteUser(user)) {
      items.push({
        key: 'delete',
        label: '删除',
        icon: <DeleteOutlined />
      } as any)
    }

    return {
      items,
      onClick: ({ key }: any) => {
        switch (key) {
          case 'view':
            handleViewUser(user)
            break
          case 'edit':
            handleEditUser(user)
            break
          case 'lock':
            handleLockUser(user)
            break
          case 'unlock':
            handleUnlockUser(user)
            break
          case 'resetPassword':
            handleResetPassword(user)
            break
          case 'delete':
            // 删除操作需要确认，这里只触发 Popconfirm
            break
        }
      }
    }
  }, [
    canEditUser, 
    canLockUser, 
    canUnlockUser, 
    canDeleteUser,
    handleViewUser,
    handleEditUser,
    handleLockUser,
    handleUnlockUser,
    handleResetPassword
  ])

  // 表格列定义
  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (text: string, record: User) => (
        <Button 
          type="link" 
          onClick={() => handleViewUser(record)}
          style={{ padding: 0, height: 'auto', fontWeight: 'bold' }}
        >
          {text}
        </Button>
      )
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email'
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => {
        const roleConfig: Record<string, { color: string; text: string }> = {
          'ADMIN': { color: 'red', text: '管理员' },
          'RESEARCHER': { color: 'blue', text: '研究员' },
          'OPERATOR': { color: 'green', text: '操作员' },
          'VIEWER': { color: 'default', text: '观察者' }
        }
        const config = roleConfig[role] || { color: 'default', text: role }
        return <Tag color={config.color}>{config.text}</Tag>
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <StatusIndicator
          status={
            status === 'ACTIVE' ? 'active' : 
            status === 'LOCKED' ? 'error' : 'inactive'
          }
          text={
            status === 'ACTIVE' ? '活跃' : 
            status === 'LOCKED' ? '已锁定' : '禁用'
          }
          variant="badge"
          size="small"
        />
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => {
        if (!date) return '-'
        return new Date(date).toLocaleString('zh-CN')
      }
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record: User) => {
        const actionMenu = getUserActionMenu(record)
        const isOperating = operationLoading[`delete-${record.userId}`] || 
                           operationLoading[`lock-${record.userId}`] || 
                           operationLoading[`unlock-${record.userId}`]

        return (
          <Space size={4}>
            <Tooltip title="查看详情">
              <Button 
                type="text" 
                size="small" 
                icon={<EyeOutlined />}
                onClick={() => handleViewUser(record)}
              />
            </Tooltip>

            {canEditUser(record) && (
              <Tooltip title="编辑">
                <Button 
                  type="text" 
                  size="small" 
                  icon={<EditOutlined />}
                  onClick={() => handleEditUser(record)}
                />
              </Tooltip>
            )}

            {canDeleteUser(record) && (
              <Popconfirm
                title="确认删除"
                description={`确定要删除用户 "${record.username}" 吗？此操作不可恢复。`}
                onConfirm={() => handleDeleteUser(record)}
                okText="确定"
                cancelText="取消"
                okType="danger"
              >
                <Tooltip title="删除">
                  <Button 
                    type="text" 
                    size="small" 
                    danger
                    icon={<DeleteOutlined />}
                    loading={operationLoading[`delete-${record.userId}`]}
                  />
                </Tooltip>
              </Popconfirm>
            )}

            <Dropdown menu={actionMenu} trigger={['click']} placement="bottomRight">
              <Button 
                type="text" 
                size="small" 
                icon={<MoreOutlined />}
                loading={isOperating}
              />
            </Dropdown>
          </Space>
        )
      }
    }
  ]

  // 过滤数据
  const filteredData = userList?.filter(user => {
    const matchText = !searchText || 
      user.username.toLowerCase().includes(searchText.toLowerCase()) ||
      user.email.toLowerCase().includes(searchText.toLowerCase())
    
    const matchStatus = statusFilter === 'all' || user.status === statusFilter
    
    return matchText && matchStatus
  })

  // 刷新数据
  const handleRefresh = useCallback(() => {
    fetchUserList()
  }, [fetchUserList])

  // 模态框成功回调
  const handleModalSuccess = useCallback(() => {
    fetchUserList()
  }, [fetchUserList])

  return (
    <div className="fed-user-management">
      {/* 操作栏 */}
      <div className="fed-user-toolbar">
        <Space>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleAddUser}
          >
            添加用户
          </Button>
          <Button onClick={handleRefresh}>
            刷新
          </Button>
        </Space>

        <Space>
          <Search
            placeholder="搜索用户名或邮箱"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 250 }}
            allowClear
          />
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 120 }}
          >
            <Option value="all">全部状态</Option>
            <Option value="ACTIVE">活跃</Option>
            <Option value="INACTIVE">禁用</Option>
            <Option value="LOCKED">已锁定</Option>
          </Select>
        </Space>
      </div>

      {/* 用户表格 */}
      <Table
        columns={columns}
        dataSource={filteredData}
        loading={userListLoading}
        rowKey="userId"
        showRefresh={false}
        showColumnSetting={false}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total, range) => 
            `第 ${range[0]}-${range[1]} 条 / 共 ${total} 条`
        }}
      />

      {/* 用户表单模态框 */}
      <UserFormModal
        visible={userFormVisible}
        mode={formMode}
        editUser={editingUser}
        onCancel={() => {
          setUserFormVisible(false)
          setEditingUser(null)
        }}
        onSuccess={handleModalSuccess}
      />

      {/* 用户详情模态框 */}
      <UserDetailModal
        visible={userDetailVisible}
        userId={selectedUser?.userId}
        onCancel={() => {
          setUserDetailVisible(false)
          setSelectedUser(null)
        }}
        onEdit={(user) => {
          setUserDetailVisible(false)
          setEditingUser(user)
          setFormMode('edit')
          setUserFormVisible(true)
        }}
        onResetPassword={(user) => {
          setUserDetailVisible(false)
          setSelectedUser(user)
          setResetPasswordVisible(true)
        }}
      />

      {/* 重置密码模态框 */}
      <ResetPasswordModal
        visible={resetPasswordVisible}
        user={selectedUser}
        onCancel={() => {
          setResetPasswordVisible(false)
          setSelectedUser(null)
        }}
        onSuccess={handleModalSuccess}
      />
    </div>
  )
}

export default UserManagement
