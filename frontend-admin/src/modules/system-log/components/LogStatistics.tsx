/**
 * 日志统计组件
 * 显示日志统计数据，包括级别分布、类别分布、时间分布等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { 
  Row, 
  Col, 
  Card, 
  Statistic, 
  Progress,
  DatePicker,
  Select,
  Button,
  Space,
  Typography,
  Spin,
  Empty
} from 'antd'
import { 
  BarChartOutlined,
  ReloadOutlined,
  FilterOutlined
} from '@ant-design/icons'
import { useSystem } from '@/store/system'
import dayjs from 'dayjs'

const { RangePicker } = DatePicker
const { Title, Text } = Typography
const { Option } = Select

export const LogStatistics: React.FC = () => {
  const {
    logStatistics,
    logStatisticsLoading,
    fetchLogStatistics
  } = useSystem()

  const [timeRange, setTimeRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(7, 'days'),
    dayjs()
  ])
  const [category, setCategory] = useState<string>()
  const [vmId, setVmId] = useState<string>()
  const [taskId, setTaskId] = useState<string>()

  // 初始加载
  useEffect(() => {
    handleRefresh()
  }, [])

  // 刷新统计数据
  const handleRefresh = async () => {
    const params: any = {}
    
    // 只添加有值的参数，避免传递 undefined
    if (category) params.category = category
    if (vmId) params.vmId = vmId
    if (taskId) params.taskId = taskId
    
    // 只有当时间范围有效时才添加时间参数
    if (timeRange && timeRange[0] && timeRange[1]) {
      // 使用ISO 8601格式，符合API文档要求
      params.startTime = timeRange[0].format('YYYY-MM-DDTHH:mm:ss')
      params.endTime = timeRange[1].format('YYYY-MM-DDTHH:mm:ss')
    }
    
    console.log('📊 [LogStatistics] 发送统计查询请求:', params)
    
    try {
      await fetchLogStatistics(params)
      console.log('✅ [LogStatistics] 统计数据获取成功')
    } catch (error) {
      console.error('❌ [LogStatistics] 统计数据获取失败:', error)
      // 可以在这里添加用户友好的错误提示
    }
  }

  // 应用筛选
  const handleFilter = () => {
    handleRefresh()
  }

  // 重置筛选
  const handleReset = () => {
    setTimeRange([dayjs().subtract(7, 'days'), dayjs()])
    setCategory(undefined)
    setVmId(undefined)
    setTaskId(undefined)
  }

  if (!logStatistics && !logStatisticsLoading) {
    return (
      <Card>
        <Empty description="暂无统计数据" />
      </Card>
    )
  }

  return (
    <div>
      {/* 筛选条件 */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col span={8}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text strong>时间范围</Text>
              <RangePicker
                value={timeRange}
                onChange={(dates) => dates && setTimeRange(dates)}
                showTime
                format="YYYY-MM-DD HH:mm:ss"
                style={{ width: '100%' }}
              />
            </Space>
          </Col>
          <Col span={4}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text strong>日志类别</Text>
              <Select
                placeholder="选择类别"
                value={category}
                onChange={setCategory}
                allowClear
                style={{ width: '100%' }}
              >
                <Option value="SYSTEM">系统日志</Option>
                <Option value="USER">用户操作</Option>
                <Option value="VM">水下机器人</Option>
                <Option value="TASK">任务执行</Option>
                <Option value="DATA">数据管理</Option>
                <Option value="MODEL">模型管理</Option>
                <Option value="SECURITY">安全审计</Option>
                <Option value="PERFORMANCE">性能监控</Option>
              </Select>
            </Space>
          </Col>
          <Col span={4}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text strong>水下机器人ID</Text>
              <Select
                placeholder="选择水下机器人"
                value={vmId}
                onChange={setVmId}
                allowClear
                style={{ width: '100%' }}
                showSearch
              >
                {/* 这里可以从后端获取水下机器人列表 */}
              </Select>
            </Space>
          </Col>
          <Col span={4}>
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text strong>任务ID</Text>
              <Select
                placeholder="选择任务"
                value={taskId}
                onChange={setTaskId}
                allowClear
                style={{ width: '100%' }}
                showSearch
              >
                {/* 这里可以从后端获取任务列表 */}
              </Select>
            </Space>
          </Col>
          <Col span={4}>
            <Space direction="vertical" size="small">
              <Text strong>&nbsp;</Text>
              <Space>
                <Button 
                  type="primary"
                  icon={<FilterOutlined />}
                  onClick={handleFilter}
                  loading={logStatisticsLoading}
                >
                  查询
                </Button>
                <Button 
                  icon={<ReloadOutlined />}
                  onClick={handleReset}
                >
                  重置
                </Button>
              </Space>
            </Space>
          </Col>
        </Row>
      </Card>

      <Spin spinning={logStatisticsLoading}>
        {/* 总体统计 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card>
              <Statistic
                title="日志总数"
                value={logStatistics?.totalLogs || 0}
                prefix={<BarChartOutlined />}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="错误日志"
                value={logStatistics?.levelDistribution?.ERROR || 0}
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="警告日志"
                value={logStatistics?.levelDistribution?.WARN || 0}
                valueStyle={{ color: '#fa8c16' }}
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card>
              <Statistic
                title="信息日志"
                value={logStatistics?.levelDistribution?.INFO || 0}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
        </Row>

        {/* 级别分布 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={12}>
            <Card title="日志级别分布" size="small">
              {logStatistics?.levelDistribution ? (
                <div>
                  {Object.entries(logStatistics.levelDistribution).map(([level, count]) => {
                    const total = logStatistics.totalLogs || 1
                    const numCount = typeof count === 'number' ? count : 0
                    const percentage = (numCount / total) * 100
                    const colors = {
                      ERROR: '#ff4d4f',
                      WARN: '#faad14',
                      INFO: '#1890ff',
                      DEBUG: '#52c41a'
                    }
                    return (
                      <div key={level} style={{ marginBottom: 12 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                          <Text>{level}</Text>
                          <Text>{String(numCount)}</Text>
                        </div>
                        <Progress 
                          percent={percentage} 
                          strokeColor={colors[level as keyof typeof colors] || '#1890ff'}
                          showInfo={false}
                        />
                      </div>
                    )
                  })}
                </div>
              ) : (
                <Empty description="暂无数据" />
              )}
            </Card>
          </Col>
          <Col span={12}>
            <Card title="日志类别分布" size="small">
              {logStatistics?.categoryDistribution ? (
                <div>
                  {Object.entries(logStatistics.categoryDistribution).map(([category, count]) => {
                    const total = logStatistics.totalLogs || 1
                    const numCount = typeof count === 'number' ? count : 0
                    const percentage = (numCount / total) * 100
                    return (
                      <div key={category} style={{ marginBottom: 12 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                          <Text>{category}</Text>
                          <Text>{String(numCount)}</Text>
                        </div>
                        <Progress 
                          percent={percentage} 
                          strokeColor="#1890ff"
                          showInfo={false}
                        />
                      </div>
                    )
                  })}
                </div>
              ) : (
                <Empty description="暂无数据" />
              )}
            </Card>
          </Col>
        </Row>

        {/* 时间分布 */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={12}>
            <Card title="时间分布（按小时）" size="small">
              {logStatistics?.timeDistribution && logStatistics.timeDistribution.length > 0 ? (
                <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                  {logStatistics.timeDistribution.map(({ hour, count }) => (
                    <div key={hour} style={{ marginBottom: 8 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text>{hour}:00</Text>
                        <Text>{count}</Text>
                      </div>
                      <Progress 
                        percent={(count / Math.max(...logStatistics.timeDistribution.map(t => t.count))) * 100} 
                        strokeColor="#52c41a"
                        showInfo={false}
                      />
                    </div>
                  ))}
                </div>
              ) : (
                <Empty description="暂无数据" />
              )}
            </Card>
          </Col>
          <Col span={12}>
            <Card title="错误趋势" size="small">
              {logStatistics?.errorTrend && logStatistics.errorTrend.length > 0 ? (
                <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                  {logStatistics.errorTrend.map(({ date, errorCount }) => (
                    <div key={date} style={{ marginBottom: 8 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text>{date}</Text>
                        <Text style={{ color: errorCount > 0 ? '#ff4d4f' : '#52c41a' }}>{errorCount}</Text>
                      </div>
                      <Progress 
                        percent={errorCount > 0 ? Math.min((errorCount / 100) * 100, 100) : 0}
                        strokeColor="#ff4d4f"
                        showInfo={false}
                      />
                    </div>
                  ))}
                </div>
              ) : (
                <Empty description="暂无数据" />
              )}
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  )
}

export default LogStatistics
