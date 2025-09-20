# 联邦学习任务API接口删除文档 v1.3

## 文档说明
- **版本**: v1.3
- **创建时间**: 2024年
- **修改范围**: 联邦学习任务管理相关接口
- **目的**: 记录废弃和删除的接口及配置项

本文档记录了从 v1.0 到 v1.3 版本中所有**废弃**和**删除**的接口、字段和配置项。

## 废弃的配置字段

### 1. 任务创建接口中的废弃字段

#### 1.1 简化的参与者配置格式
**废弃字段**: `participants` 数组中的简化格式

**原格式 (v1.0)**:
```json
{
  "participants": [
    {
      "vmId": "a1b2c3d4e5f678901234567890123456",
      "role": "PARTICIPANT",
      "dataSource": "bellhop_features_001.csv"
    }
  ]
}
```

**废弃原因**:
- `dataSource` 字段设计过于简化，无法支持复杂的数据分配策略
- 缺乏对虚拟机能力和资源约束的描述
- 不支持智能化的数据分配和验证

**替代方案**:
使用新的 `datasetConfig` 和 `participantConfig` 结构：
```json
{
  "datasetConfig": {
    "datasetId": "e5f67890123456789012345678901234",
    "distributionStrategy": "BALANCED",
    "distributionRatios": {
      "a1b2c3d4e5f678901234567890123456": 0.6
    }
  },
  "participantConfig": {
    "participants": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "role": "PARTICIPANT",
        "dataRatio": 0.6,
        "capabilities": ["GPU"],
        "constraints": {
          "maxCpuUsage": 80,
          "maxMemoryUsage": 75
        }
      }
    ]
  }
}
```

**兼容性说明**:
- v1.3 版本仍支持原格式，但会发出废弃警告
- v2.0 版本将完全移除对原格式的支持
- 建议在 v1.3 发布后 6 个月内完成迁移

#### 1.2 直接数据源配置字段
**废弃字段**: `participants[].dataSource`

**废弃原因**:
- 无法支持数据集的统一管理和验证
- 缺乏数据分配策略的灵活性
- 不支持数据预处理和质量控制

**替代方案**:
使用 `datasetConfig` 进行统一的数据集配置：
```json
{
  "datasetConfig": {
    "datasetId": "统一的数据集ID",
    "distributionStrategy": "分配策略",
    "distributionRatios": "分配比例配置"
  }
}
```

## 废弃的错误码

### 2. 参与者相关错误码

#### 2.1 数据源错误码
**废弃错误码**: `PARTICIPANT_DATA_SOURCE_ERROR`

**错误码定义 (v1.0)**:
```json
{
  "code": "PARTICIPANT_DATA_SOURCE_ERROR",
  "httpStatus": 400,
  "message": "参与者数据源配置错误"
}
```

**废弃原因**:
- 随着 `dataSource` 字段的废弃，相关错误码也不再适用
- 新的数据配置结构有更详细的错误分类

**替代错误码**:
```json
{
  "code": "DATASET_CONFIG_ERROR",
  "httpStatus": 400,
  "message": "数据集配置错误"
}
```

#### 2.2 简化配置废弃警告码
**新增警告码**: `SIMPLE_PARTICIPANT_CONFIG_DEPRECATED`

**警告码定义 (v1.3)**:
```json
{
  "code": "SIMPLE_PARTICIPANT_CONFIG_DEPRECATED",
  "httpStatus": 200,
  "message": "简化的参与者配置格式已废弃，建议使用新的participantConfig结构",
  "details": {
    "deprecationVersion": "v1.3",
    "removalVersion": "v2.0",
    "migrationGuide": "/docs/api/migration-guide-v1.3.md"
  }
}
```

**使用场景**:
当客户端使用旧格式的参与者配置时，服务端会返回此警告码

## 废弃的响应字段

### 3. 任务创建响应中的废弃字段

#### 3.1 简化的参与者信息
**废弃字段**: 响应中的简化参与者信息格式

**原格式 (v1.0)**:
```json
{
  "data": {
    "taskId": "c3d4e5f6789012345678901234567890",
    "participants": [
      {
        "vmId": "a1b2c3d4e5f678901234567890123456",
        "dataSource": "bellhop_features_001.csv"
      }
    ]
  }
}
```

**废弃原因**:
- 信息过于简化，不能反映实际的配置复杂性
- 缺乏对数据分配结果的详细描述

**替代格式**:
```json
{
  "data": {
    "taskId": "c3d4e5f6789012345678901234567890",
    "configSummary": {
      "dataset": {
        "datasetId": "e5f67890123456789012345678901234",
        "totalRows": 10000,
        "distributionStrategy": "BALANCED"
      },
      "participants": [
        {
          "vmId": "a1b2c3d4e5f678901234567890123456",
          "vmName": "水声联邦学习节点-001",
          "role": "PARTICIPANT",
          "dataRatio": 0.6,
          "status": "PENDING"
        }
      ]
    }
  }
}
```

## 废弃的查询参数

### 4. 任务列表查询中的废弃参数

#### 4.1 简化的状态过滤
**废弃参数**: 部分过于简化的状态枚举值

**废弃状态值**:
- `SIMPLE_CREATED` - 替换为 `CREATED`
- `SIMPLE_RUNNING` - 替换为 `RUNNING`

**废弃原因**:
- 状态定义不够清晰
- 与新的任务生命周期管理不匹配

## 迁移指南

### 5. 从 v1.0 到 v1.3 的迁移步骤

#### 5.1 参与者配置迁移

**步骤 1**: 识别使用旧格式的代码
```javascript
// 旧格式 (需要迁移)
const oldConfig = {
  participants: [
    {
      vmId: "vm-001",
      role: "PARTICIPANT",
      dataSource: "data.csv"
    }
  ]
};
```

**步骤 2**: 转换为新格式
```javascript
// 新格式 (推荐)
const newConfig = {
  datasetConfig: {
    datasetId: "dataset-001", // 需要先上传数据集获取ID
    distributionStrategy: "BALANCED",
    distributionRatios: {
      "vm-001": 1.0
    }
  },
  participantConfig: {
    selectionMode: "MANUAL",
    participants: [
      {
        vmId: "vm-001",
        role: "PARTICIPANT",
        dataRatio: 1.0
      }
    ]
  }
};
```

**步骤 3**: 数据集预处理
在使用新格式前，需要：
1. 将数据文件上传到训练数据管理系统
2. 获取数据集ID
3. 确保数据集状态为 `READY`

#### 5.2 错误处理迁移

**旧的错误处理**:
```javascript
// v1.0 错误处理
if (response.code === 'PARTICIPANT_DATA_SOURCE_ERROR') {
  console.error('数据源配置错误');
}
```

**新的错误处理**:
```javascript
// v1.3 错误处理
if (response.code === 'DATASET_CONFIG_ERROR') {
  console.error('数据集配置错误');
}

// 处理废弃警告
if (response.code === 'SIMPLE_PARTICIPANT_CONFIG_DEPRECATED') {
  console.warn('使用了废弃的配置格式，请尽快迁移');
}
```

#### 5.3 响应数据处理迁移

**旧的响应处理**:
```javascript
// v1.0 响应处理
const participants = response.data.participants;
participants.forEach(p => {
  console.log(`VM ${p.vmId} 使用数据源: ${p.dataSource}`);
});
```

**新的响应处理**:
```javascript
// v1.3 响应处理
const configSummary = response.data.configSummary;
const participants = configSummary.participants;
participants.forEach(p => {
  console.log(`VM ${p.vmName} (${p.vmId}) 分配数据比例: ${p.dataRatio}`);
});
```

### 6. 兼容性时间表

| 版本 | 旧格式支持 | 警告提示 | 完全移除 |
|------|------------|----------|----------|
| v1.3 | ✅ 支持 | ⚠️ 显示警告 | ❌ |
| v1.4 | ✅ 支持 | ⚠️ 显示警告 | ❌ |
| v1.5 | ⚠️ 有限支持 | ⚠️ 显示警告 | ❌ |
| v2.0 | ❌ 不支持 | ❌ | ✅ 完全移除 |

### 7. 迁移检查清单

#### 7.1 代码迁移检查
- [ ] 替换所有使用 `participants[].dataSource` 的代码
- [ ] 更新为使用 `datasetConfig` 和 `participantConfig`
- [ ] 更新错误处理代码
- [ ] 更新响应数据处理逻辑
- [ ] 添加废弃警告的处理

#### 7.2 数据迁移检查
- [ ] 将现有数据文件上传到训练数据管理系统
- [ ] 获取所有数据集的正确ID
- [ ] 验证数据集状态为 `READY`
- [ ] 测试新的数据分配预览功能

#### 7.3 测试迁移检查
- [ ] 更新单元测试以使用新格式
- [ ] 更新集成测试
- [ ] 验证向后兼容性
- [ ] 测试废弃警告的正确显示

## 废弃时间线

### 8. 详细的废弃时间线

#### 8.1 v1.3 版本 (当前)
- **新增**: 废弃警告机制
- **保持**: 完全向后兼容
- **开始**: 废弃警告提示

#### 8.2 v1.4 版本 (预计 3 个月后)
- **增强**: 废弃警告更加明显
- **新增**: 迁移辅助工具
- **保持**: 完全向后兼容

#### 8.3 v1.5 版本 (预计 6 个月后)
- **限制**: 仅在兼容模式下支持旧格式
- **要求**: 必须明确启用旧格式支持
- **推荐**: 强烈建议完成迁移

#### 8.4 v2.0 版本 (预计 12 个月后)
- **移除**: 完全移除旧格式支持
- **清理**: 移除相关兼容代码
- **强制**: 必须使用新格式

## 支持和帮助

### 9. 迁移支持资源

#### 9.1 文档资源
- [API迁移指南详细版](/docs/api/migration-guide-v1.3-detailed.md)
- [新接口使用示例](/docs/api/examples/federated-task-v1.3.md)
- [常见迁移问题FAQ](/docs/api/migration-faq.md)

#### 9.2 工具支持
- 配置格式转换工具: `/tools/config-converter-v1.3.js`
- 兼容性检查工具: `/tools/compatibility-checker.js`
- 自动迁移脚本: `/tools/auto-migration.js`

#### 9.3 技术支持
- 技术支持邮箱: api-support@feduwacomm.edu
- 迁移问题讨论群: FedUWAComm-Migration-v1.3
- 在线文档: https://docs.feduwacomm.edu/migration/v1.3

本文档详细记录了 v1.3 版本中所有废弃和删除的内容，并提供了完整的迁移指南，确保系统升级的平滑过渡。