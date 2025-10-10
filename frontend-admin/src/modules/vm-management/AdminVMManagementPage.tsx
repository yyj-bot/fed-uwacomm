/**
 * 管理员虚拟机管理页面
 * 提供管理员专用的虚拟机管理功能，包括：
 * - 查看所有虚拟机（不受权限限制）
 * - 虚拟机分配管理
 * - 用户虚拟机管理
 * - 虚拟机强制控制
 * - 未分配虚拟机列表
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useEffect, useState } from 'react'
import { Card, Tabs, Button, message, Space, Drawer } from 'antd'
import {
  ReloadOutlined,
  TeamOutlined,
  DesktopOutlined,
  UserOutlined
} from '@ant-design/icons'

import { useAdminVM } from '@/store/vm/useAdminVM'
import { 
  VMAssignmentOverview, 
  VMAssignmentManager,
  UserVmList,
  UserVmManagement,
  VMForceControl,
  VMDetail
} from './components'
import { AdminVMList, UnassignedVMList } from './components'

import './VMManagementPage.css'

const { TabPane } = Tabs

const AdminVMManagementPage: React.FC = () => {
  const {
    adminVmList,
    adminVmListLoading,
    unassignedVms,
    unassignedVmsLoading,
    assignmentOverview,
    fetchAdminVmList,
    fetchUnassignedVmList,
    fetchVmAssignmentOverview
  } = useAdminVM()

  const [activeTab, setActiveTab] = useState('all-vms')
  const [assignmentDrawerVisible, setAssignmentDrawerVisible] = useState(false)
  const [selectedVm, setSelectedVm] = useState<any>(null)
  const [userVmDrawerVisible, setUserVmDrawerVisible] = useState(false)
  const [selectedUserId, setSelectedUserId] = useState<string>('')
  const [forceControlDrawerVisible, setForceControlDrawerVisible] = useState(false)
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false)
  const [selectedVmForDetail, setSelectedVmForDetail] = useState<any>(null)

  // 初始化数据
  useEffect(() => {
    handleRefresh()
  }, [])

  // 刷新数据
  const handleRefresh = async () => {
    try {
      await Promise.all([
        fetchAdminVmList(),
        fetchUnassignedVmList(),
        fetchVmAssignmentOverview()
      ])
      message.success('数据刷新成功')
    } catch (error) {
      message.error('数据刷新失败')
    }
  }

  // 处理查看VM详情
  const handleViewDetail = (vm: any) => {
    setSelectedVmForDetail(vm)
    setDetailDrawerVisible(true)
  }

  // 处理关闭详情抽屉
  const handleCloseDetailDrawer = () => {
    setDetailDrawerVisible(false)
    setSelectedVmForDetail(null)
  }

  // 处理查看VM分配
  const handleViewAssignment = (vm: any) => {
    setSelectedVm(vm)
    setAssignmentDrawerVisible(true)
  }

  // 处理关闭分配抽屉
  const handleCloseAssignmentDrawer = () => {
    setAssignmentDrawerVisible(false)
    setSelectedVm(null)
    // 刷新数据
    fetchAdminVmList()
    fetchVmAssignmentOverview()
  }

  // 处理查看用户VM
  const handleViewUserVm = (userId: string) => {
    setSelectedUserId(userId)
    setUserVmDrawerVisible(true)
  }

  // 处理关闭用户VM抽屉
  const handleCloseUserVmDrawer = () => {
    setUserVmDrawerVisible(false)
    setSelectedUserId('')
    // 刷新数据
    fetchAdminVmList()
    fetchVmAssignmentOverview()
  }

  // 处理强制控制VM
  const handleForceControl = (vm: any) => {
    setSelectedVm(vm)
    setForceControlDrawerVisible(true)
  }

  // 处理关闭强制控制抽屉
  const handleCloseForceControlDrawer = () => {
    setForceControlDrawerVisible(false)
    setSelectedVm(null)
    // 刷新数据
    fetchAdminVmList()
  }

  return (
    <div className="vm-management-page">
      <Card 
        title={
          <Space>
            <DesktopOutlined />
            <span>管理员虚拟机管理</span>
          </Space>
        }
        extra={
          <Space>
            <Button 
              type="primary" 
              icon={<ReloadOutlined />} 
              onClick={handleRefresh}
              loading={adminVmListLoading || unassignedVmsLoading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Tabs 
          activeKey={activeTab} 
          onChange={setActiveTab}
          type="card"
        >
          <TabPane 
            tab={
              <Space>
                <DesktopOutlined />
                <span>所有虚拟机</span>
              </Space>
            } 
            key="all-vms"
          >
            <AdminVMList 
              vmList={adminVmList}
              loading={adminVmListLoading}
              onViewDetail={handleViewDetail}
              onViewAssignment={handleViewAssignment}
              onForceControl={handleForceControl}
              onRefresh={fetchAdminVmList}
            />
          </TabPane>

          <TabPane 
            tab={
              <Space>
                <DesktopOutlined />
                <span>未分配虚拟机</span>
              </Space>
            } 
            key="unassigned-vms"
          >
            <UnassignedVMList 
              vmList={unassignedVms}
              loading={unassignedVmsLoading}
              onViewDetail={handleViewDetail}
              onViewAssignment={handleViewAssignment}
              onRefresh={fetchUnassignedVmList}
            />
          </TabPane>

          <TabPane 
            tab={
              <Space>
                <UserOutlined />
                <span>用户VM管理</span>
              </Space>
            } 
            key="user-vm-management"
          >
            <UserVmManagement />
          </TabPane>

          <TabPane 
            tab={
              <Space>
                <TeamOutlined />
                <span>分配概况</span>
              </Space>
            } 
            key="assignment-overview"
          >
            <VMAssignmentOverview />
          </TabPane>
        </Tabs>
      </Card>

      {/* VM分配管理抽屉 */}
      <Drawer
        title={`虚拟机分配管理 - ${selectedVm?.name || ''}`}
        placement="right"
        width={720}
        open={assignmentDrawerVisible}
        onClose={handleCloseAssignmentDrawer}
        destroyOnClose
      >
        {selectedVm && (
          <VMAssignmentManager 
            vmId={selectedVm.vmId}
            vmName={selectedVm.name}
          />
        )}
      </Drawer>

      {/* 用户VM管理抽屉 */}
      <Drawer
        title="用户虚拟机管理"
        placement="right"
        width={720}
        open={userVmDrawerVisible}
        onClose={handleCloseUserVmDrawer}
        destroyOnClose
      >
        {selectedUserId && (
          <UserVmList 
            userId={selectedUserId}
            username="用户"
          />
        )}
      </Drawer>

      {/* 虚拟机详情抽屉 */}
      <Drawer
        title="虚拟机详情"
        placement="right"
        width={900}
        open={detailDrawerVisible}
        onClose={handleCloseDetailDrawer}
        destroyOnClose
      >
        {selectedVmForDetail && (
          <VMDetail 
            vm={selectedVmForDetail}
            onBack={handleCloseDetailDrawer}
          />
        )}
      </Drawer>

      {/* 强制控制抽屉 */}
      <Drawer
        title={`强制控制虚拟机 - ${selectedVm?.name || ''}`}
        placement="right"
        width={520}
        open={forceControlDrawerVisible}
        onClose={handleCloseForceControlDrawer}
        destroyOnClose
      >
        {selectedVm && (
          <VMForceControl 
            vmId={selectedVm.vmId}
            vmName={selectedVm.name}
            currentStatus={selectedVm.status}
            onSuccess={handleCloseForceControlDrawer}
          />
        )}
      </Drawer>
    </div>
  )
}

export default AdminVMManagementPage

