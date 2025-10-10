/**
 * VM强制控制组件
 * 管理员强制控制虚拟机（启动/停止/重启）
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState } from 'react'
import {
  Modal,
  Form,
  Select,
  Input,
  InputNumber,
  Button,
  Space,
  message,
  Tag,
  Alert
} from 'antd'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  RedoOutlined,
  WarningOutlined
} from '@ant-design/icons'
import { useAdminVM } from '@/store/vm'
import type { VmControlAction, VmStatus } from '@/services/admin/type'

const { Option } = Select
const { TextArea } = Input

interface VMForceControlProps {
  vmId: string
  vmName: string
  currentStatus: VmStatus
  onSuccess?: () => void
}

const VMForceControl: React.FC<VMForceControlProps> = ({
  vmId,
  vmName,
  currentStatus,
  onSuccess
}) => {
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const { forceControlVm, isVmOperating } = useAdminVM()

  // 控制操作选项
  const actionOptions: { label: string; value: VmControlAction; icon: React.ReactNode; color: string }[] = [
    {
      label: '启动 (START)',
      value: 'START',
      icon: <PlayCircleOutlined />,
      color: 'success'
    },
    {
      label: '停止 (STOP)',
      value: 'STOP',
      icon: <PauseCircleOutlined />,
      color: 'warning'
    },
    {
      label: '重启 (RESTART)',
      value: 'RESTART',
      icon: <RedoOutlined />,
      color: 'processing'
    },
    {
      label: '强制停止 (FORCE_STOP)',
      value: 'FORCE_STOP',
      icon: <WarningOutlined />,
      color: 'error'
    }
  ]

  // 获取推荐操作
  const getRecommendedActions = (): VmControlAction[] => {
    switch (currentStatus) {
      case 'STOPPED':
        return ['START']
      case 'RUNNING':
        return ['STOP', 'RESTART']
      case 'ERROR':
        return ['RESTART', 'FORCE_STOP']
      case 'STARTING':
      case 'STOPPING':
        return ['FORCE_STOP']
      default:
        return []
    }
  }

  const recommendedActions = getRecommendedActions()

  // 处理强制控制
  const handleForceControl = async () => {
    try {
      const values = await form.validateFields()
      
      if (!values.reason || values.reason.trim().length === 0) {
        message.error('必须填写操作原因')
        return
      }

      const result = await forceControlVm(
        vmId,
        values.action,
        values.reason,
        values.timeout
      )

      if (result.success) {
        message.success('强制控制命令已发送')
        setModalVisible(false)
        form.resetFields()
        onSuccess?.()
      } else {
        message.error(result.error || '强制控制失败')
      }
    } catch (error) {
      console.error('强制控制失败:', error)
    }
  }

  // 快捷操作按钮
  const renderQuickActions = () => {
    return (
      <Space>
        {recommendedActions.map(action => {
          const option = actionOptions.find(opt => opt.value === action)
          if (!option) return null

          return (
            <Button
              key={action}
              type={action === 'FORCE_STOP' ? 'primary' : 'default'}
              danger={action === 'FORCE_STOP'}
              icon={option.icon}
              onClick={() => {
                form.setFieldsValue({ action })
                setModalVisible(true)
              }}
              loading={isVmOperating(vmId, `force-control-${vmId}-${action}`)}
            >
              {option.label.split(' ')[0]}
            </Button>
          )
        })}
        <Button
          onClick={() => setModalVisible(true)}
        >
          更多操作
        </Button>
      </Space>
    )
  }

  return (
    <>
      {renderQuickActions()}

      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#ff4d4f' }} />
            强制控制虚拟机 - {vmName}
          </Space>
        }
        open={modalVisible}
        onOk={handleForceControl}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
        }}
        okText="确定执行"
        cancelText="取消"
        okButtonProps={{
          danger: true,
          loading: isVmOperating(vmId)
        }}
      >
        <Alert
          message="警告"
          description="强制控制将直接操作虚拟机，可能会影响正在运行的任务。请谨慎操作！"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <Form form={form} layout="vertical">
          <Form.Item
            label="当前状态"
          >
            <Tag color={
              currentStatus === 'RUNNING' ? 'success' :
              currentStatus === 'STOPPED' ? 'default' :
              currentStatus === 'ERROR' ? 'error' : 'processing'
            }>
              {currentStatus}
            </Tag>
          </Form.Item>

          <Form.Item
            label="控制操作"
            name="action"
            rules={[{ required: true, message: '请选择控制操作' }]}
          >
            <Select placeholder="请选择控制操作">
              {actionOptions.map(option => (
                <Option key={option.value} value={option.value}>
                  <Space>
                    {option.icon}
                    {option.label}
                    {recommendedActions.includes(option.value) && (
                      <Tag color="blue">推荐</Tag>
                    )}
                  </Space>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="操作原因"
            name="reason"
            rules={[
              { required: true, message: '请输入操作原因' },
              { min: 5, message: '操作原因至少5个字符' }
            ]}
          >
            <TextArea
              placeholder="请详细说明强制控制的原因，例如：系统维护、紧急故障处理等"
              rows={4}
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item
            label="超时时间（秒）"
            name="timeout"
            initialValue={300}
            tooltip="操作的最大等待时间"
          >
            <InputNumber
              min={30}
              max={600}
              style={{ width: '100%' }}
              placeholder="超时时间（秒）"
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}

export default VMForceControl

