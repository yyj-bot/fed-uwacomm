# Mock虚拟机重构指南 - WebSocket v1.5协议升级

## 📋 文档概览

**文档版本**: v1.5.0
**创建时间**: 2025-01-29
**适用范围**: MockVirtualMachine类重构为v1.5协议
**文件路径**: `/home/hlh/dev/fed-uwacomm/backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/mock/MockVirtualMachine.java`

## 🎯 重构目标

### 核心升级目标
将现有MockVirtualMachine从v1.4协议升级到v1.5协议，实现：
- **🆕 assignedDatasetId完全依赖**: 移除所有自主数据集ID生成，完全依赖后端分配
- **📋 13步流程支持**: 支持标准联邦学习13步流程，包含数据集验证阶段
- **💔 破坏性变更适应**: 完全移除对v1.4 datasetId的兼容性
- **🔄 新协议消息支持**: 实现DATASET_LIST_QUERY/RESPONSE协议

### v1.5架构变更
```java
// v1.4架构 ❌ (将被移除)
private String datasetId = generateDatasetId(); // 自主生成数据集ID

// v1.5架构 ✅ (新架构)
private String assignedDatasetId; // 完全依赖后端分配，dataConfig内部字段
private final Map<String, String> taskDatasetMappings = new ConcurrentHashMap<>(); // 任务-数据集映射
```

## 🔍 现状分析

### 当前v1.4实现问题

#### 1. 数据集ID管理不符合v1.5规范
```java
// 问题：MockVirtualMachine.java:2712 现有实现
case "INVALID_DATASET_ID":
    // v1.4使用自主生成的datasetId，与v1.5 assignedDatasetId不兼容

// 问题：FEDERATED_TASK_START处理
private void handleFederatedTaskStart(Map<String, Object> messageData) {
    // v1.4缺少对dataConfig.assignedDatasetId的解析
    // v1.4仍然期望datasetId在顶层，而非dataConfig内部
}
```

#### 2. 缺少DATASET_LIST协议支持
```java
// 问题：handleMessage方法缺少v1.5新协议
// 缺少：DATASET_LIST_QUERY处理
// 缺少：DATASET_LIST_RESPONSE发送
```

#### 3. GRADIENT_UPLOAD消息不符合v1.5规范
```java
// 问题：GRADIENT_UPLOAD构建 (line 1011)
Map<String, Object> gradientMessage = createProtocolMessage(ProtocolType.GRADIENT_UPLOAD);
// v1.4缺少assignedDatasetId字段，无法验证数据集关联
```

## 🏗️ v1.5重构设计

### 1. MockVirtualMachineV15类结构

```java
/**
 * 模拟虚拟机类 v1.5
 * 基于WebSocket协议v1.5的被动响应模式设计
 * 🆕 v1.5核心特性：assignedDatasetId完全依赖后端分配
 */
public class MockVirtualMachineV15 {

    // v1.5新增字段：数据集完全依赖管理
    private final Map<String, String> taskAssignedDatasetMappings = new ConcurrentHashMap<>();
    private final Map<String, DatasetStatus> datasetStatusMap = new ConcurrentHashMap<>();

    // v1.5移除字段（破坏性变更）
    // ❌ private String datasetId; // 完全移除自主数据集ID

    // v1.5新增：数据集状态枚举
    public enum DatasetStatus {
        PENDING, CREATED, UPLOADING, COMPLETED, FAILED
    }
}
```

### 2. v1.5协议消息处理架构

#### 📋 13步流程中的Mock VM职责
```java
// 步骤8: 接收FEDERATED_TASK_START (包含assignedDatasetId)
private void handleFederatedTaskStartV15(Map<String, Object> messageData) {
    // 1. 解析dataConfig内的assignedDatasetId
    Map<String, Object> dataConfig = extractDataConfig(messageData);
    String assignedDatasetId = (String) dataConfig.get("assignedDatasetId");

    // 2. 存储任务-数据集映射
    String taskId = extractTaskId(messageData);
    taskAssignedDatasetMappings.put(taskId, assignedDatasetId);

    // 3. 模拟数据集创建过程
    simulateDatasetCreation(assignedDatasetId);

    // 4. 发送FEDERATED_TASK_START_ACK
    sendFederatedTaskStartAckV15(taskId, "READY", assignedDatasetId);
}

// 步骤9: 响应DATASET_LIST_QUERY
private void handleDatasetListQueryV15(Map<String, Object> messageData) {
    String taskId = extractTaskId(messageData);
    String assignedDatasetId = taskAssignedDatasetMappings.get(taskId);

    // 构建数据集列表响应
    List<Map<String, Object>> datasets = Arrays.asList(
        createDatasetInfo(assignedDatasetId, DatasetStatus.CREATED)
    );

    sendDatasetListResponseV15(taskId, datasets);
}

// 步骤12+: 发送包含assignedDatasetId的GRADIENT_UPLOAD
private void sendGradientUploadV15(String taskId, int roundNumber) {
    String assignedDatasetId = taskAssignedDatasetMappings.get(taskId);
    if (assignedDatasetId == null) {
        throw new IllegalStateException("No assignedDatasetId found for task: " + taskId);
    }

    Map<String, Object> gradientMessage = createProtocolMessage(ProtocolType.GRADIENT_UPLOAD);
    Map<String, Object> data = new HashMap<>();
    data.put("taskId", taskId);
    data.put("roundNumber", roundNumber);
    data.put("assignedDatasetId", assignedDatasetId); // 🆕 v1.5新增字段
    data.put("gradientData", generateMockGradientData());
    data.put("trainingMetrics", generateTrainingMetrics(assignedDatasetId));

    gradientMessage.put("data", data);
    sendStompMessage(gradientMessage);
}
```

### 3. v1.5协议消息构建

#### DATASET_LIST_RESPONSE消息构建
```java
private void sendDatasetListResponseV15(String taskId, List<Map<String, Object>> datasets) {
    Map<String, Object> response = createProtocolMessage(ProtocolType.DATASET_LIST_RESPONSE);
    Map<String, Object> data = new HashMap<>();
    data.put("taskId", taskId);
    data.put("datasets", datasets);
    data.put("totalCount", datasets.size());

    response.put("data", data);
    sendStompMessage(response);

    log.info("🗂️ [{}] 发送DATASET_LIST_RESPONSE: taskId={}, datasets={}",
             vmData.getName(), taskId, datasets.size());
}

private Map<String, Object> createDatasetInfo(String assignedDatasetId, DatasetStatus status) {
    Map<String, Object> datasetInfo = new HashMap<>();
    datasetInfo.put("assignedDatasetId", assignedDatasetId);
    datasetInfo.put("status", status.name());
    datasetInfo.put("localPath", "/data/assigned/" + assignedDatasetId);
    datasetInfo.put("createdAt", Instant.now().toString());
    return datasetInfo;
}
```

#### v1.5 FEDERATED_TASK_START_ACK响应
```java
private void sendFederatedTaskStartAckV15(String taskId, String status, String assignedDatasetId) {
    Map<String, Object> ackMessage = createProtocolMessage(ProtocolType.FEDERATED_TASK_START_ACK);
    Map<String, Object> data = new HashMap<>();
    data.put("vmId", vmData.getVmId());
    data.put("taskId", taskId);
    data.put("status", status);
    data.put("assignedDatasetId", assignedDatasetId); // 🆕 v1.5: 确认收到的数据集ID
    data.put("readyTime", Instant.now().toString());

    // v1.5新增：数据集确认信息
    Map<String, Object> datasetConfirmation = new HashMap<>();
    datasetConfirmation.put("assignedDatasetId", assignedDatasetId);
    datasetConfirmation.put("datasetStatus", "CREATED");
    datasetConfirmation.put("estimatedSamples", 1000); // 模拟数据样本数
    data.put("datasetConfirmation", datasetConfirmation);

    ackMessage.put("data", data);
    sendStompMessage(ackMessage);
}
```

## 🛠️ 重构实施计划

### 第一阶段：数据结构升级 ⚠️ (进度: 0%)
**预计耗时: 30分钟**

#### 任务1.1: 移除v1.4数据集管理 ❌
- [ ] 删除自主数据集ID生成逻辑
- [ ] 移除datasetId相关字段和方法
- [ ] 清理v1.4兼容性代码

```java
// 需要移除的代码示例
// ❌ 移除这些v1.4字段
private String currentDatasetId; // 删除
private Map<String, String> datasetIdMappings; // 删除

// ❌ 移除这些v1.4方法
private String generateDatasetId() { /* 删除整个方法 */ }
private void handleDatasetIdMapping() { /* 删除整个方法 */ }
```

#### 任务1.2: 新增v1.5数据集依赖架构 ✅
- [ ] 添加assignedDatasetId完全依赖字段
- [ ] 实现任务-数据集映射管理
- [ ] 添加数据集状态跟踪

```java
// 🆕 需要添加的v1.5架构
private final Map<String, String> taskAssignedDatasetMappings = new ConcurrentHashMap<>();
private final Map<String, DatasetStatus> assignedDatasetStatusMap = new ConcurrentHashMap<>();
private final Set<String> backendAssignedDatasetIds = new ConcurrentHashSet<>();
```

### 第二阶段：协议消息处理升级 ⚠️ (进度: 0%)
**预计耗时: 45分钟**

#### 任务2.1: 升级FEDERATED_TASK_START处理 ❌
更新位置: `MockVirtualMachine.java:1220, 1236, 2508`

```java
// 当前v1.4实现 (需要升级)
private void handleFederatedTaskStart(Map<String, Object> messageData) {
    // ❌ v1.4实现: 缺少dataConfig.assignedDatasetId解析
}

// 🆕 v1.5目标实现
private void handleFederatedTaskStartV15(Map<String, Object> messageData) {
    // ✅ 解析dataConfig内的assignedDatasetId
    Map<String, Object> data = (Map<String, Object>) messageData.get("data");
    Map<String, Object> dataConfig = (Map<String, Object>) data.get("dataConfig");
    String assignedDatasetId = (String) dataConfig.get("assignedDatasetId");

    // ✅ 验证assignedDatasetId存在
    if (assignedDatasetId == null || assignedDatasetId.trim().isEmpty()) {
        sendErrorResponse("MISSING_ASSIGNED_DATASET_ID", "dataConfig.assignedDatasetId is required in v1.5");
        return;
    }

    // ✅ 存储后端分配的数据集ID
    String taskId = (String) data.get("taskId");
    taskAssignedDatasetMappings.put(taskId, assignedDatasetId);
    backendAssignedDatasetIds.add(assignedDatasetId);

    // ✅ 模拟数据集创建过程
    simulateDatasetCreation(assignedDatasetId);

    // ✅ 发送v1.5确认消息
    sendFederatedTaskStartAckV15(taskId, "READY", assignedDatasetId);
}
```

#### 任务2.2: 实现DATASET_LIST协议支持 ❌
新增位置: `handleMessage方法`

```java
// 🆕 需要添加到handleMessage方法
} else if (isProtocolType(type, ProtocolType.DATASET_LIST_QUERY)) {
    System.out.println("📋 [v1.5] " + vmData.getName() + " 收到数据集列表查询");
    handleDatasetListQueryV15(messageData);
} else if (isProtocolType(type, ProtocolType.DATASET_LIST_RESPONSE)) {
    // 一般情况下虚拟机不应该收到DATASET_LIST_RESPONSE (这是虚拟机发送的)
    handleProtocolViolation(type, "DATASET_LIST_RESPONSE应该由虚拟机发送，不应该接收");
```

#### 任务2.3: 升级GRADIENT_UPLOAD消息构建 ❌
更新位置: `MockVirtualMachine.java:1011, 358, 852`

```java
// 当前v1.4实现 (需要升级)
Map<String, Object> gradientMessage = createProtocolMessage(ProtocolType.GRADIENT_UPLOAD);
Map<String, Object> data = new HashMap<>();
data.put("taskId", taskId);
data.put("roundNumber", round);
// ❌ v1.4缺少assignedDatasetId字段

// 🆕 v1.5升级后实现
Map<String, Object> gradientMessage = createProtocolMessage(ProtocolType.GRADIENT_UPLOAD);
Map<String, Object> data = new HashMap<>();
data.put("taskId", taskId);
data.put("roundNumber", round);
data.put("assignedDatasetId", taskAssignedDatasetMappings.get(taskId)); // 🆕 v1.5必需字段
data.put("gradientData", generateMockGradientData());
data.put("trainingMetrics", createTrainingMetrics(taskId));
```

### 第三阶段：Mock数据生成升级 ⚠️ (进度: 0%)
**预计耗时: 30分钟**

#### 任务3.1: v1.5数据集模拟器 ❌

```java
/**
 * 🆕 v1.5数据集创建模拟器
 * 模拟虚拟机接收到assignedDatasetId后的数据集创建过程
 */
private void simulateDatasetCreation(String assignedDatasetId) {
    // 模拟数据集状态转换: PENDING -> CREATED
    assignedDatasetStatusMap.put(assignedDatasetId, DatasetStatus.PENDING);

    // 模拟异步数据集创建过程
    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
        assignedDatasetStatusMap.put(assignedDatasetId, DatasetStatus.CREATED);
        log.info("🗂️ [{}] 模拟数据集创建完成: assignedDatasetId={}",
                 vmData.getName(), assignedDatasetId);
    });
}

/**
 * 🆕 v1.5训练指标生成器 (关联assignedDatasetId)
 */
private Map<String, Object> createTrainingMetrics(String taskId) {
    String assignedDatasetId = taskAssignedDatasetMappings.get(taskId);

    Map<String, Object> metrics = new HashMap<>();
    metrics.put("samplesCount", generateSampleCount(assignedDatasetId)); // 基于数据集ID生成样本数
    metrics.put("localLoss", 0.15 + Math.random() * 0.10); // 模拟损失值
    metrics.put("accuracy", 0.85 + Math.random() * 0.10); // 模拟准确率
    metrics.put("assignedDatasetId", assignedDatasetId); // 🆕 v1.5: 数据集关联验证

    return metrics;
}

private int generateSampleCount(String assignedDatasetId) {
    // 基于assignedDatasetId生成一致的样本数 (确保可重现性)
    return 800 + Math.abs(assignedDatasetId.hashCode() % 400); // 800-1200范围
}
```

#### 任务3.2: v1.5错误模拟增强 ❌

```java
/**
 * 🆕 v1.5数据集相关错误模拟
 */
private void simulateDatasetErrors(String assignedDatasetId) {
    // 模拟assignedDatasetId不存在错误
    if (Math.random() < 0.05) { // 5%概率
        sendErrorResponse("ASSIGNED_DATASET_NOT_FOUND",
                         "AssignedDatasetId not found: " + assignedDatasetId);
        return;
    }

    // 模拟数据集状态错误
    if (Math.random() < 0.03) { // 3%概率
        assignedDatasetStatusMap.put(assignedDatasetId, DatasetStatus.FAILED);
        sendErrorResponse("DATASET_CREATION_FAILED",
                         "Failed to create dataset: " + assignedDatasetId);
    }
}
```

### 第四阶段：集成测试适配 ⚠️ (进度: 0%)
**预计耗时: 30分钟**

#### 任务4.1: CompleteFederatedLearningFlowTest集成 ❌

```java
/**
 * 🆕 v1.5测试集成适配器
 * 确保MockVirtualMachineV15与CompleteFederatedLearningFlowTestV15兼容
 */
public void initializeForV15Testing() {
    // v1.5测试模式：禁用随机错误
    this.uploadFailureRate = 0.0;
    this.protocolViolationCount = 0;

    // v1.5测试模式：启用完整协议支持
    this.isPassiveMode = true;
    this.gradientUploadReady = true;

    log.info("🧪 [{}] MockVirtualMachine初始化为v1.5测试模式", vmData.getName());
}

/**
 * 🆕 v1.5测试验证方法
 */
public boolean verifyV15Compliance() {
    // 验证没有自主生成的数据集ID
    if (!backendAssignedDatasetIds.isEmpty() &&
        taskAssignedDatasetMappings.size() == backendAssignedDatasetIds.size()) {
        log.info("✅ [{}] v1.5合规性验证通过: 完全依赖后端分配的数据集ID", vmData.getName());
        return true;
    }

    log.error("❌ [{}] v1.5合规性验证失败: 检测到非后端分配的数据集ID", vmData.getName());
    return false;
}
```

## 🔄 迁移检查清单

### 💔 破坏性变更验证
- [ ] ✅ **移除datasetId**: 确保所有datasetId引用已移除
- [ ] ✅ **assignedDatasetId位置**: 验证从dataConfig内部读取assignedDatasetId
- [ ] ✅ **完全依赖验证**: 确保没有自主数据集ID生成代码
- [ ] ✅ **GRADIENT_UPLOAD升级**: 验证包含assignedDatasetId字段

### 🆕 v1.5新功能验证
- [ ] ✅ **DATASET_LIST_QUERY处理**: 实现查询响应逻辑
- [ ] ✅ **DATASET_LIST_RESPONSE发送**: 正确构建响应消息
- [ ] ✅ **13步流程支持**: 覆盖步骤8-9的数据集验证阶段
- [ ] ✅ **状态管理**: 实现DatasetStatus状态跟踪

### 🧪 测试兼容性验证
- [ ] ✅ **CompleteFederatedLearningFlowTestV15**: 确保测试集成无问题
- [ ] ✅ **协议一致性**: 与v1.5 WebSocket协议文档100%一致
- [ ] ✅ **错误处理**: v1.5特定错误的正确处理
- [ ] ✅ **性能测试**: 确保重构后性能不退化

## 📚 相关文档

- [WebSocket协议文档v1.5](../shared/api/WebSocket/WebSocket协议文档-中心化-简化.md)
- [v1.5修改接口文档](../shared/api/WebSocket/modified/modified-interfaces-v1.5.md)
- [联邦学习完整流程](../shared/api/WebSocket/example/联邦学习完整流程.md)
- [单元测试重构文档](./unit-test-refactoring.md)
- [后端服务重构文档](./backend-service-refactoring.md)

## ⚠️ 重要注意事项

### 数据集ID管理原则
1. **完全禁止自主生成**: MockVirtualMachine不得生成任何数据集ID
2. **强制后端依赖**: 所有assignedDatasetId必须来自FEDERATED_TASK_START的dataConfig
3. **状态一致性**: 数据集状态必须与后端保持同步
4. **ID验证**: 在所有数据集操作中验证assignedDatasetId的存在

### v1.5合规性要求
```java
// ✅ 正确的v1.5实现
String assignedDatasetId = extractFromDataConfig(messageData, "assignedDatasetId");
if (assignedDatasetId == null) {
    throw new IllegalStateException("v1.5 requires assignedDatasetId in dataConfig");
}

// ❌ 禁止的v1.4遗留代码
String datasetId = generateDatasetId(); // 完全禁止！
```

这份重构指南确保MockVirtualMachine完全符合v1.5协议的assignedDatasetId统一管理机制，实现虚拟机对后端数据集ID分配的完全依赖。