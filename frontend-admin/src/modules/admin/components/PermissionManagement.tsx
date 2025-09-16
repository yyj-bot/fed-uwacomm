/**
 * 权限管理组件
 */

import React from 'react'
import { Card, Tree, Checkbox, Space, Button, Typography } from 'antd'
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

const PermissionManagement: React.FC = () => {
  const permissionTree = [
    {
      title: '用户管理',
      key: 'user',
      children: [
        { title: '查看用户', key: 'user:read' },
        { title: '创建用户', key: 'user:create' },
        { title: '编辑用户', key: 'user:update' },
        { title: '删除用户', key: 'user:delete' }
      ]
    },
    {
      title: '虚拟机管理',
      key: 'vm',
      children: [
        { title: '查看虚拟机', key: 'vm:read' },
        { title: '创建虚拟机', key: 'vm:create' },
        { title: '操作虚拟机', key: 'vm:operate' },
        { title: '删除虚拟机', key: 'vm:delete' }
      ]
    },
    {
      title: '任务管理',
      key: 'task',
      children: [
        { title: '查看任务', key: 'task:read' },
        { title: '创建任务', key: 'task:create' },
        { title: '编辑任务', key: 'task:update' },
        { title: '删除任务', key: 'task:delete' }
      ]
    }
  ]

  return (
    <div className="fed-permission-management">
      <div style={{ marginBottom: 16 }}>
        <Title level={4}>角色权限配置</Title>
        <Text type="secondary">配置不同角色的系统访问权限</Text>
      </div>

      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {['管理员', '研究员', '操作员', '观察者'].map(role => (
          <Card key={role} title={role} size="small">
            <Tree
              checkable
              defaultExpandAll
              treeData={permissionTree}
              style={{ background: 'transparent' }}
            />
          </Card>
        ))}
      </Space>

      <div style={{ marginTop: 24, textAlign: 'right' }}>
        <Space>
          <Button icon={<ReloadOutlined />}>
            重置
          </Button>
          <Button type="primary" icon={<SaveOutlined />}>
            保存配置
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default PermissionManagement
