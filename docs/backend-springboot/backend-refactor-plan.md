# 后端联邦流程重构计划

首轮记录：聚焦服务端流程、集成测试与 Mock VM 需同步收敛的事项。进度采用 0%/25%/50%/75%/100% 分档，“打卡”用于标注当前记录节点。

| 序号 | 重构主题 | 范围说明 | 预期产物 | 状态 | 进度 | 打卡 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 数据分发与 ACK 门控 | `FederatedTaskServiceImpl`、`DataDistributionServiceImpl`、`WebSocketProtocolService` | 等待全部 `DATASET_*_ACK` 后再推进模型与训练流程 | 进行中 | 50% | 2025-10-12：初始化ACK跟踪与超时等待完成 |
| 2 | 工作流阶段恢复 | `FederatedOrchestrationServiceImpl`、`DataDistributionStageHandler` | 将数据分发纳入工作流阶段，统一入口与失败回溯 | 进行中 | 50% | 2025-10-12：恢复阶段内分发调用，事件监听改为记录 |
| 3 | 集成测试校验增强 | `CompleteFederatedLearningFlowTestV151` | 替换 `sleep` 为数据库 / WebSocket 断言，验证 `task_participants` 状态与消息顺序 | 进行中 | 25% | 2025-10-12：Awaitility 引入并替换部分等待 |
| 4 | Mock VM 协议约束加固 | `MockVirtualMachine` | 强制等待 `GRADIENT_UPLOAD_PREPARE`、支持 ACK 超时/失败模拟，避免短路流程 | 进行中 | 50% | 2025-10-13：封装 Awaitility 延迟并新增 ACK 超时/失败模拟 |
| 5 | 初始模型分发筛选 | `ModelDistributionStageHandler`、`GlobalModelDistributionService` | 仅向任务参与 VM 分发模型，补充异常处理 | 进行中 | 50% | 2025-10-12：模型分发阶段与全局分发服务改用参与者列表 |
| 6 | 数据集状态持久化 | `TaskParticipantsMapper` 相关调用 | 在 ACK 后写入 `assignedDatasetId`、`datasetStatus` 并提供查询接口 | 进行中 | 75% | 2025-10-12：新增按数据集查询、ACK回写与状态查询调用 |

> 备注：所有条目需与联邦流程文档同步更新，并在完成后追加测试报告及二次打卡记录。

## 下一步计划

- 在 `CompleteFederatedLearningFlowTest`/`V151` 中接入 Mock VM ACK 超时/失败模拟，编排端到端异常场景。
- 扩充集成断言，校验后端对异常 ACK 的任务状态、告警及缓存记录。
- 根据端到端结果，补齐 `FederatedTaskServiceImpl` 与 `VmAckTracker` 的异常分支处理与超时策略。

## 更新记录（2025-10-12）

- 已新增端到端异常用例：
  - `integration/CompleteFederatedLearningFlowTestV151_AckExceptions`（v1.5.1）
  - `integration/CompleteFederatedLearningFlowTest_AckExceptions`（v1.4 最小覆盖）
  - 用例覆盖 DATASET_COMPLETE_ACK 的失败与超时两类场景，断言包含：任务状态未推进、缓存进度（AckProgress）统计准确、失败 VM 记录及错误原因。
- 已将数据集 ACK 等待时间改为可配置：`federated.datasetAck.timeoutSeconds`（默认 120s），测试环境下缩短为 2s，加速失败/超时验证。
- 后续待办：
  - 若需要 TIMEOUT 结果在缓存中落地，可在分发指令发出后按 VM 设置 `AckCacheService.setAckTimeout`，并配套调度清理；当前用例已通过“等待返回值为 false”覆盖端到端超时分支。
