# WebSocket 协议废弃接口 - v1.4 移除说明

## 概述

本文档描述了从旧版WebSocket协议（v1.3及以前）到新版简化协议（v1.4）中移除的接口和功能。v1.4版本进行了大幅度的协议精简和优化，将原有的55个复杂协议消息精简为34个核心协议，移除了冗余、过度细分和功能重复的协议消息。

## 重大变更概述

### 协议数量优化
- **移除协议总数**: 21个冗余和重复协议
- **优化效果**: 协议数量减少38%（55→34）
- **核心目标**: 消除冗余，提高效率，支持多任务并发

### 设计理念转变
- **从复杂到简洁**: 移除过度细分的ACK机制
- **从单任务到多任务**: 统一taskId字段支持多任务并发
- **从分散到集中**: 统一状态监控和任务生命周期管理
- **从协商到配置**: 将复杂协商过程简化为配置下发

## 1. 移除的协议消息类型

### 1.1 训练控制重复协议 (5个)

**移除原因**: 功能重复，与FEDERATED_TASK_START系列协议冗余，增加不必要的复杂性

#### TRAINING_START 🟢 (已移除)
```json
{
  "type": "TRAINING_START",
  "id": "cmd-1704067200000-123463",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "mlAlgorithm": "RandomForest",
    "hyperparameters": { "n_estimators": 100 },
    "globalModel": { /* 模型参数 */ }
  }
}
```
**替代方案**: 功能合并到 `FEDERATED_TASK_START`，一次性分发完整的任务配置、初始模型和训练参数

#### TRAINING_START_ACK 🟢 (已移除)
```json
{
  "type": "TRAINING_START_ACK",
  "data": {
    "status": "COMMAND_SENT",
    "message": "本地训练指令已发送给VM，等待VM确认"
  }
}
```
**替代方案**: 使用 `FEDERATED_TASK_START_ACK`

#### TRAINING_START_COMMAND 🟢 (已移除)
#### TRAINING_START_RESPONSE 🔵 (已移除)
#### TRAINING_START_RESPONSE_ACK 🟢 (已移除)

**移除原理**: 这些协议将训练启动过程过度细分为5个步骤，造成不必要的网络往返和状态管理复杂性。新协议通过FEDERATED_TASK_START一次性完成所有配置。

### 1.2 模型传输重复协议 (1个)

#### MODEL_UPLOAD 🔵 (已移除)
```json
{
  "type": "MODEL_UPLOAD",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "round": 25,
    "parameters": {
      "model": {
        "framework": "pytorch",
        "weights": { /* 完整模型权重 */ }
      }
    },
    "metrics": { "accuracy": 0.88, "loss": 0.12 }
  }
}
```
**替代方案**: 使用 `GRADIENT_UPLOAD`，专为联邦学习优化，支持梯度增量传输和压缩

**移除原理**: MODEL_UPLOAD适用于传统训练场景，联邦学习更适合传输梯度而非完整模型

### 1.3 任务管理重复协议 (2个)

#### TASK_START 🟢 (已移除)
#### TASK_START_ACK 🟢 (已移除)

**移除原因**: 功能与FEDERATED_TASK_START系列协议重复，联邦学习场景下使用专用的联邦任务协议更合适

### 1.4 训练进度过度细分协议 (4个)

**移除原因**: 将训练进度查询过度细分为4个独立协议，造成不必要的网络开销

#### TRAINING_PROGRESS 🔵 (已移除)
```json
{
  "type": "TRAINING_PROGRESS",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {
    "taskId": "task-123456",
    "currentRound": 25,
    "progress": 25.5,
    "metrics": { "accuracy": 0.85, "loss": 0.15 }
  }
}
```

#### TRAINING_PROGRESS_ACK 🟢 (已移除)
#### TRAINING_PROGRESS_RESPONSE 🔵 (已移除)
#### TRAINING_PROGRESS_RESPONSE_ACK 🟢 (已移除)

**替代方案**: 通过HEARTBEAT机制在心跳中报告状态，使用FEDERATED_TASK_STATUS_QUERY进行详细查询

**移除原理**: 训练进度通过心跳机制定期上报更高效，避免额外的协议开销

### 1.5 聚合过程过度细分协议 (6个)

**移除原因**: 将联邦聚合过程过度细分，实际上聚合过程可以隐含在轮次流程中

#### AGGREGATION_START 🟢 / AGGREGATION_START_ACK 🔵 (已移除)
```json
{
  "type": "AGGREGATION_START",
  "data": {
    "aggregationId": "agg-123456",
    "participantCount": 5,
    "timeoutSeconds": 300
  }
}
```

#### AGGREGATION_COMPLETE 🟢 / AGGREGATION_COMPLETE_ACK 🔵 (已移除)
#### GRADIENT_UPLOAD_PREPARE 🟢 / GRADIENT_UPLOAD_PREPARE_ACK 🔵 (已移除)
#### AGGREGATION_NOTIFICATION 🟢 (已移除)

**替代方案**: 聚合过程隐含在ROUND_START → GRADIENT_UPLOAD → GLOBAL_MODEL_BROADCAST流程中

**移除原理**: 聚合是联邦学习的内在过程，不需要独立的协议管理

### 1.6 协商和配置协议 (4个)

**移除原因**: 将协商过程简化为配置下发，减少不必要的网络往返

#### MODEL_TYPE_NEGOTIATION 🟢 / MODEL_TYPE_NEGOTIATION_ACK 🔵 (已移除)
```json
{
  "type": "MODEL_TYPE_NEGOTIATION",
  "data": {
    "supportedModels": ["RandomForest", "SVM", "NeuralNetwork"],
    "preferredFramework": "sklearn",
    "computeCapabilities": { "maxBatchSize": 1024 }
  }
}
```

#### ALGORITHM_CONFIG 🟢 / ALGORITHM_CONFIG_ACK 🔵 (已移除)
```json
{
  "type": "ALGORITHM_CONFIG",
  "data": {
    "federatedAlgorithm": "FEDERATED_AVERAGING",
    "aggregationMethod": "WEIGHTED_AVERAGE",
    "hyperparameters": { "learningRate": 0.01 }
  }
}
```

**替代方案**: 模型类型和算法配置在FEDERATED_TASK_START中一次性指定

**移除原理**: 协商过程增加延迟，任务启动时直接指定配置更高效

### 1.7 状态查询重复协议 (2个)

#### BATCH_STATUS_QUERY 🟢 / BATCH_STATUS_RESPONSE 🟢 (已移除)
```json
{
  "type": "BATCH_STATUS_QUERY",
  "vmId": "broadcast",
  "data": {
    "queryId": "batch-query-001",
    "targets": ["vm-001", "vm-002", "vm-003"]
  }
}
```

**替代方案**: 使用统一的VM_STATUS_QUERY获取虚拟机状态，支持多任务概览

**移除原理**: 批量查询功能可通过现有的状态查询机制实现，无需独立协议

## 2. 移除的设计模式

### 2.1 过度细分的ACK机制

**移除模式**: 为每个操作创建独立的ACK协议
```
❌ 已移除的复杂ACK链
TRAINING_START → TRAINING_START_ACK → TRAINING_START_RESPONSE → TRAINING_START_RESPONSE_ACK

✅ 简化后的ACK机制
FEDERATED_TASK_START → FEDERATED_TASK_START_ACK
```

**移除原理**: 过多的ACK消息增加网络延迟和状态管理复杂性，简化为核心的请求-响应模式

### 2.2 冗余的状态查询机制

**移除模式**: 多种重复的状态查询协议
```
❌ 已移除的冗余查询
STATUS_QUERY, BATCH_STATUS_QUERY, TRAINING_PROGRESS等

✅ 统一的双层状态监控
VM_STATUS_QUERY: 虚拟机整体状态 + 多任务概览
FEDERATED_TASK_STATUS_QUERY: 特定任务详细状态
```

### 2.3 复杂的协商流程

**移除模式**: 多步骤的能力协商和配置过程
```
❌ 已移除的协商流程
1. MODEL_TYPE_NEGOTIATION
2. MODEL_TYPE_NEGOTIATION_ACK
3. ALGORITHM_CONFIG
4. ALGORITHM_CONFIG_ACK
5. TRAINING_START

✅ 简化的配置下发
1. FEDERATED_TASK_START (包含所有配置)
```

## 3. 移除的协议分类汇总

### 3.1 按功能分类的移除统计

| 功能类别 | 移除数量 | 移除协议 | 替代方案 |
|---------|---------|---------|----------|
| 训练控制 | 5个 | TRAINING_START系列 | FEDERATED_TASK_START |
| 模型传输 | 1个 | MODEL_UPLOAD | GRADIENT_UPLOAD |
| 任务管理 | 2个 | TASK_START系列 | 联邦任务协议 |
| 训练进度 | 4个 | TRAINING_PROGRESS系列 | 心跳机制+状态查询 |
| 聚合过程 | 6个 | AGGREGATION系列 | 轮次流程隐含 |
| 协商配置 | 4个 | 协商和配置协议 | 任务启动配置 |
| 状态查询 | 2个 | 批量状态查询 | 统一状态查询 |
| **总计** | **21个** | - | - |

### 3.2 移除协议的影响范围

#### 高影响 (需要重构代码逻辑)
- TRAINING_START系列 → FEDERATED_TASK_START
- MODEL_UPLOAD → GRADIENT_UPLOAD
- 聚合协议系列 → 轮次管理流程

#### 中影响 (需要更新消息处理)
- 协商配置协议 → 任务启动配置
- 训练进度协议 → 心跳和状态查询

#### 低影响 (功能合并)
- 任务管理协议 → 联邦任务协议
- 批量状态查询 → 统一状态查询

## 4. 迁移路径指南

### 4.1 从旧协议到新协议的映射

#### 训练控制迁移
```
旧协议流程:
TRAINING_START → TRAINING_START_ACK → TRAINING_START_COMMAND →
TRAINING_START_RESPONSE → TRAINING_START_RESPONSE_ACK

新协议流程:
FEDERATED_TASK_START → FEDERATED_TASK_START_ACK
```

#### 模型传输迁移
```
旧协议:
MODEL_UPLOAD (完整模型上传)

新协议:
GRADIENT_UPLOAD (梯度增量上传)
```

#### 状态监控迁移
```
旧协议:
STATUS_QUERY, BATCH_STATUS_QUERY, TRAINING_PROGRESS

新协议:
VM_STATUS_QUERY (VM整体+多任务概览)
FEDERATED_TASK_STATUS_QUERY (特定任务详情)
```

### 4.2 代码重构建议

#### 后端服务重构
```java
// ❌ 移除的复杂训练启动流程
public void startTraining(String vmId, TrainingConfig config) {
    // 1. 发送TRAINING_START
    sendTrainingStart(vmId, config);
    // 2. 等待TRAINING_START_ACK
    waitForAck(vmId, "TRAINING_START_ACK");
    // 3. 发送TRAINING_START_COMMAND
    sendTrainingStartCommand(vmId, config);
    // 4. 等待TRAINING_START_RESPONSE
    waitForResponse(vmId, "TRAINING_START_RESPONSE");
    // 5. 发送TRAINING_START_RESPONSE_ACK
    sendResponseAck(vmId);
}

// ✅ 简化的联邦任务启动
public void startFederatedTask(List<String> vmIds, FederatedTaskConfig config) {
    // 一次性分发完整配置、初始模型和训练参数
    ProtocolMessage taskStart = MessageBuilder.buildFederatedTaskStart(
        config.getTaskId(),
        config.getFederatedAlgorithm(),
        config.getTotalRounds(),
        config.getParticipants(),
        config.getInitialGlobalModel(),
        config.getLocalTrainingConfig()
    );
    broadcastToVms(vmIds, taskStart);
}
```

#### 虚拟机端重构
```java
// ❌ 移除的复杂模型上传
public void uploadTrainedModel(TrainingResult result) {
    ModelUploadMessage message = new ModelUploadMessage();
    message.setParameters(result.getCompleteModel());
    message.setMetrics(result.getMetrics());
    sendMessage(message);
}

// ✅ 简化的梯度上传
public void uploadGradients(TrainingResult result) {
    GradientUploadMessage message = new GradientUploadMessage();
    message.setTaskId(result.getTaskId());
    message.setRoundNumber(result.getRoundNumber());
    message.setGradientData(result.getGradients()); // 只传输梯度
    message.setCompressionMethod("gzip");
    sendMessage(message);
}
```

## 5. 兼容性和迁移影响

### 5.1 破坏性变更

**不提供向后兼容性**: v1.4为了实现协议的根本性优化，不支持与旧协议的兼容

**影响范围**:
- 所有使用移除协议的客户端代码需要重构
- 服务端的协议处理逻辑需要全面更新
- 测试用例需要更新到新的协议流程

### 5.2 迁移优先级

#### 高优先级 (必须立即迁移)
1. **TRAINING_START系列** → **FEDERATED_TASK_START**
   - 影响核心训练启动流程
   - 需要重构任务配置和模型分发逻辑

2. **MODEL_UPLOAD** → **GRADIENT_UPLOAD**
   - 影响联邦学习核心功能
   - 需要修改模型参数传输格式

#### 中优先级 (建议尽快迁移)
3. **聚合协议系列** → **轮次管理流程**
   - 影响联邦聚合流程
   - 需要调整聚合时序控制

4. **状态查询协议** → **统一状态查询**
   - 影响监控和调试功能
   - 需要更新状态收集逻辑

#### 低优先级 (可逐步迁移)
5. **协商配置协议** → **任务启动配置**
   - 影响任务初始化流程
   - 相对容易迁移

### 5.3 迁移验证清单

**协议移除验证**:
- [ ] 确认所有移除的21个协议不再使用
- [ ] 验证新协议能够覆盖原有功能
- [ ] 测试多任务并发场景正常工作
- [ ] 验证任务生命周期管理完整

**功能完整性验证**:
- [ ] 联邦学习训练流程端到端测试
- [ ] 多虚拟机协同测试
- [ ] 错误恢复和异常处理测试
- [ ] 性能对比测试（确认优化效果）

**文档和工具更新**:
- [ ] 更新API文档和集成指南
- [ ] 更新客户端SDK和示例代码
- [ ] 更新开发工具和调试脚本
- [ ] 更新单元测试和集成测试

## 6. 协议优化效果

### 6.1 量化改进指标

| 优化指标 | 旧协议 | 新协议 | 改进幅度 |
|---------|-------|-------|----------|
| 协议数量 | 55个 | 34个 | 减少38% |
| 训练启动RTT | 5次往返 | 1次往返 | 减少80% |
| 状态查询协议 | 8个 | 2个 | 减少75% |
| 聚合流程步骤 | 6个独立协议 | 隐含在轮次中 | 简化100% |
| 任务并发支持 | 不支持 | 完全支持 | 新增功能 |

### 6.2 架构优势

#### 简化的通信流程
```
旧协议 (5步训练启动):
TRAINING_START → ACK → COMMAND → RESPONSE → RESPONSE_ACK

新协议 (1步任务启动):
FEDERATED_TASK_START → FEDERATED_TASK_START_ACK
```

#### 统一的状态监控
```
旧协议 (分散的查询):
STATUS_QUERY, BATCH_STATUS_QUERY, TRAINING_PROGRESS等

新协议 (双层监控):
VM_STATUS_QUERY: 虚拟机整体+多任务概览
FEDERATED_TASK_STATUS_QUERY: 特定任务详情
```

#### 高效的模型传输
```
旧协议:
MODEL_UPLOAD (完整模型，数据量大)

新协议:
GRADIENT_UPLOAD (增量梯度，数据量小，支持压缩)
```

### 6.3 设计理念变化

- **从复杂到简洁**: 消除冗余协议，提高通信效率
- **从单任务到多任务**: 支持VM并发执行多个联邦学习任务
- **从分散到统一**: 集中化的任务生命周期管理
- **从协商到配置**: 将耗时的协商过程简化为高效的配置下发
- **从完整到增量**: 模型传输从完整模型改为梯度增量传输

### 6.4 新协议的技术优势

#### 多任务并发支持
- **taskId字段**: 所有协议消息支持任务级别控制
- **资源隔离**: VM可同时运行多个联邦学习任务
- **状态独立**: 每个任务状态独立管理和监控

#### 完整的任务生命周期
- **START**: 创建并启动联邦学习任务
- **STOP**: 停止任务执行，保留数据和状态
- **RESUME**: 从停止状态恢复执行
- **DELETE**: 彻底删除任务，释放所有资源

#### 高效的模型传输
- **增量传输**: GRADIENT_UPLOAD只传输梯度增量
- **压缩支持**: 内置gzip压缩减少网络开销
- **广播机制**: GLOBAL_MODEL_BROADCAST支持一对多高效分发

#### 双层状态监控
- **VM层监控**: 整体资源和所有任务概览
- **任务层监控**: 特定任务的详细执行状态
- **实时同步**: 心跳机制集成状态上报

### 6.5 移除协议的合理性验证

每个被移除的协议都经过以下验证：
1. **功能覆盖性**: 确认新协议能完全覆盖原功能
2. **性能影响**: 验证移除后不会降低系统性能
3. **复杂度评估**: 确认简化后不会增加实现难度
4. **兼容性分析**: 评估对现有系统的影响程度

通过v1.4协议优化，系统在保持功能完整性的同时，大幅提升了效率和可维护性。

## 7. 移除的辅助功能

### 7.1 移除的通知消息 (非核心协议)

虽然新协议保留了核心的双向通信协议，但移除了一些单向通知消息，这些功能通过核心协议的组合使用来实现：

#### 原有通知消息 (已简化集成到核心协议)
```
❌ 已移除的独立通知消息:
STRATEGY_SWITCH_NOTIFICATION  → 通过FEDERATED_TASK配置变更实现
AGGREGATION_NOTIFICATION     → 隐含在ROUND_START/COMPLETE中
TRAINING_PROGRESS_UPDATE     → 通过HEARTBEAT机制实现
```

### 7.2 移除的配置端点

#### WebSocket端点简化 (v1.4.2优化)
```java
// ❌ 已移除的SockJS支持
.withSockJS()

// ❌ 已移除的双端点配置
registry.addEndpoint("/ws-native");

// ✅ 统一的原生WebSocket端点
registry.addEndpoint("/ws");
```

**移除原因**:
- SockJS与StandardWebSocketClient不兼容
- 现代环境原生支持WebSocket
- 统一端点简化配置和维护

### 7.3 移除的冗余字段

#### 消息格式简化
某些消息字段在新协议中被简化或合并：

```json
// ❌ 旧协议的冗余字段
{
  "type": "TRAINING_START",
  "federatedAlgorithm": "FEDERATED_AVERAGING",  // 冗余
  "aggregationMethod": "WEIGHTED_AVERAGE",      // 冗余
  "trainingConfig": { /* 分散的配置 */ },        // 分散
  "globalModel": { /* 独立的模型传输 */ }        // 独立
}

// ✅ 新协议的统一配置
{
  "type": "FEDERATED_TASK_START",
  "data": {
    "federatedAlgorithm": "FEDERATED_AVERAGING",
    "config": {
      "aggregationMethod": "FEDERATED_AVERAGING",  // 统一
      "minParticipants": 2,
      "timeout": 1800
    },
    "localTrainingConfig": { /* 完整配置 */ },     // 集中
    "initialGlobalModel": { /* 初始模型 */ }       // 集成
  }
}
```

## 8. 总结

### 8.1 移除协议统计

v1.4协议优化通过移除21个冗余和重复的协议消息，实现了：
- **协议数量减少38%** (55→34个)
- **网络往返减少80%** (训练启动从5次RTT降至1次)
- **状态查询协议减少75%** (8→2个)
- **实现复杂度显著降低**
- **新增多任务并发支持**

### 8.2 设计理念的根本转变

从**过度设计的复杂协议**转向**精简高效的核心协议**：
- ✅ 保留必要的双向通信协议
- ❌ 移除冗余的中间步骤协议
- ✅ 统一的状态监控机制
- ❌ 移除分散的查询协议
- ✅ 集成化的任务配置
- ❌ 移除复杂的协商流程

### 8.3 对开发者的影响

**正面影响**:
- 更少的协议消息需要实现和维护
- 更清晰的协议语义和流程
- 更高的网络传输效率
- 更强的多任务支持能力

**迁移成本**:
- 需要重构基于移除协议的代码
- 需要更新测试用例和文档
- 需要调整消息处理逻辑

v1.4协议的移除决策基于对联邦学习系统实际需求的深入分析，确保在提升效率的同时保持功能的完整性。

## 9. 相关文档

- [WebSocket 协议文档 v1.4 - 中心化简化版](../WebSocket协议文档-中心化-简化.md)
- [修改接口说明 v1.4](../modified/modified-interfaces-v1.4.md)
- [WebSocket 协议文档 v1.3 (已废弃)](../不启用/WebSocket协议文档-中心化实现（已废弃）.md)

---

**文档版本**: v1.4.0
**最后更新**: 2024-01-01
**维护者**: FedUWAComm开发团队