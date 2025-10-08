# 后端 TODO 和未完成任务总结

> **文档目的**: 系统性地跟踪后端所有待完成功能、TODO 标记和技术债务
> **最后更新**: 2025-10-04
> **维护者**: FedUWAComm 开发团队

---

## 📋 目录

- [核心未完成功能](#-核心未完成功能)
  - [高优先级任务](#-高优先级任务)
  - [中优先级任务](#-中优先级任务)
  - [低优先级任务](#-低优先级任务)
- [被禁用的测试](#-被禁用的测试)
- [测试中的未实现API](#-测试中的未实现api)
- [废弃代码清单](#-废弃代码清单)
- [实现建议](#-实现建议)
- [进度跟踪](#-进度跟踪)

---

## 🎯 核心未完成功能

### 🔴 高优先级任务

> **影响**: 核心工作流功能不完整，影响生产环境稳定性

#### 1. FederatedOrchestrationServiceImpl - 工作流编排核心逻辑

**文件位置**: `feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedOrchestrationServiceImpl.java`

##### 1.1 工作流回滚逻辑 (445行)
```java
// TODO: 实现回滚逻辑
@Override
public void rollbackToStage(String taskId, String targetStage, String reason) {
    log.info("回滚到指定阶段: taskId={}, targetStage={}, reason={}", taskId, targetStage, reason);
    // TODO: 实现回滚逻辑
}
```

**任务清单**:
- [ ] 实现阶段状态回滚
- [ ] 清理已完成阶段的副作用
- [ ] 恢复目标阶段的上下文数据
- [ ] 记录回滚操作日志
- [ ] 发布回滚事件通知

**复杂度**: 中高
**估计工时**: 3-4天
**依赖**: WorkflowContext 状态管理

##### 1.2 阶段状态更新逻辑 (450行)
```java
// TODO: 实现阶段状态更新逻辑
@Override
public boolean updateStageStatus(String orchestrationId, String stageName, String status) {
    log.info("更新阶段状态: orchestrationId={}, stageName={}, status={}", orchestrationId, stageName, status);
    // TODO: 实现阶段状态更新逻辑
    return true;
}
```

**任务清单**:
- [ ] 验证状态转换合法性
- [ ] 更新 WorkflowStageExecution 表
- [ ] 同步更新 OrchestrationWorkflow 状态
- [ ] 触发状态变更事件
- [ ] 添加并发控制（乐观锁）

**复杂度**: 中
**估计工时**: 2天
**依赖**: WorkflowStageExecutionMapper

---

#### 2. FederatedTaskServiceImpl - WebSocket 通信逻辑

**文件位置**: `feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedTaskServiceImpl.java`

##### 2.1 数据集分配 WebSocket 消息发送 (2651行)
```java
// TODO: 实现实际的WebSocket消息发送逻辑
private void sendDatasetAllocation(String vmId, String taskId, String assignedDatasetId, DatasetSlice datasetSlice) {
    // 通过WebSocket发送数据集分配消息
    // TODO: 实现实际的WebSocket消息发送逻辑
    log.info("发送数据集分配消息: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);
}
```

**任务清单**:
- [ ] 集成 WebSocketService 或 SimpMessagingTemplate
- [ ] 构建 DATASET_ALLOCATION 协议消息
- [ ] 添加消息发送失败重试逻辑
- [ ] 实现发送确认机制（ACK）
- [ ] 添加超时处理

**复杂度**: 中
**估计工时**: 1-2天
**依赖**: WebSocketProtocolService, MessageBuilder

##### 2.2 数据集状态查询 WebSocket 逻辑 (2660行)
```java
// TODO: 实现实际的WebSocket查询逻辑
private DatasetQueryResult queryDatasetStatus(String vmId, String taskId, String assignedDatasetId) {
    // 发送DATASET_LIST_QUERY查询数据集状态
    // TODO: 实现实际的WebSocket查询逻辑
    log.info("查询数据集状态: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);

    // 暂时返回成功结果，实际实现时需要真正的WebSocket查询
    return DatasetQueryResult.builder()
        .success(true)
        .datasetStatus("CREATED")
        .localPath("/data/assigned/" + assignedDatasetId)
        .build();
}
```

**任务清单**:
- [ ] 实现 DATASET_LIST_QUERY 消息构建
- [ ] 实现同步/异步查询响应处理
- [ ] 添加查询超时机制
- [ ] 缓存查询结果以优化性能
- [ ] 处理 VM 离线场景

**复杂度**: 中
**估计工时**: 2天
**依赖**: WebSocketProtocolService, VmAckTracker

---

### 🟡 中优先级任务

#### 3. DataDistributionServiceImpl - 数据集分析逻辑

**文件位置**: `feduwacomm-server/src/main/java/com/feduwacomm/service/impl/DataDistributionServiceImpl.java`

##### 3.1 真实数据集大小分析 (1603行)
```java
/**
 * 分析数据集大小
 * TODO: 实现真实的数据集分析逻辑
 */
private int analyzeDatasetSize(String datasetPath) {
    // 临时实现：返回模拟的样本数
    // 实际实现应该解析数据集文件，计算真实的样本数量
    log.debug("分析数据集大小: {}", datasetPath);

    if (datasetPath == null || datasetPath.trim().isEmpty()) {
        return 0;
    }

    // 模拟不同数据集的样本数
    if (datasetPath.contains("small")) {
        return 1000;
    } else if (datasetPath.contains("medium")) {
        return 5000;
    } else if (datasetPath.contains("large")) {
        return 10000;
    } else {
        return 3000; // 默认样本数
    }
}
```

**任务清单**:
- [ ] 支持 CSV 文件解析（行数统计）
- [ ] 支持 JSON 文件解析（数组长度）
- [ ] 支持 Parquet/Avro 等机器学习常用格式
- [ ] 添加文件读取异常处理
- [ ] 缓存分析结果
- [ ] 支持大文件流式处理（避免内存溢出）

**复杂度**: 中
**估计工时**: 2-3天
**依赖**: Apache Commons CSV, Jackson, Apache Parquet（可选）

**实现建议**:
```java
private int analyzeDatasetSize(String datasetPath) {
    try {
        Path path = Paths.get(datasetPath);
        String extension = getFileExtension(path);

        switch (extension) {
            case "csv":
                return countCsvRows(path);
            case "json":
                return countJsonRecords(path);
            case "parquet":
                return countParquetRecords(path);
            default:
                log.warn("未知数据集格式: {}", extension);
                return estimateSizeByFileSize(path);
        }
    } catch (IOException e) {
        log.error("数据集分析失败: {}", datasetPath, e);
        throw new BusinessException("数据集分析失败: " + e.getMessage());
    }
}
```

---

### 🟢 低优先级任务（v1.5 新特性）

#### 4. TrainingDataService - v1.5 数据集处理增强

**文件位置**: `feduwacomm-server/src/main/java/com/feduwacomm/service/TrainingDataService.java`

##### 4.1 数据集上传和预处理 (83-89行)
```java
/**
 * v1.5数据集上传和预处理 (v1.5)
 * 🎯 实现目标：步骤4-5：数据集上传和解析
 * 📊 实现进度：待实现
 *
 * @param file 上传的数据集文件
 * @param uploadedBy 上传者ID
 * @return 数据集上传结果
 */
DatasetUploadResult uploadAndPreprocessDataset(MultipartFile file, String uploadedBy);
```

**任务清单**:
- [ ] 实现文件上传和存储
- [ ] 数据集格式验证
- [ ] 自动数据预处理（标准化、归一化）
- [ ] 数据质量检查（缺失值、异常值）
- [ ] 生成数据集元数据
- [ ] 创建 DatasetUploadResult DTO

**复杂度**: 高
**估计工时**: 4-5天
**依赖**: 需要定义 v1.5 数据处理规范

##### 4.2 数据集分配准备 (94-101行)
```java
/**
 * v1.5数据集分配准备 (v1.5)
 * 🎯 实现目标：为联邦学习任务准备数据集分配
 * 📊 实现进度：待实现
 *
 * @param originalDatasetId 原始数据集ID
 * @param participantVmIds 参与者虚拟机ID列表
 * @return 数据集分配准备结果
 */
DatasetAllocationPreparation prepareDatasetForAllocation(String originalDatasetId,
                                                         java.util.List<String> participantVmIds);
```

**任务清单**:
- [ ] 实现数据集切片预分析
- [ ] 计算最优分配策略
- [ ] 生成分配预览
- [ ] 验证参与者VM资源充足性
- [ ] 创建 DatasetAllocationPreparation DTO

**复杂度**: 中高
**估计工时**: 3-4天
**依赖**: DataSlicingService

---

## 🧪 被禁用的测试

### AckCachePerformanceTest 性能测试套件

**文件位置**: `feduwacomm-server/src/test/java/com/feduwacomm/performance/AckCachePerformanceTest.java`

所有测试方法都标记为 `@Disabled("性能测试 - 手动运行")`，需要在特定场景下手动启用。

#### 测试清单

##### 1. 基准性能测试 (33行)
```java
@Disabled("性能测试 - 手动运行")
@Test
void testBasicPerformance() {
    // 测试基础ACK处理性能
}
```

**测试目标**:
- 衡量基础 ACK 处理吞吐量
- 建立性能基准线

**运行时机**:
- 重大性能优化后
- 发布前性能回归测试

##### 2. 内存测试 (131行)
```java
@Disabled("内存测试 - 手动运行")
@Test
void testMemoryUsage() {
    // 测试内存使用情况
}
```

**测试目标**:
- 检测内存泄漏
- 验证缓存淘汰策略

**运行时机**:
- 怀疑内存泄漏时
- 大规模场景测试

##### 3. 响应时间测试 (169行)
```java
@Disabled("响应时间测试 - 手动运行")
@Test
void testResponseTime() {
    // 测试响应时间
}
```

**测试目标**:
- 测量 P50/P95/P99 延迟
- 验证SLA指标

**运行时机**:
- 性能优化验证
- 容量规划

##### 4. 压力测试 (210行)
```java
@Disabled("压力测试 - 手动运行")
@Test
void testStressScenarios() {
    // 压力测试场景
}
```

**测试目标**:
- 测试系统极限容量
- 验证降级策略

**运行时机**:
- 上线前压力测试
- 容量扩容评估

#### 启用方式

1. **单个测试启用**:
   ```bash
   # 移除对应测试方法的 @Disabled 注解
   # 或使用 JUnit 5 条件执行
   mvn test -Dtest=AckCachePerformanceTest#testBasicPerformance
   ```

2. **批量启用**:
   ```bash
   # 添加环境变量或系统属性
   mvn test -Dtest=AckCachePerformanceTest -Dperformance.tests.enabled=true
   ```

---

## 🚧 测试中的未实现API

### CompleteFederatedLearningFlowTest 集成测试

**文件位置**: `feduwacomm-server/src/test/java/com/feduwacomm/integration/CompleteFederatedLearningFlowTest.java`

#### 未实现接口清单

| 行号 | 接口描述 | 状态 | 影响 |
|------|---------|------|------|
| 669 | 聚合引擎状态查询接口 | 暂未实现，跳过验证 | 无法监控聚合进度 |
| 700 | 聚合策略查询接口 | 暂未实现，跳过验证 | 无法动态查询可用策略 |
| 828 | 策略切换接口 | 暂未实现，跳过验证 | 无法运行时切换聚合策略 |
| 870 | 策略指标接口 | 暂未实现，跳过特性验证 | 无法获取策略性能指标 |
| 948 | 全局模型查询接口 | 暂未实现，跳过验证 | 无法查询全局模型状态 |
| 1364 | 性能测试接口 | 暂未实现，模拟测试 | 无法评估系统性能 |
| 1390 | 内存监控接口 | 暂未实现，跳过验证 | 无法监控内存使用 |
| 1695 | 数据库状态验证 | API可能尚未实现 | 无法验证数据库一致性 |

**建议**:
- 将这些接口纳入 v1.6 或 v2.0 开发计划
- 优先实现聚合引擎状态和策略相关接口（运维关键）

---

### CompleteFederatedLearningFlowTestV151 集成测试

**文件位置**: `feduwacomm-server/src/test/java/com/feduwacomm/integration/CompleteFederatedLearningFlowTestV151.java`

#### 未实现功能清单

| 行号 | 功能描述 | 状态 | 影响 |
|------|---------|------|------|
| 665 | 梯度存储验证 | 可能未实现存储逻辑 | 无法持久化训练梯度 |
| 728 | 最终模型查询 | 可能未实现聚合逻辑 | 无法获取聚合后的模型 |
| 1007 | 全局模型查询 | 可能未实现聚合 | 与上述问题相关 |

**建议**:
- 确认是否需要持久化梯度（通常只需要模型参数）
- 补充聚合后模型查询接口的实现

---

## 🗑️ 废弃代码清单

> **注意**: 以下文件包含 `@Deprecated` 标记，建议在下个主要版本中移除

### 标记为废弃的文件

1. **WebSocketProtocolServiceTest.java**
   - 路径: `feduwacomm-server/src/test/java/com/feduwacomm/service/WebSocketProtocolServiceTest.java`
   - 原因: 待确认（可能被新测试替代）
   - 建议: 迁移到新测试框架

2. **FederatedTaskService.java**
   - 路径: `feduwacomm-server/src/main/java/com/feduwacomm/service/FederatedTaskService.java`
   - 原因: 部分接口被废弃
   - 建议: 标注具体废弃方法，提供迁移指南

3. **FederatedTasksMapper.java**
   - 路径: `feduwacomm-server/src/main/java/com/feduwacomm/mapper/FederatedTasksMapper.java`
   - 原因: 可能有废弃的查询方法
   - 建议: 审查并移除未使用的方法

4. **TaskCreateDTO.java**
   - 路径: `feduwacomm-pojo/src/main/java/com/feduwacomm/dto/TaskCreateDTO.java`
   - 原因: 部分字段被废弃
   - 建议: 迁移到新的 DTO 结构

5. **WebSocketMessage.java**
   - 路径: `feduwacomm-pojo/src/main/java/com/feduwacomm/dto/WebSocketMessage.java`
   - 原因: 可能被协议v1.5替代
   - 建议: 迁移到 ProtocolMessage

6. **ModelVersion.java**
   - 路径: `feduwacomm-pojo/src/main/java/com/feduwacomm/entity/ModelVersion.java`
   - 原因: 部分字段或方法被废弃
   - 建议: 审查实体定义，移除废弃字段

### 清理计划

- **v1.6**: 添加废弃警告日志
- **v2.0**: 完全移除废弃代码
- **迁移指南**: 在 `docs/migration/` 中创建迁移文档

---

## 💡 实现建议

### 优先级排序（基于业务影响）

#### P0 - 立即处理（影响核心功能）
1. **WebSocket 通信逻辑**
   - `sendDatasetAllocation()` 和 `queryDatasetStatus()`
   - 原因: 阻塞数据分发流程，影响联邦学习任务执行
   - 估计工时: 3-4天

#### P1 - 近期处理（影响工作流完整性）
2. **工作流编排逻辑**
   - `rollbackToStage()` 和 `updateStageStatus()`
   - 原因: 影响任务失败恢复能力
   - 估计工时: 5-6天

#### P2 - 中期处理（性能优化）
3. **数据集分析逻辑**
   - `analyzeDatasetSize()`
   - 原因: 当前模拟实现够用，但不够精确
   - 估计工时: 2-3天

#### P3 - 长期处理（新特性）
4. **v1.5 数据处理增强**
   - `uploadAndPreprocessDataset()` 和 `prepareDatasetForAllocation()`
   - 原因: 新版本特性，不影响现有功能
   - 估计工时: 7-9天

### 技术债务清理建议

1. **启用性能测试**
   - 建立 CI/CD 性能测试流水线
   - 每周自动运行性能回归测试

2. **废弃代码清理**
   - Q1 2026: 完成废弃标记和迁移指南
   - Q2 2026: 移除所有废弃代码

3. **补充未实现API**
   - 聚合引擎监控接口 (2周)
   - 策略管理接口 (1周)
   - 性能和内存监控接口 (1周)

---

## 📊 进度跟踪

### 当前状态

| 类别 | 总数 | 已完成 | 进行中 | 待开始 | 完成率 |
|------|------|--------|--------|--------|--------|
| 高优先级任务 | 4 | 0 | 0 | 4 | 0% |
| 中优先级任务 | 1 | 0 | 0 | 1 | 0% |
| 低优先级任务 | 2 | 0 | 0 | 2 | 0% |
| 性能测试 | 4 | 0 | 0 | 4 | 0% |
| 废弃代码清理 | 6 | 0 | 0 | 6 | 0% |

### 更新历史

| 日期 | 更新内容 | 更新人 |
|------|---------|--------|
| 2025-10-04 | 初始版本创建 | Claude Code |

---

## 📝 备注

### 如何使用此文档

1. **开发人员**: 认领任务前查看任务清单和依赖
2. **项目经理**: 跟踪进度，调整优先级
3. **测试人员**: 了解未实现功能，调整测试范围
4. **代码审查**: 验证 TODO 是否已正确移除

### 贡献指南

完成任务后，请：
1. 更新任务清单（勾选完成项）
2. 更新进度跟踪表
3. 在更新历史中记录变更
4. 移除代码中对应的 TODO 注释

### 相关文档

- [后端重构指南](./backend-service-refactoring.md)
- [单元测试重构](./unit-test-refactoring.md)
- [v1.5.1 切片增强计划](./v1.5.1-slice-enhancement-refactoring-plan.md)

---

**文档维护**: 请每次 Sprint 结束后更新此文档，确保信息准确性。
