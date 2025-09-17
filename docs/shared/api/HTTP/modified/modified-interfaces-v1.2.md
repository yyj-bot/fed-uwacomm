# API 接口变更记录 - v1.2

## 概述

本文档记录了水声联邦学习系统在 v1.2 版本中的所有API接口变更，重点修正了训练数据管理模块的架构设计问题。

## 变更原因

### 问题分析

在 v1.1 版本中，训练数据上传接口要求提供虚拟机ID参数，这存在以下问题：
1. **业务逻辑错误**：训练数据应该是用户上传的操作，不应该在上传时绑定到特定虚拟机
2. **架构设计问题**：数据应该在联邦学习任务创建后，通过数据分配服务智能分配到虚拟机
3. **不符合联邦学习流程**：正确的流程应该是先上传数据到数据池，然后根据任务需求分配数据

### 解决方案

修改训练数据管理API，移除虚拟机ID参数，使数据上传成为纯用户操作，数据分配通过专门的数据分配服务处理。

## 修改的接口

### 1. 训练数据文件上传接口

**接口地址**: `POST /api/training-data/upload`

#### 变更内容
- **移除参数**: `vmId` (虚拟机ID)
- **请求方式**: 保持不变 (multipart/form-data)
- **其他参数**: 保持不变

#### 修改前 (v1.1)
```
请求参数:
- vmId: a1b2c3d4e5f678901234567890123456 (必需) - 虚拟机ID
- dataType: ACOUSTIC (必需) - 数据类型
- description: 水声传播特征数据 (可选) - 数据描述
- tags: ["feature", "acoustic"] (可选) - 数据标签
- metadata: {"source": "bellhop", "version": "1.0"} (可选) - 元数据
- file: [文件] (必需) - 上传的文件
```

#### 修改后 (v1.2)
```
请求参数:
- dataType: ACOUSTIC (必需) - 数据类型
- description: 水声传播特征数据 (可选) - 数据描述
- tags: ["feature", "acoustic"] (可选) - 数据标签
- metadata: {"source": "bellhop", "version": "1.0"} (可选) - 元数据
- file: [文件] (必需) - 上传的文件
```

#### 响应变更
- **移除字段**: `vmId` 字段从响应中删除
- **其他字段**: 保持不变

#### 修改前响应 (v1.1)
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "datasetId": "e5f67890123456789012345678901234",
    "datasetDescription": "水声传播特征数据",
    "datasetType": "ACOUSTIC",
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "UPLOADING",
    "uploadTime": "2024-01-01T10:00:00.000Z",
    "uploadedBy": "f6789012345678901234567890123456",
    "progress": 0
  }
}
```

#### 修改后响应 (v1.2)
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "datasetId": "e5f67890123456789012345678901234",
    "datasetDescription": "水声传播特征数据",
    "datasetType": "ACOUSTIC",
    "status": "UPLOADING",
    "uploadTime": "2024-01-01T10:00:00.000Z",
    "uploadedBy": "f6789012345678901234567890123456",
    "progress": 0
  }
}
```

### 2. 训练数据文本上传接口

**接口地址**: `POST /api/training-data/text`

#### 变更内容
- **移除参数**: `vmId` (虚拟机ID)
- **请求方式**: 保持不变 (application/json)
- **其他参数**: 保持不变

#### 修改前 (v1.1)
```json
{
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "dataType": "ENVIRONMENT",
  "title": "声学传播环境配置",
  "content": "声学传播环境配置文件内容...",
  "description": "声学传播环境配置描述",
  "tags": ["environment", "acoustic"],
  "metadata": {
    "source": "bellhop",
    "version": "1.0",
    "author": "张三"
  }
}
```

#### 修改后 (v1.2)
```json
{
  "dataType": "ENVIRONMENT",
  "title": "声学传播环境配置",
  "content": "声学传播环境配置文件内容...",
  "description": "声学传播环境配置描述",
  "tags": ["environment", "acoustic"],
  "metadata": {
    "source": "bellhop",
    "version": "1.0",
    "author": "张三"
  }
}
```

#### 响应变更
- **移除字段**: `vmId` 字段从响应中删除
- **其他字段**: 保持不变

### 3. 训练数据列表查询接口

**接口地址**: `GET /api/training-data`

#### 变更内容
- **移除查询参数**: `vmId` (虚拟机ID过滤)
- **其他参数**: 保持不变

#### 修改前查询参数 (v1.1)
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 20)
- `vmId`: 虚拟机ID过滤
- `dataType`: 数据类型过滤
- `status`: 状态过滤
- `keyword`: 关键词搜索
- `startDate`: 开始日期
- `endDate`: 结束日期
- `tags`: 标签过滤

#### 修改后查询参数 (v1.2)
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 20)
- `dataType`: 数据类型过滤
- `status`: 状态过滤
- `keyword`: 关键词搜索
- `startDate`: 开始日期
- `endDate`: 结束日期
- `tags`: 标签过滤

#### 响应变更
- **移除字段**: 数据列表项中的 `vmId` 字段删除
- **其他字段**: 保持不变

### 4. 训练数据详情查询接口

**接口地址**: `GET /api/training-data/{datasetId}`

#### 变更内容
- **响应变更**: 移除 `vmId` 字段
- **其他字段**: 保持不变

### 5. 训练数据统计接口

**接口地址**: `GET /api/training-data/statistics`

#### 变更内容
- **移除查询参数**: `vmId` (虚拟机ID过滤)
- **响应变更**: 移除虚拟机分布统计相关内容
- **其他参数**: 保持不变

#### 修改前查询参数 (v1.1)
- `vmId`: 虚拟机ID过滤
- `dataType`: 数据类型过滤
- `startDate`: 开始日期
- `endDate`: 结束日期

#### 修改后查询参数 (v1.2)
- `dataType`: 数据类型过滤
- `startDate`: 开始日期
- `endDate`: 结束日期

#### 响应变更
- **移除字段**: `vmDistribution` (虚拟机数据分布)相关统计信息删除
- **其他字段**: 保持不变

### 6. 训练数据导出接口

**接口地址**: `POST /api/training-data/export`

#### 变更内容
- **移除参数**: 导出过滤条件中的 `vmId` 字段
- **其他参数**: 保持不变

#### 修改前 (v1.1)
```json
{
  "exportType": "CSV",
  "filters": {
    "dataType": "ACOUSTIC",
    "vmId": "a1b2c3d4e5f678901234567890123456",
    "status": "READY",
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-01-31T23:59:59"
  },
  "fields": ["datasetId", "datasetName", "datasetType", "vmId", "status", "uploadTime"],
  "format": "ZIP"
}
```

#### 修改后 (v1.2)
```json
{
  "exportType": "CSV",
  "filters": {
    "dataType": "ACOUSTIC",
    "status": "READY",
    "startTime": "2024-01-01T00:00:00",
    "endTime": "2024-01-31T23:59:59"
  },
  "fields": ["datasetId", "datasetDescription", "datasetType", "status", "uploadTime"],
  "format": "ZIP"
}
```

## 数据库变更

### training_dataset 表结构变更

#### 修改前 (v1.1)
```sql
CREATE TABLE training_dataset (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据集唯一标识(32位UUID)',
    vm_id VARCHAR(32) NOT NULL COMMENT '虚拟机ID(32位UUID)',
    name VARCHAR(255) NOT NULL COMMENT '数据集名称',
    description TEXT COMMENT '数据集描述',
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据集元信息'
);
```

#### 修改后 (v1.2)
```sql
CREATE TABLE training_dataset (
    id VARCHAR(32) PRIMARY KEY COMMENT '数据集唯一标识(32位UUID)',
    name VARCHAR(255) NOT NULL COMMENT '数据集名称',
    description TEXT COMMENT '数据集描述',
    data_type ENUM('ACOUSTIC', 'ENVIRONMENT', 'MODEL', 'OTHER') NOT NULL COMMENT '数据类型',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(32) NULL COMMENT '上传者ID(32位UUID)',
    status ENUM('UPLOADING', 'PROCESSING', 'READY', 'ERROR') DEFAULT 'UPLOADING',
    metadata JSON COMMENT '数据集元信息'
);
```

#### 变更说明
- **删除字段**: `vm_id` - 虚拟机ID字段
- **新增字段**: `uploaded_by` - 上传者用户ID
- **删除约束**: 删除 `vm_id` 相关的外键约束和索引
- **新增约束**: 添加 `uploaded_by` 的外键约束到 `users` 表

## 业务流程变更

### 修改前流程 (v1.1)
1. 用户上传数据时必须指定目标虚拟机
2. 数据直接绑定到指定虚拟机
3. 联邦学习任务只能使用已绑定到参与虚拟机的数据

### 修改后流程 (v1.2)
1. **数据上传阶段**: 用户上传数据到系统数据池，不绑定虚拟机
2. **任务创建阶段**: 创建联邦学习任务，指定参与虚拟机和使用的数据集
3. **数据分配阶段**: 通过 `DataDistributionService` 根据策略将数据智能分配到虚拟机
4. **训练执行阶段**: 虚拟机使用分配到的数据进行联邦学习训练

## 客户端适配指南

### 前端应用适配

1. **文件上传表单**
   - 移除虚拟机ID选择字段
   - 更新表单验证逻辑

2. **数据列表页面**
   - 移除虚拟机ID列显示
   - 移除虚拟机ID过滤选项

3. **数据统计页面**
   - 移除虚拟机数据分布图表
   - 更新统计查询参数

### API调用适配

```javascript
// 修改前 (v1.1)
const uploadData = {
  vmId: 'a1b2c3d4e5f678901234567890123456',
  dataType: 'ACOUSTIC',
  file: fileObject
};

// 修改后 (v1.2)
const uploadData = {
  dataType: 'ACOUSTIC',
  file: fileObject
};
```

### 虚拟机客户端适配

由于数据不再预绑定到虚拟机，虚拟机客户端需要：
1. 等待数据分配服务的通知
2. 接收分配的数据集信息
3. 从数据分配服务获取训练数据

## 兼容性说明

### 破坏性变更

⚠️ **注意**: 此版本包含破坏性变更，不向后兼容 v1.1 版本

1. **API 接口**: 移除了 vmId 参数，调用方必须更新代码
2. **数据库结构**: 删除了 vm_id 字段，需要数据迁移
3. **业务流程**: 数据管理流程发生根本变化

### 迁移步骤

1. **数据备份**: 在升级前备份现有数据
2. **API 更新**: 更新所有调用训练数据API的客户端代码
3. **数据库迁移**: 执行数据库结构变更脚本
4. **流程适配**: 适配新的数据分配流程

## 相关文档

- [训练数据管理 API 参考文档 v1.2](../train-data/training-data-api-reference.md)
- [数据分配服务文档](../data-distribution/data-distribution-api-reference.md)
- [联邦学习任务管理 API 参考文档](../federated-task/federated-task-api-reference.md)
- [数据库迁移指南](../../database/migration/v1.1-to-v1.2-migration.md)

## 版本信息

- **修改版本**: v1.2
- **修改日期**: 2024-01-16
- **影响范围**: 训练数据管理模块
- **兼容性**: 破坏性变更，不向后兼容