/**
 * VM编辑模态框组件
 * 用于编辑水下机器人的配置信息
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect } from 'react'
import { 
  Modal, 
  Form, 
  Input, 
  InputNumber, 
  Select, 
  Card,
  Row,
  Col,
  Space,
  Tag
} from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { VirtualMachine } from '@/api/vm'

const { Option } = Select
const { TextArea } = Input

interface VMEditModalProps {
  visible: boolean
  vm: VirtualMachine | null
  onSubmit: (values: any) => Promise<void>
  onCancel: () => void
}

const VMEditModal: React.FC<VMEditModalProps> = ({ 
  visible, 
  vm, 
  onSubmit, 
  onCancel 
}) => {
  const [form] = Form.useForm()

  // 当VM数据变化时，更新表单
  useEffect(() => {
    if (vm && visible) {
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
        'capabilities.supportedAlgorithms': vm.capabilities?.supportedAlgorithms,
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
        'metadata.tags': vm.metadata?.tags
      })
    }
  }, [vm, visible, form])

  // 处理表单提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      
      // 构建嵌套对象结构
      const formattedValues = {
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
          supportedAlgorithms: values['capabilities.supportedAlgorithms'],
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
          tags: values['metadata.tags']
        }
      }

      await onSubmit(formattedValues)
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  // 处理取消
  const handleCancel = () => {
    form.resetFields()
    onCancel()
  }

  return (
    <Modal
      title={`编辑水下机器人 - ${vm?.name || ''}`}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      width={800}
      okText="保存"
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        scrollToFirstError
      >
        {/* 基本信息 */}
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="水下机器人名称"
                name="name"
                rules={[
                  { required: true, message: '请输入水下机器人名称' },
                  { max: 100, message: '名称不能超过100个字符' }
                ]}
              >
                <Input placeholder="请输入水下机器人名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="操作系统"
                name="osType"
                rules={[{ required: true, message: '请输入操作系统类型' }]}
              >
                <Input placeholder="如: Ubuntu 20.04" />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="IP地址"
                name="ipAddress"
                rules={[
                  { required: true, message: '请输入IP地址' },
                  { pattern: /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/, message: '请输入有效的IP地址' }
                ]}
              >
                <Input placeholder="如: 192.168.1.100" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="端口"
                name="port"
                rules={[
                  { required: true, message: '请输入端口号' },
                  { type: 'number', min: 1, max: 65535, message: '端口号必须在1-65535范围内' }
                ]}
              >
                <InputNumber 
                  placeholder="如: 22" 
                  style={{ width: '100%' }}
                  min={1}
                  max={65535}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* 硬件配置 */}
        <Card title="硬件配置" size="small" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="CPU核心数"
                name="cpuCores"
                rules={[
                  { required: true, message: '请输入CPU核心数' },
                  { type: 'number', min: 1, message: 'CPU核心数必须大于0' }
                ]}
              >
                <InputNumber 
                  placeholder="如: 4" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="内存大小(MB)"
                name="memoryMb"
                rules={[
                  { required: true, message: '请输入内存大小' },
                  { type: 'number', min: 1024, message: '内存必须大于等于1024MB' }
                ]}
              >
                <InputNumber 
                  placeholder="如: 8192" 
                  style={{ width: '100%' }}
                  min={1024}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="磁盘大小(GB)"
                name="diskGb"
                rules={[
                  { required: true, message: '请输入磁盘大小' },
                  { type: 'number', min: 20, message: '磁盘必须大于等于20GB' }
                ]}
              >
                <InputNumber 
                  placeholder="如: 100" 
                  style={{ width: '100%' }}
                  min={20}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* 系统信息 */}
        <Card title="系统信息" size="small" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="操作系统详情"
                name="systemInfo.os"
              >
                <Input placeholder="如: Ubuntu 20.04 LTS" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="内核版本"
                name="systemInfo.kernel"
              >
                <Input placeholder="如: 5.4.0-42-generic" />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="Python版本"
                name="systemInfo.python"
              >
                <Input placeholder="如: 3.8.10" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="CUDA版本"
                name="systemInfo.cuda"
              >
                <Input placeholder="如: 11.0" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="cuDNN版本"
                name="systemInfo.cudnn"
              >
                <Input placeholder="如: 8.0.5" />
              </Form.Item>
            </Col>
          </Row>
          
          <Form.Item
            label="GPU信息"
            name="systemInfo.gpu"
          >
            <Input placeholder="如: NVIDIA Tesla V100" />
          </Form.Item>
        </Card>

        {/* 能力信息 */}
        <Card title="能力信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label="支持的算法"
            name="capabilities.supportedAlgorithms"
          >
            <Select
              mode="tags"
              placeholder="选择或输入支持的算法"
              style={{ width: '100%' }}
            >
              <Option value="FEDAVG">FEDAVG</Option>
              <Option value="FEDPROX">FEDPROX</Option>
              <Option value="FEDNOVA">FEDNOVA</Option>
              <Option value="SCAFFOLD">SCAFFOLD</Option>
            </Select>
          </Form.Item>
          
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="最大批次大小"
                name="capabilities.maxBatchSize"
              >
                <InputNumber 
                  placeholder="如: 128" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="最大内存使用(MB)"
                name="capabilities.maxMemoryUsage"
              >
                <InputNumber 
                  placeholder="如: 6144" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="GPU内存(MB)"
                name="capabilities.gpuMemory"
              >
                <InputNumber 
                  placeholder="如: 16384" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
          </Row>
          
          <Form.Item
            label="网络速度(Mbps)"
            name="capabilities.networkSpeed"
          >
            <InputNumber 
              placeholder="如: 1000" 
              style={{ width: '100%' }}
              min={1}
            />
          </Form.Item>
        </Card>

        {/* 网络配置 */}
        <Card title="网络配置" size="small" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="上传速度(KB/s)"
                name="networkConfig.uploadSpeed"
              >
                <InputNumber 
                  placeholder="如: 200" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="下载速度(KB/s)"
                name="networkConfig.downloadSpeed"
              >
                <InputNumber 
                  placeholder="如: 400" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="延迟(ms)"
                name="networkConfig.latency"
              >
                <InputNumber 
                  placeholder="如: 30" 
                  style={{ width: '100%' }}
                  min={0}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="带宽(Mbps)"
                name="networkConfig.bandwidth"
              >
                <InputNumber 
                  placeholder="如: 1000" 
                  style={{ width: '100%' }}
                  min={1}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* 元数据信息 */}
        <Card title="元数据信息" size="small">
          <Form.Item
            label="描述"
            name="metadata.description"
          >
            <TextArea 
              placeholder="水下机器人描述信息"
              rows={3}
            />
          </Form.Item>
          
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="位置"
                name="metadata.location"
              >
                <Input placeholder="如: 实验室A-机架01" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="负责人"
                name="metadata.owner"
              >
                <Input placeholder="如: 张三" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="部门"
                name="metadata.department"
              >
                <Input placeholder="如: 水声工程学院" />
              </Form.Item>
            </Col>
          </Row>
          
          <Form.Item
            label="标签"
            name="metadata.tags"
          >
            <Select
              mode="tags"
              placeholder="添加标签"
              style={{ width: '100%' }}
            >
              <Option value="水声">水声</Option>
              <Option value="联邦学习">联邦学习</Option>
              <Option value="GPU节点">GPU节点</Option>
              <Option value="高性能">高性能</Option>
            </Select>
          </Form.Item>
        </Card>
      </Form>
    </Modal>
  )
}

export default VMEditModal










