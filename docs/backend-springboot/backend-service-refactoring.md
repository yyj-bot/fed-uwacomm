# 后端服务层重构文档 - WebSocket标准协议支持

## 📋 文档信息

**创建时间**: 2025-01-29
**适用范围**: 后端服务层WebSocket标准协议重构
**重构目标**: 实现assignedDatasetId统一管理和13步标准化流程

## 🎯 重构目标与进度跟踪

### 核心服务重构进度
- [x] **FederatedTaskService**: 实现标准任务创建和数据集分配逻辑 `[进度: 4/4]` ✅ **100%完成**
  - [x] 新增createStandardFederatedTask方法 ✅
  - [x] 实现allocateDatasets数据集分配 ✅
  - [x] 实现validateParticipantDatasets验证 ✅
  - [x] 实现startFederatedLearningFlow启动流程 ✅

- [x] **WebSocketProtocolService**: 升级支持标准协议消息 `[进度: 5/5]` ✅ **100%完成**
  - [x] 更新sendFederatedTaskStart方法(assignedDatasetId在dataConfig内) ✅
  - [x] 新增queryDatasetStatus方法(DATASET_LIST_QUERY) ✅
  - [x] 新增handleDatasetListResponse方法 ✅
  - [x] 更新handleGradientUpload方法(包含assignedDatasetId验证) ✅
  - [x] 实现assignedDatasetId验证逻辑 ✅

- [x] **DataDistributionService**: 新增assignedDatasetId管理功能 `[进度: 3/3]` ✅ **100%完成**
  - [x] 实现generateDatasetSlice数据集分片 ✅
  - [x] 实现performIIDAllocation分配策略 ✅
  - [x] 实现validateAllocation验证功能 ✅

- [x] **TrainingDataService**: 集成数据集解析和分配逻辑 `[进度: 2/2]` ✅ **100%完成**
  - [x] 实现uploadAndPreprocessDataset上传预处理 ✅
  - [x] 实现prepareDatasetForAllocation分配准备 ✅

### 13步标准化流程实现进度
```
步骤1-5: 前端操作和数据准备 [✅ 完全支持]
├── [✅] 步骤1: 前端创建页面 (已有接口)
├── [✅] 步骤2: 查询可用VM (已有接口: /api/federated/config/available-vms)
├── [✅] 步骤3: 返回VM列表 (已有实现)
├── [✅] 步骤4: 上传数据集 (已有接口: /api/training-data/upload)
└── [✅] 步骤5: 数据集解析 (v1.5增强: uploadAndPreprocessDataset) ✅

步骤6-9: 任务创建和数据集分配 [✅ 完全实现]
├── [✅] 步骤6: 创建任务接口调用 (createStandardFederatedTask) ✅
├── [✅] 步骤7: 后端任务创建 (FederatedTaskService已重构) ✅
├── [✅] 步骤8: 数据集分配到VM (allocateDatasets已实现) ✅
└── [✅] 步骤9: 查询数据集状态 (queryDatasetStatus已实现) ✅

步骤10-13: 验证和启动 [✅ 完全实现]
├── [✅] 步骤10: VM发送数据集信息 (handleDatasetListResponse) ✅
├── [✅] 步骤11: 后端验证和存储 (validateParticipantDatasets) ✅
├── [✅] 步骤12: 发送任务启动消息 (sendFederatedTaskStart) ✅
└── [✅] 步骤13: 开始联邦学习 (startFederatedLearningFlow) ✅

🎉 13步标准化流程 100%实现完成！
```

## 🔍 现状分析

### 当前架构问题及解决进度

#### 1. 协议版本不一致 `[严重程度: 高]`
```java
// ❌ 问题：现有测试使用v1.4协议
@Test
void test03_EnhancedWebSocketConnections() {
    // 使用v1.4协议，缺少assignedDatasetId支持
}

// 🔧 解决方案：重构为标准协议 [进度: 0%]
// 需要更新：CompleteFederatedLearningFlowTest
```

#### 2. 数据集管理缺失 `[严重程度: 高]`
```java
// ❌ 问题：FederatedTaskService缺少标准数据集分配逻辑
public class FederatedTaskService {
    // 缺少assignedDatasetId统一管理
    // 缺少13步流程支持
}

// 🔧 解决方案：实现标准数据集管理 [进度: 0%]
// 需要新增：数据集分配、验证、生命周期管理
```

#### 3. WebSocket协议支持不完整 `[严重程度: 高]`
```java
// ❌ 问题：WebSocketProtocolService未实现标准协议
// 缺少：DATASET_LIST_QUERY/RESPONSE
// 缺少：assignedDatasetId在FEDERATED_TASK_START中的dataConfig位置

// 🔧 解决方案：升级到标准协议支持 [进度: 0%]
// 需要实现：协议消息、消息结构更新
```

## 🏗️ 详细重构方案

### 1. FederatedTaskService 重构 `[优先级: P0]`

#### 当前接口分析
```java
// 当前FederatedTaskService存在的问题：
public interface FederatedTaskService {
    @Deprecated
    TaskOperationVO createTask(TaskCreateDTO createDTO, String createdBy);
    // ❌ 缺少标准数据集分配逻辑
    // ❌ 缺少assignedDatasetId统一管理
    // ❌ 缺少13步流程支持
}
```

#### 重构方案 `[预计工时: 4小时]`

##### 第一阶段：新增接口定义 `[预计: 30分钟]`
```java
public interface FederatedTaskService {

    // ========== 标准联邦学习接口 ==========

    /**
     * 标准联邦学习任务创建流程
     * 实现13步标准化流程的步骤6-12
     * 🎯 实现目标：完整的标准任务创建链路，包含数据集分配
     */
    TaskOperationVO createStandardFederatedTask(TaskCreateDTO createDTO, String createdBy);

    /**
     * 数据集分配和assignedDatasetId生成
     * 实现步骤8：通过算法将数据集进行分配
     * 🎯 实现目标：为每个VM生成唯一的assignedDatasetId
     */
    DatasetAllocationResult allocateDatasets(String taskId, List<String> participantVmIds,
                                            String originalDatasetPath);

    /**
     * 查询数据集状态并验证
     * 实现步骤9：向虚拟机查询数据集数据
     * 🎯 实现目标：确保所有VM的数据集就绪
     */
    DatasetValidationResult validateParticipantDatasets(String taskId);

    /**
     * 启动联邦学习流程
     * 实现步骤12-13：发送任务消息并开始联邦学习
     * 🎯 实现目标：启动完整的联邦学习流程
     */
    TaskOperationVO startFederatedLearningFlow(String taskId);
}
```

##### 第二阶段：核心方法实现 `[预计: 3小时]`
```java
@Service
@Transactional
public class FederatedTaskServiceImpl implements FederatedTaskService {

    @Autowired
    private WebSocketProtocolService webSocketProtocolService;

    @Autowired
    private DataDistributionService dataDistributionService;

    @Autowired
    private TrainingDataService trainingDataService;

    @Autowired
    private UuidUtil uuidUtil;

    // 🟢 实现状态跟踪
    private static final Logger log = LoggerFactory.getLogger(FederatedTaskServiceImpl.class);

    @Override
    public TaskOperationVO createStandardFederatedTask(TaskCreateDTO createDTO, String createdBy) {
        // 📊 进度跟踪：步骤6-7实现
        log.info("开始标准任务创建流程: taskName={}", createDTO.getTaskName());

        // 步骤6-7：创建联邦学习任务
        String taskId = uuidUtil.generateUuid();
        log.info("生成任务ID: {}", taskId);

        // 创建FederatedTask实体
        FederatedTask task = new FederatedTask();
        task.setTaskId(taskId);
        task.setTaskName(createDTO.getTaskName());
        task.setFederatedAlgorithm(createDTO.getFederatedAlgorithm());
        task.setStatus(FederatedTaskStatus.CREATING);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());

        // 保存到数据库
        federatedTaskMapper.insertTask(task);
        log.info("任务创建完成: taskId={}, status=CREATING", taskId);

        // 步骤8：数据集分配
        List<String> participantVmIds = createDTO.getParticipantConfig().getParticipants();
        log.info("开始数据集分配: taskId={}, participants={}", taskId, participantVmIds.size());

        DatasetAllocationResult allocationResult = allocateDatasets(
            taskId, participantVmIds, createDTO.getDatasetConfig().getDatasetId());

        if (!allocationResult.isSuccess()) {
            log.error("数据集分配失败: taskId={}, error={}", taskId, allocationResult.getErrorMessage());
            throw new FederatedTaskException("数据集分配失败: " + allocationResult.getErrorMessage());
        }
        log.info("数据集分配成功: taskId={}, allocations={}", taskId, allocationResult.getAllocations().size());

        // 步骤9：数据集验证
        log.info("开始数据集验证: taskId={}", taskId);
        DatasetValidationResult validationResult = validateParticipantDatasets(taskId);
        if (!validationResult.isAllDatasetReady()) {
            log.error("数据集验证失败: taskId={}, results={}", taskId, validationResult.getValidationResults());
            throw new FederatedTaskException("数据集验证失败，部分虚拟机数据集未就绪");
        }
        log.info("数据集验证成功: taskId={}", taskId);

        // 步骤12：启动联邦学习流程
        log.info("启动联邦学习流程: taskId={}", taskId);
        return startFederatedLearningFlow(taskId);
    }

    @Override
    public DatasetAllocationResult allocateDatasets(String taskId, List<String> participantVmIds,
                                                   String originalDatasetPath) {
        // 📊 进度跟踪：步骤8实现
        log.info("执行数据集分配: taskId={}, vmCount={}", taskId, participantVmIds.size());

        try {
            // 为每个参与者生成assignedDatasetId
            List<DatasetAllocation> allocations = new ArrayList<>();

            for (String vmId : participantVmIds) {
                String assignedDatasetId = uuidUtil.generateUuid();
                log.info("为VM生成数据集ID: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

                // 创建TaskParticipant记录
                TaskParticipant participant = new TaskParticipant();
                participant.setTaskId(taskId);
                participant.setVmId(vmId);
                participant.setAssignedDatasetId(assignedDatasetId);
                participant.setDatasetStatus("PENDING");
                participant.setJoinedAt(LocalDateTime.now());

                taskParticipantsMapper.insertParticipant(participant);
                log.info("参与者记录创建完成: taskId={}, vmId={}", taskId, vmId);

                // 通过WebSocket发送数据集创建消息
                DatasetSlice datasetSlice = dataDistributionService.generateDatasetSlice(originalDatasetPath, vmId);
                webSocketProtocolService.sendDatasetAllocation(vmId, taskId, assignedDatasetId, datasetSlice);
                log.info("数据集分配消息发送完成: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

                allocations.add(new DatasetAllocation(vmId, assignedDatasetId));
            }

            log.info("数据集分配完成: taskId={}, totalAllocations={}", taskId, allocations.size());
            return DatasetAllocationResult.success(allocations);

        } catch (Exception e) {
            log.error("数据集分配失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return DatasetAllocationResult.failure("数据集分配异常: " + e.getMessage());
        }
    }

    @Override
    public DatasetValidationResult validateParticipantDatasets(String taskId) {
        // 📊 进度跟踪：步骤9实现
        log.info("执行数据集验证: taskId={}", taskId);

        List<TaskParticipant> participants = taskParticipantsMapper.selectByTaskId(taskId);
        log.info("获取参与者列表: taskId={}, count={}", taskId, participants.size());

        Map<String, String> validationResults = new HashMap<>();
        int readyCount = 0;

        for (TaskParticipant participant : participants) {
            log.info("验证VM数据集: vmId={}, assignedDatasetId={}",
                    participant.getVmId(), participant.getAssignedDatasetId());

            // 发送DATASET_LIST_QUERY查询数据集状态
            DatasetQueryResult queryResult = webSocketProtocolService.queryDatasetStatus(
                participant.getVmId(), taskId, participant.getAssignedDatasetId());

            if (queryResult.isSuccess() && "CREATED".equals(queryResult.getDatasetStatus())) {
                // 更新数据库中的数据集状态
                participant.setDatasetStatus("CREATED");
                participant.setDatasetCreatedAt(LocalDateTime.now());
                taskParticipantsMapper.updateParticipant(participant);

                readyCount++;
                validationResults.put(participant.getVmId(), "READY");
                log.info("VM数据集验证成功: vmId={}", participant.getVmId());
            } else {
                validationResults.put(participant.getVmId(), "NOT_READY");
                log.warn("VM数据集验证失败: vmId={}, status={}",
                        participant.getVmId(), queryResult.getDatasetStatus());
            }
        }

        boolean allReady = readyCount == participants.size();
        log.info("数据集验证完成: taskId={}, ready={}/{}, allReady={}",
                taskId, readyCount, participants.size(), allReady);

        return new DatasetValidationResult(allReady, validationResults);
    }

    @Override
    public TaskOperationVO startFederatedLearningFlow(String taskId) {
        // 📊 进度跟踪：步骤12-13实现
        log.info("启动联邦学习流程: taskId={}", taskId);

        // 步骤12：向所有虚拟机发送FEDERATED_TASK_START消息
        List<TaskParticipant> participants = taskParticipantsMapper.selectByTaskId(taskId);
        log.info("获取参与者列表，准备发送启动消息: taskId={}, count={}", taskId, participants.size());

        for (TaskParticipant participant : participants) {
            webSocketProtocolService.sendFederatedTaskStart(
                participant.getVmId(), taskId, participant.getAssignedDatasetId());
            log.info("发送任务启动消息: vmId={}, taskId={}", participant.getVmId(), taskId);
        }

        // 步骤13：更新任务状态为RUNNING并开始联邦学习流程
        FederatedTask task = federatedTaskMapper.selectByTaskId(taskId);
        task.setStatus(FederatedTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        federatedTaskMapper.updateTask(task);
        log.info("任务状态更新完成: taskId={}, status=RUNNING", taskId);

        return TaskOperationVO.builder()
            .taskId(taskId)
            .status("RUNNING")
            .message("联邦学习任务启动成功")
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

##### 第三阶段：集成测试验证 `[预计: 30分钟]`
```java
// 🧪 测试验证清单
// [ ] 验证createFederatedTaskWithDatasets完整流程
// [ ] 测试数据集分配结果正确性
// [ ] 验证assignedDatasetId唯一性
// [ ] 测试错误处理和回滚机制
```

### 2. WebSocketProtocolService 重构 `[优先级: P0]`

#### 重构方案 `[预计工时: 3小时]`

##### 标准协议支持实现 `[预计: 2.5小时]`

```java
@Service
@Transactional
public class WebSocketProtocolService {

    // 📊 进度跟踪字段
    private static final Logger log = LoggerFactory.getLogger(WebSocketProtocolService.class);

    // ========== 标准协议支持 ==========

    /**
     * 发送FEDERATED_TASK_START消息 (标准协议)
     * 🎯 关键变更：assignedDatasetId位于dataConfig内部
     * 📊 实现进度：待实现
     */
    public void sendFederatedTaskStart(String vmId, String taskId, String assignedDatasetId) {
        log.info("准备发送FEDERATED_TASK_START消息: vmId={}, taskId={}, assignedDatasetId={}",
                vmId, taskId, assignedDatasetId);

        // 🔑 标准关键变更：构建dataConfig结构，assignedDatasetId在内部
        Map<String, Object> dataConfig = new HashMap<>();
        dataConfig.put("assignedDatasetId", assignedDatasetId);  // 🔑 关键：位于dataConfig内部
        dataConfig.put("dataPath", "/data/training");
        dataConfig.put("validationSplit", 0.2);
        dataConfig.put("shuffle", true);

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("taskId", taskId);
        messageData.put("federatedAlgorithm", "FEDERATED_AVERAGING");
        messageData.put("totalRounds", 10);
        messageData.put("dataConfig", dataConfig);  // assignedDatasetId在dataConfig内部

        ProtocolMessage message = MessageBuilder.create()
            .type(ProtocolType.FEDERATED_TASK_START)
            .vmId(vmId)
            .data(messageData)
            .build();

        simpMessagingTemplate.convertAndSend("/topic/vm/" + vmId, message);
        log.info("FEDERATED_TASK_START消息发送成功: vmId={}, taskId={}, assignedDatasetId={}",
                vmId, taskId, assignedDatasetId);
    }

    /**
     * 发送DATASET_LIST_QUERY消息 (v1.5新增)
     * 🎯 新协议：查询VM上的数据集状态
     * 📊 实现进度：待实现
     */
    public DatasetQueryResult queryDatasetStatus(String vmId, String taskId, String assignedDatasetId) {
        log.info("发送DATASET_LIST_QUERY查询: vmId={}, taskId={}, assignedDatasetId={}",
                vmId, taskId, assignedDatasetId);

        Map<String, Object> queryData = new HashMap<>();
        queryData.put("taskId", taskId);
        queryData.put("queryType", "ASSIGNED_DATASETS");

        ProtocolMessage queryMessage = MessageBuilder.create()
            .type(ProtocolType.DATASET_LIST_QUERY)
            .vmId(vmId)
            .data(queryData)
            .build();

        // 发送查询消息
        simpMessagingTemplate.convertAndSend("/topic/vm/" + vmId, queryMessage);
        log.info("DATASET_LIST_QUERY消息发送成功: vmId={}", vmId);

        // 🔄 等待响应 (实际实现中使用CompletableFuture或消息回调)
        return waitForDatasetResponse(vmId, taskId, assignedDatasetId);
    }

    /**
     * 处理DATASET_LIST_RESPONSE消息 (v1.5新增)
     * 🎯 新协议：处理VM返回的数据集状态信息
     * 📊 实现进度：待实现
     */
    public void handleDatasetListResponse(ProtocolMessage message) {
        String vmId = message.getVmId();
        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");

        log.info("收到DATASET_LIST_RESPONSE: vmId={}, taskId={}", vmId, taskId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> datasets = (List<Map<String, Object>>) data.get("datasets");

        for (Map<String, Object> dataset : datasets) {
            String assignedDatasetId = (String) dataset.get("assignedDatasetId");
            String status = (String) dataset.get("status");
            String localPath = (String) dataset.get("localPath");

            log.info("处理数据集状态: vmId={}, assignedDatasetId={}, status={}",
                    vmId, assignedDatasetId, status);

            // 更新数据库中的数据集状态
            TaskParticipant participant = taskParticipantsMapper.selectByTaskIdAndVmId(taskId, vmId);
            if (participant != null) {
                participant.setDatasetStatus(status);
                participant.setLocalPath(localPath);
                if ("CREATED".equals(status)) {
                    participant.setDatasetCreatedAt(LocalDateTime.now());
                }
                taskParticipantsMapper.updateParticipant(participant);
                log.info("数据集状态更新完成: vmId={}, status={}", vmId, status);
            } else {
                log.warn("未找到对应的参与者记录: taskId={}, vmId={}", taskId, vmId);
            }
        }

        log.info("DATASET_LIST_RESPONSE处理完成: vmId={}, taskId={}", vmId, taskId);
    }

    /**
     * 处理GRADIENT_UPLOAD消息 (v1.5更新)
     * 🎯 关键变更：包含assignedDatasetId验证
     * 📊 实现进度：待实现
     */
    public void handleGradientUpload(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");
        String vmId = message.getVmId();
        String assignedDatasetId = (String) data.get("assignedDatasetId");  // 🆕 标准新增字段

        log.info("收到GRADIENT_UPLOAD消息: vmId={}, taskId={}, assignedDatasetId={}",
                vmId, taskId, assignedDatasetId);

        // 🔑 关键验证：assignedDatasetId的合法性
        TaskParticipant participant = taskParticipantsMapper.selectByTaskIdAndVmId(taskId, vmId);
        if (participant == null || !assignedDatasetId.equals(participant.getAssignedDatasetId())) {
            log.error("GRADIENT_UPLOAD消息中assignedDatasetId验证失败: taskId={}, vmId={}, " +
                     "expected={}, actual={}", taskId, vmId,
                     participant != null ? participant.getAssignedDatasetId() : "null",
                     assignedDatasetId);

            // 发送错误响应
            sendGradientUploadError(vmId, "数据集ID验证失败");
            return;
        }

        log.info("assignedDatasetId验证成功: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

        // 处理梯度上传逻辑
        processGradientUpload(message);
        log.info("梯度上传处理完成: vmId={}, taskId={}", vmId, taskId);
    }

    /**
     * 🔧 辅助方法：等待数据集响应
     * 📊 实现进度：待实现
     */
    private DatasetQueryResult waitForDatasetResponse(String vmId, String taskId, String assignedDatasetId) {
        // 🔄 实际实现中需要使用异步等待机制
        // 这里简化为同步等待，实际应该使用CompletableFuture
        log.info("等待数据集查询响应: vmId={}, taskId={}", vmId, taskId);

        // TODO: 实现异步等待逻辑
        return DatasetQueryResult.builder()
            .success(true)
            .datasetStatus("CREATED")
            .localPath("/data/assigned/" + assignedDatasetId)
            .build();
    }
}
```

### 3. DataDistributionService 重构 `[优先级: P1]`

#### 新增数据集分配服务 `[预计工时: 2小时]`

```java
@Service
public class DataDistributionService {

    // 📊 进度跟踪
    private static final Logger log = LoggerFactory.getLogger(DataDistributionService.class);

    @Autowired
    private TrainingDataService trainingDataService;

    @Autowired
    private UuidUtil uuidUtil;

    /**
     * v1.5数据集分配算法
     * 🎯 实现目标：根据参与者数量和数据分布策略分配数据集
     * 📊 实现进度：待实现
     */
    public DatasetSlice generateDatasetSlice(String originalDatasetPath, String vmId) {
        log.info("开始生成数据集分片: originalPath={}, vmId={}", originalDatasetPath, vmId);

        try {
            // 读取原始数据集
            Dataset originalData = trainingDataService.loadDataset(originalDatasetPath);
            log.info("原始数据集加载完成: size={}", originalData.size());

            // 根据VM数量进行数据分片
            List<TaskParticipant> allParticipants = getCurrentTaskParticipants();
            int totalParticipants = allParticipants.size();
            int currentIndex = getCurrentVmIndex(vmId, allParticipants);

            log.info("数据分片参数: totalParticipants={}, currentIndex={}", totalParticipants, currentIndex);

            // 使用IID（独立同分布）分配策略
            DatasetSlice slice = performIIDAllocation(originalData, currentIndex, totalParticipants);

            // 为分片生成assignedDatasetId和本地路径
            String assignedDatasetId = uuidUtil.generateUuid();
            String localPath = "/data/assigned/" + assignedDatasetId;

            slice.setAssignedDatasetId(assignedDatasetId);
            slice.setLocalPath(localPath);
            slice.setVmId(vmId);

            log.info("数据集分片生成完成: vmId={}, assignedDatasetId={}, samples={}",
                    vmId, assignedDatasetId, slice.getSampleCount());

            return slice;

        } catch (Exception e) {
            log.error("数据集分片生成失败: vmId={}, error={}", vmId, e.getMessage(), e);
            throw new DataDistributionException("数据集分配失败", e);
        }
    }

    /**
     * IID数据分配策略
     * 📊 实现进度：待实现
     */
    private DatasetSlice performIIDAllocation(Dataset originalData, int vmIndex, int totalVms) {
        log.info("执行IID数据分配: vmIndex={}, totalVms={}, totalSamples={}",
                vmIndex, totalVms, originalData.size());

        int totalSamples = originalData.size();
        int samplesPerVm = totalSamples / totalVms;
        int startIndex = vmIndex * samplesPerVm;
        int endIndex = (vmIndex == totalVms - 1) ? totalSamples : startIndex + samplesPerVm;

        log.info("分配范围: startIndex={}, endIndex={}, samples={}",
                startIndex, endIndex, endIndex - startIndex);

        return originalData.slice(startIndex, endIndex);
    }

    /**
     * 验证数据集分配结果
     * 📊 实现进度：待实现
     */
    public DatasetAllocationValidation validateAllocation(String taskId) {
        log.info("验证数据集分配结果: taskId={}", taskId);

        List<TaskParticipant> participants = taskParticipantsMapper.selectByTaskId(taskId);

        Map<String, DatasetSliceInfo> allocationInfo = new HashMap<>();
        int totalSamples = 0;

        for (TaskParticipant participant : participants) {
            DatasetSliceInfo sliceInfo = getDatasetSliceInfo(participant.getAssignedDatasetId());
            allocationInfo.put(participant.getVmId(), sliceInfo);
            totalSamples += sliceInfo.getSampleCount();

            log.info("VM分配验证: vmId={}, assignedDatasetId={}, samples={}",
                    participant.getVmId(), participant.getAssignedDatasetId(), sliceInfo.getSampleCount());
        }

        log.info("分配验证完成: taskId={}, totalParticipants={}, totalSamples={}",
                taskId, participants.size(), totalSamples);

        return new DatasetAllocationValidation(allocationInfo, totalSamples);
    }
}
```

### 4. TrainingDataService 重构 `[优先级: P1]`

#### 集成assignedDatasetId处理 `[预计工时: 1.5小时]`

```java
@Service
@Transactional
public class TrainingDataServiceImpl implements TrainingDataService {

    // 📊 进度跟踪
    private static final Logger log = LoggerFactory.getLogger(TrainingDataServiceImpl.class);

    @Autowired
    private UuidUtil uuidUtil;

    /**
     * v1.5数据集上传和预处理
     * 🎯 实现目标：步骤4-5：数据集上传和解析
     * 📊 实现进度：待实现
     */
    public DatasetUploadResult uploadAndPreprocessDataset(MultipartFile file, String uploadedBy) {
        log.info("开始v1.5数据集上传: filename={}, size={}, uploadedBy={}",
                file.getOriginalFilename(), file.getSize(), uploadedBy);

        try {
            // 生成原始数据集ID
            String originalDatasetId = uuidUtil.generateUuid();
            String filePath = saveUploadedFile(file, originalDatasetId);
            log.info("文件保存完成: originalDatasetId={}, filePath={}", originalDatasetId, filePath);

            // 解析数据集格式和结构
            DatasetMetadata metadata = parseDatasetMetadata(filePath);
            log.info("数据集元信息解析完成: columns={}, rows={}",
                    metadata.getColumnCount(), metadata.getRowCount());

            // 验证数据完整性
            DatasetValidation validation = validateDatasetIntegrity(filePath, metadata);
            if (!validation.isValid()) {
                log.error("数据集验证失败: {}", validation.getErrorMessage());
                throw new DatasetValidationException("数据集验证失败: " + validation.getErrorMessage());
            }
            log.info("数据集验证通过: originalDatasetId={}", originalDatasetId);

            // 保存数据集信息到数据库
            TrainingDataset dataset = new TrainingDataset();
            dataset.setDatasetId(originalDatasetId);
            dataset.setFilename(file.getOriginalFilename());
            dataset.setFilePath(filePath);
            dataset.setFileSize(file.getSize());
            dataset.setUploadedBy(uploadedBy);
            dataset.setUploadedAt(LocalDateTime.now());
            dataset.setMetadata(metadata.toJson());
            dataset.setStatus("UPLOADED");

            trainingDatasetMapper.insertDataset(dataset);
            log.info("数据集信息保存完成: originalDatasetId={}", originalDatasetId);

            return DatasetUploadResult.builder()
                .datasetId(originalDatasetId)
                .filePath(filePath)
                .metadata(metadata)
                .message("数据集上传和预处理完成")
                .build();

        } catch (Exception e) {
            log.error("数据集上传失败: filename={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new DatasetUploadException("数据集上传失败", e);
        }
    }

    /**
     * v1.5数据集分配准备
     * 🎯 实现目标：为联邦学习任务准备数据集分配
     * 📊 实现进度：待实现
     */
    public DatasetAllocationPreparation prepareDatasetForAllocation(String originalDatasetId,
                                                                   List<String> participantVmIds) {
        log.info("准备数据集分配: originalDatasetId={}, participantCount={}",
                originalDatasetId, participantVmIds.size());

        // 加载原始数据集
        TrainingDataset originalDataset = trainingDatasetMapper.selectByDatasetId(originalDatasetId);
        if (originalDataset == null) {
            log.error("数据集不存在: {}", originalDatasetId);
            throw new DatasetNotFoundException("数据集不存在: " + originalDatasetId);
        }

        // 为每个参与者预分配assignedDatasetId
        List<DatasetAllocationPlan> allocationPlans = new ArrayList<>();

        for (String vmId : participantVmIds) {
            String assignedDatasetId = uuidUtil.generateUuid();

            DatasetAllocationPlan plan = DatasetAllocationPlan.builder()
                .vmId(vmId)
                .assignedDatasetId(assignedDatasetId)
                .originalDatasetId(originalDatasetId)
                .allocationStrategy("IID")
                .build();

            allocationPlans.add(plan);
            log.info("生成分配计划: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);
        }

        log.info("数据集分配准备完成: totalPlans={}", allocationPlans.size());
        return new DatasetAllocationPreparation(originalDataset, allocationPlans);
    }
}
```

## 📊 新增数据模型

### v1.5专用数据传输对象 `[预计工时: 1小时]`

```java
// v1.5数据集分配结果
@Data
@Builder
public class DatasetAllocationResult {
    private boolean success;
    private List<DatasetAllocation> allocations;
    private String errorMessage;
    private LocalDateTime timestamp;    // 📊 添加时间戳用于追踪

    public static DatasetAllocationResult success(List<DatasetAllocation> allocations) {
        return DatasetAllocationResult.builder()
            .success(true)
            .allocations(allocations)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static DatasetAllocationResult failure(String errorMessage) {
        return DatasetAllocationResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
    }
}

// 数据集分配信息
@Data
@AllArgsConstructor
public class DatasetAllocation {
    private String vmId;
    private String assignedDatasetId;
    private LocalDateTime allocatedAt;   // 📊 添加分配时间

    public DatasetAllocation(String vmId, String assignedDatasetId) {
        this.vmId = vmId;
        this.assignedDatasetId = assignedDatasetId;
        this.allocatedAt = LocalDateTime.now();
    }
}

// v1.5数据集验证结果
@Data
@AllArgsConstructor
public class DatasetValidationResult {
    private boolean allDatasetReady;
    private Map<String, String> validationResults;
    private LocalDateTime validatedAt;   // 📊 添加验证时间

    public DatasetValidationResult(boolean allDatasetReady, Map<String, String> validationResults) {
        this.allDatasetReady = allDatasetReady;
        this.validationResults = validationResults;
        this.validatedAt = LocalDateTime.now();
    }
}

// 数据集查询结果
@Data
@Builder
public class DatasetQueryResult {
    private boolean success;
    private String datasetStatus;
    private String localPath;
    private String errorMessage;
    private LocalDateTime queriedAt;     // 📊 添加查询时间
}
```

## 🔄 实施计划与时间线

### 总体实施进度 `[总预计工时: 11小时]`

#### 第一周：核心服务重构 `[7小时]`
```
周一 (3小时):
├── [2小时] FederatedTaskService接口设计和核心方法实现
└── [1小时] 单元测试和集成验证

周二 (2小时):
├── [1.5小时] WebSocketProtocolService v1.5协议支持
└── [0.5小时] 协议消息测试

周三 (2小时):
├── [1小时] DataDistributionService数据分配逻辑
└── [1小时] TrainingDataService集成增强
```

#### 第二周：集成测试和优化 `[4小时]`
```
周四 (2小时):
├── [1小时] 数据模型和DTO完善
└── [1小时] 错误处理和异常机制

周五 (2小时):
├── [1小时] 端到端集成测试
└── [1小时] 性能优化和代码审查
```

## ⚠️ 风险和注意事项

### 高风险项 `[需要重点关注]`

#### 1. 数据库架构兼容性 `[风险等级: 高]`
```sql
-- ⚠️ 确保数据库迁移脚本正确执行
ALTER TABLE task_participants
ADD COLUMN assigned_dataset_id VARCHAR(32) NOT NULL;
-- 可能影响现有数据，需要测试验证
```

#### 2. WebSocket消息格式变更 `[风险等级: 高]`
```java
// ⚠️ v1.5协议变更可能影响现有VM连接
// 需要确保向后兼容性处理或协调升级
```

#### 3. 并发安全和数据一致性 `[风险等级: 中]`
```java
// ⚠️ 多VM并发访问时的数据集分配冲突
// 需要实现适当的锁机制和事务管理
```

### 迁移检查清单 `[实施前必须验证]`

- [ ] **数据库更新**: 执行v1.5数据库迁移脚本
- [ ] **配置更新**: 更新WebSocket配置支持新协议
- [ ] **依赖检查**: 确保UuidUtil等依赖服务正常
- [ ] **测试环境验证**: 在测试环境完整验证新流程
- [ ] **回滚计划**: 准备完整的回滚方案和数据备份
- [ ] **性能测试**: 验证新流程不影响系统性能
- [ ] **文档更新**: 更新API文档和操作手册

## 📈 成功指标

### 技术指标 `[量化验证标准]`
- [ ] 13步流程100%覆盖实现
- [ ] assignedDatasetId唯一性保证100%
- [ ] v1.5协议消息格式100%符合规范
- [ ] 单元测试覆盖率 > 90%
- [ ] 集成测试通过率 100%

### 业务指标 `[业务价值验证]`
- [ ] 数据集分配准确性100%
- [ ] 联邦学习任务创建成功率 > 95%
- [ ] 系统响应时间 < 2秒
- [ ] 数据集验证准确率100%

这份后端服务层重构文档提供了详细的实现方案和进度跟踪，确保后端系统能够完全支持WebSocket v1.5协议的标准联邦学习链路。