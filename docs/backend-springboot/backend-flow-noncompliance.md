# 后端联邦流程合规性问题清单

记录当前实现、集成测试与模拟虚拟机中未严格遵循《backend-complete-workflow》流程规范的要点。

## 服务端实现
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedTaskServiceImpl.java:214`  
  任务启动时即时调用 `sendTrainingStartCommand`，未等待数据分发与 VM 确认完成；风险是训练指令在数据集尚未分配时提前发送。（2025-10-12：已加入数据集ACK等待，观察中）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedTaskServiceImpl.java:742`  
  当 `assignedDatasetId` 为空时直接跳过发送训练指令，会在数据分发迟缓时导致任务无法继续，但流程层面仍视为成功。（2025-10-12：已在启动前校验并在发送指令时强制验证COMPLETED状态）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/orchestration/handler/DataDistributionStageHandler.java:57`  
  DATA_DISTRIBUTION 阶段被硬编码为“跳过”，真实的数据分发逻辑与编排工作流脱节，无法保证文档要求的阶段化顺序。（2025-10-12：已恢复阶段内触发分发，待验证）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/DataDistributionServiceImpl.java:818`  
  数据分发完成状态仅依据消息发送结果更新为 `COMPLETED`，未等待任何 VM 返回 `DATASET_COMPLETE_ACK`。（2025-10-12：已停止提前置为COMPLETED，待联调）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/WebSocketProtocolService.java:1480`  
  服务端对 `DATASET_COMPLETE_ACK` 仅记录日志，没有更新任务参与者状态或触发下一阶段的门控逻辑。（2025-10-12：新增状态持久化与ACK记录）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/service/impl/FederatedTaskServiceImpl.java:2649`  
  `queryDatasetStatus` 仍为 TODO 并直接返回成功，导致 `startFederatedLearningFlow` 未真实校验 VM 数据集就绪状态。（2025-10-12：已改为调用 WebSocket 查询并写入失败原因）
- `backend-springboot/feduwacomm-server/src/main/java/com/feduwacomm/orchestration/handler/ModelDistributionStageHandler.java:64`  
  当上下文缺失 `targetVmIds` 时会退化为查询前几个可用 VM 发放初始模型，可能与任务实际参与者不符。

## 集成测试缺陷
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/CompleteFederatedLearningFlowTestV151.java:421`  
  `waitForDatasetAllocation()` 仅做固定时长 `Thread.sleep`，未校验服务端是否真正收到并处理全部数据集确认。
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/CompleteFederatedLearningFlowTestV151.java:448`  
  数据切片验证完全依赖 Mock VM 内部状态，未检查 `task_participants` 表中的 `assigned_dataset_id` 与状态是否更新，无法发现服务端未持久化的问题。
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/CompleteFederatedLearningFlowTestV151.java:520`  
  训练阶段仅通过再次 `Thread.sleep` 等待梯度和模型，未断言 `FEDERATED_TASK_START` 是否在数据分发全部确认后才发送。

## 模拟虚拟机缺陷
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/mock/MockVirtualMachine.java:1028`  
  `prepareGradientUpload` 在未收到服务器 `GRADIENT_UPLOAD_PREPARE` 的情况下直接将通道标记为就绪，掩盖服务器未按协议发送准备指令的问题。
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/mock/MockVirtualMachine.java:3914`  
  `handleDatasetCompleteV15` 立即回送 `DATASET_COMPLETE_ACK`，缺乏等待或失败场景，导致测试无法覆盖“服务端应等待所有 ACK”这一要求。
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/mock/MockVirtualMachine.java:3424`  
  `scheduleGradientUploadV15` 在接收全局模型后自动排程梯度上传，即便服务器未按要求广播轮次开始或准备指令也会继续执行。
- `backend-springboot/feduwacomm-server/src/test/java/com/feduwacomm/integration/mock/MockVirtualMachine.java:1036`  
  `uploadGradients` 会在未收到服务器信号时自发调用 `prepareGradientUpload`，降低了对真实握手流程的约束力。
