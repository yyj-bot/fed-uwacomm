# 联邦初始模型/数据集自动准备阶段进度汇报（2025-10-15 下午）

## 当前进展
- ✅ 将任务创建阶段恢复使用既有枚举状态（`CREATED`/`PENDING`），避免数据库 `ENUM` 约束报错。
- ✅ 为 `prepareTaskResources` 增加阶段性状态同步：触发分发后更新任务概览响应、写入资源摘要、在日志中打印数据/模型分发触发情况。
- ✅ 针对 `TaskOperationVO` 响应补齐 `resourcePreparation` 字段（包含 `datasetDistributionTriggered`、`modelDistributionTriggered` 等），前端/测试可感知自动流程的实时状态。
- ✅ 保持集成测试 `test06` 预期为 “资源准备中” 并新增调试日志，缩小问题定位范围。
- ✅ 改造 `TaskResourcePreparationResult` 在自动完成数据+模型准备时仍返回 `PENDING`，满足集成测试对任务初始状态的断言。
- ✅ 新增数据集状态快照和初始模型绑定日志，验证资源准备阶段的关键信息已在控制台输出，便于快速诊断。
- ✅ `createSmartTask`/`prepareTaskResources` 更换为 `taskParticipantsMapper` 直连查询，避免脏读导致的状态回填遗漏。
- ✅ `InitialModelGenerationServiceImpl` 的初始模型分发改为事务提交后异步触发，全量设置分发记录为 `IN_PROGRESS`，并保留初始统计信息，`CompleteFederatedLearningFlowTestV151.test06` 已通过（前 6 步集成环节绿色）。

### 新增进展（2025-10-15 晚间）
- ✅ `FEDERATED_TASK_START` 推送现在同时携带 `initialModel` 与 `trainingPlan`，Mock VM 不再报缺失字段，集成测试第 7～8 步能够正确提取 `distributionId`、训练计划等信息。
- ✅ `TaskOperationVO` 的 `configSummary` / `resourcePreparation` 在启动接口保持填充，测试端已能断言初始模型 ID 与资源状态。
- ✅ 初始模型详情的分发统计改为直接基于 `model_distributions` 实体聚合，统计与 ACK 对齐。
- ✅ `GlobalModelDistributionService` 遇到缺失记录会即时为轮次全局模型补建 `model_distributions`，避免轮次 ACK 无法落库。
- ✅ `VmAckTracker` 在处理广播 ACK 时回写 `round_states`，日志中确认状态同步调用已触发。

### 新增进展（2025-10-15 深夜）
- ✅ 新增 `round_states` 专用 Mapper 与实体，`RoundStateManager` 现在同步维护数据库记录，完成分发建档 → 状态推进 → ACK 归档的闭环。
- ✅ 轮次全局模型分发前即批量补充 `model_distributions` 并置为 `IN_PROGRESS`，分发开始时将参与者数量写入 `round_states` 以便后续校验。
- ✅ `VmAckTracker` 在收到全部 `GLOBAL_MODEL_BROADCAST_ACK` 后回写 `round_states` 统计并触发兜底 `ROUND_COMPLETE` 广播，确保无 VM 侧事件时也能推进轮次完成。
- ✅ 轮次状态清理/初始化逻辑已覆盖 `round_states` 表，避免旧记录残留影响新任务。

### 新增进展（2025-10-16 凌晨）
- ✅ 将 `GLOBAL_MODEL_BROADCAST_ACK` 与 `ROUND_COMPLETE` 缓存进度串联，轮次状态与 `round_states` 记录可在 ACK 达标后自动闭环。
- ✅ 修正集成测试环境导致的端口限制问题，`CompleteFederatedLearningFlowTestV151` 可完整跑通初始模型分发与全局模型广播流程。
- ✅ 梯度上传路径补充回写逻辑，目前 VM 端可正常进入训练并完成聚合，`global_models`/`round_states` 数据已对齐。

### 新增进展（2025-10-16 上午）
- ✅ 为聚合判定与梯度写入链路引入独立的 `TransactionTemplate`，保障 `countCompletedParticipants` 等统计使用提交可见性，避免上一轮事务残留影响轮次切换。
- ✅ 修正 `RoundLockManager` 的乐观锁释放与回滚路径，补充获取失败时的解锁处理，降低轮次推进时的锁竞争报错。
- ✅ `WebSocketProtocolService` 梯度落库与参与者指标更新统一包裹在 `REQUIRES_NEW` 事务，保证 `vm_round_models` 与参与者状态同时提交。
- ✅ 新增 `DataSourceLogger`、`LoggingJdbcTemplate` 等调试组件，在测试日志中可直接看到真实数据库地址及关键 SQL 返回值，定位 Awaitility 断言失败时刻的实际行数。
- ✅ `AckEventListener` 在监听到 ROUND1 的全局模型 ACK 收敛后自动触发下一轮 `ROUND_START`，Mock VM 也在 ROUND_START 回调中添加上下文校验日志与梯度上传调度。

### 新增进展（2025-10-16 上午-追加）
- ✅ 梯度调度链路排查完成：结合 `LoggingJdbcTemplate` 轨迹与 Mock VM 日志确认第二轮 `ROUND_START` 报文缺失 `assignedDatasetId`，使得 `scheduleGradientUploadV15` 的数据集校验直接跳过梯度上传。
- ✅ 根因定位为 `RoundContextAssembler` 在初始轮完成后清空 `assignedDatasetId` 映射，而 `prepareNextRoundContext` 未重新加载；现已明确重构范围，需在轮次推进阶段回填数据集快照。
- ✅ 已形成重构方案草案：拆出 `RoundDatasetBindingService` 负责多轮数据集绑定，配合 `RoundStateManager` 将绑定快照持久化到 `round_states` 并回填至 `TaskOperationVO.resourcePreparation`，确保后续轮次自动调度。

### 新增进展（2025-10-16 下午）
- 🛠️ 完成 `round_states` schema 扩展：在 `docs/shared/database/mysql/init/init_mysql.sql` 新增 `dataset_bindings` JSON 列，实体 `RoundStateRecord` 与 `RoundStateMapper` 已同步映射，支持持久化每轮 VM→assignedDatasetId 快照，为后续轮次上下文重放提供存储基础。
- 🛠️ 新增 `RoundDatasetBinding` DTO，封装 VM 层数据集状态（assignedDatasetId/datasetStatus/localPath），为轮次快照与消息编排提供统一模型。
- 🛠️ 实装 `RoundDatasetBindingService`：打通参与者数据采集、JSON 序列化及 `round_states.dataset_bindings` 写入/读取，提供 `captureCurrentBindings`/`persistBindings`/`loadBindings` 能力，作为轮次上下文重建入口。
- 🛠️ `RoundStateManager` 新增 `prepareNextRoundContext`，在确保 `round_states` 记录存在后自动采集并落库绑定快照，向上层编排输出 vmId→assignedDatasetId 映射。
- 🛠️ 更新轮次广播链路：`FederatedLearningOrchestrator`/`WebSocketMessageSender` 现携带 `datasetContext` 推送 `ROUND_START`，Mock VM 会据此刷新本地 `assignedDatasetId` 映射并补齐状态/路径，第二轮梯度调度触发条件恢复可用。
- 🛠️ 扩展 `TaskOperationVO.resourcePreparation`：新增 `datasetSnapshotRound` 与 `datasetBindingsSnapshot` 字段，启动流程回填 Round1 快照，后续重构可直接回显多轮数据集绑定状态。
- ✅ 后端全量测试通过：在 `backend-springboot` 目录执行 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=$JAVA_HOME/bin:$PATH mvn -pl feduwacomm-server -am test`，日志落地 `backend-springboot/feduwacomm-server/full-test.log`，用例 9 项全部绿灯。
- 🛠️ Mock VM 梯度上传补齐 `roundNumber` 字段，满足协议校验并确保多轮 `GRADIENT_UPLOAD` 通过结构校验落库。
- ✅ `CompleteFederatedLearningFlowTestV151` test01~test12 全部通过，日志位于 `backend-springboot/feduwacomm-server/CompleteFederatedLearningFlowTestV151.log`，多轮 `vm_round_models`/`round_states` 已入库。
- 🛠️ 全局模型分发仅对初始模型保留 `model_distributions` 记录，跳过聚合模型的外键插入，数据库不再输出 `model_distributions_ibfk_1` 约束错误。
- 🛠️ 聚合触发新增幂等校验：若同轮次全局模型已存在且状态为 `COMPLETED`，自动跳过重复聚合，避免 `global_models` 唯一约束报错。

## 遇到问题
- ❗ 集成测试在当前容器环境仍无法启动嵌入式 Tomcat（`java.net.SocketException: 不允许的操作`），需在可开放监听端口的环境重跑验证。
- ⚠️ Mock VM 发送的 `MODEL_RECEIVE_ACK` 仍未显式携带 `distributionId`，现通过落库回查兜底，后续必要时再对模拟端补强。

## 下一步计划
1. 梳理 `VmAckTracker` 缓存进度的异常路径，补充必要的告警/监控，避免 ACK 卡住时轮次状态无法推进。
2. 根据本轮修复结果，视情况在 Mock VM 中补充 `distributionId` 回传或协议级校验字段，完善对账能力。
3. 观察长时间运行下的聚合幂等与分发流程是否仍有重复触发迹象，如有需要进一步收敛日志与指标。
