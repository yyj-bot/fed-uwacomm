# 后端联邦学习完整流程分析报告

> **文档版本**: v1.0
> **创建日期**: 2025-10-11
> **适用协议**: v1.4 / v1.5 / v1.5.1
> **作者**: 系统架构团队

---

## 目录
- [一、整体架构概览](#一整体架构概览)
- [二、完整的联邦学习流程](#二完整的联邦学习流程)
- [三、关键类与方法映射表](#三关键类与方法映射表)
- [四、数据流向图](#四数据流向图)
- [五、关键数据结构汇总](#五关键数据结构汇总)
- [六、并发控制与同步机制](#六并发控制与同步机制)
- [七、协议版本与兼容性](#七协议版本与兼容性)
- [八、错误处理与重试机制](#八错误处理与重试机制)

---

## 一、整体架构概览

### 1.1 核心模块划分
```
FedUWAComm后端采用多层架构：
├── Controller层 - RESTful API接口
│   └── FederatedTaskController: 任务管理API
├── Service层 - 业务逻辑
│   ├── FederatedTaskService: 任务生命周期管理
│   ├── FederatedOrchestrationService: 工作流编排
│   ├── WebSocketProtocolService: WebSocket协议处理
│   ├── DataDistributionService: 数据分发
│   └── UniversalAggregationEngine: 通用聚合引擎
├── Mapper层 - 数据访问
│   ├── FederatedTasksMapper: 任务数据访问
│   ├── TaskParticipantsMapper: 参与者数据
│   ├── GlobalModelMapper: 全局模型数据
│   └── VmRoundModelsMapper: VM轮次模型数据
└── Entity层 - 数据模型
    ├── FederatedTask: 联邦学习任务
    ├── TaskParticipant: 任务参与者
    ├── GlobalModel: 全局聚合模型
    └── VmRoundModel: VM轮次训练模型
```

---

## 二、完整的联邦学习流程（13步标准化流程）

### 阶段1️⃣：任务创建与配置（v1.3/v1.5协议）

#### 入口API
- **接口**: `POST /api/federated/tasks`
- **控制器**: `FederatedTaskController.createTask(TaskCreateDTO createDTO)`
  - 位置: `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/controller/FederatedTaskController.java:270-303`

#### 核心流程
```java
// FederatedTaskServiceImpl.createSmartTask()
// 位置: backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedTaskServiceImpl.java:104-157

1. 验证任务配置 (isValidTaskConfig)
2. 生成任务ID (UuidUtil.generateUuid())
3. 构建FederatedTask实体
   - taskId, taskName, taskType
   - algorithm (FederatedAlgorithm枚举)
   - status = CREATED
   - datasetId, distributionStrategy
   - totalRounds, learningRate, batchSize等超参数
4. 插入任务记录到federated_tasks表
5. 创建TaskParticipant记录（每个VM一条）
   - taskId, vmId, role, status=CONNECTED
   - assignedDatasetId (v1.5新增，初始为null)
   - dataRatio (数据分配权重)
6. 记录系统日志
7. 返回TaskOperationVO (包含taskId, status=CREATED)
```

#### 关键数据结构
```java
// FederatedTask实体
// 位置: backend-springboot/feduwacomm-pojo/src/main/java/com/feduwacomm/entity/FederatedTask.java
public class FederatedTask {
    String id;                      // 任务ID (32位UUID)
    String taskName;               // 任务名称
    FederatedAlgorithm algorithm;  // 联邦算法 (FEDERATED_AVERAGING, FEDERATED_PROXIMAL等)
    FederatedTaskStatus status;    // 任务状态 (CREATED→CONFIGURED→RUNNING→COMPLETED)

    // 超参数配置
    Double learningRate;
    Integer batchSize;
    Integer epochs;
    Integer totalRounds;           // 总轮次数
    Integer currentRound;          // 当前轮次

    // v1.3/v1.5新增
    String datasetId;              // 数据集ID
    String distributionStrategy;   // 分配策略 (IID/NON_IID)

    // 模型配置
    String modelType;              // 模型类型 (RANDOM_FOREST/NEURAL_NETWORK)
    String featureColumns;         // 特征列
    String targetColumn;           // 目标列

    // 配置JSON
    String config;                 // 完整配置JSON
    String finalResults;           // 最终结果JSON
}

// TaskParticipant实体
// 位置: backend-springboot/feduwacomm-pojo/src/main/java/com/feduwacomm/entity/TaskParticipant.java
public class TaskParticipant {
    String id;
    String taskId;
    String vmId;
    ParticipantRole role;          // PARTICIPANT
    ParticipantStatus status;      // CONNECTED→TRAINING→COMPLETED
    String dataSource;

    // v1.5数据集分配
    String assignedDatasetId;      // 分配给该VM的数据集ID
    String datasetStatus;          // PENDING→CREATED→UPLOADING→COMPLETED
    Integer dataRatio;             // 数据分配千分比权重 (和为1000)

    // 训练状态
    Integer currentEpoch;
    Double loss;
    Double accuracy;
    LocalDateTime lastHeartbeat;
}
```

---

### 阶段2️⃣：任务启动与数据分发（v1.5.1增强）

#### 入口API
- **接口**: `POST /api/federated/tasks/{taskId}/start`
- **控制器**: `FederatedTaskController.startTask(String taskId)`
  - 位置: `FederatedTaskController.java:335-353`

#### 核心流程
```java
// FederatedTaskServiceImpl.startTask()
// 位置: FederatedTaskServiceImpl.java:198-282

1. 检查任务状态 (必须是CREATED或CONFIGURED)
2. 更新任务状态为RUNNING
3. 发布FederatedTaskStartedEvent事件
   - 包含: taskId, datasetId, participantVmIds, distributionStrategy
4. 事件监听器触发数据分发流程
5. 更新所有参与者状态为CONNECTED
6. 发送训练启动指令给所有VM (通过WebSocket)
```

#### 数据分发事件处理
```java
// FederatedOrchestrationServiceImpl.onTaskStarted()
// 位置: backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedOrchestrationServiceImpl.java:117-141

监听FederatedTaskStartedEvent → 创建工作流编排
→ 执行WorkflowStage.DATA_DISTRIBUTION阶段

// DataDistributionService.distributeDatasetWithSlice() (v1.5.1)
1. 根据distributionStrategy调用DataSlicingService
2. 为每个VM生成DataSlice (包含SliceInfo)
   - SliceInfo包含: sliceStartIndex, sliceEndIndex, totalDataSize
3. 通过WebSocket发送DATASET_CREATE消息给每个VM
   - 包含assignedDatasetId和SliceInfo
4. 分批发送DATASET_APPEND_ROWS消息 (包含实际数据行)
5. 发送DATASET_COMPLETE消息标记数据集完成
6. 更新TaskParticipant.assignedDatasetId和datasetStatus
```

#### 数据流向
```
MySQL数据库 (training_dataset表)
  ↓ 查询原始数据集
DataSlicingService (数据切片)
  ↓ 根据IID/NON_IID策略切片
DataDistributionService (数据分发)
  ↓ 生成assignedDatasetId
WebSocketProtocolService (WebSocket协议)
  ↓ 发送DATASET_CREATE, DATASET_APPEND_ROWS, DATASET_COMPLETE
Python VM (虚拟机)
  ↓ 接收并存储本地数据集
TaskParticipant表 (更新assignedDatasetId和datasetStatus)
```

---

### 阶段3️⃣：WebSocket连接建立与心跳

#### WebSocket协议处理
```java
// WebSocketProtocolService.handle(ProtocolMessage msg)
// 位置: backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/WebSocketProtocolService.java:127-237

处理v1.4协议消息类型：

1. CONNECT (VM连接请求)
   → onConnect() → 验证vmId → 返回CONNECT_ACK

2. HEARTBEAT (心跳消息)
   → onHeartbeat() → 更新lastHeartbeat时间戳 → 返回HEARTBEAT_ACK

3. VM_STATUS_QUERY (状态查询)
   → onVmStatusQuery() → 返回VM当前状态

4. VM_STATUS_RESPONSE (状态响应)
   → onVmStatusResponse() → 更新statusCache缓存
```

#### 数据结构
```java
// ProtocolMessage (WebSocket消息基类)
public class ProtocolMessage {
    ProtocolType type;        // 消息类型 (CONNECT, HEARTBEAT, ROUND_START等)
    String id;                // 消息ID
    String vmId;              // 虚拟机ID
    Instant timestamp;        // 时间戳
    Map<String, Object> data; // 消息数据载荷
    String signature;         // 数字签名
}

// ProtocolAck (应答消息)
public class ProtocolAck {
    ProtocolType type;        // ACK类型
    String id;
    String vmId;
    Instant timestamp;
    Map<String, Object> data;
    String signature;
}
```

---

### 阶段4️⃣：初始模型生成与分发

#### 初始模型生成
```java
// InitialModelGenerationService.generateInitialModel()
1. 根据taskConfig解析模型类型 (RANDOM_FOREST/NEURAL_NETWORK)
2. 调用SklearnModelParameterGenerator生成初始参数
   - RandomForest: n_estimators, max_depth, feature_importances_
   - NeuralNetwork: weights, biases, layers配置
3. 创建InitialModel记录
   - taskId, modelType, parameters (JSON格式)
4. 发布InitialModelGeneratedEvent事件
```

#### 模型分发流程
```java
// GlobalModelDistributionService.distributeModel()
1. 获取初始模型参数
2. 构建ProtocolMessage (type=GLOBAL_MODEL_BROADCAST)
   data包含:
   - taskId
   - roundNumber (初始为0)
   - modelParameters (JSON序列化的参数)
   - modelType
3. 通过WebSocket向所有参与者VM广播
4. 记录ModelDistribution分发记录
   - taskId, vmId, roundNumber, status=DISTRIBUTING
5. 等待所有VM返回GLOBAL_MODEL_BROADCAST_ACK
6. 更新分发状态为DISTRIBUTED
```

---

### 阶段5️⃣：联邦训练轮次执行（核心流程）

#### 轮次状态管理
```java
// RoundStateManager (轮次状态管理器)
管理每个任务的轮次状态:
- RoundState枚举: INITIALIZING → TRAINING → AGGREGATING → DISTRIBUTING → COMPLETED

getCurrentRoundState(taskId) → 获取当前轮次状态
advanceRound(taskId) → 推进到下一轮次
createRoundModel(taskId, roundNumber) → 创建轮次全局模型记录
```

#### 训练启动流程
```java
// WebSocketProtocolService.onFederatedTaskStart()
// 位置: WebSocketProtocolService.java:488-503

1. 接收FEDERATED_TASK_START消息
2. 提取taskId和训练配置
3. 返回FEDERATED_TASK_START_ACK
4. 更新任务状态为RUNNING
5. 初始化轮次状态为TRAINING

// Python VM端收到后:
1. 加载assignedDatasetId对应的本地数据集
2. 使用初始模型参数初始化本地模型
3. 开始第1轮本地训练
```

#### 本地训练与梯度上传
```java
// WebSocketProtocolService处理模型上传
实际使用GRADIENT_UPLOAD消息:

1. VM完成本地训练后，发送包含模型参数的消息
   ProtocolMessage {
       type: GRADIENT_UPLOAD,
       vmId: "vm_xxx",
       data: {
           taskId: "task_xxx",
           roundNumber: 1,
           parameters: "{...}", // 模型参数JSON
           accuracy: 0.85,
           loss: 0.15,
           samplesCount: 1000
       }
   }

2. 后端处理:
   - 解析模型参数JSON
   - 创建VmRoundModel记录
   - 更新TaskParticipant状态为TRAINING
   - 发布ModelUploadEvent事件

3. 检查所有VM是否完成本地训练
   - 使用VmAckTracker.waitForAllAcknowledgments()
   - 等待所有VM的GRADIENT_UPLOAD消息
```

#### 关键数据结构
```java
// VmRoundModel (VM轮次训练模型)
// 位置: backend-springboot/feduwacomm-pojo/src/main/java/com/feduwacomm/entity/VmRoundModel.java
public class VmRoundModel {
    String id;
    String taskId;
    String vmId;
    Integer roundNumber;       // 训练轮次
    BigDecimal accuracy;       // 准确率
    BigDecimal loss;           // 损失值
    String parameters;         // 模型参数JSON
    LocalDateTime createdAt;
}

// 参数JSON示例 (RandomForest)
{
    "model_parameters": {
        "n_estimators": 100,
        "feature_importances_": [0.3, 0.2, 0.15, ...],
        "tree_depths": [12, 15, 10, ...]
    },
    "training_metadata": {
        "samples_count": 1000,
        "algorithm": "RandomForest",
        "training_duration": 45.2
    }
}
```

---

### 阶段6️⃣：模型聚合（核心算法）

#### 聚合触发条件
```java
// 当满足以下条件时触发聚合:
1. 所有参与者VM都完成了当前轮次训练
2. VmAckTracker确认所有GRADIENT_UPLOAD消息已收到
3. 轮次状态为TRAINING → 转换为AGGREGATING
```

#### 聚合引擎流程
```java
// UniversalAggregationEngine.aggregate()
// 位置: backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/aggregation/UniversalAggregationEngine.java:84-135

1. 输入: List<VmRoundModel> models (所有VM的轮次模型)
        FederatedAlgorithm algorithm (聚合算法)
        Map<String, Object> taskConfig (任务配置)

2. 模型类型检测:
   - detectModelType() → 解析parameters JSON
   - 检查是否为RandomForest或NeuralNetwork

3. 模型一致性验证:
   - validateModelConsistency() → 确保所有模型类型一致

4. 获取聚合策略:
   - AggregationStrategyFactory.getStrategy(algorithm)
   - 支持策略: FedAvg, FedProx, FedNova, Scaffold

5. 执行聚合:
   - strategy.aggregate(models, taskConfig)

6. 计算全局指标:
   - calculateGlobalMetrics() → 平均准确率、平均损失、样本总数

7. 返回AggregationResult:
   - globalParameters (聚合后的模型参数)
   - globalMetrics (全局性能指标)
   - participantCount
   - aggregationDuration
```

#### 聚合策略示例：FedAvg
```java
// FedAvgStrategy.aggregate()
1. 计算每个VM的权重 (基于样本数量)
   weight_i = samples_i / total_samples

2. 加权平均聚合参数:
   global_param = Σ (weight_i × local_param_i)

3. 对于RandomForest:
   - 聚合feature_importances_
   - 平均tree_depths

4. 对于NeuralNetwork:
   - 逐层聚合weights和biases矩阵
```

#### 聚合结果存储
```java
// 创建GlobalModel记录
// 位置: backend-springboot/feduwacomm-pojo/src/main/java/com/feduwacomm/entity/GlobalModel.java
GlobalModel {
    id: "gm_xxx",
    taskId: "task_xxx",
    roundNumber: 1,
    aggregationMethod: FEDERATED_AVERAGING,
    globalParameters: "{...}", // 聚合后的全局参数JSON
    globalLoss: 0.12,
    globalAccuracy: 0.88,
    participantCount: 3,
    aggregationDuration: 150, // 毫秒
    status: COMPLETED,
    completedAt: LocalDateTime.now()
}

// 插入到global_models表
GlobalModelMapper.insert(globalModel)
```

---

### 阶段7️⃣：全局模型分发（广播到所有VM）

#### 分发流程
```java
// WebSocketProtocolService.notifyRoundComplete()
// 位置: WebSocketProtocolService.java:760-781
或使用WebSocketMessageSender.sendGlobalModel()

1. 构建GLOBAL_MODEL_BROADCAST消息:
   ProtocolMessage {
       type: GLOBAL_MODEL_BROADCAST,
       data: {
           taskId: "task_xxx",
           roundNumber: 1,
           modelParameters: "{...}", // 全局参数JSON
           globalAccuracy: 0.88,
           globalLoss: 0.12,
           nextRoundNumber: 2
       }
   }

2. 广播给所有参与者VM:
   messagingTemplate.convertAndSend("/topic/vm/{vmId}", message)

3. 记录ModelDistribution:
   - taskId, vmId, roundNumber, status=DISTRIBUTING

4. 等待所有VM返回GLOBAL_MODEL_BROADCAST_ACK:
   VmAckTracker.waitForAllAcknowledgments(taskId, roundNumber)

5. 确认分发完成:
   - 更新ModelDistribution.status = DISTRIBUTED
   - 更新GlobalModel.distributionStatus = DISTRIBUTED
```

#### VM端处理
```java
// Python VM收到GLOBAL_MODEL_BROADCAST后:
1. 解析全局模型参数
2. 更新本地模型 (替换为全局聚合参数)
3. 返回GLOBAL_MODEL_BROADCAST_ACK
4. 准备开始下一轮训练
```

---

### 阶段8️⃣：轮次推进与迭代

#### 轮次推进检查
```java
// WebSocketProtocolService.checkAndAdvanceTaskRound()
// 位置: WebSocketProtocolService.java:398-485

触发条件: 所有VM确认收到全局模型

1. 获取轮次锁: roundLockManager.acquireRoundLock(taskId)
2. 检查当前轮次状态: roundStateManager.getCurrentRoundState()
3. 统计完成参与者数量:
   completedCount = taskParticipantsMapper.countCompletedParticipants(taskId, currentRound)
   totalCount = taskParticipantsMapper.countTotalParticipants(taskId)

4. 如果所有参与者完成 (completedCount == totalCount):
   a. 推进轮次: roundStateManager.advanceRound(taskId)
   b. 更新任务: currentRound += 1
   c. 重置参与者状态: status=TRAINING for下一轮
   d. 创建新轮次模型记录: roundStateManager.createRoundModel(taskId, newRound)
   e. 发送ROUND_START消息通知所有VM开始新轮次训练

5. 释放锁: roundLockManager.releaseRoundLock(taskId)
```

#### 轮次消息流
```
Server → VM: ROUND_START {roundNumber: 2}
  ↓
VM → Server: ROUND_START_ACK {status: "READY"}
  ↓
VM进行本地训练 (epochs轮)
  ↓
VM → Server: GRADIENT_UPLOAD {parameters, accuracy, loss}
  ↓
Server聚合所有VM模型
  ↓
Server → VM: GLOBAL_MODEL_BROADCAST {globalParameters}
  ↓
VM → Server: GLOBAL_MODEL_BROADCAST_ACK
  ↓
重复直到 currentRound == totalRounds
```

---

### 阶段9️⃣：任务完成与结果收集

#### 完成条件判断
```java
// 当满足以下任一条件时任务完成:
1. currentRound >= totalRounds (达到预设总轮次)
2. 收敛条件满足 (全局损失变化 < threshold)
3. 手动停止任务
```

#### 完成流程
```java
// FederatedTaskServiceImpl.completeTask()
1. 更新任务状态: status = COMPLETED
2. 设置完成时间: completedAt = LocalDateTime.now()
3. 收集最终结果:
   - 获取最后一轮GlobalModel
   - 提取globalAccuracy, globalLoss
   - 统计总训练时间

4. 生成结果报告:
   finalResults = {
       "finalRound": totalRounds,
       "finalAccuracy": 0.92,
       "finalLoss": 0.08,
       "totalDuration": "PT2H30M",
       "participantCount": 3,
       "modelType": "RANDOM_FOREST",
       "algorithm": "FEDERATED_AVERAGING"
   }

5. 更新FederatedTask.finalResults
6. 更新所有TaskParticipant.status = COMPLETED
7. 发布FederatedTaskCompletedEvent事件
8. 记录完成日志
```

#### 结果查询API
```java
// FederatedTaskController.getTaskResult()
// 位置: FederatedTaskController.java:542-560
GET /api/federated/tasks/{taskId}/results

返回TaskResultVO包含:
- 任务基本信息
- 最终准确率和损失
- 所有轮次的GlobalModel列表
- 参与者性能统计
- 训练曲线数据 (accuracy_curve, loss_curve)
```

---

## 三、关键类与方法映射表

### 3.1 Controller层
| 类名 | 位置 | 核心方法 |
|------|------|---------|
| FederatedTaskController | feduwacomm-server/controller/FederatedTaskController.java | createTask() (Line 270)<br>startTask() (Line 335)<br>getTaskDetail() (Line 467)<br>getTaskResult() (Line 542) |

### 3.2 Service层
| 类名 | 位置 | 核心方法 |
|------|------|---------|
| FederatedTaskServiceImpl | feduwacomm-server/service/impl/FederatedTaskServiceImpl.java | createSmartTask() (Line 104)<br>startTask() (Line 198)<br>pauseTask() (Line 284)<br>stopTask() |
| FederatedOrchestrationServiceImpl | feduwacomm-server/service/impl/FederatedOrchestrationServiceImpl.java | createWorkflow() (Line 145)<br>startWorkflowExecution() (Line 166)<br>onTaskStarted() (Line 117) |
| WebSocketProtocolService | feduwacomm-server/service/WebSocketProtocolService.java | handle() (Line 127)<br>onConnect() <br>onGradientUpload()<br>checkAndAdvanceTaskRound() (Line 398) |
| UniversalAggregationEngine | feduwacomm-server/aggregation/UniversalAggregationEngine.java | aggregate() (Line 84)<br>detectModelType() (Line 141)<br>calculateGlobalMetrics() (Line 243) |
| DataDistributionService | feduwacomm-server/service/DataDistributionService.java | distributeDatasetWithSlice()<br>generateDatasetSlice()<br>performIIDAllocation() |

### 3.3 核心工具类
| 类名 | 位置 | 功能 |
|------|------|------|
| RoundStateManager | feduwacomm-server/service/RoundStateManager.java | 轮次状态管理<br>advanceRound()<br>getCurrentRoundState() |
| VmAckTracker | feduwacomm-server/service/VmAckTracker.java | VM确认跟踪<br>waitForAllAcknowledgments()<br>recordAck() |
| RoundLockManager | feduwacomm-server/service/RoundLockManager.java | 轮次锁管理<br>acquireRoundLock()<br>releaseRoundLock() |
| MessageBuilder | feduwacomm-common/utils/MessageBuilder.java | WebSocket消息构建 |

---

## 四、数据流向图

```
用户请求 (HTTP POST)
    ↓
FederatedTaskController.createTask()
    ↓
FederatedTaskServiceImpl.createSmartTask()
    ↓
FederatedTasksMapper.insertTask()
    ↓
MySQL数据库 (federated_tasks表)
    ↓
返回TaskOperationVO给前端

用户启动任务 (HTTP POST /tasks/{id}/start)
    ↓
FederatedTaskController.startTask()
    ↓
FederatedTaskServiceImpl.startTask()
    ↓  发布FederatedTaskStartedEvent
    ↓
FederatedOrchestrationServiceImpl监听事件
    ↓
创建OrchestrationWorkflow
    ↓
执行DATA_DISTRIBUTION阶段
    ↓
DataDistributionService.distributeDatasetWithSlice()
    ↓
查询training_dataset表获取原始数据
    ↓
DataSlicingService切片数据
    ↓
通过WebSocket发送DATASET_CREATE/APPEND_ROWS/COMPLETE
    ↓
Python VM接收数据并存储
    ↓
VM返回DATASET_COMPLETE_ACK
    ↓
更新TaskParticipant.assignedDatasetId和datasetStatus
    ↓
发送FEDERATED_TASK_START消息
    ↓
VM开始本地训练
    ↓
VM完成训练后发送GRADIENT_UPLOAD
    ↓
后端创建VmRoundModel记录
    ↓
检查所有VM是否完成 (VmAckTracker)
    ↓
触发聚合: UniversalAggregationEngine.aggregate()
    ↓
读取所有VmRoundModel → 执行FedAvg/FedProx等算法 → 生成全局参数
    ↓
创建GlobalModel记录
    ↓
广播GLOBAL_MODEL_BROADCAST给所有VM
    ↓
VM更新本地模型并返回ACK
    ↓
检查轮次推进条件: checkAndAdvanceTaskRound()
    ↓
推进到下一轮次 (currentRound++)
    ↓
重复训练-聚合-分发循环
    ↓
达到totalRounds → 任务完成
    ↓
生成finalResults并更新任务状态为COMPLETED
    ↓
用户查询结果: GET /tasks/{id}/results
```

---

## 五、关键数据结构汇总

### 5.1 核心实体类

| 实体类 | 表名 | 核心字段 |
|--------|------|---------|
| FederatedTask | federated_tasks | id, taskName, algorithm, status, totalRounds, currentRound, datasetId, distributionStrategy, finalResults |
| TaskParticipant | task_participants | id, taskId, vmId, role, status, assignedDatasetId, datasetStatus, currentEpoch, accuracy, loss |
| GlobalModel | global_models | id, taskId, roundNumber, globalParameters, globalAccuracy, globalLoss, participantCount, aggregationMethod |
| VmRoundModel | vm_round_models | id, taskId, vmId, roundNumber, parameters, accuracy, loss |
| OrchestrationWorkflow | orchestration_workflows | id, taskId, status, currentStage |

### 5.2 枚举类型

| 枚举类 | 值 | 说明 |
|--------|---|------|
| FederatedTaskStatus | CREATED, CONFIGURED, RUNNING, PAUSED, STOPPED, COMPLETED, FAILED, CANCELLED | 任务状态 |
| FederatedAlgorithm | FEDERATED_AVERAGING, FEDERATED_PROXIMAL, FEDERATED_NOVA, SCAFFOLD | 联邦算法 |
| ParticipantStatus | CONNECTED, DISCONNECTED, TRAINING, COMPLETED, FAILED | 参与者状态 |
| RoundState | INITIALIZING, TRAINING, AGGREGATING, DISTRIBUTING, COMPLETED | 轮次状态 |
| WorkflowStage | INITIALIZATION, DATA_DISTRIBUTION, INITIAL_MODEL_GENERATION, MODEL_DISTRIBUTION, FEDERATED_TRAINING, FINAL_AGGREGATION | 工作流阶段 |
| ProtocolType | CONNECT, HEARTBEAT, DATASET_CREATE, FEDERATED_TASK_START, GRADIENT_UPLOAD, GLOBAL_MODEL_BROADCAST, ROUND_START, ROUND_COMPLETE | WebSocket消息类型 |

---

## 六、并发控制与同步机制

### 6.1 轮次锁机制
```java
// RoundLockManager防止并发推进轮次
private final ConcurrentHashMap<String, ReentrantLock> roundLocks;

acquireRoundLock(taskId) → 获取锁
releaseRoundLock(taskId) → 释放锁
```

### 6.2 VM确认跟踪
```java
// VmAckTracker跟踪VM确认状态
waitForAllAcknowledgments(taskId, roundNumber, timeout)
→ 使用CountDownLatch等待所有VM确认
→ 超时机制防止永久阻塞
```

### 6.3 缓存机制
```java
// MetricsCacheService缓存训练指标
缓存层级:
- ParticipantMetrics (每个VM的指标)
- GlobalMetrics (全局聚合指标)

避免重复查询数据库提升性能
```

---

## 七、协议版本与兼容性

### 当前支持协议
- **v1.4协议**: 核心WebSocket消息流 (CONNECT, HEARTBEAT, DATASET管理, 轮次管理)
- **v1.5协议**: 增强数据分配 (assignedDatasetId支持)
- **v1.5.1协议**: 数据切片增强 (SliceInfo双重索引)

### 协议兼容性检查
```java
// WebSocketProtocolService.validateProtocolCompliance()
检查客户端消息是否符合服务端只允许发送的消息类型
拒绝客户端发送仅限服务端的消息 (如GLOBAL_MODEL_BROADCAST)
```

---

## 八、错误处理与重试机制

### 错误处理
```java
// 各Service层统一异常处理
try-catch捕获异常 → 记录日志 → 返回Result.error()
WebSocket消息错误 → 返回MESSAGE_ERROR或CONNECTION_ERROR

// 任务失败处理
更新任务状态为FAILED
记录失败原因到finalResults
通知所有参与者VM停止训练
```

### 重试机制
```java
// RetryService提供指数退避重试
用于数据分发失败、模型聚合失败等场景
最大重试次数、重试间隔可配置
```

---

## 附录：参考文档

1. [WebSocket协议文档](../api/WebSocket/WebSocket消息格式定义.md)
2. [联邦任务API参考](../api/HTTP/federated-task/federated-task-api-reference.md)
3. [数据分发API参考](../api/HTTP/train-data/data-distribution-api-reference.md)
4. [梯度聚合指南](./gradient-aggregation-guide.md)

---

**文档维护**: 本文档应随代码更新同步维护，确保流程描述的准确性。
