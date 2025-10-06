/**
 * VM管理主页面
 * 提供虚拟机管理的统一入口，包括列表、详情、控制等功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { Tabs, Card, Button, Space, message, Spin } from 'antd'
import { 
  DesktopOutlined, 
  DatabaseOutlined, 
  BarChartOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons'
import { useVM } from '@/store/vm'
import VMList from './components/VMList'
import VMDetail from './components/VMDetail'
import VMModels from './components/VMModels'
import VMStatistics from './components/VMStatistics'
import type { VirtualMachine } from '@/api/vm'
import './VMManagementPage.css'

const { TabPane } = Tabs

const VMManagementPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('list')
  const [selectedVM, setSelectedVM] = useState<VirtualMachine | null>(null)
  
  const {
    vmList,
    vmListLoading,
    vmListError,
    fetchVMList,
    fetchAllVMStatus,
    onlineVMCount,
    offlineVMCount,
    errorVMCount
  } = useVM()

  // 初始化数据
  useEffect(() => {
    handleRefresh()
  }, [])

  // 刷新数据
  const handleRefresh = async () => {
    try {
      await fetchVMList()
      await fetchAllVMStatus()
      message.success('数据刷新成功')
    } catch (error) {
      message.error('数据刷新失败')
    }
  }

  // 选择虚拟机
  const handleSelectVM = (vm: VirtualMachine) => {
    setSelectedVM(vm)
    setActiveTab('detail')
  }

  // 查看VM模型
  const handleViewModels = (vm: VirtualMachine) => {
    setSelectedVM(vm)
    setActiveTab('models')
  }

  // 渲染页面头部
  const renderHeader = () => (
    <Card className="vm-header-card">
      <div className="vm-header">
        <div className="vm-header-left">
          <h2>虚拟机管理</h2>
          <div className="vm-stats">
            <Space size="large">
              <div className="stat-item">
                <span className="stat-label">总数:</span>
                <span className="stat-value">{vmList.length}</span>
              </div>
              <div className="stat-item online">
                <span className="stat-label">在线:</span>
                <span className="stat-value">{onlineVMCount()}</span>
              </div>
              <div className="stat-item offline">
                <span className="stat-label">离线:</span>
                <span className="stat-value">{offlineVMCount()}</span>
              </div>
              <div className="stat-item error">
                <span className="stat-label">异常:</span>
                <span className="stat-value">{errorVMCount()}</span>
              </div>
            </Space>
          </div>
        </div>
        <div className="vm-header-right">
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={handleRefresh}
              loading={vmListLoading}
            >
              刷新
            </Button>
          </Space>
        </div>
      </div>
    </Card>
  )

  // 渲染错误状态
  if (vmListError) {
    return (
      <div className="vm-management-page">
        {renderHeader()}
        <Card>
          <div className="error-state">
            <p>加载失败: {vmListError}</p>
            <Button type="primary" onClick={handleRefresh}>
              重试
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  return (
    <div className="vm-management-page">
      {renderHeader()}
      
      <Card className="vm-content-card">
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          className="vm-tabs"
        >
          <TabPane 
            tab={
              <span>
                <DesktopOutlined />
                虚拟机列表
              </span>
            } 
            key="list"
          >
            <VMList 
              onSelectVM={handleSelectVM}
              onViewModels={handleViewModels}
            />
          </TabPane>
          
          <TabPane 
            tab={
              <span>
                <DatabaseOutlined />
                虚拟机详情
              </span>
            } 
            key="detail"
            disabled={!selectedVM}
          >
            {selectedVM && (
              <VMDetail 
                vm={selectedVM}
                onBack={() => setActiveTab('list')}
              />
            )}
          </TabPane>
          
          <TabPane 
            tab={
              <span>
                <BarChartOutlined />
                本地模型
              </span>
            } 
            key="models"
            disabled={!selectedVM}
          >
            {selectedVM && (
              <VMModels 
                vm={selectedVM}
                onBack={() => setActiveTab('list')}
              />
            )}
          </TabPane>
          
          <TabPane 
            tab={
              <span>
                <BarChartOutlined />
                统计分析
              </span>
            } 
            key="statistics"
          >
            <VMStatistics />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  )
}

export default VMManagementPage
