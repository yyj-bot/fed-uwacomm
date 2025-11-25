/**
 * 管理员水下节点管理页面
 * 提供管理员专用的水下节点管理功能，包括：
 * - 查看所有水下节点（不受权限限制）
 * - 水下节点分配管理
 * - 用户水下节点管理
 * - 水下节点强制控制
 * - 未分配水下节点列表
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
  VMEditDrawer
} from './components'
import { AdminVMList, UnassignedVMList } from './components'

import './VMManagementPage.css'
import './vm-common.css'

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
  const [editDrawerVisible, setEditDrawerVisible] = useState(false)

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

  // 处理查看VM分配
  const handleViewAssignment = (vm: any) => {
    setSelectedVm(vm)
    setAssignmentDrawerVisible(true)
  }

  // 处理编辑VM
  const handleEdit = (vm: any) => {
    setSelectedVm(vm)
    setEditDrawerVisible(true)
  }

  // 处理关闭编辑抽屉
  const handleCloseEditDrawer = () => {
    setEditDrawerVisible(false)
    setSelectedVm(null)
  }

  // 处理编辑成功
  const handleEditSuccess = () => {
    fetchAdminVmList()
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
    // 延迟刷新数据，给状态变更一点时间
    // 注意：mock 延迟时间为 START:3s, STOP:2s, RESTART:5s, FORCE_STOP:0s
    setTimeout(() => {
      fetchAdminVmList()
    }, 5500)
  }

  return (
    <div className="vm-management-page">
      <Card 
        title={
          <Space>
            <DesktopOutlined />
            <span>管理员水下节点管理</span>
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
                <span>所有水下节点</span>
              </Space>
            } 
            key="all-vms"
          >
            <AdminVMList 
              vmList={adminVmList}
              loading={adminVmListLoading}
              onViewAssignment={handleViewAssignment}
              onForceControl={handleForceControl}
              onRefresh={fetchAdminVmList}
              onEdit={handleEdit}
            />
          </TabPane>

          <TabPane 
            tab={
              <Space>
                <DesktopOutlined />
                <span>未分配水下节点</span>
              </Space>
            } 
            key="unassigned-vms"
          >
            <UnassignedVMList 
              vmList={unassignedVms}
              loading={unassignedVmsLoading}
              onViewAssignment={handleViewAssignment}
              onRefresh={fetchUnassignedVmList}
            />
          </TabPane>

          <TabPane 
            tab={
              <Space>
                <UserOutlined />
                <span>用户水下节点管理</span>
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
        title={`水下节点分配管理 - ${selectedVm?.name || ''}`}
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
        title="用户水下节点管理"
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

      {/* 强制控制抽屉 */}
      <Drawer
        title={`强制控制水下节点 - ${selectedVm?.name || ''}`}
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

      {/* 编辑水下节点抽屉 */}
      <VMEditDrawer
        open={editDrawerVisible}
        vm={selectedVm}
        onClose={handleCloseEditDrawer}
        onSuccess={handleEditSuccess}
      />
    </div>
  )
}

export default AdminVMManagementPage

