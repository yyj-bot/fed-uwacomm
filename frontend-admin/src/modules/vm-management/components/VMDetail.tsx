/**
 * VM详情组件
 * 显示虚拟机的详细信息，包括基本信息、系统信息、能力信息等
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
  Progress, 
  Statistic,
  Row,
  Col,
  Badge,
  Tooltip,
  message,
  Spin
} from 'antd'
import { 
  ArrowLeftOutlined,
  ReloadOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  RedoOutlined,
  EditOutlined
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
    fetchVMStatus,
    startVM,
    stopVM,
    restartVM,
    getVMStatus,
    isVMOperating,
    canStartVM,
    canStopVM,
    canRestartVM
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
      await Promise.all([
        fetchVMDetail(vm.vmId),
        fetchVMStatus(vm.vmId)
      ])
    } catch (error) {
      message.error('刷新数据失败')
    } finally {
      setRefreshing(false)
    }
  }

  // VM操作处理
  const handleStartVM = async () => {
    try {
      await startVM(vm.vmId)
      message.success('虚拟机启动成功')
    } catch (error) {
      message.error(`启动失败: ${error instanceof Error ? error.message : '未知错误'}`)
    }
  }

  const handleStopVM = async () => {
    try {
      await stopVM(vm.vmId)
      message.success('虚拟机停止成功')
    } catch (error) {
      message.error(`停止失败: ${error instanceof Error ? error.message : '未知错误'}`)
    }
  }

  const handleRestartVM = async () => {
    try {
      await restartVM(vm.vmId)
      message.success('虚拟机重启成功')
    } catch (error) {
      message.error(`重启失败: ${error instanceof Error ? error.message : '未知错误'}`)
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

  // 渲染资源使用情况
  const renderResourceUsage = () => {
    if (!vmStatus?.resourceUsage) {
      return <span>暂无数据</span>
    }

    const { cpu, memory, disk, gpu } = vmStatus.resourceUsage

    return (
      <Row gutter={16}>
        <Col span={6}>
          <Statistic
            title="CPU使用率"
            value={cpu}
            suffix="%"
            valueStyle={{ color: cpu > 80 ? '#ff4d4f' : cpu > 60 ? '#fa8c16' : '#3f8600' }}
          />
          <Progress percent={cpu} size="small" />
        </Col>
        <Col span={6}>
          <Statistic
            title="内存使用率"
            value={memory}
            suffix="%"
            valueStyle={{ color: memory > 80 ? '#ff4d4f' : memory > 60 ? '#fa8c16' : '#3f8600' }}
          />
          <Progress percent={memory} size="small" />
        </Col>
        <Col span={6}>
          <Statistic
            title="磁盘使用率"
            value={disk}
            suffix="%"
            valueStyle={{ color: disk > 80 ? '#ff4d4f' : disk > 60 ? '#fa8c16' : '#3f8600' }}
          />
          <Progress percent={disk} size="small" />
        </Col>
        {gpu !== undefined && (
          <Col span={6}>
            <Statistic
              title="GPU使用率"
              value={gpu}
              suffix="%"
              valueStyle={{ color: gpu > 80 ? '#ff4d4f' : gpu > 60 ? '#fa8c16' : '#3f8600' }}
            />
            <Progress percent={gpu} size="small" />
          </Col>
        )}
      </Row>
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
              
              {canStartVM(vm) && (
                <Button 
                  type="primary"
                  icon={<PlayCircleOutlined />} 
                  onClick={handleStartVM}
                  loading={isVMOperating(vm.vmId, 'start')}
                  style={{ background: '#52c41a', borderColor: '#52c41a' }}
                >
                  启动
                </Button>
              )}
              
              {canStopVM(vm) && (
                <Button 
                  icon={<PauseCircleOutlined />} 
                  onClick={handleStopVM}
                  loading={isVMOperating(vm.vmId, 'stop')}
                  style={{ color: '#fa8c16', borderColor: '#fa8c16' }}
                >
                  停止
                </Button>
              )}
              
              {canRestartVM(vm) && (
                <Button 
                  icon={<RedoOutlined />} 
                  onClick={handleRestartVM}
                  loading={isVMOperating(vm.vmId, 'restart')}
                >
                  重启
                </Button>
              )}
            </Space>
          </div>
        </Card>

        {/* 基本信息 */}
        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Descriptions column={2} bordered>
            <Descriptions.Item label="虚拟机ID">{vm.vmId}</Descriptions.Item>
            <Descriptions.Item label="虚拟机名称">{vm.name}</Descriptions.Item>
            <Descriptions.Item label="IP地址">{vm.ipAddress}</Descriptions.Item>
            <Descriptions.Item label="端口">{vm.port}</Descriptions.Item>
            <Descriptions.Item label="运行状态">{renderStatus()}</Descriptions.Item>
            <Descriptions.Item label="连接状态">{renderConnectionStatus()}</Descriptions.Item>
            <Descriptions.Item label="操作系统">{vm.osType}</Descriptions.Item>
            <Descriptions.Item label="WebSocket会话">
              {vmStatus?.wsSessionId || vm.wsSessionId || '无'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">
              {new Date(vm.createdAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="更新时间">
              {new Date(vm.updatedAt).toLocaleString()}
            </Descriptions.Item>
            <Descriptions.Item label="最后心跳">
              {vmStatus?.lastHeartbeat || vm.lastHeartbeat 
                ? new Date(vmStatus?.lastHeartbeat || vm.lastHeartbeat!).toLocaleString()
                : '无'
              }
            </Descriptions.Item>
            <Descriptions.Item label="运行时间">
              {vmStatus?.uptime ? `${Math.floor(vmStatus.uptime / 3600)}小时${Math.floor((vmStatus.uptime % 3600) / 60)}分钟` : '无'}
            </Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 硬件配置 */}
        <Card title="硬件配置" style={{ marginBottom: 16 }}>
          <Descriptions column={3} bordered>
            <Descriptions.Item label="CPU核心数">{vm.cpuCores}核</Descriptions.Item>
            <Descriptions.Item label="内存大小">{(vm.memoryMb / 1024).toFixed(1)}GB</Descriptions.Item>
            <Descriptions.Item label="磁盘大小">{vm.diskGb}GB</Descriptions.Item>
          </Descriptions>
        </Card>

        {/* 资源使用情况 */}
        <Card title="资源使用情况" style={{ marginBottom: 16 }}>
          {renderResourceUsage()}
        </Card>

        {/* 系统信息 */}
        {vm.systemInfo && (
          <Card title="系统信息" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              {vm.systemInfo.os && (
                <Descriptions.Item label="操作系统">{vm.systemInfo.os}</Descriptions.Item>
              )}
              {vm.systemInfo.kernel && (
                <Descriptions.Item label="内核版本">{vm.systemInfo.kernel}</Descriptions.Item>
              )}
              {vm.systemInfo.python && (
                <Descriptions.Item label="Python版本">{vm.systemInfo.python}</Descriptions.Item>
              )}
              {vm.systemInfo.gpu && (
                <Descriptions.Item label="GPU">{vm.systemInfo.gpu}</Descriptions.Item>
              )}
              {vm.systemInfo.cuda && (
                <Descriptions.Item label="CUDA版本">{vm.systemInfo.cuda}</Descriptions.Item>
              )}
              {vm.systemInfo.cudnn && (
                <Descriptions.Item label="cuDNN版本">{vm.systemInfo.cudnn}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 能力信息 */}
        {vm.capabilities && (
          <Card title="能力信息" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              {vm.capabilities.supportedAlgorithms && (
                <Descriptions.Item label="支持的算法">
                  <Space wrap>
                    {vm.capabilities.supportedAlgorithms.map(algo => (
                      <Tag key={algo} color="blue">{algo}</Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}
              {vm.capabilities.maxBatchSize && (
                <Descriptions.Item label="最大批次大小">{vm.capabilities.maxBatchSize}</Descriptions.Item>
              )}
              {vm.capabilities.maxMemoryUsage && (
                <Descriptions.Item label="最大内存使用">{vm.capabilities.maxMemoryUsage}MB</Descriptions.Item>
              )}
              {vm.capabilities.gpuMemory && (
                <Descriptions.Item label="GPU内存">{vm.capabilities.gpuMemory}MB</Descriptions.Item>
              )}
              {vm.capabilities.networkSpeed && (
                <Descriptions.Item label="网络速度">{vm.capabilities.networkSpeed}Mbps</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 网络配置 */}
        {vm.networkConfig && (
          <Card title="网络配置" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              {vm.networkConfig.uploadSpeed && (
                <Descriptions.Item label="上传速度">{vm.networkConfig.uploadSpeed}KB/s</Descriptions.Item>
              )}
              {vm.networkConfig.downloadSpeed && (
                <Descriptions.Item label="下载速度">{vm.networkConfig.downloadSpeed}KB/s</Descriptions.Item>
              )}
              {vm.networkConfig.latency && (
                <Descriptions.Item label="延迟">{vm.networkConfig.latency}ms</Descriptions.Item>
              )}
              {vm.networkConfig.bandwidth && (
                <Descriptions.Item label="带宽">{vm.networkConfig.bandwidth}Mbps</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 网络状态 */}
        {vmStatus?.network && (
          <Card title="网络状态" style={{ marginBottom: 16 }}>
            <Descriptions column={2} bordered>
              <Descriptions.Item label="IP地址">{vmStatus.network.ipAddress}</Descriptions.Item>
              <Descriptions.Item label="端口">{vmStatus.network.port}</Descriptions.Item>
              {vmStatus.network.macAddress && (
                <Descriptions.Item label="MAC地址">{vmStatus.network.macAddress}</Descriptions.Item>
              )}
              {vmStatus.network.uploadSpeed && (
                <Descriptions.Item label="实时上传速度">{vmStatus.network.uploadSpeed}KB/s</Descriptions.Item>
              )}
              {vmStatus.network.downloadSpeed && (
                <Descriptions.Item label="实时下载速度">{vmStatus.network.downloadSpeed}KB/s</Descriptions.Item>
              )}
              {vmStatus.network.latency && (
                <Descriptions.Item label="实时延迟">{vmStatus.network.latency}ms</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}

        {/* 进程信息 */}
        {vmStatus?.processes && (
          <Card title="进程信息" style={{ marginBottom: 16 }}>
            <Row gutter={16}>
              <Col span={6}>
                <Statistic title="总进程数" value={vmStatus.processes.total} />
              </Col>
              <Col span={6}>
                <Statistic title="活跃进程" value={vmStatus.processes.active} />
              </Col>
              <Col span={6}>
                <Statistic title="系统进程" value={vmStatus.processes.system} />
              </Col>
              <Col span={6}>
                <Statistic title="用户进程" value={vmStatus.processes.user} />
              </Col>
            </Row>
          </Card>
        )}

        {/* 元数据信息 */}
        {vm.metadata && (
          <Card title="元数据信息">
            <Descriptions column={2} bordered>
              {vm.metadata.description && (
                <Descriptions.Item label="描述">{vm.metadata.description}</Descriptions.Item>
              )}
              {vm.metadata.location && (
                <Descriptions.Item label="位置">{vm.metadata.location}</Descriptions.Item>
              )}
              {vm.metadata.owner && (
                <Descriptions.Item label="负责人">{vm.metadata.owner}</Descriptions.Item>
              )}
              {vm.metadata.department && (
                <Descriptions.Item label="部门">{vm.metadata.department}</Descriptions.Item>
              )}
              {vm.metadata.tags && (
                <Descriptions.Item label="标签">
                  <Space wrap>
                    {vm.metadata.tags.map(tag => (
                      <Tag key={tag} color="geekblue">{tag}</Tag>
                    ))}
                  </Space>
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}
      </Spin>
    </div>
  )
}

export default VMDetail










