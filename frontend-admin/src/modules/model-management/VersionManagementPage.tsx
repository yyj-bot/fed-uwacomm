/**
 * 模型版本管理页面
 * 覆盖model-version-api-reference.md的版本管理相关接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */

import React, { useState, useEffect } from 'react'
import {
  Card,
  Button,
  Table,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Upload,
  message,
  Tag,
  Descriptions,
  Row,
  Col,
  Tabs,
  InputNumber,
  Popconfirm,
  Tooltip
} from 'antd'
import {
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ReloadOutlined,
  CloudUploadOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useModel } from '@/store/model-version/useModelVersionStore'
import type {
  ModelVersionListParams,
  ModelVersionDetail
} from '@/services'

const { TextArea } = Input
const { Option } = Select

const VersionManagementPage: React.FC = () => {
  const {
    // 状态
    modelList,
    modelListTotal,
    modelListLoading,
    modelListError,
    currentModel,
    currentModelLoading,
    taskModels,
    pagination,
    
    // 操作方法
    fetchModelList,
    fetchModelDetail,
    fetchTaskModels,
    downloadModel,
    downloadModelBatch,
    deleteModel,
    deleteModelBatch,
    setPagination,
    
    // 工具方法
    getTaskModels,
    canDeleteModel
  } = useModel()

  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [taskModelsModalVisible, setTaskModelsModalVisible] = useState(false)
  
  const [selectedModelId, setSelectedModelId] = useState<string>('')
  const [selectedTaskId, setSelectedTaskId] = useState<string>('')
  const [fileList, setFileList] = useState<any[]>([])
  const [batchFileList, setBatchFileList] = useState<any[]>([])
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  
  const [searchForm] = Form.useForm()

  // 加载模型列表
  useEffect(() => {
    fetchModelList()
  }, [])

  // 模型状态颜色
  const getStatusColor = (status: string) => {
    const colorMap: Record<string, string> = {
      'UPLOADING': 'processing',
      'UPLOADED': 'success',
      'VALIDATING': 'processing',
      'VALIDATED': 'success',
      'DEPLOYED': 'success',
      'DEPRECATED': 'warning',
      'FAILED': 'error'
    }
    return colorMap[status] || 'default'
  }

  // 模型列表表格列定义
  const columns: ColumnsType<any> = [
    {
      title: '模型ID',
      dataIndex: 'modelId',
      key: 'modelId',
      width: 200,
      ellipsis: true,
      fixed: 'left'
    },
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
      ellipsis: true
    },
    {
      title: '轮次',
      dataIndex: 'roundNumber',
      key: 'roundNumber',
      width: 80,
      sorter: true
    },
    {
      title: '准确率',
      dataIndex: 'accuracy',
      key: 'accuracy',
      width: 100,
      render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-'
    },
    {
      title: '损失',
      dataIndex: 'loss',
      key: 'loss',
      width: 100,
      render: (value: number) => value ? value.toFixed(6) : '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={getStatusColor(status)}>{status}</Tag>
      )
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (time: string) => new Date(time).toLocaleString()
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 220,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => handleViewDetail(record.modelId)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record.modelId)}
          >
            下载
          </Button>
          <Popconfirm
            title="确定要删除此模型版本吗？"
            onConfirm={() => handleDelete(record.modelId)}
            okText="确定"
            cancelText="取消"
            disabled={!canDeleteModel(record)}
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={!canDeleteModel(record)}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 表格行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => {
      setSelectedRowKeys(keys)
    }
  }

  // 处理搜索
  const handleSearch = async () => {
    const values = await searchForm.validateFields()
    const params: ModelVersionListParams = {
      taskId: values.taskId || undefined,
      roundNumber: values.roundNumber || undefined,
      status: values.status || undefined,
      page: 1,
      size: pagination.size,
      sort: values.sort || 'createdAt',
      order: values.order || 'desc'
    }
    await fetchModelList(params)
  }

  // 重置搜索
  const handleResetSearch = () => {
    searchForm.resetFields()
    fetchModelList({ page: 1, size: 10 })
  }


  // 查看详情
  const handleViewDetail = async (modelId: string) => {
    setSelectedModelId(modelId)
    setDetailModalVisible(true)
    await fetchModelDetail(modelId)
  }

  // 下载模型
  const handleDownload = async (modelId: string, format: 'original' | 'onnx' = 'original', compressed = true) => {
    try {
      const result = await downloadModel(modelId, { format, compressed })
      if (result.success) {
        message.success('模型下载成功')
      } else {
        message.error(result.error || '模型下载失败')
      }
    } catch (error) {
      message.error('模型下载失败')
    }
  }

  // 批量下载
  const handleBatchDownload = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要下载的模型')
      return
    }

    try {
      const result = await downloadModelBatch(selectedRowKeys as string[])
      if (result.success) {
        message.success('批量下载成功')
        setSelectedRowKeys([])
      } else {
        message.error(result.error || '批量下载失败')
      }
    } catch (error) {
      message.error('批量下载失败')
    }
  }

  // 删除模型
  const handleDelete = async (modelId: string) => {
    try {
      const result = await deleteModel(modelId, { force: false, deleteFile: true })
      if (result.success) {
        message.success('模型删除成功')
        await fetchModelList()
      } else {
        message.error(result.error || '模型删除失败')
      }
    } catch (error) {
      message.error('模型删除失败')
    }
  }

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的模型')
      return
    }

    Modal.confirm({
      title: '批量删除确认',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个模型版本吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          const result = await deleteModelBatch(selectedRowKeys as string[])
          if (result.success) {
            message.success('批量删除成功')
            setSelectedRowKeys([])
            await fetchModelList()
          } else {
            message.error(result.error || '批量删除失败')
          }
        } catch (error) {
          message.error('批量删除失败')
        }
      }
    })
  }

  // 查看任务模型版本
  const handleViewTaskModels = async (taskId: string) => {
    setSelectedTaskId(taskId)
    setTaskModelsModalVisible(true)
    await fetchTaskModels(taskId)
  }

  // 分页变化处理
  const handleTableChange = (paginationConfig: any, filters: any, sorter: any) => {
    setPagination(paginationConfig.current, paginationConfig.pageSize)
    
    const params: ModelVersionListParams = {
      page: paginationConfig.current,
      size: paginationConfig.pageSize,
      sort: sorter.field || 'createdAt',
      order: sorter.order === 'ascend' ? 'asc' : 'desc'
    }
    
    fetchModelList(params)
  }

  const currentTaskModels = selectedTaskId ? getTaskModels(selectedTaskId) : null

  return (
    <div style={{ padding: '24px' }}>
      <Card
        title="模型版本管理"
        extra={
          <Space>
            <Button
              icon={<DownloadOutlined />}
              onClick={handleBatchDownload}
              disabled={selectedRowKeys.length === 0}
            >
              批量下载
            </Button>
            <Button
              danger
              icon={<DeleteOutlined />}
              onClick={handleBatchDelete}
              disabled={selectedRowKeys.length === 0}
            >
              批量删除
            </Button>
          </Space>
        }
      >
        {/* 搜索表单 */}
        <Form
          form={searchForm}
          layout="inline"
          style={{ marginBottom: 16 }}
          onFinish={handleSearch}
        >
          <Form.Item name="taskId" label="任务ID">
            <Input placeholder="任务ID" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item name="roundNumber" label="轮次">
            <InputNumber placeholder="轮次" style={{ width: 120 }} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select placeholder="选择状态" style={{ width: 150 }} allowClear>
              <Option value="UPLOADING">上传中</Option>
              <Option value="UPLOADED">已上传</Option>
              <Option value="VALIDATING">验证中</Option>
              <Option value="VALIDATED">已验证</Option>
              <Option value="DEPLOYED">已部署</Option>
              <Option value="DEPRECATED">已废弃</Option>
              <Option value="FAILED">失败</Option>
            </Select>
          </Form.Item>
          <Form.Item name="sort" label="排序">
            <Select placeholder="排序字段" style={{ width: 150 }} allowClear>
              <Option value="createdAt">创建时间</Option>
              <Option value="roundNumber">轮次</Option>
              <Option value="accuracy">准确率</Option>
              <Option value="loss">损失</Option>
            </Select>
          </Form.Item>
          <Form.Item name="order">
            <Select placeholder="排序方向" style={{ width: 100 }} allowClear>
              <Option value="desc">降序</Option>
              <Option value="asc">升序</Option>
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<FileTextOutlined />}>
                搜索
              </Button>
              <Button onClick={handleResetSearch} icon={<ReloadOutlined />}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {/* 模型列表表格 */}
        <Table
          rowSelection={rowSelection}
          columns={columns}
          dataSource={modelList}
          loading={modelListLoading}
          rowKey="modelId"
          scroll={{ x: 1600 }}
          pagination={{
            current: pagination.page,
            pageSize: pagination.size,
            total: modelListTotal,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`
          }}
          onChange={handleTableChange}
        />
      </Card>


      {/* 模型详情对话框 */}
      <Modal
        title="模型版本详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={800}
      >
        {currentModel && (
          <Descriptions bordered column={2}>
            <Descriptions.Item label="模型ID" span={2}>
              {currentModel.modelId}
            </Descriptions.Item>
            <Descriptions.Item label="任务ID" span={2}>
              {currentModel.taskId}
            </Descriptions.Item>
            <Descriptions.Item label="轮次">
              {currentModel.roundNumber}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={getStatusColor(currentModel.status)}>{currentModel.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="准确率">
              {currentModel?.metrics?.accuracy ? (currentModel.metrics.accuracy * 100).toFixed(2) + '%' : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="损失">
              {currentModel?.metrics?.loss ? currentModel.metrics.loss.toFixed(6) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间" span={2}>
              {currentModel?.createdAt ? new Date(currentModel.createdAt).toLocaleString() : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="聚合完成时间" span={2}>
              {currentModel?.aggregatedAt ? new Date(currentModel.aggregatedAt).toLocaleString() : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="模型数据" span={2}>
              <pre style={{ margin: 0, maxHeight: '300px', overflow: 'auto' }}>
                {currentModel?.modelJson ? JSON.stringify(currentModel.modelJson, null, 2) : '-'}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 任务模型版本对话框 */}
      <Modal
        title={`任务模型版本 - ${selectedTaskId}`}
        open={taskModelsModalVisible}
        onCancel={() => setTaskModelsModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setTaskModelsModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={1000}
      >
        {currentTaskModels && (
          <>
            <Descriptions bordered column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="任务ID">
                {currentTaskModels.taskId}
              </Descriptions.Item>
              <Descriptions.Item label="任务名称">
                {currentTaskModels.taskName}
              </Descriptions.Item>
              <Descriptions.Item label="模型总数" span={2}>
                {currentTaskModels.totalModels}
              </Descriptions.Item>
            </Descriptions>

            <Table
              dataSource={currentTaskModels.versions}
              rowKey="modelId"
              size="small"
              scroll={{ y: 400 }}
              pagination={false}
              columns={[
                {
                  title: '轮次',
                  dataIndex: 'roundNumber',
                  key: 'roundNumber',
                  width: 80
                },
                {
                  title: '准确率',
                  dataIndex: 'accuracy',
                  key: 'accuracy',
                  width: 100,
                  render: (value: number) => value ? (value * 100).toFixed(2) + '%' : '-'
                },
                {
                  title: '损失',
                  dataIndex: 'loss',
                  key: 'loss',
                  width: 100,
                  render: (value: number) => value ? value.toFixed(6) : '-'
                },
                {
                  title: '状态',
                  dataIndex: 'status',
                  key: 'status',
                  width: 100,
                  render: (status: string) => (
                    <Tag color={getStatusColor(status)}>{status}</Tag>
                  )
                },
                {
                  title: '创建时间',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  render: (time: string) => new Date(time).toLocaleString()
                }
              ]}
            />
          </>
        )}
      </Modal>
    </div>
  )
}

export default VersionManagementPage

