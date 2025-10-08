# 后端重构指南 - WebSocket v1.5协议标准联邦学习链路

## 📋 文档概览

**文档版本**: v1.5.0
**创建时间**: 2025-01-29
**适用范围**: FedUWAComm后端系统重构
**协议版本**: WebSocket v1.5（破坏性变更版本）

## 🎯 重构目标

### 核心目标
将现有后端系统重构为严格符合WebSocket v1.5协议的标准联邦学习链路，实现：
- **13步标准化流程**：完整的联邦学习生命周期管理
- **assignedDatasetId统一管理**：后端UuidUtil统一生成和分配数据集ID
- **破坏性变更支持**：完全移除对v1.4 datasetId的兼容
- **中心化控制架构**：后端作为"大脑"，虚拟机作为"手脚"

### 业务流程目标
实现标准联邦学习链路：
```
前端创建页面 → 查询可用VM → 上传数据集 → 数据集解析 →
填写信息创建任务 → 后端数据集分配 → WebSocket数据集创建 →
数据集查询验证 → 存储assignedDatasetId → 发送任务消息 →
开始联邦学习流程
```

## 🔍 现状分析

### 当前架构问题

#### 1. 协议版本不一致
```java
// 问题：现有测试使用v1.4协议
@Test
void test03_EnhancedWebSocketConnections() {
    // 使用v1.4协议，缺少assignedDatasetId支持
}

// 问题：MockVirtualMachine仍使用datasetId
public class MockVirtualMachine {
    // v1.4核心特性，未实现assignedDatasetId依赖
}
```

#### 2. 数据集管理缺失
```java
// 问题：FederatedTaskService缺少v1.5数据集分配逻辑
public class FederatedTaskService {
    // 缺少assignedDatasetId统一管理
    // 缺少13步流程支持
}
```

#### 3. WebSocket协议支持不完整
```java
// 问题：WebSocketProtocolService未实现v1.5新协议
// 缺少：DATASET_LIST_QUERY/RESPONSE
// 缺少：assignedDatasetId在FEDERATED_TASK_START中的dataConfig位置
```

## 🏗️ 标准联邦学习链路设计

### 13步标准化流程

基于WebSocket v1.5协议的完整联邦学习链路：

#### 阶段A: 前端操作和任务准备 (步骤1-5)
```
步骤1: 前端进入创建联邦学习任务页面
├── 用户访问联邦学习任务创建界面
└── 前端初始化任务创建表单

步骤2: 前端查询可用虚拟机列表
├── GET /api/federated/config/available-vms
├── 可选参数：algorithm, minCpuCores, minMemoryMb
└── 后端返回可用虚拟机列表

步骤3: 后端返回可用虚拟机列表
├── 过滤可用状态的虚拟机
├── 返回虚拟机详细信息（vmId, 配置, 状态）
└── 前端展示虚拟机选择列表

步骤4: 前端上传完整数据集到后端
├── POST /api/training-data/upload (multipart/form-data)
├── 上传CSV/Excel训练数据文件
└── 后端保存原始数据集文件

步骤5: 后端进行数据集解析和预处理
├── 解析数据集格式和结构
├── 验证数据完整性
├── 生成数据集元信息
└── 为后续分配做准备
```

#### 阶段B: 任务创建和数据集分配 (步骤6-9)
```
步骤6: 前端填写信息并调用创建联邦学习任务接口
├── POST /api/federated/tasks (TaskCreateDTO)
├── 包含：算法选择、参与者配置、训练参数
└── 触发后端任务创建流程

步骤7: 后端接收信息并开始创建联邦学习任务
├── 验证任务参数和权限
├── 创建FederatedTask实体
├── 分配任务ID (UuidUtil.generateUuid())
└── 初始化任务状态为CREATING

步骤8: 后端通过算法将数据集进行分配，在每台参与训练的虚拟机创建数据集
├── 数据集分片算法（按参与者数量分配）
├── 为每个参与者生成assignedDatasetId (UuidUtil.generateUuid())
├── 通过WebSocket发送FEDERATED_TASK_START消息
└── dataConfig包含assignedDatasetId和数据集信息

步骤9: 后端向虚拟机查询数据集数据（通过WebSocket协议）
├── 发送DATASET_LIST_QUERY消息
├── 确保对应assignedDatasetId的数据集存在
└── 准备将数据集ID存储在数据库中
```

#### 阶段C: 数据集验证和任务启动 (步骤10-13)
```
步骤10: 虚拟机向后端发送数据集信息
├── 响应DATASET_LIST_RESPONSE消息
├── 确认assignedDatasetId对应的数据集状态
└── 状态：PENDING/CREATED/UPLOADING/COMPLETED/FAILED

步骤11: 后端接收信息并选择对应的数据集ID
├── 验证所有参与者的数据集状态
├── 将assignedDatasetId存储在task_participants表
├── 更新数据集状态和关联信息
└── 确保数据集ID全局唯一性

步骤12: 后端继续运行联邦学习创建逻辑，对每台虚拟机发送创建联邦学习task消息
├── 向所有参与者发送FEDERATED_TASK_START_ACK确认
├── 配置联邦学习参数（算法、轮次、聚合方法）
└── 初始化全局模型

步骤13: 开始完整的联邦学习流程
├── 启动联邦学习轮次循环
├── 执行标准的训练-聚合-广播流程
└── 监控任务执行状态
```

### v1.5协议关键消息

#### FEDERATED_TASK_START (更新)
```json
{
  "type": "FEDERATED_TASK_START",
  "id": "server-generated-uuid",
  "vmId": "target-vm-id",
  "data": {
    "taskId": "fedtask-uuid",
    "federatedAlgorithm": "FEDERATED_AVERAGING",
    "totalRounds": 10,
    "participants": ["vm1", "vm2", "vm3"],
    "config": {
      "aggregationMethod": "FEDERATED_AVERAGING",
      "minParticipants": 2,
      "timeout": 1800
    },
    "dataConfig": {
      "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：位于dataConfig内
      "dataPath": "/data/training",
      "validationSplit": 0.2,
      "shuffle": true
    }
  }
}
```

#### DATASET_LIST_QUERY (新增)
```json
{
  "type": "DATASET_LIST_QUERY",
  "id": "query-uuid",
  "vmId": "target-vm-id",
  "data": {
    "taskId": "fedtask-uuid",
    "queryType": "ASSIGNED_DATASETS"
  }
}
```

#### DATASET_LIST_RESPONSE (新增)
```json
{
  "type": "DATASET_LIST_RESPONSE",
  "id": "response-uuid",
  "data": {
    "taskId": "fedtask-uuid",
    "datasets": [
      {
        "assignedDatasetId": "dataset-uuid-backend",
        "status": "CREATED",
        "localPath": "/data/assigned/dataset-uuid-backend",
        "createdAt": "2025-01-29T10:30:00.000Z"
      }
    ],
    "totalCount": 1
  }
}
```

#### GRADIENT_UPLOAD (更新)
```json
{
  "type": "GRADIENT_UPLOAD",
  "data": {
    "taskId": "fedtask-uuid",
    "roundNumber": 1,
    "assignedDatasetId": "dataset-uuid-generated-by-backend", // 🆕 v1.5：数据集关联验证
    "gradientData": {
      // 梯度数据
    },
    "trainingMetrics": {
      "samplesCount": 1000,
      "localLoss": 0.25
    }
  }
}
```

## 🛠️ 重构任务分解

### 1. 后端服务层重构
- **FederatedTaskService**: v1.5任务创建和数据集分配
- **WebSocketProtocolService**: v1.5协议支持
- **DataDistributionService**: assignedDatasetId管理
- **TrainingDataService**: 数据集解析和分配逻辑

### 2. 单元测试重构
- **CompleteFederatedLearningFlowTest**: 完整v1.5流程测试
- 新增13步流程覆盖测试
- assignedDatasetId验证测试

### 3. Mock虚拟机重构
- **MockVirtualMachine**: v1.5协议支持
- 实现assignedDatasetId完全依赖
- 新增DATASET_LIST协议支持

## 📊 数据库架构更新

### task_participants表更新
```sql
-- v1.5新增字段
ALTER TABLE task_participants
ADD COLUMN assigned_dataset_id VARCHAR(32) NOT NULL
COMMENT 'v1.5后端分配的数据集ID(32位UUID)';

ALTER TABLE task_participants
ADD COLUMN dataset_status ENUM('PENDING', 'CREATED', 'UPLOADING', 'COMPLETED', 'FAILED')
DEFAULT 'PENDING' COMMENT 'v1.5数据集状态';

ALTER TABLE task_participants
ADD COLUMN dataset_created_at TIMESTAMP NULL
COMMENT 'v1.5数据集创建时间';

-- 新增索引
CREATE INDEX idx_assigned_dataset_id ON task_participants(assigned_dataset_id);
CREATE INDEX idx_dataset_status ON task_participants(dataset_status);
```

## 🔄 实施计划

### 第一阶段：后端服务重构 (预计耗时: 2-3小时)
1. 更新FederatedTaskService实现v1.5任务创建
2. 重构WebSocketProtocolService支持新协议
3. 实现DataDistributionService数据集分配逻辑
4. 更新TrainingDataService集成assignedDatasetId

### 第二阶段：测试重构 (预计耗时: 1-2小时)
1. 重写CompleteFederatedLearningFlowTest
2. 添加13步流程单元测试
3. 集成v1.5协议验证测试

### 第三阶段：Mock VM重构 (预计耗时: 1小时)
1. 升级MockVirtualMachine到v1.5协议
2. 实现assignedDatasetId依赖逻辑
3. 添加新协议消息支持

### 第四阶段：集成测试和验证 (预计耗时: 1小时)
1. 运行完整测试套件
2. 验证v1.5协议一致性
3. 性能和稳定性验证

## 📚 相关文档

- [WebSocket协议文档v1.5](../shared/api/WebSocket/WebSocket协议文档-中心化-简化.md)
- [v1.5修改接口文档](../shared/api/WebSocket/modified/modified-interfaces-v1.5.md)
- [v1.5移除接口文档](../shared/api/WebSocket/removed/removed-interfaces-v1.5.md)
- [联邦学习完整流程](../shared/api/WebSocket/example/联邦学习完整流程.md)

## ⚠️ 重要注意事项

### 破坏性变更警告
- **v1.5版本不兼容v1.4**：数据集管理完全重写
- **assignedDatasetId为必需字段**：所有数据集操作必须使用后端分配的ID
- **协议消息结构变更**：FEDERATED_TASK_START和GRADIENT_UPLOAD结构更新

### 迁移检查清单
- [ ] 确保所有datasetId引用替换为assignedDatasetId
- [ ] 验证assignedDatasetId在dataConfig内部
- [ ] 实现DATASET_LIST_QUERY/RESPONSE协议
- [ ] 更新数据库表结构
- [ ] 测试13步完整流程
- [ ] 验证虚拟机完全依赖后端ID分配

这份重构指南确保了后端系统完全符合WebSocket v1.5协议标准，实现了assignedDatasetId的统一管理和13步标准化联邦学习流程。