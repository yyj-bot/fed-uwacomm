/**
 * VM详情组件
 * 显示水下节点的详细信息，包括基本信息、系统信息、能力信息等
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { 
  Card, 
  Descriptions, 
  Button, 
  Space, 
  Tag, 
  Badge,
  Tooltip,
  message,
  Spin
} from 'antd'
import { 
  ArrowLeftOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { useVM } from '@/store/vm'
import type { VirtualMachine } from '@/api/vm'

interface VMDetailProps {
  vm: VirtualMachine
  onBack: () => void
}

const VMDetail: React.FC<VMDetailProps> = ({ vm, onBack }) => {
  const [refreshing, setRefreshing] = useState(false)

  const {
    fetchVMDetail,
    getVMStatus
  } = useVM()

  // 获取实时状态
  const vmStatus = getVMStatus(vm.vmId)

  // 初始化和刷新数据
  useEffect(() => {
    refreshVMData()
  }, [vm.vmId])

  const refreshVMData = async () => {
    setRefreshing(true)
    try {
      await fetchVMDetail(vm.vmId)
    } catch (error) {
      message.error('刷新数据失败')
    } finally {
      setRefreshing(false)
    }
  }


  // 渲染状态标签
  const renderStatus = () => {
    const currentStatus = vmStatus?.status || vm.status
    const statusConfig = {
      RUNNING: { color: 'green', text: '运行中' },
      STOPPED: { color: 'default', text: '已停止' },
      STARTING: { color: 'blue', text: '启动中' },
      STOPPING: { color: 'orange', text: '停止中' },
      ERROR: { color: 'red', text: '异常' },
      OFFLINE: { color: 'default', text: '离线' }
    }

    const config = statusConfig[currentStatus] || { color: 'default', text: currentStatus }
    return <Badge status={config.color as any} text={config.text} />
  }

  // 渲染连接状态
  const renderConnectionStatus = () => {
    const connectionStatus = vmStatus?.connectionStatus || vm.connectionStatus
    return (
      <Tag color={connectionStatus === 'CONNECTED' ? 'green' : 'red'}>
        {connectionStatus === 'CONNECTED' ? '已连接' : '未连接'}
      </Tag>
    )
  }

  return (
    <div className="vm-detail">
      <Spin spinning={refreshing}>
        {/* 头部操作栏 */}
        <Card className="vm-detail-header" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <Button 
                icon={<ArrowLeftOutlined />} 
                onClick={onBack}
                style={{ marginRight: 16 }}
              >
                返回列表
              </Button>
              <h3 style={{ display: 'inline', margin: 0 }}>{vm.name}</h3>
            </div>
            <Space>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={refreshVMData}
                loading={refreshing}
              >
                刷新
              </Button>
            </Space>
          </div>
        </Card>

        {/* 基本信息 */}
        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Descriptions column={2} bordered>
            <Descriptions.Item label="水下节点ID">{vm.vmId}</Descriptions.Item>
            <Descriptions.Item label="水下节点名称">{vm.name}</Descriptions.Item>
            <Descriptions.Item label="运行状态">{renderStatus()}</Descriptions.Item>
            <Descriptions.Item label="连接状态">{renderConnectionStatus()}</Descriptions.Item>
            <Descriptions.Item label="操作系统">{vm.osType}</Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {new Date(vm.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {new Date(vm.updatedAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="最后心跳">
              {vm.lastHeartbeat
                ? new Date(vm.lastHeartbeat).toLocaleString()
                : '无'}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 核心规格 */}
        {(vm.specs || vm.capabilities) && (
          <Card title="核心规格" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              {vm.specs?.dimensions && (
                <Descriptions.Item label="尺寸">{vm.specs.dimensions}</Descriptions.Item>
              )}
              {vm.specs?.airWeight && (
                <Descriptions.Item label="空气中重量">{vm.specs.airWeight}</Descriptions.Item>
              )}
              {vm.specs?.depthRating && (
                <Descriptions.Item label="深度等级">{vm.specs.depthRating}</Descriptions.Item>
              )}
              {vm.specs?.thrusters && (
                <Descriptions.Item label="推进系统">{vm.specs.thrusters}</Descriptions.Item>
              )}
              {vm.specs?.manipulator && (
                <Descriptions.Item label="机械手">{vm.specs.manipulator}</Descriptions.Item>
              )}
              {vm.specs?.gimbal && (
                <Descriptions.Item label="云台">{vm.specs.gimbal}</Descriptions.Item>
              )}
              {vm.specs?.camera && (
                <Descriptions.Item label="摄像系统">{vm.specs.camera}</Descriptions.Item>
              )}
              {vm.specs?.lights && (
                <Descriptions.Item label="水下灯">{vm.specs.lights}</Descriptions.Item>
              )}
              {vm.specs?.sensors && (
                <Descriptions.Item label="传感器">{vm.specs.sensors}</Descriptions.Item>
              )}
              {vm.specs?.payloadCapacity && (
                <Descriptions.Item label="负载能力">{vm.specs.payloadCapacity}</Descriptions.Item>
              )}
              {vm.specs?.powerSupply && (
                <Descriptions.Item label="供电方式">{vm.specs.powerSupply}</Descriptions.Item>
              )}
              {vm.specs?.maxPower && (
                <Descriptions.Item label="最大功率">{vm.specs.maxPower}</Descriptions.Item>
              )}
              {vm.specs?.tether && (
                <Descriptions.Item label="纤缆">
                  {[
                    vm.specs.tether.length,
                    vm.specs.tether.diameter,
                    vm.specs.tether.breakStrength && `破断力${vm.specs.tether.breakStrength}`
                  ].filter(Boolean).join('，')}
                </Descriptions.Item>
              )}
              {vm.capabilities?.supportedAlgorithms && (
                <Descriptions.Item label="支持算法">
                  <Space wrap>
                    {vm.capabilities.supportedAlgorithms.map(algo => (
                      <Tag key={algo} color="blue">{algo}</Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 运行资源 */}
        <Card title="运行资源" style={{ marginBottom: 16 }}>
          <Descriptions column={3} bordered>
            <Descriptions.Item label="CPU核心数">{vm.cpuCores}核</Descriptions.Item>
            <Descriptions.Item label="内存大小">{(vm.memoryMb / 1024).toFixed(1)}GB</Descriptions.Item>
            <Descriptions.Item label="磁盘大小">{vm.diskGb}GB</Descriptions.Item>
            {vm.systemInfo?.gpu && (
              <Descriptions.Item label="GPU">{vm.systemInfo.gpu}</Descriptions.Item>
            )}
          </Descriptions>
        </Card>
      </Spin>
    </div>
  )
}

export default VMDetail










