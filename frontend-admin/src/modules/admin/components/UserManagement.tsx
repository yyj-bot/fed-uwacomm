/**
 * 用户管理组件
 * 提供用户的增删改查功能
 */

import React, { useState } from 'react'
import { Space, Button, Input, Select, Tag } from 'antd'
import { PlusOutlined, SearchOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { Table, StatusIndicator } from '@/components'
import { useAdmin } from '@/store'

const { Search } = Input
const { Option } = Select

const UserManagement: React.FC = () => {
  const { userList, userListLoading, fetchUserList } = useAdmin()
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')

  // 表格列定义
  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (text: string) => <strong>{text}</strong>
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
          status={status === 'ACTIVE' ? 'active' : 'inactive'}
          text={status === 'ACTIVE' ? '活跃' : '禁用'}
          variant="badge"
          size="small"
        />
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => new Date(date).toLocaleDateString()
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record: any) => (
        <Space>
          <Button 
            type="link" 
            size="small" 
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button 
            type="link" 
            size="small" 
            danger 
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ]

  const handleEdit = (record: any) => {
    console.log('编辑用户:', record)
  }

  const handleDelete = (record: any) => {
    console.log('删除用户:', record)
  }

  const handleAdd = () => {
    console.log('添加用户')
  }

  // 过滤数据
  const filteredData = userList?.filter(user => {
    const matchText = !searchText || 
      user.username.toLowerCase().includes(searchText.toLowerCase()) ||
      user.email.toLowerCase().includes(searchText.toLowerCase())
    
    const matchStatus = statusFilter === 'all' || user.status === statusFilter
    
    return matchText && matchStatus
  })

  return (
    <div className="fed-user-management">
      {/* 操作栏 */}
      <div className="fed-user-toolbar">
        <Space>
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={handleAdd}
          >
            添加用户
          </Button>
          <Button onClick={() => fetchUserList()}>
            刷新
          </Button>
        </Space>

        <Space>
          <Search
            placeholder="搜索用户名或邮箱"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            style={{ width: 120 }}
          >
            <Option value="all">全部状态</Option>
            <Option value="ACTIVE">活跃</Option>
            <Option value="INACTIVE">禁用</Option>
          </Select>
        </Space>
      </div>

      {/* 用户表格 */}
      <Table
        columns={columns}
        dataSource={filteredData}
        loading={userListLoading}
        rowKey="id"
        showRefresh
        showColumnSetting
        onRefresh={() => fetchUserList()}
      />
    </div>
  )
}

export default UserManagement
