/**
 * 日志筛选组件
 * 提供日志级别、类别、时间范围等筛选功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Form, 
  Select, 
  DatePicker, 
  Input, 
  Button, 
  Space, 
  Row, 
  Col,
  Typography
} from 'antd'
import { 
  SearchOutlined, 
  ReloadOutlined,
  FilterOutlined
} from '@ant-design/icons'
import type { SystemLogListParams } from '@/services/system-log'
import { useSystem } from '@/store/system'

const { RangePicker } = DatePicker
const { Option } = Select
const { Search } = Input
const { Text } = Typography

// 日志级别选项
const LOG_LEVELS = [
  { value: 'DEBUG', label: 'DEBUG' },
  { value: 'INFO', label: 'INFO' },
  { value: 'WARN', label: 'WARN' },
  { value: 'ERROR', label: 'ERROR' }
]

// 日志类别选项
const LOG_CATEGORIES = [
  { value: 'SYSTEM', label: '系统日志' },
  { value: 'USER', label: '用户操作' },
  { value: 'VM', label: '虚拟机' },
  { value: 'TASK', label: '任务执行' },
  { value: 'DATA', label: '数据管理' },
  { value: 'MODEL', label: '模型管理' },
  { value: 'SECURITY', label: '安全审计' },
  { value: 'PERFORMANCE', label: '性能监控' }
]

interface LogFiltersProps {
  onFilter?: (params: SystemLogListParams) => void
  loading?: boolean
}

export const LogFilters: React.FC<LogFiltersProps> = ({ 
  onFilter, 
  loading = false 
}) => {
  const { 
    queryParams, 
    setQueryParams, 
    resetQueryParams, 
    fetchLogList 
  } = useSystem()

  const [form] = Form.useForm()
  const [collapsed, setCollapsed] = useState(false)

  // 组件加载调试
  useEffect(() => {
    console.log('🎯 LogFilters 组件已加载')
  }, [])

  // 应用筛选
  const handleFilter = () => {
    console.log('🔍 LogFilters - handleFilter 被调用了!')
    
    const values = form.getFieldsValue()
    console.log('📝 表单原始值:', values)
    
    // 处理时间范围
    let params: SystemLogListParams = {
      level: values.level,
      category: values.category,
      keyword: values.keyword,
      vmId: values.vmId,
      taskId: values.taskId
    }

    // 处理时间范围
    if (values.timeRange && values.timeRange.length === 2) {
      params = {
        ...params,
        startTime: values.timeRange[0].format('YYYY-MM-DDTHH:mm:ss'),
        endTime: values.timeRange[1].format('YYYY-MM-DDTHH:mm:ss')
      }
    }

    // 调试信息
    console.log('🚀 最终筛选参数:', params)
    console.log('📂 category值检查:', { 
      category: values.category, 
      type: typeof values.category,
      isUndefined: values.category === undefined,
      isNull: values.category === null 
    })

    // 更新查询参数
    setQueryParams(params)
    
    // 执行查询（重置到第一页）
    fetchLogList({ ...params, page: 1, size: 10 })
    
    // 回调
    onFilter?.(params)
  }

  // 重置筛选
  const handleReset = () => {
    form.resetFields()
    resetQueryParams()
    fetchLogList()
  }

  // 快速搜索
  const handleQuickSearch = (keyword: string) => {
    const params = { keyword }
    form.setFieldsValue({ keyword })
    setQueryParams(params)
    fetchLogList(params)
    onFilter?.(params)
  }

  return (
    <Card 
      title={
        <Space>
          <FilterOutlined />
          筛选条件
        </Space>
      }
      size="small"
      extra={
        <Button 
          type="text" 
          onClick={() => setCollapsed(!collapsed)}
        >
          {collapsed ? '展开' : '收起'}
        </Button>
      }
      style={{ marginBottom: 16 }}
    >
      {/* 快速搜索 */}
      <div style={{ marginBottom: collapsed ? 0 : 16 }}>
        <Search
          placeholder="搜索日志消息内容..."
          allowClear
          enterButton={<SearchOutlined />}
          size="middle"
          onSearch={handleQuickSearch}
          loading={loading}
        />
      </div>

      {/* 高级筛选 */}
      {!collapsed && (
        <Form
          form={form}
          layout="vertical"
          initialValues={queryParams}
        >
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item label="日志级别" name="level">
                <Select placeholder="选择日志级别" allowClear>
                  {LOG_LEVELS.map(level => (
                    <Option key={level.value} value={level.value}>
                      {level.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            
            <Col span={6}>
              <Form.Item label="日志类别" name="category">
                <Select placeholder="选择日志类别" allowClear>
                  {LOG_CATEGORIES.map(category => (
                    <Option key={category.value} value={category.value}>
                      {category.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            
            <Col span={12}>
              <Form.Item label="时间范围" name="timeRange">
                <RangePicker 
                  showTime 
                  format="YYYY-MM-DD HH:mm:ss"
                  placeholder={['开始时间', '结束时间']}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="虚拟机ID" name="vmId">
                <Input placeholder="输入虚拟机ID" allowClear />
              </Form.Item>
            </Col>
            
            <Col span={8}>
              <Form.Item label="任务ID" name="taskId">
                <Input placeholder="输入任务ID" allowClear />
              </Form.Item>
            </Col>
            
            <Col span={8}>
              <Form.Item label="关键词" name="keyword">
                <Input placeholder="搜索日志消息" allowClear />
              </Form.Item>
            </Col>
          </Row>

          <Row>
            <Col span={24}>
              <Space>
                <Button 
                  type="primary" 
                  icon={<SearchOutlined />}
                  onClick={handleFilter}
                  loading={loading}
                >
                  查询
                </Button>
                <Button 
                  icon={<ReloadOutlined />}
                  onClick={handleReset}
                >
                  重置
                </Button>
                <Text type="secondary">
                  共找到 {queryParams ? '筛选后的' : '全部'} 日志
                </Text>
              </Space>
            </Col>
          </Row>
        </Form>
      )}
    </Card>
  )
}

export default LogFilters
