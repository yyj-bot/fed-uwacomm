/**
 * 系统设置组件
 */

import React from 'react'
import { Form, Input, Switch, Button, Card, Space, InputNumber, Select } from 'antd'
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons'

const { Option } = Select
const { TextArea } = Input

const SystemSettings: React.FC = () => {
  const [form] = Form.useForm()

  const handleSave = (values: any) => {
    console.log('保存系统设置:', values)
  }

  const handleReset = () => {
    form.resetFields()
  }

  return (
    <div className="fed-system-settings">
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSave}
        initialValues={{
          systemName: 'FedUWAComm',
          systemDesc: '联邦学习水声通信平台',
          enableRegistration: true,
          maxUsers: 1000,
          sessionTimeout: 30,
          logLevel: 'INFO'
        }}
      >
        <Card title="基本设置" className="fed-setting-card">
          <Form.Item
            label="系统名称"
            name="systemName"
            rules={[{ required: true, message: '请输入系统名称' }]}
          >
            <Input placeholder="请输入系统名称" />
          </Form.Item>

          <Form.Item
            label="系统描述"
            name="systemDesc"
          >
            <TextArea 
              rows={3} 
              placeholder="请输入系统描述"
            />
          </Form.Item>

          <Form.Item
            label="允许用户注册"
            name="enableRegistration"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="最大用户数"
            name="maxUsers"
          >
            <InputNumber 
              min={1} 
              max={10000} 
              style={{ width: '100%' }}
            />
          </Form.Item>
        </Card>

        <Card title="安全设置" className="fed-setting-card">
          <Form.Item
            label="会话超时时间（分钟）"
            name="sessionTimeout"
          >
            <InputNumber 
              min={5} 
              max={1440} 
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item
            label="日志级别"
            name="logLevel"
          >
            <Select style={{ width: '100%' }}>
              <Option value="DEBUG">调试</Option>
              <Option value="INFO">信息</Option>
              <Option value="WARN">警告</Option>
              <Option value="ERROR">错误</Option>
            </Select>
          </Form.Item>
        </Card>

        <div style={{ textAlign: 'right', marginTop: 24 }}>
          <Space>
            <Button onClick={handleReset} icon={<ReloadOutlined />}>
              重置
            </Button>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>
              保存设置
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  )
}

export default SystemSettings
