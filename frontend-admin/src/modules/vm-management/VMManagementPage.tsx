/**
 * 水下机器人列表页面（普通用户）
 * 提供普通用户的水下机器人管理功能，包括：
 * - 查看水下机器人列表（接口4.1）
 * - 查看水下机器人详情（接口4.2）
 * - 水下机器人控制：启动（接口5.1）、停止（接口5.2）、重启（接口5.3）
 * - 水下机器人状态查询（接口6.1）
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import { Card, Button, message, Drawer } from 'antd'
import { useVM } from '@/store/vm'
import VMList from './components/VMList'
import VMDetail from './components/VMDetail'
import VMModels from './components/VMModels'
import type { VirtualMachine } from '@/api/vm'
import './VMManagementPage.css'
import './vm-common.css'

const VMManagementPage: React.FC = () => {
  const [selectedVM, setSelectedVM] = useState<VirtualMachine | null>(null)
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false)
  const [modelsDrawerVisible, setModelsDrawerVisible] = useState(false)
  
  const {
    vmList,
    vmListLoading,
    vmListError,
    fetchVMList,
    fetchAllVMStatus
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

  // 选择水下机器人 - 打开详情抽屉
  const handleSelectVM = (vm: VirtualMachine) => {
    setSelectedVM(vm)
    setDetailDrawerVisible(true)
  }

  // 查看机器人模型 - 打开模型抽屉
  const handleViewModels = (vm: VirtualMachine) => {
    setSelectedVM(vm)
    setModelsDrawerVisible(true)
  }

  // 关闭详情抽屉
  const handleCloseDetailDrawer = () => {
    setDetailDrawerVisible(false)
    setSelectedVM(null)
  }

  // 关闭模型抽屉
  const handleCloseModelsDrawer = () => {
    setModelsDrawerVisible(false)
    setSelectedVM(null)
  }

  // 渲染错误状态
  if (vmListError) {
    return (
      <div className="vm-management-page">
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
      <Card className="vm-content-card" bodyStyle={{ padding: 0 }}>
        {/* 只保留下潜机器人列表 */}
        <VMList 
          onSelectVM={handleSelectVM}
          onViewModels={handleViewModels}
        />
      </Card>

      {/* 水下机器人详情抽屉 */}
      <Drawer
        title={`水下机器人详情 - ${selectedVM?.name || ''}`}
        placement="right"
        width={720}
        onClose={handleCloseDetailDrawer}
        open={detailDrawerVisible}
        destroyOnClose
      >
        {selectedVM && (
          <VMDetail 
            vm={selectedVM}
            onBack={handleCloseDetailDrawer}
          />
        )}
      </Drawer>

      {/* 本地模型抽屉 */}
      <Drawer
        title={`本地模型 - ${selectedVM?.name || ''}`}
        placement="right"
        width={900}
        onClose={handleCloseModelsDrawer}
        open={modelsDrawerVisible}
        destroyOnClose
      >
        {selectedVM && (
          <VMModels 
            vm={selectedVM}
            onBack={handleCloseModelsDrawer}
          />
        )}
      </Drawer>
    </div>
  )
}

export default VMManagementPage
