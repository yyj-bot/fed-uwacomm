/**
 * VM统计分析组件
 * 显示虚拟机的统计信息和分析图表
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { 
  Card, 
  Row, 
  Col, 
  Statistic, 
  Progress,
  Table,
  Tag,
  Space,
  Select,
  Button,
  Empty
} from 'antd'
import { 
  DesktopOutlined,
  CloudServerOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { Pie, Column, Line } from '@ant-design/plots'
import { useVM } from '@/store/vm'
import type { VirtualMachine } from '@/api/vm'
import type { ColumnsType } from 'antd/es/table'

const { Option } = Select

const VMStatistics: React.FC = () => {
  const [timeRange, setTimeRange] = useState<string>('24h')
  const [refreshing, setRefreshing] = useState(false)

  const {
    vmList,
    vmListLoading,
    fetchVMList,
    fetchAllVMStatus,
    getVMStatus,
    onlineVMCount,
    offlineVMCount,
    errorVMCount
  } = useVM()

  // 初始化数据
  useEffect(() => {
    refreshData()
  }, [])

  // 刷新数据
  const refreshData = async () => {
    setRefreshing(true)
    try {
      await Promise.all([
        fetchVMList(),
        fetchAllVMStatus()
      ])
    } catch (error) {
      console.error('刷新数据失败:', error)
    } finally {
      setRefreshing(false)
    }
  }

  // 计算统计数据
  const totalVMs = vmList.length
  const onlineCount = onlineVMCount()
  const offlineCount = offlineVMCount()
  const errorCount = errorVMCount()

  // 状态分布数据
  const statusDistribution = [
    { type: '在线', value: onlineCount, color: '#52c41a' },
    { type: '离线', value: offlineCount, color: '#fa8c16' },
    { type: '异常', value: errorCount, color: '#ff4d4f' }
  ].filter(item => item.value > 0)

  // 操作系统分布
  const osDistribution = vmList.reduce((acc, vm) => {
    const osType = vm.osType || '未知'
    acc[osType] = (acc[osType] || 0) + 1
    return acc
  }, {} as Record<string, number>)

  const osDistributionData = Object.entries(osDistribution).map(([type, value]) => ({
    type,
    value
  }))

  // 资源配置分布
  const resourceData = vmList.map(vm => ({
    name: vm.name,
    cpu: vm.cpuCores,
    memory: Math.round(vm.memoryMb / 1024), // 转换为GB
    disk: vm.diskGb
  }))

  // 性能指标表格列
  const performanceColumns: ColumnsType<VirtualMachine> = [
    {
      title: '虚拟机名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_, record: VirtualMachine) => {
        const status = getVMStatus(record.vmId)
        const currentStatus = status?.status || record.status
        
        const statusConfig = {
          RUNNING: { color: 'green', text: '运行中' },
          STOPPED: { color: 'default', text: '已停止' },
          STARTING: { color: 'blue', text: '启动中' },
          STOPPING: { color: 'orange', text: '停止中' },
          ERROR: { color: 'red', text: '异常' },
          OFFLINE: { color: 'default', text: '离线' }
        }

        const config = statusConfig[currentStatus] || { color: 'default', text: currentStatus }
        return <Tag color={config.color}>{config.text}</Tag>
      }
    },
    {
      title: 'CPU使用率',
      key: 'cpuUsage',
      width: 150,
      render: (_, record: VirtualMachine) => {
        const status = getVMStatus(record.vmId)
        const cpuUsage = status?.resourceUsage?.cpu
        
        if (cpuUsage === undefined) return '-'
        
        return (
          <div>
            <div>{cpuUsage.toFixed(1)}%</div>
            <Progress 
              percent={cpuUsage} 
              size="small" 
              status={cpuUsage > 80 ? 'exception' : cpuUsage > 60 ? 'active' : 'success'}
            />
          </div>
        )
      }
    },
    {
      title: '内存使用率',
      key: 'memoryUsage',
      width: 150,
      render: (_, record: VirtualMachine) => {
        const status = getVMStatus(record.vmId)
        const memoryUsage = status?.resourceUsage?.memory
        
        if (memoryUsage === undefined) return '-'
        
        return (
          <div>
            <div>{memoryUsage.toFixed(1)}%</div>
            <Progress 
              percent={memoryUsage} 
              size="small" 
              status={memoryUsage > 80 ? 'exception' : memoryUsage > 60 ? 'active' : 'success'}
            />
          </div>
        )
      }
    },
    {
      title: '磁盘使用率',
      key: 'diskUsage',
      width: 150,
      render: (_, record: VirtualMachine) => {
        const status = getVMStatus(record.vmId)
        const diskUsage = status?.resourceUsage?.disk
        
        if (diskUsage === undefined) return '-'
        
        return (
          <div>
            <div>{diskUsage.toFixed(1)}%</div>
            <Progress 
              percent={diskUsage} 
              size="small" 
              status={diskUsage > 80 ? 'exception' : diskUsage > 60 ? 'active' : 'success'}
            />
          </div>
        )
      }
    },
    {
      title: '运行时间',
      key: 'uptime',
      width: 120,
      render: (_, record: VirtualMachine) => {
        const status = getVMStatus(record.vmId)
        const uptime = status?.uptime
        
        if (!uptime) return '-'
        
        const hours = Math.floor(uptime / 3600)
        const minutes = Math.floor((uptime % 3600) / 60)
        
        return `${hours}h ${minutes}m`
      }
    }
  ]

  // 饼图配置
  const pieConfig = {
    data: statusDistribution,
    angleField: 'value',
    colorField: 'type',
    radius: 0.8,
    label: {
      type: 'outer',
      content: '{name} {percentage}'
    },
    interactions: [{ type: 'element-active' }],
    color: statusDistribution.map(item => item.color)
  }

  // 操作系统分布柱状图配置
  const columnConfig = {
    data: osDistributionData,
    xField: 'type',
    yField: 'value',
    color: '#1890ff',
    label: {
      position: 'top' as const,
      style: {
        fill: '#000000',
        opacity: 0.8
      }
    },
    meta: {
      type: { alias: '操作系统' },
      value: { alias: '数量' }
    }
  }

  // 资源配置对比图配置
  const resourceConfig = {
    data: resourceData.slice(0, 10), // 只显示前10个
    xField: 'name',
    yField: ['cpu', 'memory', 'disk'],
    geometryOptions: [
      {
        geometry: 'column',
        color: '#5B8FF9'
      },
      {
        geometry: 'line',
        color: '#5AD8A6'
      },
      {
        geometry: 'line',
        color: '#FF6B3B'
      }
    ],
    meta: {
      cpu: { alias: 'CPU(核)' },
      memory: { alias: '内存(GB)' },
      disk: { alias: '磁盘(GB)' }
    }
  }

  return (
    <div className="vm-statistics">
      {/* 头部控制栏 */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>虚拟机统计分析</h3>
          <Space>
            <Select
              value={timeRange}
              onChange={setTimeRange}
              style={{ width: 120 }}
            >
              <Option value="1h">最近1小时</Option>
              <Option value="24h">最近24小时</Option>
              <Option value="7d">最近7天</Option>
              <Option value="30d">最近30天</Option>
            </Select>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={refreshData}
              loading={refreshing}
            >
              刷新
            </Button>
          </Space>
        </div>
      </Card>

      {/* 概览统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="虚拟机总数"
              value={totalVMs}
              prefix={<DesktopOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="在线数量"
              value={onlineCount}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="离线数量"
              value={offlineCount}
              prefix={<CloudServerOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="异常数量"
              value={errorCount}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 图表分析 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card title="状态分布" loading={vmListLoading}>
            {statusDistribution.length > 0 ? (
              <Pie {...pieConfig} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
        <Col span={16}>
          <Card title="操作系统分布" loading={vmListLoading}>
            {osDistributionData.length > 0 ? (
              <Column {...columnConfig} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
      </Row>

      {/* 资源配置对比 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="资源配置对比（前10台）" loading={vmListLoading}>
            {resourceData.length > 0 ? (
              <Column {...resourceConfig} />
            ) : (
              <Empty description="暂无数据" />
            )}
          </Card>
        </Col>
      </Row>

      {/* 性能监控表格 */}
      <Card title="实时性能监控" loading={vmListLoading}>
        <Table
          columns={performanceColumns}
          dataSource={vmList}
          rowKey="vmId"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
          scroll={{ x: 1000 }}
          size="middle"
        />
      </Card>
    </div>
  )
}

export default VMStatistics










