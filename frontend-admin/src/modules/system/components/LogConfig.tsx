/**
 * 日志配置组件
 * 提供日志配置查询和更新功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { 
  Card, 
  Form, 
  Input, 
  InputNumber, 
  Switch, 
  Select, 
  Button, 
  Space, 
  Typography, 
  message,
  Divider,
  Row,
  Col,
  Spin,
  Tag
} from 'antd'
import { 
  SettingOutlined, 
  SaveOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import { useSystem } from '@/store/system'

const { Title, Text } = Typography
const { Option } = Select

// 日志级别选项
const LOG_LEVELS = [
  { value: 'DEBUG', label: 'DEBUG', color: 'default' },
  { value: 'INFO', label: 'INFO', color: 'blue' },
  { value: 'WARN', label: 'WARN', color: 'orange' },
  { value: 'ERROR', label: 'ERROR', color: 'red' }
]

// 日志类别选项
const LOG_CATEGORIES = [
  { key: 'SYSTEM', label: '系统日志' },
  { key: 'USER', label: '用户操作' },
  { key: 'VM', label: '虚拟机' },
  { key: 'TASK', label: '任务执行' },
  { key: 'DATA', label: '数据管理' },
  { key: 'MODEL', label: '模型管理' },
  { key: 'SECURITY', label: '安全审计' },
  { key: 'PERFORMANCE', label: '性能监控' }
]

// 导出格式选项
const EXPORT_FORMATS = ['CSV', 'JSON', 'EXCEL']

export const LogConfig: React.FC = () => {
  const {
    logConfig,
    fetchLogConfig,
    updateLogConfig
  } = useSystem()

  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  // 初始加载配置
  useEffect(() => {
    handleRefresh()
  }, [])

  // 配置加载完成后填充表单
  useEffect(() => {
    if (logConfig) {
      form.setFieldsValue({
        logLevel: logConfig.logLevel,
        retentionDays: logConfig.retentionDays,
        maxFileSize: logConfig.maxFileSize,
        categories: logConfig.categories,
        exportSettings: logConfig.exportSettings
      })
    }
  }, [logConfig, form])

  // 刷新配置
  const handleRefresh = async () => {
    setLoading(true)
    try {
      await fetchLogConfig()
    } catch (error) {
      message.error('获取日志配置失败')
    } finally {
      setLoading(false)
    }
  }

  // 保存配置
  const handleSave = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      
      await updateLogConfig(values)
      message.success('日志配置保存成功')
      
      // 重新获取配置
      await fetchLogConfig()
    } catch (error) {
      if (error.errorFields) {
        message.error('请检查表单输入')
      } else {
        message.error('保存日志配置失败')
      }
    } finally {
      setSaving(false)
    }
  }

  // 重置配置
  const handleReset = () => {
    if (logConfig) {
      form.setFieldsValue({
        logLevel: logConfig.logLevel,
        retentionDays: logConfig.retentionDays,
        maxFileSize: logConfig.maxFileSize,
        categories: logConfig.categories,
        exportSettings: logConfig.exportSettings
      })
      message.info('配置已重置')
    }
  }

  return (
    <div>
      {/* 操作栏 */}
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button 
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={loading}
          >
            刷新配置
          </Button>
          <Button 
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
          >
            保存配置
          </Button>
          <Button 
            onClick={handleReset}
          >
            重置
          </Button>
        </Space>
      </div>

      <Spin spinning={loading}>
        <Form
          form={form}
          layout="vertical"
          size="large"
        >
          {/* 基础配置 */}
          <Card 
            title={
              <Space>
                <SettingOutlined />
                基础配置
              </Space>
            }
            style={{ marginBottom: 16 }}
          >
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="全局日志级别"
                  name="logLevel"
                  rules={[{ required: true, message: '请选择日志级别' }]}
                >
                  <Select placeholder="选择日志级别">
                    {LOG_LEVELS.map(level => (
                      <Option key={level.value} value={level.value}>
                        <Tag color={level.color}>{level.label}</Tag>
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="日志保留天数"
                  name="retentionDays"
                  rules={[
                    { required: true, message: '请输入保留天数' },
                    { type: 'number', min: 1, max: 365, message: '保留天数必须在1-365之间' }
                  ]}
                >
                  <InputNumber 
                    min={1} 
                    max={365} 
                    style={{ width: '100%' }}
                    placeholder="输入保留天数"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="最大文件大小(MB)"
                  name="maxFileSize"
                  rules={[
                    { required: true, message: '请输入最大文件大小' },
                    { type: 'number', min: 1, max: 1024, message: '文件大小必须在1-1024MB之间' }
                  ]}
                >
                  <InputNumber 
                    min={1} 
                    max={1024} 
                    style={{ width: '100%' }}
                    placeholder="输入最大文件大小"
                  />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* 类别配置 */}
          <Card 
            title="日志类别配置"
            style={{ marginBottom: 16 }}
          >
            {LOG_CATEGORIES.map(category => (
              <Row key={category.key} gutter={16} style={{ marginBottom: 16 }}>
                <Col span={6}>
                  <Text strong>{category.label}</Text>
                </Col>
                <Col span={6}>
                  <Form.Item
                    name={['categories', category.key, 'level']}
                    label="级别"
                    rules={[{ required: true, message: '请选择日志级别' }]}
                  >
                    <Select placeholder="选择级别">
                      {LOG_LEVELS.map(level => (
                        <Option key={level.value} value={level.value}>
                          <Tag color={level.color}>{level.label}</Tag>
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={6}>
                  <Form.Item
                    name={['categories', category.key, 'enabled']}
                    label="启用状态"
                    valuePropName="checked"
                  >
                    <Switch 
                      checkedChildren="启用" 
                      unCheckedChildren="禁用"
                    />
                  </Form.Item>
                </Col>
              </Row>
            ))}
          </Card>

          {/* 导出设置 */}
          <Card 
            title="导出设置"
            style={{ marginBottom: 16 }}
          >
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  label="单次最大导出记录数"
                  name={['exportSettings', 'maxRecordsPerExport']}
                  rules={[
                    { required: true, message: '请输入最大导出记录数' },
                    { type: 'number', min: 1000, max: 1000000, message: '记录数必须在1000-1000000之间' }
                  ]}
                >
                  <InputNumber 
                    min={1000} 
                    max={1000000} 
                    style={{ width: '100%' }}
                    placeholder="输入最大记录数"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="导出文件保留天数"
                  name={['exportSettings', 'exportRetentionDays']}
                  rules={[
                    { required: true, message: '请输入保留天数' },
                    { type: 'number', min: 1, max: 30, message: '保留天数必须在1-30之间' }
                  ]}
                >
                  <InputNumber 
                    min={1} 
                    max={30} 
                    style={{ width: '100%' }}
                    placeholder="输入保留天数"
                  />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  label="支持的导出格式"
                  name={['exportSettings', 'supportedFormats']}
                  rules={[{ required: true, message: '请选择支持的格式' }]}
                >
                  <Select 
                    mode="multiple"
                    placeholder="选择支持的格式"
                    style={{ width: '100%' }}
                  >
                    {EXPORT_FORMATS.map(format => (
                      <Option key={format} value={format}>
                        {format}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Card>

          {/* 配置说明 */}
          <Card title="配置说明" size="small">
            <div style={{ color: '#666' }}>
              <p><ExclamationCircleOutlined style={{ color: '#faad14', marginRight: 8 }} />
                配置修改后将立即生效，请谨慎操作</p>
              <ul>
                <li><strong>全局日志级别</strong>：控制整个系统的最低日志级别</li>
                <li><strong>保留天数</strong>：超过此天数的日志将被自动清理</li>
                <li><strong>最大文件大小</strong>：单个日志文件的最大大小，超过后会滚动</li>
                <li><strong>类别配置</strong>：可以为不同类别设置不同的级别和启用状态</li>
                <li><strong>导出设置</strong>：控制日志导出的限制和格式支持</li>
              </ul>
            </div>
          </Card>
        </Form>
      </Spin>
    </div>
  )
}

export default LogConfig
