/**
 * 虚拟机编辑抽屉组件
 * 用于管理员编辑虚拟机配置信息
 * 基于 vm-api-reference.md 4.3 虚拟机更新接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import {
  Drawer,
  Form,
  Input,
  InputNumber,
  Button,
  Space,
  message,
  Tabs,
  Select,
  Tag
} from 'antd'
import { vmService } from '@/services/vm/vmService'

const { TextArea } = Input
const { TabPane } = Tabs
const { Option } = Select

interface VMEditDrawerProps {
  open: boolean
  vm: any | null
  onClose: () => void
  onSuccess: () => void
}

const VMEditDrawer: React.FC<VMEditDrawerProps> = ({
  open,
  vm,
  onClose,
  onSuccess
}) => {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [tags, setTags] = useState<string[]>([])
  const [inputTag, setInputTag] = useState('')

  useEffect(() => {
    if (vm && open) {
      // 填充表单数据
      form.setFieldsValue({
        name: vm.name,
        ipAddress: vm.ipAddress,
        port: vm.port,
        osType: vm.osType,
        cpuCores: vm.cpuCores,
        memoryMb: vm.memoryMb,
        diskGb: vm.diskGb,
        // 系统信息
        'systemInfo.os': vm.systemInfo?.os,
        'systemInfo.kernel': vm.systemInfo?.kernel,
        'systemInfo.python': vm.systemInfo?.python,
        'systemInfo.gpu': vm.systemInfo?.gpu,
        'systemInfo.cuda': vm.systemInfo?.cuda,
        'systemInfo.cudnn': vm.systemInfo?.cudnn,
        // 能力信息
        'capabilities.maxBatchSize': vm.capabilities?.maxBatchSize,
        'capabilities.maxMemoryUsage': vm.capabilities?.maxMemoryUsage,
        'capabilities.gpuMemory': vm.capabilities?.gpuMemory,
        'capabilities.networkSpeed': vm.capabilities?.networkSpeed,
        // 网络配置
        'networkConfig.uploadSpeed': vm.networkConfig?.uploadSpeed,
        'networkConfig.downloadSpeed': vm.networkConfig?.downloadSpeed,
        'networkConfig.latency': vm.networkConfig?.latency,
        'networkConfig.bandwidth': vm.networkConfig?.bandwidth,
        // 元数据
        'metadata.description': vm.metadata?.description,
        'metadata.location': vm.metadata?.location,
        'metadata.owner': vm.metadata?.owner,
        'metadata.department': vm.metadata?.department,
      })
      
      // 设置标签
      setTags(vm.metadata?.tags || [])
    }
  }, [vm, open, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      // 构建更新数据
      const updateData = {
        name: values.name,
        ipAddress: values.ipAddress,
        port: values.port,
        osType: values.osType,
        cpuCores: values.cpuCores,
        memoryMb: values.memoryMb,
        diskGb: values.diskGb,
        systemInfo: {
          os: values['systemInfo.os'],
          kernel: values['systemInfo.kernel'],
          python: values['systemInfo.python'],
          gpu: values['systemInfo.gpu'],
          cuda: values['systemInfo.cuda'],
          cudnn: values['systemInfo.cudnn']
        },
        capabilities: {
          supportedAlgorithms: vm.capabilities?.supportedAlgorithms || [],
          maxBatchSize: values['capabilities.maxBatchSize'],
          maxMemoryUsage: values['capabilities.maxMemoryUsage'],
          gpuMemory: values['capabilities.gpuMemory'],
          networkSpeed: values['capabilities.networkSpeed']
        },
        networkConfig: {
          uploadSpeed: values['networkConfig.uploadSpeed'],
          downloadSpeed: values['networkConfig.downloadSpeed'],
          latency: values['networkConfig.latency'],
          bandwidth: values['networkConfig.bandwidth']
        },
        metadata: {
          description: values['metadata.description'],
          location: values['metadata.location'],
          owner: values['metadata.owner'],
          department: values['metadata.department'],
          tags: tags
        }
      }

      await vmService.updateVM(vm.vmId, updateData)
      message.success('虚拟机信息更新成功')
      onSuccess()
      handleClose()
    } catch (error: any) {
      if (error.errorFields) {
        message.error('请检查表单填写是否正确')
      } else {
        message.error(error.message || '虚拟机信息更新失败')
        console.error('更新虚拟机失败:', error)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    form.resetFields()
    setTags([])
    setInputTag('')
    onClose()
  }

  const handleAddTag = () => {
    if (inputTag && !tags.includes(inputTag)) {
      setTags([...tags, inputTag])
      setInputTag('')
    }
  }

  const handleRemoveTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag))
  }

  return (
    <Drawer
      title={`编辑虚拟机 - ${vm?.name || ''}`}
      placement="right"
      width={720}
      open={open}
      onClose={handleClose}
      footer={
        <div style={{ textAlign: 'right' }}>
          <Space>
            <Button onClick={handleClose}>取消</Button>
            <Button type="primary" onClick={handleSubmit} loading={loading}>
              保存
            </Button>
          </Space>
        </div>
      }
    >
      <Form
        form={form}
        layout="vertical"
        autoComplete="off"
      >
        <Tabs defaultActiveKey="basic">
          <TabPane tab="基本信息" key="basic">
            <Form.Item
              name="name"
              label="虚拟机名称"
              rules={[{ required: true, message: '请输入虚拟机名称' }]}
            >
              <Input placeholder="请输入虚拟机名称" />
            </Form.Item>

            <Form.Item
              name="ipAddress"
              label="IP地址"
              rules={[
                { required: true, message: '请输入IP地址' },
                { pattern: /^(\d{1,3}\.){3}\d{1,3}$/, message: '请输入有效的IP地址' }
              ]}
            >
              <Input placeholder="192.168.1.100" />
            </Form.Item>

            <Form.Item
              name="port"
              label="端口"
              rules={[{ required: true, message: '请输入端口号' }]}
            >
              <InputNumber min={1} max={65535} style={{ width: '100%' }} placeholder="22" />
            </Form.Item>

            <Form.Item
              name="osType"
              label="操作系统"
              rules={[{ required: true, message: '请输入操作系统类型' }]}
            >
              <Input placeholder="Ubuntu 20.04" />
            </Form.Item>

            <Form.Item
              name="cpuCores"
              label="CPU核心数"
              rules={[{ required: true, message: '请输入CPU核心数' }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} placeholder="4" />
            </Form.Item>

            <Form.Item
              name="memoryMb"
              label="内存 (MB)"
              rules={[{ required: true, message: '请输入内存大小' }]}
            >
              <InputNumber min={512} style={{ width: '100%' }} placeholder="8192" />
            </Form.Item>

            <Form.Item
              name="diskGb"
              label="硬盘 (GB)"
              rules={[{ required: true, message: '请输入硬盘大小' }]}
            >
              <InputNumber min={10} style={{ width: '100%' }} placeholder="100" />
            </Form.Item>
          </TabPane>

          <TabPane tab="系统信息" key="system">
            <Form.Item name="systemInfo.os" label="操作系统">
              <Input placeholder="Ubuntu 20.04 LTS" />
            </Form.Item>

            <Form.Item name="systemInfo.kernel" label="内核版本">
              <Input placeholder="5.4.0-42-generic" />
            </Form.Item>

            <Form.Item name="systemInfo.python" label="Python版本">
              <Input placeholder="3.8.10" />
            </Form.Item>

            <Form.Item name="systemInfo.gpu" label="GPU型号">
              <Input placeholder="NVIDIA Tesla V100" />
            </Form.Item>

            <Form.Item name="systemInfo.cuda" label="CUDA版本">
              <Input placeholder="11.0" />
            </Form.Item>

            <Form.Item name="systemInfo.cudnn" label="cuDNN版本">
              <Input placeholder="8.0.5" />
            </Form.Item>
          </TabPane>

          <TabPane tab="能力配置" key="capabilities">
            <Form.Item name="capabilities.maxBatchSize" label="最大批处理大小">
              <InputNumber min={1} style={{ width: '100%' }} placeholder="128" />
            </Form.Item>

            <Form.Item name="capabilities.maxMemoryUsage" label="最大内存使用 (MB)">
              <InputNumber min={512} style={{ width: '100%' }} placeholder="8192" />
            </Form.Item>

            <Form.Item name="capabilities.gpuMemory" label="GPU内存 (MB)">
              <InputNumber min={0} style={{ width: '100%' }} placeholder="8192" />
            </Form.Item>

            <Form.Item name="capabilities.networkSpeed" label="网络速度 (Mbps)">
              <InputNumber min={1} style={{ width: '100%' }} placeholder="1000" />
            </Form.Item>
          </TabPane>

          <TabPane tab="网络配置" key="network">
            <Form.Item name="networkConfig.uploadSpeed" label="上传速度 (Mbps)">
              <InputNumber min={1} style={{ width: '100%' }} placeholder="100" />
            </Form.Item>

            <Form.Item name="networkConfig.downloadSpeed" label="下载速度 (Mbps)">
              <InputNumber min={1} style={{ width: '100%' }} placeholder="200" />
            </Form.Item>

            <Form.Item name="networkConfig.latency" label="延迟 (ms)">
              <InputNumber min={0} style={{ width: '100%' }} placeholder="20" />
            </Form.Item>

            <Form.Item name="networkConfig.bandwidth" label="带宽 (Mbps)">
              <InputNumber min={1} style={{ width: '100%' }} placeholder="1000" />
            </Form.Item>
          </TabPane>

          <TabPane tab="元数据" key="metadata">
            <Form.Item name="metadata.description" label="描述">
              <TextArea rows={3} placeholder="虚拟机用途说明" />
            </Form.Item>

            <Form.Item name="metadata.location" label="位置">
              <Input placeholder="实验室A-机架01" />
            </Form.Item>

            <Form.Item name="metadata.owner" label="负责人">
              <Input placeholder="张三" />
            </Form.Item>

            <Form.Item name="metadata.department" label="部门">
              <Input placeholder="水声工程学院" />
            </Form.Item>

            <Form.Item label="标签">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Space wrap>
                  {tags.map(tag => (
                    <Tag key={tag} closable onClose={() => handleRemoveTag(tag)}>
                      {tag}
                    </Tag>
                  ))}
                </Space>
                <Space.Compact style={{ width: '100%' }}>
                  <Input
                    value={inputTag}
                    onChange={e => setInputTag(e.target.value)}
                    onPressEnter={handleAddTag}
                    placeholder="输入标签后按回车添加"
                  />
                  <Button onClick={handleAddTag}>添加</Button>
                </Space.Compact>
              </Space>
            </Form.Item>
          </TabPane>
        </Tabs>
      </Form>
    </Drawer>
  )
}

export default VMEditDrawer

