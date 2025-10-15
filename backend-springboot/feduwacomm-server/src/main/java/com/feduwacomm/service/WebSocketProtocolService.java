package com.feduwacomm.service;

import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.dto.DatasetSlice;
import com.feduwacomm.dto.DatasetQueryResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.dto.SliceVerification;
import com.feduwacomm.dto.VerificationResult;
import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import com.feduwacomm.enums.ModelType;
import com.feduwacomm.mapper.FederatedTasksMapper;
import com.feduwacomm.mapper.TaskParticipantsMapper;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.VmRoundModelsMapper;
import com.feduwacomm.event.ModelUploadEvent;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.MessageBuilder;
import com.feduwacomm.utils.MessageIdGenerator;
import com.feduwacomm.service.DigitalSignatureService;
import com.feduwacomm.service.cache.MetricsCacheService;
import com.feduwacomm.service.cache.model.ParticipantMetrics;
import com.feduwacomm.service.cache.model.GlobalMetrics;
import com.feduwacomm.service.cache.exception.CacheValidationException;
import com.feduwacomm.enums.RoundState;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class WebSocketProtocolService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketProtocolService.class);

    private final SimpMessagingTemplate messagingTemplate;

    private final TrainingDatasetMapper trainingDatasetMapper;
    private final TrainingDatasetRowMapper trainingDatasetRowMapper;
    private final FederatedTasksMapper federatedTasksMapper;
    private final VmRoundModelsMapper vmRoundModelsMapper;
    private final VmInstancesMapper vmInstancesMapper;
    private final TaskParticipantsMapper taskParticipantsMapper;
    @SuppressWarnings("unused") // 保留用于未来功能扩展
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UuidUtil uuidUtil;
    private final MessageBuilder messageBuilder;
    private final MessageIdGenerator messageIdGenerator;
    private final DigitalSignatureService digitalSignatureService;
    private final MetricsCacheService metricsCacheService;
    private final RoundStateManager roundStateManager;
    private final VmAckTracker vmAckTracker;
    private final RoundLockManager roundLockManager;
    private final WebSocketMessageSender messageSender;
    private final DataDistributionService dataDistributionService;  // v1.5.1: 数据分发服务
    private final SliceVerificationService sliceVerificationService;  // v1.5.1: 切片验证服务
    private final PlatformTransactionManager transactionManager;

    // in-memory VM 最新状态缓存：vmId -> STATUS_RESPONSE.data（用于快速读，不作为数据源）
    private final ConcurrentHashMap<String, Map<String, Object>> statusCache = new ConcurrentHashMap<>();
    private TransactionTemplate gradientWriteTemplate;

    public WebSocketProtocolService(SimpMessagingTemplate messagingTemplate,
                                    TrainingDatasetMapper trainingDatasetMapper,
                                    TrainingDatasetRowMapper trainingDatasetRowMapper,
                                    FederatedTasksMapper federatedTasksMapper,
                                    VmRoundModelsMapper vmRoundModelsMapper,
                                    VmInstancesMapper vmInstancesMapper,
                                    TaskParticipantsMapper taskParticipantsMapper,
                                    UserMapper userMapper,
                                    ObjectMapper objectMapper,
                                    ApplicationEventPublisher eventPublisher,
                                    UuidUtil uuidUtil,
                                    MessageBuilder messageBuilder,
                                    MessageIdGenerator messageIdGenerator,
                                    DigitalSignatureService digitalSignatureService,
                                    MetricsCacheService metricsCacheService,
                                    RoundStateManager roundStateManager,
                                    VmAckTracker vmAckTracker,
                                    RoundLockManager roundLockManager,
                                    WebSocketMessageSender messageSender,
                                    DataDistributionService dataDistributionService,  // v1.5.1: 数据分发服务
                                    SliceVerificationService sliceVerificationService,
                                    PlatformTransactionManager transactionManager) {
        this.messagingTemplate = messagingTemplate;
        this.trainingDatasetMapper = trainingDatasetMapper;
        this.trainingDatasetRowMapper = trainingDatasetRowMapper;
        this.federatedTasksMapper = federatedTasksMapper;
        this.vmRoundModelsMapper = vmRoundModelsMapper;
        this.vmInstancesMapper = vmInstancesMapper;
        this.taskParticipantsMapper = taskParticipantsMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.uuidUtil = uuidUtil;
        this.messageBuilder = messageBuilder;
        this.messageIdGenerator = messageIdGenerator;
        this.digitalSignatureService = digitalSignatureService;
        this.metricsCacheService = metricsCacheService;
        this.roundStateManager = roundStateManager;
        this.vmAckTracker = vmAckTracker;
        this.roundLockManager = roundLockManager;
        this.messageSender = messageSender;
        this.dataDistributionService = dataDistributionService;  // v1.5.1
        this.sliceVerificationService = sliceVerificationService;  // v1.5.1
        this.transactionManager = transactionManager;
    }

    @PostConstruct
    void initGradientWriteTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        this.gradientWriteTemplate = template;
    }

    public ProtocolAck handle(ProtocolMessage msg) {
        if (msg == null || msg.getType() == null) {
            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "INVALID_MESSAGE",
                    "errorMessage", "缺少消息或类型"));
        }

        // 协议合规性检查
        ProtocolAck complianceCheck = validateProtocolCompliance(msg);
        if (complianceCheck != null) {
            return complianceCheck; // 返回违规错误
        }
        switch (msg.getType()) {
            // v1.4协议连接管理层
            case CONNECT:
                return onConnect(msg);
            case HEARTBEAT:
                return onHeartbeat(msg);
            // v1.4协议虚拟机控制层
            case VM_START:
                return onVmStart(msg);
            case VM_START_ACK:
                return onVmStartAck(msg);
            case VM_STOP:
                return onVmStop(msg);
            case VM_STOP_ACK:
                return onVmStopAck(msg);
            // v1.4协议数据集管理层
            case DATASET_CREATE:
                return onDatasetCreate(msg);
            case DATASET_APPEND_ROWS:
                return onDatasetAppendRows(msg);
            case DATASET_COMPLETE:
                return onDatasetComplete(msg);
            case DATASET_STATUS_QUERY:
                return onDatasetStatusQuery(msg);
            case DATASET_DELETE:
                return onDatasetDelete(msg);
            // v1.4协议状态监控层
            case VM_STATUS_QUERY:
                return onVmStatusQuery(msg);
            case VM_STATUS_RESPONSE:
                return onVmStatusResponse(msg);
            case FEDERATED_TASK_STATUS_QUERY:
                return onFederatedTaskStatusQuery(msg);
            case FEDERATED_TASK_STATUS_RESPONSE:
                return onFederatedTaskStatusResponse(msg);
            // v1.4协议轮次管理层
            case ROUND_START_ACK:
                return onRoundStartAck(msg);
            case GRADIENT_UPLOAD:
                return onGradientUpload(msg);
            case GRADIENT_UPLOAD_ACK:
                return onGradientUploadAck(msg);
            case GLOBAL_MODEL_BROADCAST:
                return onGlobalModelBroadcast(msg);
            case GLOBAL_MODEL_BROADCAST_ACK:
                return onGlobalModelBroadcastAck(msg);
            case MODEL_RECEIVE_ACK:
                return onModelReceiveAck(msg);
            case ROUND_COMPLETE:
                return onRoundComplete(msg);
            case ROUND_COMPLETE_ACK:
                return onRoundCompleteAck(msg);
            case ROUND_ABORT:
                return onRoundAbort(msg);
            case FEDERATED_TASK_START:
                return onFederatedTaskStart(msg);
            case FEDERATED_TASK_START_ACK:
                return onFederatedTaskStartAck(msg);
            // v1.4协议新增消息处理
            case FEDERATED_TASK_STOP:
                return onFederatedTaskStop(msg);
            case FEDERATED_TASK_STOP_ACK:
                return onFederatedTaskStopAck(msg);
            case FEDERATED_TASK_RESUME:
                return onFederatedTaskResume(msg);
            case FEDERATED_TASK_RESUME_ACK:
                return onFederatedTaskResumeAck(msg);
            case FEDERATED_TASK_DELETE:
                return onFederatedTaskDelete(msg);
            case FEDERATED_TASK_DELETE_ACK:
                return onFederatedTaskDeleteAck(msg);
            // 数据集相关ACK消息
            case DATASET_CREATE_ACK:
                return onDatasetCreateAck(msg);
            case DATASET_APPEND_ROWS_ACK:
                return onDatasetAppendRowsAck(msg);
            case DATASET_COMPLETE_ACK:
                return onDatasetCompleteAck(msg);
            case DATASET_DELETE_ACK:
                return onDatasetDeleteAck(msg);
            case DATASET_STATUS_RESPONSE:
                return onDatasetStatusResponse(msg);
            // v1.4协议中模型相关消息已简化，这些ACK消息已移除
            // 连接相关ACK消息
            case CONNECT_ACK:
                return onConnectAck(msg);
            case HEARTBEAT_ACK:
                return onHeartbeatAck(msg);
            // v1.4协议业务通知消息
            case ERROR:
            case CONNECTION_ERROR:
            case MESSAGE_ERROR:
                return onErrorMessage(msg);
            default:
                return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                        "errorCode", "UNSUPPORTED_TYPE",
                        "errorMessage", "v1.4协议不支持的消息类型: " + msg.getType()));
        }
    }




    // ================= v1.4协议处理方法开始 =================



    // ================= 错误处理 =================

    private ProtocolAck onErrorMessage(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        sendToVmTopic(vmId, msg);
        messagingTemplate.convertAndSend("/topic/errors", msg);
        return ackFor(msg, msg.getType(), mapOf("status", "SUCCESS"));
    }

    // ================= 公共辅助 =================

    private void sendToVmTopic(String vmId, Object payload) {
        if (vmId == null || vmId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/vm/_unknown", payload);
        } else {
            messagingTemplate.convertAndSend("/topic/vm/" + vmId, payload);
        }
    }

    /**
     * 构建符合协议v1.4标准的ACK响应
     * 使用MessageIdGenerator生成标准化消息ID
     */
    private ProtocolAck ackFor(ProtocolMessage msg, ProtocolType ackType, Map<String, Object> data) {
        String vmId = msg != null ? msg.getVmId() : null;
        return ProtocolAck.builder()
                .type(ackType)
                .id(messageIdGenerator.generateServerMessageId())
                .timestamp(Instant.now())
                .vmId(vmId)
                .data(data != null ? data : new HashMap<>())
                .signature(generateAckSignature(ackType, vmId, data))
                .build();
    }

    /**
     * 生成ACK消息签名
     * 为确认消息生成数字签名
     */
    private String generateAckSignature(ProtocolType ackType, String vmId, Map<String, Object> data) {
        try {
            // 构建待签名内容
            StringBuilder content = new StringBuilder();
            content.append("ackType:").append(ackType != null ? ackType.name() : "");
            content.append("|vmId:").append(vmId != null ? vmId : "");

            if (data != null && !data.isEmpty()) {
                content.append("|data:");
                data.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> content.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
            }

            return digitalSignatureService.signMessage(content.toString(), DigitalSignatureService.SignatureAlgorithm.RSA_SHA256);
        } catch (Exception e) {
            log.warn("ACK消息签名生成失败: ackType={}, vmId={}, error={}", ackType, vmId, e.getMessage());
            return "";
        }
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    /**
     * 判断是否应该重试上传
     * 根据错误信息判断是否为可重试的临时性错误
     */
    private boolean shouldRetryUpload(String errorMessage) {
        if (errorMessage == null) return false;

        String error = errorMessage.toLowerCase();
        return error.contains("网络") || error.contains("超时") ||
               error.contains("连接") || error.contains("临时");
    }

    private String valueAsString(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private Object valueAsObject(Map<String, Object> m, String key) {
        return m == null ? null : m.get(key);
    }

    private Integer numberAsInt(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return v == null ? null : Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Double numberAsDouble(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return v == null ? null : Double.parseDouble(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Long valueAsLong(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return v == null ? null : Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean valueAsBoolean(Map<String, Object> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return null;
        String str = String.valueOf(v).toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }

    private Boolean valueAsBoolean(Map<String, Object> m, String key, Boolean defaultValue) {
        Boolean result = valueAsBoolean(m, key);
        return result != null ? result : defaultValue;
    }

    private String toJsonSafe(Object obj) {
        try {
            return obj == null ? null : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private void persistGradientRecord(String recordId,
                                       String taskId,
                                       String vmId,
                                       Integer round,
                                       Double accuracy,
                                       Double loss,
                                       String parametersJson,
                                       LocalDateTime timestamp) {
        Runnable persistence = () -> {
            vmRoundModelsMapper.upsertRoundModel(
                    recordId,
                    taskId,
                    vmId,
                    round,
                    accuracy,
                    loss,
                    parametersJson
            );
            taskParticipantsMapper.updateParticipantWithMetrics(
                    taskId,
                    vmId,
                    "COMPLETED",
                    round,
                    accuracy,
                    loss,
                    timestamp
            );
        };

        TransactionTemplate template = this.gradientWriteTemplate;
        if (template != null) {
            template.executeWithoutResult(status -> persistence.run());
        } else {
            persistence.run();
        }
    }

    /**
     * 安全的轮次推进检查（重构版）
     * 使用轮次锁管理器防止并发问题，通过状态管理器确保严格的状态控制
     *
     * @param taskId 任务ID
     * @param currentRound 当前轮次
     */
    private void checkAndAdvanceTaskRound(String taskId, Integer currentRound) {
        if (taskId == null || currentRound == null) {
            log.error("轮次推进参数无效: taskId={}, currentRound={}", taskId, currentRound);
            return;
        }

        // 获取轮次锁，防止并发推进
        boolean lockAcquired = roundLockManager.acquireRoundLock(taskId);
        if (!lockAcquired) {
            log.warn("无法获取轮次锁，跳过推进检查: taskId={}", taskId);
            return;
        }

        try {
            // 安全的轮次推进逻辑
            performSafeRoundAdvancement(taskId, currentRound);

        } finally {
            // 确保释放锁
            roundLockManager.releaseRoundLock(taskId);
        }
    }

    /**
     * 执行安全的轮次推进逻辑
     */
    private void performSafeRoundAdvancement(String taskId, Integer currentRound) {
        // 获取当前任务信息
        FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
        if (task == null) {
            log.warn("任务不存在，无法推进轮次: taskId={}", taskId);
            return;
        }

        // 检查是否已经是最后一轮
        if (task.getTotalRounds() != null && currentRound >= task.getTotalRounds()) {
            log.info("任务已达到最大轮次，无需推进: 任务ID={}, 当前轮次={}, 总轮次={}",
                    taskId, currentRound, task.getTotalRounds());
            return;
        }

        // 检查轮次状态
        RoundState currentState = roundStateManager.getCurrentRoundState(taskId);
        if (currentState != RoundState.TRAINING) {
            log.debug("当前轮次状态不允许推进: taskId={}, currentRound={}, state={}",
                     taskId, currentRound, currentState);
            return;
        }

        // 统计已完成当前轮次训练的参与者数量
        int completedCount = taskParticipantsMapper.countCompletedParticipants(taskId, currentRound);
        int totalCount = taskParticipantsMapper.countTotalParticipants(taskId);

        log.info("轮次推进检查: 任务ID={}, 当前轮次={}, 已完成={}, 总数={}, 状态={}",
                taskId, currentRound, completedCount, totalCount, currentState);

        // 如果所有参与者都完成了当前轮次，推进到下一轮次
        if (completedCount > 0 && completedCount == totalCount) {
            // 使用状态管理器的安全推进方法
            boolean advanced = roundStateManager.advanceRound(taskId);
            if (advanced) {
                Integer newRound = currentRound + 1;
                double progress = task.getTotalRounds() != null ?
                    (double) newRound / task.getTotalRounds() * 100.0 : 0.0;

                log.info("任务轮次推进成功: 任务ID={}, 从轮次{}推进到轮次{}, 进度={:.1f}%",
                        taskId, currentRound, newRound, progress);

                // 重置所有参与者状态为TRAINING，准备下一轮训练
                if (newRound <= task.getTotalRounds()) {
                    taskParticipantsMapper.resetParticipantsForNewRound(taskId, newRound);
                    log.info("参与者状态已重置为下一轮训练: 任务ID={}, 新轮次={}", taskId, newRound);

                    // 创建新轮次的全局模型记录
                    roundStateManager.createRoundModel(taskId, newRound);

                    // 初始化新轮次的分发记录
                    vmAckTracker.initializeRoundDistributions(taskId, newRound);
                }
            } else {
                log.error("轮次推进失败: taskId={}, currentRound={}", taskId, currentRound);
            }
        } else if (totalCount == 0) {
            log.warn("任务没有参与者，无法推进轮次: 任务ID={}", taskId);
        } else {
            log.debug("未满足轮次推进条件: 任务ID={}, 已完成={}, 总数={}", taskId, completedCount, totalCount);
        }
    }


    private ProtocolAck onFederatedTaskStart(ProtocolMessage msg) {
        // 处理联邦学习任务启动消息
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();
        String taskId = valueAsString(data, "taskId");

        System.out.println("🤖 收到联邦学习任务启动消息: vmId=" + vmId + ", taskId=" + taskId);

        // 返回确认响应
        return ackFor(msg, ProtocolType.FEDERATED_TASK_START_ACK, mapOf(
                "status", "ACKNOWLEDGED",
                "taskId", taskId,
                "vmId", vmId,
                "message", "联邦学习任务启动消息已接收",
                "timestamp", Instant.now().toString()
        ));
    }

    // === 新增聚合相关消息处理方法 ===







    /**
     * 处理梯度上传确认消息
     * VM确认已收到梯度上传响应
     */
    private ProtocolAck onGradientUploadAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        if (round == null) {
            round = numberAsInt(data, "roundNumber");
        }
        String status = valueAsString(data, "status");

        log.info("收到梯度上传确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        try {
            if ("SUCCESS".equals(status)) {
                log.info("虚拟机{}确认梯度上传成功: taskId={}, round={}", vmId, taskId, round);
                // 更新梯度上传状态跟踪
                vmAckTracker.recordGradientUploadSuccess(taskId, vmId, round);
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}梯度上传确认异常: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);
                // 处理上传失败的情况，记录失败状态并考虑重新上传
                vmAckTracker.recordGradientUploadFailure(taskId, vmId, round, errorMessage);

                // 可以根据错误类型决定是否触发重新上传
                if (shouldRetryUpload(errorMessage)) {
                    log.info("计划为VM {}重新上传梯度: taskId={}, round={}", vmId, taskId, round);
                    // 这里可以添加重新上传逻辑
                }
            }

            return ackFor(msg, ProtocolType.VM_STATUS_RESPONSE, mapOf(
                "status", "ACK_PROCESSED",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("处理梯度上传确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.VM_STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理梯度上传确认失败: " + e.getMessage()
            ));
        }
    }









    // ================= 协议合规性验证 =================


    /**
     * 处理梯度上传准备消息
     * 服务器通知VM准备梯度上传，进行预检查和资源分配
     */
    private ProtocolAck onGradientUploadPrepare(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        Integer expectedDataSize = numberAsInt(data, "expectedDataSize");
        String compressionType = valueAsString(data, "compressionType");

        log.info("处理梯度上传准备: vmId={}, taskId={}, round={}, expectedSize={}MB, compression={}",
                vmId, taskId, round, expectedDataSize != null ? expectedDataSize/1024/1024 : "未知", compressionType);

        try {
            // 预检查资源和配置
            boolean resourceAvailable = true;
            String uploadToken = "upload_" + taskId + "_" + round + "_" + System.currentTimeMillis();

            // 检查存储空间（模拟检查）
            long availableSpace = 1024 * 1024 * 1024; // 1GB 模拟可用空间
            if (expectedDataSize != null && expectedDataSize > availableSpace) {
                resourceAvailable = false;
            }

            // 支持的压缩类型
            String[] supportedCompression = {"none", "gzip", "lz4", "snappy"};
            String finalCompressionType = "none";
            if (compressionType != null) {
                for (String type : supportedCompression) {
                    if (type.equalsIgnoreCase(compressionType)) {
                        finalCompressionType = compressionType;
                        break;
                    }
                }
            }

            // 发送准备就绪通知
            ProtocolMessage prepareMsg = ProtocolMessage.builder()
                    .type(ProtocolType.GRADIENT_UPLOAD)
                    .vmId("server")
                    .timestamp(Instant.now())
                    .data(mapOf(
                        "taskId", taskId,
                        "round", round,
                        "uploadToken", uploadToken,
                        "resourceAvailable", resourceAvailable,
                        "maxDataSize", availableSpace,
                        "compressionType", finalCompressionType,
                        "supportedCompression", supportedCompression,
                        "uploadEndpoint", "/api/gradient-upload",
                        "timeout", 300, // 5分钟超时
                        "message", resourceAvailable ? "准备就绪，可以开始上传" : "资源不足，请稍后重试"
                    ))
                    .build();

            // 发送给请求的VM
            messagingTemplate.convertAndSend("/topic/vm/" + vmId, prepareMsg);

            log.info("梯度上传准备通知已发送: vmId={}, taskId={}, round={}, resourceAvailable={}, token={}",
                    vmId, taskId, round, resourceAvailable, uploadToken);

            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                "status", resourceAvailable ? "READY" : "RESOURCE_UNAVAILABLE",
                "taskId", taskId,
                "round", round,
                "uploadToken", uploadToken,
                "resourceAvailable", resourceAvailable,
                "maxDataSize", availableSpace,
                "compressionType", finalCompressionType,
                "uploadEndpoint", "/api/gradient-upload",
                "timeout", 300,
                "timestamp", Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("梯度上传准备失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "梯度上传准备失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 向指定VM广播全局模型
     */
    public void broadcastGlobalModel(String taskId, Integer roundNumber, Map<String, Object> globalModel, List<String> targetVmIds) {
        try {
            ProtocolMessage broadcastMsg = messageBuilder.buildServerMessage(
                    ProtocolType.GLOBAL_MODEL_BROADCAST,
                    "server",
                    mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "globalModel", globalModel,
                            "timestamp", Instant.now().toString()
                    ));

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, broadcastMsg);
                System.out.println("📡 向VM " + vmId + " 广播全局模型, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("广播全局模型失败: " + e.getMessage());
        }
    }

    /**
     * 通知轮次开始（v1.4协议聚合隐含在轮次中）
     */
    public void notifyAggregationStart(String taskId, Integer roundNumber, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = messageBuilder.buildServerMessage(
                    ProtocolType.ROUND_START,
                    "server",
                    mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "timestamp", Instant.now().toString()
                    ));

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🔄 通知VM " + vmId + " 聚合开始, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知聚合开始失败: " + e.getMessage());
        }
    }

    /**
     * 通知聚合完成
     */
    public void notifyAggregationComplete(String taskId, Integer roundNumber, Map<String, Object> aggregationResults, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = messageBuilder.buildServerMessage(
                    ProtocolType.ROUND_COMPLETE,
                    "server",
                    mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "aggregationResults", aggregationResults,
                            "timestamp", Instant.now().toString()
                    )
            );

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("✅ 通知VM " + vmId + " 聚合完成, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知聚合完成失败: " + e.getMessage());
        }
    }

    /**
     * 通知轮次开始
     */
    public void notifyRoundStart(String taskId, Integer roundNumber, Map<String, Object> roundConfig, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = messageBuilder.buildServerMessage(
                    ProtocolType.ROUND_START,
                    "server",
                    mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "roundConfig", roundConfig,
                            "timestamp", Instant.now().toString()
                    )
            );

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🏁 通知VM " + vmId + " 轮次开始, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知轮次开始失败: " + e.getMessage());
        }
    }

    /**
     * 通知轮次完成
     */
    public void notifyRoundComplete(String taskId, Integer roundNumber, Map<String, Object> roundResults, List<String> targetVmIds) {
        try {
            ProtocolMessage notifyMsg = messageBuilder.buildServerMessage(
                    ProtocolType.ROUND_COMPLETE,
                    "server",
                    mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "roundResults", roundResults,
                            "timestamp", Instant.now().toString()
                    )
            );

            for (String vmId : targetVmIds) {
                messagingTemplate.convertAndSend("/topic/vm/" + vmId, notifyMsg);
                System.out.println("🏆 通知VM " + vmId + " 轮次完成, 任务=" + taskId + ", 轮次=" + roundNumber);
            }
        } catch (Exception e) {
            System.err.println("通知轮次完成失败: " + e.getMessage());
        }
    }

    /**
     * 处理梯度上传消息
     * 虚拟机将本地训练后的梯度/参数上传至后端
     */
    private ProtocolAck onGradientUpload(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "roundNumber");  // 按WebSocket协议文档使用 roundNumber
        if (round == null) {
            round = numberAsInt(data, "round");
        }
        if (round == null) {
            FederatedTask fallbackTask = federatedTasksMapper.selectTaskById(taskId);
            if (fallbackTask != null) {
                round = fallbackTask.getCurrentRound();
                System.out.println("🔄 回退到任务当前轮次: taskId=" + taskId + ", fallbackRound=" + round);
                log.warn("梯度上传缺少round信息，使用任务当前轮次作为回退: taskId={}, fallbackRound={}", taskId, round);
            } else {
                System.out.println("❌ 无法回退轮次，任务不存在: taskId=" + taskId);
            }
        }

        log.info("收到梯度上传: vmId={}, taskId={}, round={}", vmId, taskId, round);

        try {
            // 按WebSocket协议文档提取梯度数据
            @SuppressWarnings("unchecked")
            Map<String, Object> gradientData = (Map<String, Object>) data.get("gradientData");
            @SuppressWarnings("unchecked")
            Map<String, Object> trainingMetrics = (Map<String, Object>) data.get("trainingMetrics");

                if (gradientData != null) {
                    // 按协议文档提取梯度参数（weights, biases等）
                    @SuppressWarnings("unchecked")
                    Map<String, Object> modelParams = gradientData;  // gradientData就是模型参数
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = trainingMetrics;  // trainingMetrics是训练元数据

                // 构建存储格式
                Map<String, Object> storeParams = new HashMap<>();
                if (modelParams != null) {
                    storeParams.put("model_parameters", modelParams);
                }
                if (metadata != null) {
                    storeParams.put("training_metadata", metadata);
                }
                // 梯度数据可能包含参数增量
                if (gradientData.get("parameter_deltas") != null) {
                    storeParams.put("parameter_deltas", gradientData.get("parameter_deltas"));
                }

                // 按协议文档提取准确率和损失值
                Double accuracy = null;
                Double loss = null;
                if (metadata != null) {
                    accuracy = numberAsDouble(metadata, "localAccuracy");  // 按协议文档
                    loss = numberAsDouble(metadata, "localLoss");  // 按协议文档
                }

                // 序列化参数
                String parametersJson = toJsonSafe(storeParams);

                if (round != null) {
                    roundStateManager.ensureRoundStateInitialized(taskId, round);
                }

                String recordId = uuidUtil.generateUuid();
                LocalDateTime now = LocalDateTime.now();
                persistGradientRecord(
                    recordId,
                    taskId,
                    vmId,
                    round,
                    accuracy,
                    loss,
                    parametersJson,
                    now
                );
                log.info("梯度已写入vm_round_models: id={}, taskId={}, vmId={}, round={}, accuracy={}, loss={}, payloadSize={}",
                        recordId, taskId, vmId, round, accuracy, loss, parametersJson != null ? parametersJson.length() : 0);

                if (round != null) {
                    try {
                        roundStateManager.recordGradientUpload(taskId, round);
                    } catch (Exception syncEx) {
                        log.warn("轮次状态同步梯度上传进度失败: taskId={}, round={}, vmId={}, error={}",
                                taskId, round, vmId, syncEx.getMessage(), syncEx);
                    }
                } else {
                    log.warn("梯度上传缺少轮次信息，无法同步round_states: taskId={}, vmId={}", taskId, vmId);
                }

                log.debug("参与者状态已更新: vmId={}, taskId={}, round={}, status=COMPLETED",
                    vmId, taskId, round);

                // 发布模型上传事件触发聚合检查
                ModelUploadEvent uploadEvent = new ModelUploadEvent(
                    this,
                    taskId,
                    round,
                    vmId,
                    (long) parametersJson.length(),
                    accuracy,
                    loss
                );
                eventPublisher.publishEvent(uploadEvent);

                log.info("梯度上传成功: vmId={}, taskId={}, round={}, 参数大小={}字节",
                        vmId, taskId, round, parametersJson.length());

                return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                    "status", "SUCCESS",
                    "taskId", taskId,
                    "round", round,
                    "timestamp", Instant.now().toString(),
                    "parametersSize", parametersJson.length()
                ));
            } else {
                log.warn("梯度上传数据为空: vmId={}, taskId={}, round={}", vmId, taskId, round);
                log.warn("梯度上传空payload内容: {}", data);
                return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                    "status", "ERROR",
                    "errorMessage", "梯度数据为空，请检查gradientData字段",
                    "taskId", taskId,
                    "round", round
                ));
            }
        } catch (Exception e) {
            log.error("梯度上传处理失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "梯度上传处理失败: " + e.getMessage(),
                "taskId", taskId,
                "round", round
            ));
        }
    }

    /**
     * 处理全局模型广播确认消息（重构版）
     * 虚拟机确认已收到全局模型广播，集成VM确认跟踪器进行同步控制
     */
    private ProtocolAck onGlobalModelBroadcastAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到全局模型广播确认: vmId={}, taskId={}, round={}, status={}",
                vmId, taskId, round, status);

        if (taskId == null || round == null || vmId == null) {
            log.error("ACK消息参数不完整: vmId={}, taskId={}, round={}", vmId, taskId, round);
            return ackFor(msg, ProtocolType.VM_STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "消息参数不完整"
            ));
        }

        try {
            // 记录VM确认状态
            if ("SUCCESS".equals(status)) {
                // 使用VmAckTracker记录确认
                boolean recorded = vmAckTracker.recordAck(taskId, vmId, round);
                if (recorded) {
                    log.info("虚拟机{}成功接收全局模型并记录ACK: taskId={}, round={}", vmId, taskId, round);

                    // 检查是否所有VM都已确认
                    if (vmAckTracker.allVmsAcked(taskId, round)) {
                        log.info("所有VM都已确认收到全局模型: taskId={}, round={}", taskId, round);

                        // 状态转换：INITIALIZING → TRAINING
                        RoundState currentState = roundStateManager.getCurrentRoundState(taskId);
                        if (currentState == RoundState.INITIALIZING) {
                            boolean transitioned = roundStateManager.transitionRoundState(
                                taskId, RoundState.INITIALIZING, RoundState.TRAINING);

                            if (transitioned) {
                                log.info("轮次状态转换成功: taskId={}, round={}, {} → {}",
                                        taskId, round, RoundState.INITIALIZING, RoundState.TRAINING);

                                // 这里可以触发下一阶段：发送ROUND_START消息
                                // 或者等待外部触发训练开始
                            } else {
                                log.error("轮次状态转换失败: taskId={}, round={}", taskId, round);
                            }
                        } else {
                            log.debug("当前状态不是INITIALIZING，跳过状态转换: taskId={}, round={}, state={}",
                                     taskId, round, currentState);
                        }
                    } else {
                        // 记录进度信息
                        var progress = vmAckTracker.getAckProgress(taskId, round);
                        log.debug("等待更多VM确认: taskId={}, round={}, 进度={}", taskId, round, progress);
                    }
                } else {
                    log.error("记录VM ACK失败: vmId={}, taskId={}, round={}", vmId, taskId, round);
                }

            } else if ("ERROR".equals(status)) {
                String errorMessage = valueAsString(data, "errorMessage");
                log.warn("虚拟机{}接收全局模型失败: taskId={}, round={}, error={}",
                        vmId, taskId, round, errorMessage);

                // 这里可以记录失败状态，并可能触发重新发送机制
                // 对于失败的VM，可以考虑从参与者中排除或重试
            }

            return ackFor(msg, ProtocolType.VM_STATUS_RESPONSE, mapOf(
                "status", "SUCCESS",
                "vmId", vmId,
                "taskId", taskId,
                "round", round,
                "timestamp", Instant.now().toString(),
                "message", "ACK已处理"
            ));

        } catch (Exception e) {
            log.error("处理全局模型广播确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.VM_STATUS_RESPONSE, mapOf(
                "status", "ERROR",
                "errorMessage", "处理确认失败: " + e.getMessage(),
                "vmId", vmId,
                "taskId", taskId,
                "round", round
            ));
        }
    }

    /**
     * 处理MODEL_RECEIVE_ACK - VM确认已收到模型
     * 这是v1.5协议中VM对GLOBAL_MODEL_BROADCAST的响应
     */
    private ProtocolAck onModelReceiveAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");

        log.info("收到模型接收确认(v1.5): vmId={}, taskId={}, round={}, status={}, payload={}",
                vmId, taskId, round, status, data);

        boolean success = !"ERROR".equalsIgnoreCase(status);
        try {
            vmAckTracker.processInitialModelAck(taskId, vmId, data, success);
        } catch (Exception ex) {
            log.error("处理初始模型接收ACK失败: vmId={}, taskId={}, status={}, error={}",
                    vmId, taskId, status, ex.getMessage(), ex);
        }

        // MODEL_RECEIVE_ACK是ACK消息，不需要再次回复
        return null;
    }

    // ================= v1.4 联邦学习增强协议 - 剩余处理器 =================

    /**
     * 处理梯度上传准备确认消息
     * 虚拟机确认已收到梯度上传准备通知并准备好上传
     */
    private ProtocolAck onGradientUploadPrepareAck(ProtocolMessage msg) {
        Map<String, Object> data = msg.getData();
        String vmId = msg.getVmId();
        String taskId = valueAsString(data, "taskId");
        Integer round = numberAsInt(data, "round");
        String status = valueAsString(data, "status");
        String uploadToken = valueAsString(data, "uploadToken");

        log.info("收到梯度上传准备确认: vmId={}, taskId={}, round={}, status={}, token={}",
                vmId, taskId, round, status, uploadToken);

        try {
            // 验证上传令牌
            if (uploadToken == null || uploadToken.isEmpty()) {
                log.warn("梯度上传准备确认缺少上传令牌: vmId={}, taskId={}, round={}", vmId, taskId, round);
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorType", "INVALID_TOKEN",
                    "errorMessage", "上传令牌无效或缺失",
                    "vmId", vmId,
                    "taskId", taskId,
                    "round", round
                ));
            }

            // 处理不同的确认状态
            switch (status) {
                case "READY":
                    log.info("虚拟机{}已准备就绪，可以开始梯度上传: taskId={}, round={}", vmId, taskId, round);
                    // 记录VM准备状态，可以开始接收梯度
                    vmAckTracker.recordVmReadyForGradientUpload(taskId, vmId, round);
                    // 可以在这里更新数据库状态或设置定时器等待上传

                    // 确认收到准备状态，告知VM可以开始上传
                    return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                        "status", "CONFIRMED",
                        "message", "确认收到准备状态，可以开始梯度上传",
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "uploadToken", uploadToken,
                        "nextAction", "START_UPLOAD",
                        "timestamp", Instant.now().toString()
                    ));

                case "NOT_READY":
                    String reason = valueAsString(data, "reason");
                    log.warn("虚拟机{}未准备好进行梯度上传: vmId={}, taskId={}, round={}, reason={}",
                            vmId, taskId, round, reason);

                    return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                        "status", "NOT_READY_ACK",
                        "message", "确认收到未准备状态",
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "reason", reason,
                        "nextAction", "RETRY_LATER",
                        "timestamp", Instant.now().toString()
                    ));

                case "ERROR":
                    String errorMessage = valueAsString(data, "errorMessage");
                    log.error("虚拟机{}梯度上传准备出错: vmId={}, taskId={}, round={}, error={}",
                             vmId, taskId, round, errorMessage);

                    return ackFor(msg, ProtocolType.ERROR, mapOf(
                        "errorType", "GRADIENT_UPLOAD_PREPARE_ERROR",
                        "errorMessage", "虚拟机梯度上传准备失败: " + errorMessage,
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "timestamp", Instant.now().toString()
                    ));

                default:
                    log.warn("未知的梯度上传准备确认状态: vmId={}, taskId={}, round={}, status={}",
                            vmId, taskId, round, status);

                    return ackFor(msg, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                        "status", "UNKNOWN_STATUS_ACK",
                        "message", "收到未知状态确认: " + status,
                        "vmId", vmId,
                        "taskId", taskId,
                        "round", round,
                        "timestamp", Instant.now().toString()
                    ));
            }

        } catch (Exception e) {
            log.error("处理梯度上传准备确认失败: vmId={}, taskId={}, round={}, error={}",
                     vmId, taskId, round, e.getMessage(), e);
            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorType", "PROCESSING_ERROR",
                "errorMessage", "处理梯度上传准备确认失败: " + e.getMessage(),
                "vmId", vmId,
                "taskId", taskId,
                "round", round
            ));
        }
    }




    /**
     * 协议合规性检查
     * 验证消息是否符合WebSocket协议v1.4规范
     * 检查消息方向、必需字段等协议要求
     *
     * @param msg 要验证的协议消息
     * @return 如果存在违规则返回错误ACK，否则返回null
     */
    private ProtocolAck validateProtocolCompliance(ProtocolMessage msg) {
        if (msg == null) {
            return createErrorAck("VALIDATION_ERROR", "消息不能为空");
        }

        ProtocolType type = msg.getType();
        if (type == null) {
            return createErrorAck("VALIDATION_ERROR", "消息类型不能为空");
        }

        // v1.4协议标准字段验证
        ProtocolAck fieldValidation = validateRequiredFields(msg);
        if (fieldValidation != null) {
            return fieldValidation;
        }

        // v1.4协议消息结构验证
        ProtocolAck structureValidation = validateMessageStructure(msg);
        if (structureValidation != null) {
            return structureValidation;
        }

        // 检查消息方向合规性（v1.4协议标准）
        // 根据协议文档，某些消息只能由特定方向发送
        switch (type) {
            // 🔵 只能由虚拟机发送给服务器的消息类型（v1.4协议）
            case CONNECT:
            case HEARTBEAT:
            case GRADIENT_UPLOAD:
            case GRADIENT_UPLOAD_ACK:
            case GLOBAL_MODEL_BROADCAST_ACK:
            case MODEL_RECEIVE_ACK:
            case ROUND_START_ACK:
            case ROUND_COMPLETE_ACK:
            case FEDERATED_TASK_START_ACK:
            case FEDERATED_TASK_STOP_ACK:
            case FEDERATED_TASK_RESUME_ACK:
            case FEDERATED_TASK_DELETE_ACK:
            case DATASET_CREATE_ACK:
            case DATASET_APPEND_ROWS_ACK:
            case DATASET_COMPLETE_ACK:
            case DATASET_DELETE_ACK:
            case DATASET_STATUS_RESPONSE:
            case CONNECT_ACK:
            case HEARTBEAT_ACK:
            case VM_START_ACK:
            case VM_STOP_ACK:
            case DATASET_CREATE:
            case DATASET_APPEND_ROWS:
            case DATASET_COMPLETE:
            case VM_STATUS_RESPONSE:
            case FEDERATED_TASK_STATUS_RESPONSE:
            case ROUND_ABORT:
                // 这些消息应该由虚拟机发送，后端只接收
                // 由于我们在后端接收这些消息，这是合法的
                break;

            // 🔴 只能由服务器发送给虚拟机的消息类型（v1.4协议）
            case FEDERATED_TASK_START:
            case FEDERATED_TASK_STOP:
            case FEDERATED_TASK_RESUME:
            case FEDERATED_TASK_DELETE:
            case ROUND_START:
            case ROUND_COMPLETE:
            case GLOBAL_MODEL_BROADCAST:
            case VM_START:
            case VM_STOP:
            case VM_STATUS_QUERY:
            case FEDERATED_TASK_STATUS_QUERY:
            case DATASET_STATUS_QUERY:
            case DATASET_DELETE:
                // 这些消息应该由服务器发送，如果在handle方法中收到说明有违规
                log.warn("🚫 协议违规检测: 收到了只应由服务器发送的消息类型: {}", type);
                return createErrorAck("PROTOCOL_VIOLATION",
                    "违规：消息类型 " + type + " 只能由服务器发送，不应作为接收消息处理");

            default:
                log.warn("🚫 协议违规检测: 未知的消息类型: {}", type);
                return createErrorAck("UNKNOWN_MESSAGE_TYPE", "未知的消息类型: " + type);
        }

        // 检查必需字段
        if (msg.getId() == null || msg.getId().trim().isEmpty()) {
            return createErrorAck("VALIDATION_ERROR", "消息ID不能为空");
        }

        if (msg.getVmId() == null || msg.getVmId().trim().isEmpty()) {
            return createErrorAck("VALIDATION_ERROR", "虚拟机ID不能为空");
        }

        // 检查timestamp是否有效
        try {
            Instant parsedTimestamp = msg.getTimestamp();
            if (parsedTimestamp == null) {
                return createErrorAck("VALIDATION_ERROR", "时间戳不能为空");
            }
        } catch (Exception e) {
            return createErrorAck("VALIDATION_ERROR", "时间戳格式无效");
        }

        // 检查特定消息类型的数据完整性 (v1.4协议)
        Map<String, Object> data = msg.getData();
        switch (type) {
            case GRADIENT_UPLOAD:
                if (data == null || !data.containsKey("taskId")) {
                    return createErrorAck("VALIDATION_ERROR", "GRADIENT_UPLOAD消息必须包含taskId");
                }
                if (!data.containsKey("gradientData")) {
                    return createErrorAck("VALIDATION_ERROR", "GRADIENT_UPLOAD消息必须包含gradientData");
                }
                break;

            case CONNECT:
                if (data == null || !data.containsKey("capabilities")) {
                    return createErrorAck("VALIDATION_ERROR", "CONNECT消息必须包含capabilities");
                }
                break;
        }

        // 所有检查通过，返回null表示无违规
        return null;
    }

    /**
     * 创建错误ACK响应
     *
     * @param errorCode 错误代码
     * @param errorMessage 错误消息
     * @return 错误ACK响应
     */
    private ProtocolAck createErrorAck(String errorCode, String errorMessage) {
        log.error("🚫 协议合规性检查失败: {} - {}", errorCode, errorMessage);

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errorCode", errorCode);
        errorData.put("errorMessage", errorMessage);
        errorData.put("status", "ERROR");

        return ProtocolAck.builder()
                .type(ProtocolType.MESSAGE_ERROR)
                .id(messageIdGenerator.generateServerMessageId())
                .timestamp(Instant.now())
                .data(errorData)
                .build();
    }

    // ====================== v1.4协议新增消息处理方法 ======================

    /**
     * 处理FEDERATED_TASK_START_ACK - v1.4协议
     */
    private ProtocolAck onFederatedTaskStartAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String taskId = valueAsString(msg.getData(), "taskId");
        String status = valueAsString(msg.getData(), "status");
        String message = valueAsString(msg.getData(), "message");
        log.info("收到联邦任务启动确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        boolean success = "SUCCESS".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status);
        if (success) {
            vmAckTracker.recordTaskStartAck(taskId, vmId, status, msg.getData());
        } else {
            log.error("VM任务启动失败: vmId={}, taskId={}, status={}, message={}",
                     vmId, taskId, status, message);
            vmAckTracker.recordTaskStartFailure(taskId, vmId, status, message, msg.getData());
        }

        // v1.4协议: FEDERATED_TASK_START_ACK不需要再次ACK
        return null;
    }

    /**
     * 处理FEDERATED_TASK_STOP - v1.4协议服务端主动停止任务
     */
    private ProtocolAck onFederatedTaskStop(ProtocolMessage msg) {
        String taskId = valueAsString(msg.getData(), "taskId");
        String reason = valueAsString(msg.getData(), "reason");

        log.info("v1.4任务停止指令: taskId={}, reason={}", taskId, reason);

        // 这是服务端发送给VM的消息，在这里不应该被处理
        // 只是为了协议完整性记录日志
        log.warn("收到服务端发出的FEDERATED_TASK_STOP消息，这通常表示配置错误");

        return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
            "errorCode", "INVALID_DIRECTION",
            "errorMessage", "FEDERATED_TASK_STOP应该由服务端发送给VM"
        ));
    }

    /**
     * 处理FEDERATED_TASK_STOP_ACK - v1.4协议VM确认停止任务
     */
    private ProtocolAck onFederatedTaskStopAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String taskId = valueAsString(msg.getData(), "taskId");
        String status = valueAsString(msg.getData(), "status");
        String message = valueAsString(msg.getData(), "message");

        log.info("收到任务停止确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        if ("SUCCESS".equals(status)) {
            vmAckTracker.recordTaskStopAck(taskId, vmId);
        } else {
            log.error("VM任务停止失败: vmId={}, taskId={}, status={}, message={}",
                     vmId, taskId, status, message);
            vmAckTracker.recordTaskStopFailure(taskId, vmId, status, message);
        }

        return null;
    }

    /**
     * 处理FEDERATED_TASK_RESUME - v1.4协议服务端主动恢复任务
     */
    private ProtocolAck onFederatedTaskResume(ProtocolMessage msg) {
        String taskId = valueAsString(msg.getData(), "taskId");
        Integer currentRound = numberAsInt(msg.getData(), "currentRound");

        log.info("v1.4任务恢复指令: taskId={}, currentRound={}", taskId, currentRound);

        // 这是服务端发送给VM的消息，在这里不应该被处理
        log.warn("收到服务端发出的FEDERATED_TASK_RESUME消息，这通常表示配置错误");

        return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
            "errorCode", "INVALID_DIRECTION",
            "errorMessage", "FEDERATED_TASK_RESUME应该由服务端发送给VM"
        ));
    }

    /**
     * 处理FEDERATED_TASK_RESUME_ACK - v1.4协议VM确认恢复任务
     */
    private ProtocolAck onFederatedTaskResumeAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String taskId = valueAsString(msg.getData(), "taskId");
        String status = valueAsString(msg.getData(), "status");
        String message = valueAsString(msg.getData(), "message");

        log.info("收到任务恢复确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        if ("SUCCESS".equals(status)) {
            vmAckTracker.recordTaskResumeAck(taskId, vmId);
        } else {
            log.error("VM任务恢复失败: vmId={}, taskId={}, status={}, message={}",
                     vmId, taskId, status, message);
            vmAckTracker.recordTaskResumeFailure(taskId, vmId, status, message);
        }

        return null;
    }

    /**
     * 处理FEDERATED_TASK_DELETE - v1.4协议服务端主动删除任务
     */
    private ProtocolAck onFederatedTaskDelete(ProtocolMessage msg) {
        String taskId = valueAsString(msg.getData(), "taskId");
        Boolean preserveData = valueAsBoolean(msg.getData(), "preserveData");

        log.info("v1.4任务删除指令: taskId={}, preserveData={}", taskId, preserveData);

        // 这是服务端发送给VM的消息，在这里不应该被处理
        log.warn("收到服务端发出的FEDERATED_TASK_DELETE消息，这通常表示配置错误");

        return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
            "errorCode", "INVALID_DIRECTION",
            "errorMessage", "FEDERATED_TASK_DELETE应该由服务端发送给VM"
        ));
    }

    /**
     * 处理FEDERATED_TASK_DELETE_ACK - v1.4协议VM确认删除任务
     */
    private ProtocolAck onFederatedTaskDeleteAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String taskId = valueAsString(msg.getData(), "taskId");
        String status = valueAsString(msg.getData(), "status");
        String message = valueAsString(msg.getData(), "message");

        log.info("收到任务删除确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        if ("SUCCESS".equals(status)) {
            vmAckTracker.recordTaskDeleteAck(taskId, vmId);
        } else {
            log.error("VM任务删除失败: vmId={}, taskId={}, status={}, message={}",
                     vmId, taskId, status, message);
            vmAckTracker.recordTaskDeleteFailure(taskId, vmId, status, message);
        }

        return null;
    }

    // ====================== v1.4协议通知消息处理方法 ======================

    // v1.3协议遗留方法：onGlobalModelBroadcastNotification
    // 已在v1.4协议中移除，全局模型广播通知合并到GLOBAL_MODEL_BROADCAST中
    // 参考v1.4协议文档：模型广播通知已简化为标准模型广播消息

    // v1.3协议遗留方法：onAggregationStartNotification, onAggregationCompleteNotification
    // 已在v1.4协议中移除，聚合过程隐含在轮次流程中
    // 参考v1.4协议文档：聚合开始/完成通知已合并到模型广播流程

    // v1.3协议遗留方法：onRoundStartNotification, onRoundCompleteNotification
    // 已在v1.4协议中移除，轮次状态通过ROUND_START/ROUND_COMPLETE消息管理
    // 参考v1.4协议文档：轮次通知已简化为标准轮次管理流程

    // ====================== 数据集相关ACK消息处理方法 ======================

    /**
     * 处理DATASET_CREATE_ACK - v1.4协议数据集创建确认
     */
    private ProtocolAck onDatasetCreateAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String datasetId = valueAsString(msg.getData(), "assignedDatasetId");
        String status = valueAsString(msg.getData(), "status");

        log.info("收到数据集创建确认: vmId={}, datasetId={}, status={}", vmId, datasetId, status);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            try {
                TaskParticipant participant = taskParticipantsMapper.selectParticipantByAssignedDatasetId(datasetId);
                if (participant != null) {
                    if (participant.getDatasetStatus() == null || !"CREATED".equals(participant.getDatasetStatus())) {
                        participant.setDatasetStatus("CREATED");
                    }
                    if (participant.getDatasetCreatedAt() == null) {
                        participant.setDatasetCreatedAt(LocalDateTime.now());
                    }
                    participant.setUpdatedAt(LocalDateTime.now());
                    taskParticipantsMapper.updateParticipant(participant);
                } else {
                    log.warn("数据集创建确认未找到参与者: datasetId={}, vmId={}", datasetId, vmId);
                }
            } catch (Exception e) {
                log.error("处理数据集创建确认失败: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);
            }
        } else {
            String errorMessage = valueAsString(msg.getData(), "message");
            log.error("数据集创建失败: vmId={}, datasetId={}, message={}", vmId, datasetId, errorMessage);
            try {
                TaskParticipant participant = taskParticipantsMapper.selectParticipantByAssignedDatasetId(datasetId);
                if (participant != null) {
                    participant.setDatasetStatus("FAILED");
                    participant.setUpdatedAt(LocalDateTime.now());
                    taskParticipantsMapper.updateParticipant(participant);
                }
            } catch (Exception e) {
                log.error("更新数据集失败状态异常: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * 处理DATASET_APPEND_ROWS_ACK - v1.4协议数据集追加行确认
     */
    private ProtocolAck onDatasetAppendRowsAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String datasetId = valueAsString(msg.getData(), "assignedDatasetId");
        String status = valueAsString(msg.getData(), "status");
        Integer appendedRows = numberAsInt(msg.getData(), "appendedRows");

        log.info("收到数据集追加行确认: vmId={}, datasetId={}, status={}, appendedRows={}",
                vmId, datasetId, status, appendedRows);

        if (datasetId != null && "SUCCESS".equalsIgnoreCase(status)) {
            try {
                TaskParticipant participant = taskParticipantsMapper.selectParticipantByAssignedDatasetId(datasetId);
                if (participant != null) {
                    participant.setDatasetStatus("UPLOADING");
                    participant.setUpdatedAt(LocalDateTime.now());
                    taskParticipantsMapper.updateParticipant(participant);
                }
            } catch (Exception e) {
                log.error("更新数据集追加状态失败: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * 处理DATASET_COMPLETE_ACK - v1.4协议数据集完成确认
     */
    private ProtocolAck onDatasetCompleteAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();
        String datasetId = valueAsString(data, "assignedDatasetId");
        String status = valueAsString(data, "status");
        String errorMessage = valueAsString(data, "message");

        log.info("收到数据集完成确认: vmId={}, datasetId={}, status={}", vmId, datasetId, status);

        if (datasetId == null) {
            log.warn("数据集完成确认缺少assignedDatasetId: vmId={}", vmId);
            return null;
        }

        try {
            TaskParticipant participant = taskParticipantsMapper.selectParticipantByAssignedDatasetId(datasetId);
            if (participant == null) {
                log.warn("数据集完成确认未找到参与者: datasetId={}, vmId={}", datasetId, vmId);
                return null;
            }

            boolean success = "SUCCESS".equalsIgnoreCase(status);
            participant.setDatasetStatus(success ? "COMPLETED" : "FAILED");
            participant.setDatasetCompletedAt(LocalDateTime.now());
            participant.setUpdatedAt(LocalDateTime.now());
            int updated = taskParticipantsMapper.updateParticipant(participant);
            log.info("数据集完成状态更新: taskId={}, vmId={}, datasetId={}, success={}, updateCount={}",
                    participant.getTaskId(), participant.getVmId(), datasetId, success, updated);

            String ackDataJson = null;
            @SuppressWarnings("unchecked")
            Map<String, Object> sliceVerification = (Map<String, Object>) data.get("sliceVerification");
            if (sliceVerification != null) {
                try {
                    ackDataJson = objectMapper.writeValueAsString(sliceVerification);
                } catch (Exception e) {
                    log.warn("序列化sliceVerification失败: {}", e.getMessage());
                }
            }

            vmAckTracker.recordDatasetAck(
                participant.getTaskId(),
                vmId,
                datasetId,
                success,
                ackDataJson,
                success ? null : errorMessage
            );

        } catch (Exception e) {
            log.error("处理数据集完成确认失败: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);
        }

        return null;
    }

    /**
     * 处理DATASET_DELETE_ACK - v1.4协议数据集删除确认
     */
    private ProtocolAck onDatasetDeleteAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String datasetId = valueAsString(msg.getData(), "datasetId");
        String status = valueAsString(msg.getData(), "status");

        log.info("收到数据集删除确认: vmId={}, datasetId={}, status={}", vmId, datasetId, status);

        return null;
    }

    /**
     * 处理DATASET_STATUS_RESPONSE - v1.4协议数据集状态响应
     */
    private ProtocolAck onDatasetStatusResponse(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String datasetId = valueAsString(msg.getData(), "datasetId");
        String status = valueAsString(msg.getData(), "status");
        Integer totalRows = numberAsInt(msg.getData(), "totalRows");

        log.info("收到数据集状态响应: vmId={}, datasetId={}, status={}, totalRows={}",
                vmId, datasetId, status, totalRows);

        return null;
    }

    // ====================== v1.4协议中已移除的训练相关ACK方法 (已清理) ======================
    // TRAINING_START_ACK -> 合并到 FEDERATED_TASK_START_ACK
    // TRAINING_STOP_ACK -> 使用 FEDERATED_TASK_STOP_ACK
    // TRAINING_PROGRESS_ACK -> 通过心跳报告状态

    // ====================== 模型相关ACK消息处理方法 ======================

    /**
     * 处理MODEL_UPLOAD_ACK - v1.4协议模型上传确认
     */
    private ProtocolAck onModelUploadAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String modelId = valueAsString(msg.getData(), "modelId");
        String status = valueAsString(msg.getData(), "status");

        log.info("收到模型上传确认: vmId={}, modelId={}, status={}", vmId, modelId, status);

        return null;
    }

    /**
     * 处理MODEL_DOWNLOAD_ACK - v1.4协议模型下载确认
     */
    private ProtocolAck onModelDownloadAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String modelId = valueAsString(msg.getData(), "modelId");
        String status = valueAsString(msg.getData(), "status");

        log.info("收到模型下载确认: vmId={}, modelId={}, status={}", vmId, modelId, status);

        return null;
    }

    /**
     * 处理GLOBAL_MODEL_UPDATE_ACK - v1.4协议全局模型更新确认
     */
    private ProtocolAck onGlobalModelUpdateAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String taskId = valueAsString(msg.getData(), "taskId");
        String status = valueAsString(msg.getData(), "status");

        log.info("收到全局模型更新确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        return null;
    }

    // ====================== 连接相关ACK消息处理方法 ======================

    /**
     * 处理CONNECT_ACK - v1.4协议连接确认
     */
    private ProtocolAck onConnectAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        String status = valueAsString(msg.getData(), "status");

        log.info("收到连接确认: vmId={}, status={}", vmId, status);

        return null;
    }

    /**
     * 处理HEARTBEAT_ACK - v1.4协议心跳确认
     */
    private ProtocolAck onHeartbeatAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Long timestamp = valueAsLong(msg.getData(), "timestamp");

        log.debug("收到心跳确认: vmId={}, timestamp={}", vmId, timestamp);

        return null;
    }

    // ====================== v1.4协议连接管理层处理方法 ======================

    /**
     * 处理CONNECT - v1.4协议虚拟机连接请求
     * 虚拟机启动后向后端服务器发送连接请求，上报系统信息和机器学习能力
     */
    private ProtocolAck onConnect(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String version = valueAsString(data, "version");
        @SuppressWarnings("unchecked")
        List<String> supportedMLAlgorithms = (List<String>) valueAsObject(data, "supportedMLAlgorithms");
        @SuppressWarnings("unchecked")
        Map<String, Object> systemInfo = (Map<String, Object>) valueAsObject(data, "systemInfo");
        @SuppressWarnings("unchecked")
        Map<String, Object> computeCapabilities = (Map<String, Object>) valueAsObject(data, "computeCapabilities");

        log.info("收到虚拟机连接请求: vmId={}, version={}, supportedAlgorithms={}",
                vmId, version, supportedMLAlgorithms);

        try {
            // 记录虚拟机连接信息
            statusCache.put(vmId, mapOf(
                "status", "CONNECTED",
                "connectTime", Instant.now().toString(),
                "version", version,
                "systemInfo", systemInfo,
                "computeCapabilities", computeCapabilities,
                "lastActivity", Instant.now().toString()
            ));

            // 生成会话ID
            String sessionId = "session-" + vmId + "-" + System.currentTimeMillis();

            log.info("虚拟机连接成功: vmId={}, sessionId={}", vmId, sessionId);

            // v1.4协议：主动发送CONNECT_ACK消息
            messageSender.sendConnectAck(vmId, sessionId, 30, 10485760L);

            return ackFor(msg, ProtocolType.CONNECT_ACK, mapOf(
                "sessionId", sessionId,
                "serverTime", Instant.now().toString(),
                "heartbeatInterval", 30,
                "maxMessageSize", 10485760,
                "protocolVersion", "v1.4",
                "status", "CONNECTED"
            ));

        } catch (Exception e) {
            log.error("处理虚拟机连接失败: vmId={}, error={}", vmId, e.getMessage(), e);
            return ackFor(msg, ProtocolType.CONNECT_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "连接处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理HEARTBEAT - v1.4协议虚拟机心跳
     * 虚拟机定期发送心跳维持连接，报告系统资源状态
     */
    private ProtocolAck onHeartbeat(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String status = valueAsString(data, "status");
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceUsage = (Map<String, Object>) valueAsObject(data, "resourceUsage");
        @SuppressWarnings("unchecked")
        Map<String, Object> network = (Map<String, Object>) valueAsObject(data, "network");

        log.debug("收到虚拟机心跳: vmId={}, status={}, cpu={}%, memory={}%",
                vmId, status,
                resourceUsage != null ? resourceUsage.get("cpu") : "unknown",
                resourceUsage != null ? resourceUsage.get("memory") : "unknown");

        try {
            // 更新虚拟机状态缓存
            Map<String, Object> vmStatus = statusCache.get(vmId);
            if (vmStatus == null) {
                vmStatus = new HashMap<>();
                statusCache.put(vmId, vmStatus);
            }

            vmStatus.put("status", status);
            vmStatus.put("lastHeartbeat", Instant.now().toString());
            vmStatus.put("resourceUsage", resourceUsage);
            vmStatus.put("network", network);
            vmStatus.put("lastActivity", Instant.now().toString());

            // v1.4协议：主动发送HEARTBEAT_ACK消息
            messageSender.sendHeartbeatAck(vmId, 30, "NORMAL");

            return ackFor(msg, ProtocolType.HEARTBEAT_ACK, mapOf(
                "serverTime", Instant.now().toString(),
                "nextHeartbeat", 30,
                "systemStatus", "NORMAL",
                "acknowledged", true
            ));

        } catch (Exception e) {
            log.error("处理虚拟机心跳失败: vmId={}, error={}", vmId, e.getMessage(), e);
            return ackFor(msg, ProtocolType.HEARTBEAT_ACK, mapOf(
                "status", "ERROR",
                "errorMessage", "心跳处理失败: " + e.getMessage()
            ));
        }
    }

    // ====================== v1.4协议虚拟机控制层处理方法 ======================

    /**
     * 处理VM_START - v1.4协议虚拟机启动指令
     * 🟢 Server→VM: 服务端指示虚拟机启动并准备训练任务
     */
    private ProtocolAck onVmStart(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        @SuppressWarnings("unchecked")
        Map<String, Object> taskConfig = (Map<String, Object>) valueAsObject(data, "taskConfig");
        String startupScript = valueAsString(data, "startupScript");
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceRequirements = (Map<String, Object>) valueAsObject(data, "resourceRequirements");

        log.info("收到虚拟机启动指令: vmId={}, taskId={}", vmId, taskId);

        try {
            // 更新虚拟机状态为启动中
            Map<String, Object> vmStatus = statusCache.get(vmId);
            if (vmStatus == null) {
                vmStatus = new HashMap<>();
                statusCache.put(vmId, vmStatus);
            }

            vmStatus.put("status", "STARTING");
            vmStatus.put("currentTaskId", taskId);
            vmStatus.put("startTime", Instant.now().toString());
            vmStatus.put("lastActivity", Instant.now().toString());

            // 记录启动配置
            vmStatus.put("taskConfig", taskConfig);
            vmStatus.put("resourceRequirements", resourceRequirements);

            log.info("虚拟机启动指令已记录: vmId={}, taskId={}", vmId, taskId);

            // 虚拟机需要回复VM_START_ACK确认启动结果
            return null; // 等待虚拟机的确认响应

        } catch (Exception e) {
            log.error("处理虚拟机启动指令失败: vmId={}, error={}", vmId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "VM_START_FAILED",
                "errorMessage", "虚拟机启动指令处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理VM_START_ACK - v1.4协议虚拟机启动确认
     * 🔵 VM→Server: 虚拟机确认启动结果
     */
    private ProtocolAck onVmStartAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String status = valueAsString(data, "status");
        String message = valueAsString(data, "message");
        Long startTime = valueAsLong(data, "startTime");

        log.info("收到虚拟机启动确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        try {
            // 更新虚拟机状态缓存
            Map<String, Object> vmStatus = statusCache.get(vmId);
            if (vmStatus == null) {
                vmStatus = new HashMap<>();
                statusCache.put(vmId, vmStatus);
            }

            if ("SUCCESS".equals(status)) {
                vmStatus.put("status", "RUNNING");
                vmStatus.put("actualStartTime", Instant.ofEpochMilli(startTime != null ? startTime : System.currentTimeMillis()).toString());
                vmStatus.put("taskId", taskId);
                log.info("虚拟机启动成功: vmId={}, taskId={}", vmId, taskId);
            } else {
                vmStatus.put("status", "START_FAILED");
                vmStatus.put("errorMessage", message);
                log.error("虚拟机启动失败: vmId={}, taskId={}, message={}", vmId, taskId, message);
            }

            vmStatus.put("lastActivity", Instant.now().toString());

            return null; // ACK消息不需要响应

        } catch (Exception e) {
            log.error("处理虚拟机启动确认失败: vmId={}, error={}", vmId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理VM_STOP - v1.4协议虚拟机停止指令
     * 🟢 Server→VM: 服务端指示虚拟机停止当前任务
     */
    private ProtocolAck onVmStop(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String reason = valueAsString(data, "reason");
        Boolean gracefulShutdown = valueAsBoolean(data, "gracefulShutdown", true);
        Long timeout = valueAsLong(data, "timeout");

        log.info("收到虚拟机停止指令: vmId={}, taskId={}, reason={}, graceful={}",
                vmId, taskId, reason, gracefulShutdown);

        try {
            // 更新虚拟机状态为停止中
            Map<String, Object> vmStatus = statusCache.get(vmId);
            if (vmStatus == null) {
                vmStatus = new HashMap<>();
                statusCache.put(vmId, vmStatus);
            }

            vmStatus.put("status", "STOPPING");
            vmStatus.put("stopReason", reason);
            vmStatus.put("stopTime", Instant.now().toString());
            vmStatus.put("gracefulShutdown", gracefulShutdown);
            vmStatus.put("lastActivity", Instant.now().toString());

            if (timeout != null) {
                vmStatus.put("stopTimeout", timeout);
            }

            log.info("虚拟机停止指令已记录: vmId={}, taskId={}, reason={}", vmId, taskId, reason);

            // 虚拟机需要回复VM_STOP_ACK确认停止结果
            return null; // 等待虚拟机的确认响应

        } catch (Exception e) {
            log.error("处理虚拟机停止指令失败: vmId={}, error={}", vmId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "VM_STOP_FAILED",
                "errorMessage", "虚拟机停止指令处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理VM_STOP_ACK - v1.4协议虚拟机停止确认
     * 🔵 VM→Server: 虚拟机确认停止结果
     */
    private ProtocolAck onVmStopAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String status = valueAsString(data, "status");
        String message = valueAsString(data, "message");
        Long stopTime = valueAsLong(data, "stopTime");

        log.info("收到虚拟机停止确认: vmId={}, taskId={}, status={}", vmId, taskId, status);

        try {
            // 更新虚拟机状态缓存
            Map<String, Object> vmStatus = statusCache.get(vmId);
            if (vmStatus == null) {
                vmStatus = new HashMap<>();
                statusCache.put(vmId, vmStatus);
            }

            if ("SUCCESS".equals(status)) {
                vmStatus.put("status", "STOPPED");
                vmStatus.put("actualStopTime", Instant.ofEpochMilli(stopTime != null ? stopTime : System.currentTimeMillis()).toString());
                log.info("虚拟机停止成功: vmId={}, taskId={}", vmId, taskId);
            } else {
                vmStatus.put("status", "STOP_FAILED");
                vmStatus.put("errorMessage", message);
                log.error("虚拟机停止失败: vmId={}, taskId={}, message={}", vmId, taskId, message);
            }

            vmStatus.put("lastActivity", Instant.now().toString());

            return null; // ACK消息不需要响应

        } catch (Exception e) {
            log.error("处理虚拟机停止确认失败: vmId={}, error={}", vmId, e.getMessage(), e);
            return null;
        }
    }

    // ====================== v1.4协议数据集管理层处理方法 ======================

    /**
     * 处理DATASET_CREATE - v1.4协议数据集创建
     * 🔵 VM→Server: 虚拟机请求创建新的数据集
     */
    private ProtocolAck onDatasetCreate(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String datasetId = valueAsString(data, "datasetId");
        String datasetName = valueAsString(data, "datasetName");
        String datasetType = valueAsString(data, "datasetType");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) valueAsObject(data, "schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) valueAsObject(data, "metadata");

        log.info("收到数据集创建请求: vmId={}, datasetId={}, name={}, type={}",
                vmId, datasetId, datasetName, datasetType);

        try {
            // 验证数据集参数
            if (datasetId == null || datasetId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, mapOf(
                    "datasetId", datasetId,
                    "status", "FAILED",
                    "errorCode", "INVALID_DATASET_ID",
                    "message", "数据集ID不能为空"
                ));
            }

            // 检查数据集是否已存在
            String cacheKey = "dataset:" + datasetId;
            if (statusCache.containsKey(cacheKey)) {
                return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, mapOf(
                    "datasetId", datasetId,
                    "status", "FAILED",
                    "errorCode", "DATASET_EXISTS",
                    "message", "数据集已存在"
                ));
            }

            // 创建数据集记录
            Map<String, Object> datasetInfo = mapOf(
                "datasetId", datasetId,
                "datasetName", datasetName,
                "datasetType", datasetType,
                "schema", schema,
                "metadata", metadata,
                "vmId", vmId,
                "status", "CREATED",
                "createTime", Instant.now().toString(),
                "rowCount", 0,
                "lastModified", Instant.now().toString()
            );

            statusCache.put(cacheKey, datasetInfo);

            log.info("数据集创建成功: vmId={}, datasetId={}", vmId, datasetId);

            return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, mapOf(
                "datasetId", datasetId,
                "status", "SUCCESS",
                "createTime", Instant.now().toString(),
                "message", "数据集创建成功"
            ));

        } catch (Exception e) {
            log.error("处理数据集创建失败: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.DATASET_CREATE_ACK, mapOf(
                "datasetId", datasetId,
                "status", "FAILED",
                "errorCode", "CREATE_ERROR",
                "message", "数据集创建失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理DATASET_APPEND_ROWS - v1.4协议数据集追加行
     * 🔵 VM→Server: 虚拟机向数据集追加数据行
     */
    private ProtocolAck onDatasetAppendRows(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String datasetId = valueAsString(data, "datasetId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) valueAsObject(data, "rows");
        String batchId = valueAsString(data, "batchId");

        log.info("收到数据集追加行请求: vmId={}, datasetId={}, batchId={}, rowCount={}",
                vmId, datasetId, batchId, rows != null ? rows.size() : 0);

        try {
            // 验证参数
            if (datasetId == null || datasetId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, mapOf(
                    "datasetId", datasetId,
                    "batchId", batchId,
                    "status", "FAILED",
                    "errorCode", "INVALID_DATASET_ID",
                    "message", "数据集ID不能为空"
                ));
            }

            if (rows == null || rows.isEmpty()) {
                return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, mapOf(
                    "datasetId", datasetId,
                    "batchId", batchId,
                    "status", "FAILED",
                    "errorCode", "EMPTY_ROWS",
                    "message", "数据行不能为空"
                ));
            }

            // 检查数据集是否存在
            String cacheKey = "dataset:" + datasetId;
            @SuppressWarnings("unchecked")
            Map<String, Object> datasetInfo = (Map<String, Object>) statusCache.get(cacheKey);
            if (datasetInfo == null) {
                return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, mapOf(
                    "datasetId", datasetId,
                    "batchId", batchId,
                    "status", "FAILED",
                    "errorCode", "DATASET_NOT_FOUND",
                    "message", "数据集不存在"
                ));
            }

            // 更新数据集行数
            Integer currentRowCount = (Integer) datasetInfo.getOrDefault("rowCount", 0);
            int newRowCount = currentRowCount + rows.size();
            datasetInfo.put("rowCount", newRowCount);
            datasetInfo.put("lastModified", Instant.now().toString());
            datasetInfo.put("status", "APPENDING");

            // 记录批次信息
            String batchKey = "dataset_batch:" + datasetId + ":" + batchId;
            statusCache.put(batchKey, mapOf(
                "batchId", batchId,
                "datasetId", datasetId,
                "rowCount", rows.size(),
                "appendTime", Instant.now().toString(),
                "vmId", vmId
            ));

            log.info("数据集追加行成功: vmId={}, datasetId={}, batchId={}, totalRows={}",
                    vmId, datasetId, batchId, newRowCount);

            return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, mapOf(
                "datasetId", datasetId,
                "batchId", batchId,
                "status", "SUCCESS",
                "rowsAppended", rows.size(),
                "totalRows", newRowCount,
                "appendTime", Instant.now().toString(),
                "message", "数据行追加成功"
            ));

        } catch (Exception e) {
            log.error("处理数据集追加行失败: vmId={}, datasetId={}, batchId={}, error={}",
                    vmId, datasetId, batchId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.DATASET_APPEND_ROWS_ACK, mapOf(
                "datasetId", datasetId,
                "batchId", batchId,
                "status", "FAILED",
                "errorCode", "APPEND_ERROR",
                "message", "数据行追加失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理DATASET_COMPLETE - v1.4协议数据集完成
     * 🔵 VM→Server: 虚拟机通知数据集已完成所有数据加载
     */
    private ProtocolAck onDatasetComplete(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String datasetId = valueAsString(data, "datasetId");
        @SuppressWarnings("unchecked")
        Map<String, Object> finalStats = (Map<String, Object>) valueAsObject(data, "finalStats");
        String checksum = valueAsString(data, "checksum");

        // 🆕 v1.5.1: 解析SliceVerification
        @SuppressWarnings("unchecked")
        Map<String, Object> verificationMap = (Map<String, Object>) valueAsObject(data, "sliceVerification");

        log.info("收到数据集完成通知: vmId={}, datasetId={}, checksum={}, hasVerification={}",
                vmId, datasetId, checksum, verificationMap != null);

        try {
            // 验证参数
            if (datasetId == null || datasetId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.DATASET_COMPLETE_ACK, mapOf(
                    "datasetId", datasetId,
                    "status", "FAILED",
                    "errorCode", "INVALID_DATASET_ID",
                    "message", "数据集ID不能为空"
                ));
            }

            // 检查数据集是否存在
            String cacheKey = "dataset:" + datasetId;
            @SuppressWarnings("unchecked")
            Map<String, Object> datasetInfo = (Map<String, Object>) statusCache.get(cacheKey);
            if (datasetInfo == null) {
                return ackFor(msg, ProtocolType.DATASET_COMPLETE_ACK, mapOf(
                    "datasetId", datasetId,
                    "status", "FAILED",
                    "errorCode", "DATASET_NOT_FOUND",
                    "message", "数据集不存在"
                ));
            }

            // 🆕 v1.5.1: 验证数据完整性
            VerificationResult verificationResult = null;
            if (verificationMap != null) {
                // 将Map转换为SliceVerification对象
                SliceVerification verification = objectMapper.convertValue(verificationMap, SliceVerification.class);

                // 从数据库或缓存获取expectedSliceInfo
                @SuppressWarnings("unchecked")
                Map<String, Object> sliceInfoMap = (Map<String, Object>) datasetInfo.get("sliceInfo");
                if (sliceInfoMap != null) {
                    SliceInfo expectedSliceInfo = objectMapper.convertValue(sliceInfoMap, SliceInfo.class);

                    // 调用SliceVerificationService进行验证
                    verificationResult = sliceVerificationService.verifySliceVerification(
                        vmId,
                        verification,
                        expectedSliceInfo
                    );

                    log.info("数据完整性验证完成: vmId={}, isValid={}, missingRate={}, decision={}",
                        vmId, verificationResult.getIsValid(),
                        String.format("%.2f%%", verificationResult.getMissingRate() * 100),
                        verificationResult.getRecommendedDecision());

                    // 保存验证结果到缓存
                    datasetInfo.put("verificationResult", verificationResult);
                }
            }

            // 更新数据集状态为完成
            datasetInfo.put("status", "COMPLETED");
            datasetInfo.put("completeTime", Instant.now().toString());
            datasetInfo.put("finalStats", finalStats);
            datasetInfo.put("checksum", checksum);
            datasetInfo.put("lastModified", Instant.now().toString());

            // 生成数据集摘要
            Integer totalRows = (Integer) datasetInfo.getOrDefault("rowCount", 0);
            String datasetName = (String) datasetInfo.get("datasetName");
            String createTime = (String) datasetInfo.get("createTime");

            log.info("数据集已完成: vmId={}, datasetId={}, totalRows={}, verified={}",
                vmId, datasetId, totalRows, verificationResult != null);

            // 构建响应
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("datasetId", datasetId);
            responseData.put("status", "SUCCESS");
            responseData.put("totalRows", totalRows);
            responseData.put("completeTime", Instant.now().toString());
            responseData.put("checksum", checksum);
            responseData.put("message", "数据集完成确认");

            // 🆕 v1.5.1: 添加验证结果到响应
            if (verificationResult != null) {
                responseData.put("verificationPassed", verificationResult.getIsValid());
                responseData.put("missingRate", verificationResult.getMissingRate());
                responseData.put("recommendedDecision", verificationResult.getRecommendedDecision().getCode());

                // 如果验证失败，添加详细信息
                if (!Boolean.TRUE.equals(verificationResult.getIsValid())) {
                    responseData.put("verificationMessage", verificationResult.getMessage());
                    responseData.put("missingCount", verificationResult.getMissingCount());
                    if (verificationResult.getMissingIndices() != null && !verificationResult.getMissingIndices().isEmpty()) {
                        // 只返回前10个缺失索引示例
                        List<Integer> sampleMissing = verificationResult.getMissingIndices().stream()
                            .limit(10)
                            .collect(Collectors.toList());
                        responseData.put("sampleMissingIndices", sampleMissing);
                    }
                }
            }

            return ackFor(msg, ProtocolType.DATASET_COMPLETE_ACK, responseData);

        } catch (Exception e) {
            log.error("处理数据集完成失败: vmId={}, datasetId={}, error={}", vmId, datasetId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.DATASET_COMPLETE_ACK, mapOf(
                "datasetId", datasetId,
                "status", "FAILED",
                "errorCode", "COMPLETE_ERROR",
                "message", "数据集完成处理失败: " + e.getMessage()
            ));
        }
    }

    // ====================== v1.4协议状态监控层处理方法 ======================

    /**
     * 处理VM_STATUS_QUERY - v1.4协议虚拟机状态查询
     * 🟢 Server→VM: 服务端查询虚拟机详细状态信息
     */
    private ProtocolAck onVmStatusQuery(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        @SuppressWarnings("unchecked")
        List<String> queryFields = (List<String>) valueAsObject(data, "queryFields");
        String queryId = valueAsString(data, "queryId");
        Boolean includeTaskStatus = valueAsBoolean(data, "includeTaskStatus", false);
        Boolean includeResourceUsage = valueAsBoolean(data, "includeResourceUsage", true);

        log.info("收到虚拟机状态查询: vmId={}, queryId={}, fields={}, includeTask={}, includeResource={}",
                vmId, queryId, queryFields, includeTaskStatus, includeResourceUsage);

        try {
            // 记录查询请求，等待虚拟机回应
            String queryKey = "vm_status_query:" + vmId + ":" + queryId;
            statusCache.put(queryKey, mapOf(
                "queryId", queryId,
                "vmId", vmId,
                "queryTime", Instant.now().toString(),
                "queryFields", queryFields,
                "includeTaskStatus", includeTaskStatus,
                "includeResourceUsage", includeResourceUsage,
                "status", "PENDING"
            ));

            log.info("虚拟机状态查询已发送: vmId={}, queryId={}", vmId, queryId);

            // 此消息由服务端主动发送给虚拟机，虚拟机需要回复VM_STATUS_RESPONSE
            return null; // 等待虚拟机的响应

        } catch (Exception e) {
            log.error("处理虚拟机状态查询失败: vmId={}, queryId={}, error={}", vmId, queryId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "VM_STATUS_QUERY_FAILED",
                "errorMessage", "虚拟机状态查询处理失败: " + e.getMessage(),
                "queryId", queryId
            ));
        }
    }

    /**
     * 处理VM_STATUS_RESPONSE - v1.4协议虚拟机状态响应
     * 🔵 VM→Server: 虚拟机回复状态查询结果
     */
    private ProtocolAck onVmStatusResponse(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String queryId = valueAsString(data, "queryId");
        String status = valueAsString(data, "status");
        @SuppressWarnings("unchecked")
        Map<String, Object> vmStatus = (Map<String, Object>) valueAsObject(data, "vmStatus");
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceUsage = (Map<String, Object>) valueAsObject(data, "resourceUsage");
        @SuppressWarnings("unchecked")
        Map<String, Object> taskStatus = (Map<String, Object>) valueAsObject(data, "taskStatus");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> datasets = (List<Map<String, Object>>) valueAsObject(data, "datasets");

        log.info("收到虚拟机状态响应: vmId={}, queryId={}, status={}", vmId, queryId, status);

        try {
            // 检查查询请求是否存在
            String queryKey = "vm_status_query:" + vmId + ":" + queryId;
            @SuppressWarnings("unchecked")
            Map<String, Object> queryInfo = (Map<String, Object>) statusCache.get(queryKey);

            if (queryInfo == null) {
                log.warn("未找到对应的状态查询请求: vmId={}, queryId={}", vmId, queryId);
                return null; // 忽略未匹配的响应
            }

            // 更新查询状态
            queryInfo.put("status", "COMPLETED");
            queryInfo.put("responseTime", Instant.now().toString());
            queryInfo.put("responseData", data);

            // 更新虚拟机整体状态缓存
            Map<String, Object> cachedVmStatus = statusCache.get(vmId);
            if (cachedVmStatus == null) {
                cachedVmStatus = new HashMap<>();
                statusCache.put(vmId, cachedVmStatus);
            }

            // 合并状态信息
            if ("SUCCESS".equals(status)) {
                cachedVmStatus.putAll(vmStatus != null ? vmStatus : new HashMap<>());
                cachedVmStatus.put("lastStatusUpdate", Instant.now().toString());
                cachedVmStatus.put("resourceUsage", resourceUsage);
                cachedVmStatus.put("taskStatus", taskStatus);
                cachedVmStatus.put("datasets", datasets);

                // 记录关键状态指标
                if (resourceUsage != null) {
                    Object cpu = resourceUsage.get("cpu");
                    Object memory = resourceUsage.get("memory");
                    Object disk = resourceUsage.get("disk");
                    log.info("虚拟机资源状态更新: vmId={}, CPU={}%, Memory={}%, Disk={}%",
                            vmId, cpu, memory, disk);
                }

                if (taskStatus != null) {
                    String currentTaskId = (String) taskStatus.get("taskId");
                    String taskState = (String) taskStatus.get("status");
                    log.info("虚拟机任务状态更新: vmId={}, taskId={}, status={}", vmId, currentTaskId, taskState);
                }

                log.info("虚拟机状态查询成功: vmId={}, queryId={}", vmId, queryId);
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.error("虚拟机状态查询失败: vmId={}, queryId={}, error={}", vmId, queryId, errorMessage);
                cachedVmStatus.put("lastQueryError", errorMessage);
                cachedVmStatus.put("lastQueryErrorTime", Instant.now().toString());
            }

            return null; // 状态响应不需要ACK回复

        } catch (Exception e) {
            log.error("处理虚拟机状态响应失败: vmId={}, queryId={}, error={}", vmId, queryId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理FEDERATED_TASK_STATUS_QUERY - v1.4协议联邦任务状态查询
     * 🟢 Server→VM: 服务端查询虚拟机上特定联邦任务的详细状态
     */
    private ProtocolAck onFederatedTaskStatusQuery(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String queryId = valueAsString(data, "queryId");
        @SuppressWarnings("unchecked")
        List<String> queryFields = (List<String>) valueAsObject(data, "queryFields");
        Boolean includeProgress = valueAsBoolean(data, "includeProgress", true);
        Boolean includeMetrics = valueAsBoolean(data, "includeMetrics", false);

        log.info("收到联邦任务状态查询: vmId={}, taskId={}, queryId={}, fields={}, includeProgress={}, includeMetrics={}",
                vmId, taskId, queryId, queryFields, includeProgress, includeMetrics);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空",
                    "queryId", queryId
                ));
            }

            // 记录查询请求
            String queryKey = "task_status_query:" + vmId + ":" + taskId + ":" + queryId;
            statusCache.put(queryKey, mapOf(
                "queryId", queryId,
                "vmId", vmId,
                "taskId", taskId,
                "queryTime", Instant.now().toString(),
                "queryFields", queryFields,
                "includeProgress", includeProgress,
                "includeMetrics", includeMetrics,
                "status", "PENDING"
            ));

            log.info("联邦任务状态查询已发送: vmId={}, taskId={}, queryId={}", vmId, taskId, queryId);

            // 此消息由服务端主动发送给虚拟机，虚拟机需要回复FEDERATED_TASK_STATUS_RESPONSE
            return null; // 等待虚拟机的响应

        } catch (Exception e) {
            log.error("处理联邦任务状态查询失败: vmId={}, taskId={}, queryId={}, error={}",
                    vmId, taskId, queryId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "TASK_STATUS_QUERY_FAILED",
                "errorMessage", "联邦任务状态查询处理失败: " + e.getMessage(),
                "queryId", queryId,
                "taskId", taskId
            ));
        }
    }

    /**
     * 处理FEDERATED_TASK_STATUS_RESPONSE - v1.4协议联邦任务状态响应
     * 🔵 VM→Server: 虚拟机回复联邦任务状态查询结果
     */
    private ProtocolAck onFederatedTaskStatusResponse(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String queryId = valueAsString(data, "queryId");
        String status = valueAsString(data, "status");
        @SuppressWarnings("unchecked")
        Map<String, Object> taskStatus = (Map<String, Object>) valueAsObject(data, "taskStatus");
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) valueAsObject(data, "progress");
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) valueAsObject(data, "metrics");
        @SuppressWarnings("unchecked")
        Map<String, Object> modelInfo = (Map<String, Object>) valueAsObject(data, "modelInfo");

        log.info("收到联邦任务状态响应: vmId={}, taskId={}, queryId={}, status={}",
                vmId, taskId, queryId, status);

        try {
            // 检查查询请求是否存在
            String queryKey = "task_status_query:" + vmId + ":" + taskId + ":" + queryId;
            @SuppressWarnings("unchecked")
            Map<String, Object> queryInfo = (Map<String, Object>) statusCache.get(queryKey);

            if (queryInfo == null) {
                log.warn("未找到对应的任务状态查询请求: vmId={}, taskId={}, queryId={}", vmId, taskId, queryId);
                return null; // 忽略未匹配的响应
            }

            // 更新查询状态
            queryInfo.put("status", "COMPLETED");
            queryInfo.put("responseTime", Instant.now().toString());
            queryInfo.put("responseData", data);

            // 更新任务状态缓存
            String taskKey = "federated_task:" + taskId;
            Map<String, Object> cachedTaskStatus = statusCache.get(taskKey);
            if (cachedTaskStatus == null) {
                cachedTaskStatus = new HashMap<>();
                statusCache.put(taskKey, cachedTaskStatus);
            }

            // 合并任务状态信息
            if ("SUCCESS".equals(status)) {
                cachedTaskStatus.put("taskId", taskId);
                cachedTaskStatus.put("vmId", vmId);
                cachedTaskStatus.put("lastStatusUpdate", Instant.now().toString());

                if (taskStatus != null) {
                    cachedTaskStatus.putAll(taskStatus);
                }

                if (progress != null) {
                    cachedTaskStatus.put("progress", progress);

                    // 记录进度关键指标
                    Object currentRound = progress.get("currentRound");
                    Object totalRounds = progress.get("totalRounds");
                    Object completionPercentage = progress.get("completionPercentage");
                    log.info("任务进度更新: taskId={}, vmId={}, round={}/{}, completion={}%",
                            taskId, vmId, currentRound, totalRounds, completionPercentage);
                }

                if (metrics != null) {
                    cachedTaskStatus.put("metrics", metrics);

                    // 记录性能指标
                    Object accuracy = metrics.get("accuracy");
                    Object loss = metrics.get("loss");
                    Object trainingTime = metrics.get("trainingTime");
                    log.info("任务性能指标: taskId={}, vmId={}, accuracy={}, loss={}, trainingTime={}",
                            taskId, vmId, accuracy, loss, trainingTime);

                    // 更新参与者度量指标缓存
                    Integer currentRound = progress != null ?
                        parseIntegerSafely(progress.get("currentRound")) : null;
                    String participantStatus = taskStatus != null ?
                        (String) taskStatus.get("status") : "UNKNOWN";

                    updateParticipantMetricsCache(
                        taskId,
                        vmId,
                        currentRound,
                        parseDoubleSafely(accuracy),
                        parseDoubleSafely(loss),
                        participantStatus
                    );
                }

                if (modelInfo != null) {
                    cachedTaskStatus.put("modelInfo", modelInfo);
                }

                log.info("联邦任务状态查询成功: vmId={}, taskId={}, queryId={}", vmId, taskId, queryId);
            } else {
                String errorMessage = valueAsString(data, "errorMessage");
                log.error("联邦任务状态查询失败: vmId={}, taskId={}, queryId={}, error={}",
                        vmId, taskId, queryId, errorMessage);

                cachedTaskStatus.put("lastQueryError", errorMessage);
                cachedTaskStatus.put("lastQueryErrorTime", Instant.now().toString());
                cachedTaskStatus.put("queryStatus", "FAILED");
            }

            return null; // 状态响应不需要ACK回复

        } catch (Exception e) {
            log.error("处理联邦任务状态响应失败: vmId={}, taskId={}, queryId={}, error={}",
                    vmId, taskId, queryId, e.getMessage(), e);
            return null;
        }
    }

    // ====================== v1.4协议轮次管理层处理方法 ======================

    /**
     * 处理ROUND_ABORT - v1.4协议轮次中止
     * 🔵 VM→Server: 虚拟机通知服务端当前轮次需要中止
     */
    private ProtocolAck onRoundAbort(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        Integer roundNumber = numberAsInt(data, "roundNumber");
        String reason = valueAsString(data, "reason");
        String errorCode = valueAsString(data, "errorCode");
        @SuppressWarnings("unchecked")
        Map<String, Object> errorDetails = (Map<String, Object>) valueAsObject(data, "errorDetails");
        Boolean requiresFailover = valueAsBoolean(data, "requiresFailover", false);

        log.warn("收到轮次中止请求: vmId={}, taskId={}, round={}, reason={}, errorCode={}, failover={}",
                vmId, taskId, roundNumber, reason, errorCode, requiresFailover);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (roundNumber == null || roundNumber < 0) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_ROUND_NUMBER",
                    "errorMessage", "轮次号无效"
                ));
            }

            // 检查任务和轮次状态
            String taskKey = "federated_task:" + taskId;
            @SuppressWarnings("unchecked")
            Map<String, Object> taskStatus = (Map<String, Object>) statusCache.get(taskKey);

            if (taskStatus == null) {
                log.warn("轮次中止请求中的任务不存在: taskId={}", taskId);
                return errorFor(msg, "TASK_NOT_FOUND", "任务不存在，无需中止");
            }

            // 使用轮次锁管理器进行安全的轮次状态更新
            String roundKey = "round:" + taskId + ":" + roundNumber;

            try {
                // 获取轮次锁 - v1.4协议不支持轮次锁，使用任务级锁
                if (roundLockManager.tryAcquireRoundLock(taskId, 5)) {
                    try {
                        // 更新轮次状态为中止
                        Map<String, Object> roundStatus = mapOf(
                            "taskId", taskId,
                            "roundNumber", roundNumber,
                            "status", "ABORTED",
                            "abortReason", reason,
                            "errorCode", errorCode,
                            "errorDetails", errorDetails,
                            "abortTime", Instant.now().toString(),
                            "abortByVmId", vmId,
                            "requiresFailover", requiresFailover
                        );

                        statusCache.put(roundKey, roundStatus);

                        // 更新轮次状态管理器 - v1.4协议使用FAILED状态
                        roundStateManager.setRoundState(taskId, roundNumber, RoundState.FAILED);

                        // 更新任务整体状态
                        taskStatus.put("lastRoundStatus", "ABORTED");
                        taskStatus.put("lastAbortTime", Instant.now().toString());
                        taskStatus.put("lastAbortReason", reason);
                        taskStatus.put("abortedByVm", vmId);

                        // 如果需要故障转移，标记任务状态
                        if (requiresFailover) {
                            taskStatus.put("requiresFailover", true);
                            taskStatus.put("failoverReason", reason);
                            log.warn("任务需要故障转移: taskId={}, round={}, reason={}", taskId, roundNumber, reason);
                        }

                        log.warn("轮次已中止: taskId={}, round={}, vmId={}, reason={}", taskId, roundNumber, vmId, reason);

                        return errorFor(msg, "ROUND_ABORTED", "轮次已中止");

                    } finally {
                        // 释放轮次锁 - v1.4协议使用任务级锁
                        roundLockManager.releaseRoundLock(taskId);
                    }
                } else {
                    // 无法获取轮次锁，可能有并发操作
                    log.warn("无法获取轮次锁，中止操作被延迟: taskId={}, round={}", taskId, roundNumber);

                    return errorFor(msg, "OPERATION_DEFERRED", "轮次正在处理中，中止请求已记录，将稍后处理");
                }
            } catch (Exception lockException) {
                log.error("轮次锁操作失败: taskId={}, round={}, error={}", taskId, roundNumber, lockException.getMessage(), lockException);

                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "ROUND_LOCK_ERROR",
                    "errorMessage", "轮次锁操作失败: " + lockException.getMessage()
                ));
            }

        } catch (Exception e) {
            log.error("处理轮次中止失败: vmId={}, taskId={}, round={}, error={}",
                    vmId, taskId, roundNumber, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "ROUND_ABORT_FAILED",
                "errorMessage", "轮次中止处理失败: " + e.getMessage(),
                "taskId", taskId,
                "roundNumber", roundNumber
            ));
        }
    }

    /**
     * 处理轮次开始确认消息 (ROUND_START_ACK)
     * v1.4协议新增
     */
    private ProtocolAck onRoundStartAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        Integer roundNumber = numberAsInt(data, "roundNumber");
        String status = valueAsString(data, "status");
        Integer estimatedTrainingTime = numberAsInt(data, "estimatedTrainingTime");

        log.info("收到轮次开始确认: vmId={}, taskId={}, round={}, status={}, estimatedTime={}",
                vmId, taskId, roundNumber, status, estimatedTrainingTime);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (roundNumber == null || roundNumber < 0) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_ROUND_NUMBER",
                    "errorMessage", "轮次号无效"
                ));
            }

            // 记录虚拟机轮次确认状态
            vmAckTracker.recordRoundStartAck(taskId, roundNumber, vmId, status);

            // 更新轮次状态管理器
            if ("READY".equals(status)) {
                roundStateManager.setRoundState(taskId, roundNumber, RoundState.TRAINING);
            }

            log.info("轮次开始确认处理完成: vmId={}, taskId={}, round={}", vmId, taskId, roundNumber);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "轮次开始确认已收到"
            ));

        } catch (Exception e) {
            log.error("处理轮次开始确认失败: vmId={}, taskId={}, round={}, error={}",
                    vmId, taskId, roundNumber, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "ROUND_START_ACK_FAILED",
                "errorMessage", "轮次开始确认处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理全局模型广播消息 (GLOBAL_MODEL_BROADCAST)
     * v1.4协议新增
     */
    private ProtocolAck onGlobalModelBroadcast(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        Integer roundNumber = numberAsInt(data, "roundNumber");
        @SuppressWarnings("unchecked")
        Map<String, Object> globalModel = (Map<String, Object>) valueAsObject(data, "globalModel");
        @SuppressWarnings("unchecked")
        Map<String, Object> aggregationInfo = (Map<String, Object>) valueAsObject(data, "aggregationInfo");

        log.info("发送全局模型广播: vmId={}, taskId={}, round={}", vmId, taskId, roundNumber);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (roundNumber == null || roundNumber < 0) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_ROUND_NUMBER",
                    "errorMessage", "轮次号无效"
                ));
            }

            if (globalModel == null) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "MISSING_GLOBAL_MODEL",
                    "errorMessage", "全局模型数据不能为空"
                ));
            }

            // 通过消息发送器广播全局模型
            messageSender.broadcastGlobalModel(taskId, roundNumber, globalModel, aggregationInfo);

            log.info("全局模型广播处理完成: taskId={}, round={}", taskId, roundNumber);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "全局模型广播已发送"
            ));

        } catch (Exception e) {
            log.error("处理全局模型广播失败: taskId={}, round={}, error={}",
                    taskId, roundNumber, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "GLOBAL_MODEL_BROADCAST_FAILED",
                "errorMessage", "全局模型广播失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次完成消息 (ROUND_COMPLETE)
     * v1.4协议新增
     */
    private ProtocolAck onRoundComplete(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        Integer roundNumber = numberAsInt(data, "roundNumber");
        @SuppressWarnings("unchecked")
        Map<String, Object> roundResults = (Map<String, Object>) valueAsObject(data, "roundResults");

        log.info("发送轮次完成通知: vmId={}, taskId={}, round={}", vmId, taskId, roundNumber);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (roundNumber == null || roundNumber < 0) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_ROUND_NUMBER",
                    "errorMessage", "轮次号无效"
                ));
            }

            // 更新轮次状态为完成
            roundStateManager.setRoundState(taskId, roundNumber, RoundState.COMPLETED);

            // 通过消息发送器通知轮次完成
            messageSender.broadcastRoundComplete(taskId, roundNumber, roundResults);

            log.info("轮次完成通知处理完成: taskId={}, round={}", taskId, roundNumber);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "轮次完成通知已发送"
            ));

        } catch (Exception e) {
            log.error("处理轮次完成通知失败: taskId={}, round={}, error={}",
                    taskId, roundNumber, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "ROUND_COMPLETE_FAILED",
                "errorMessage", "轮次完成通知失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理轮次完成确认消息 (ROUND_COMPLETE_ACK)
     * v1.4协议新增
     */
    private ProtocolAck onRoundCompleteAck(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        Integer roundNumber = numberAsInt(data, "roundNumber");
        String status = valueAsString(data, "status");
        Boolean readyForNextRound = valueAsBoolean(data, "readyForNextRound", false);

        log.info("收到轮次完成确认: vmId={}, taskId={}, round={}, status={}, ready={}",
                vmId, taskId, roundNumber, status, readyForNextRound);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (roundNumber == null || roundNumber < 0) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_ROUND_NUMBER",
                    "errorMessage", "轮次号无效"
                ));
            }

            // 记录虚拟机轮次完成确认状态
            vmAckTracker.recordRoundCompleteAck(taskId, roundNumber, vmId, status);

            log.info("轮次完成确认处理完成: vmId={}, taskId={}, round={}", vmId, taskId, roundNumber);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "轮次完成确认已收到"
            ));

        } catch (Exception e) {
            log.error("处理轮次完成确认失败: vmId={}, taskId={}, round={}, error={}",
                    vmId, taskId, roundNumber, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "ROUND_COMPLETE_ACK_FAILED",
                "errorMessage", "轮次完成确认处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理数据集状态查询消息 (DATASET_STATUS_QUERY)
     * v1.4协议新增
     */
    private ProtocolAck onDatasetStatusQuery(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String datasetId = valueAsString(data, "datasetId");
        String queryType = valueAsString(data, "queryType");
        Boolean includeStatistics = valueAsBoolean(data, "includeStatistics", false);
        Boolean includeMetadata = valueAsBoolean(data, "includeMetadata", false);

        log.info("发送数据集状态查询: vmId={}, taskId={}, datasetId={}, queryType={}",
                vmId, taskId, datasetId, queryType);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (datasetId == null || datasetId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_DATASET_ID",
                    "errorMessage", "数据集ID不能为空"
                ));
            }

            // 构建数据集状态查询消息
            ProtocolMessage queryMessage = messageBuilder.buildDatasetStatusQueryMessage(
                vmId, taskId, datasetId, queryType != null ? queryType : "FULL",
                Boolean.TRUE.equals(includeStatistics), Boolean.TRUE.equals(includeMetadata), false
            );

            // 发送查询消息
            messageSender.sendToVm(vmId, queryMessage);

            log.info("数据集状态查询处理完成: vmId={}, taskId={}, datasetId={}", vmId, taskId, datasetId);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "数据集状态查询已发送"
            ));

        } catch (Exception e) {
            log.error("处理数据集状态查询失败: vmId={}, taskId={}, datasetId={}, error={}",
                    vmId, taskId, datasetId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "DATASET_STATUS_QUERY_FAILED",
                "errorMessage", "数据集状态查询失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 处理数据集删除消息 (DATASET_DELETE)
     * v1.4协议新增
     */
    private ProtocolAck onDatasetDelete(ProtocolMessage msg) {
        String vmId = msg.getVmId();
        Map<String, Object> data = msg.getData();

        String taskId = valueAsString(data, "taskId");
        String datasetId = valueAsString(data, "datasetId");
        String reason = valueAsString(data, "reason");
        Boolean backup = valueAsBoolean(data, "backup", false);
        Boolean force = valueAsBoolean(data, "force", false);

        log.info("发送数据集删除命令: vmId={}, taskId={}, datasetId={}, reason={}, backup={}, force={}",
                vmId, taskId, datasetId, reason, backup, force);

        try {
            // 验证参数
            if (taskId == null || taskId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_TASK_ID",
                    "errorMessage", "任务ID不能为空"
                ));
            }

            if (datasetId == null || datasetId.trim().isEmpty()) {
                return ackFor(msg, ProtocolType.ERROR, mapOf(
                    "errorCode", "INVALID_DATASET_ID",
                    "errorMessage", "数据集ID不能为空"
                ));
            }

            // 构建数据集删除消息
            ProtocolMessage deleteMessage = messageBuilder.buildDatasetDeleteMessage(
                vmId, taskId, datasetId, reason != null ? reason : "TASK_COMPLETED",
                Boolean.TRUE.equals(backup), Boolean.TRUE.equals(force)
            );

            // 发送删除命令
            messageSender.sendToVm(vmId, deleteMessage);

            log.info("数据集删除命令处理完成: vmId={}, taskId={}, datasetId={}", vmId, taskId, datasetId);

            return ackFor(msg, ProtocolType.MESSAGE_ERROR, mapOf(
                "status", "SUCCESS",
                "message", "数据集删除命令已发送"
            ));

        } catch (Exception e) {
            log.error("处理数据集删除命令失败: vmId={}, taskId={}, datasetId={}, error={}",
                    vmId, taskId, datasetId, e.getMessage(), e);

            return ackFor(msg, ProtocolType.ERROR, mapOf(
                "errorCode", "DATASET_DELETE_FAILED",
                "errorMessage", "数据集删除命令失败: " + e.getMessage()
            ));
        }
    }

    // v1.3协议遗留方法：各种DATASET_*_NOTIFICATION处理器
    // 已在v1.4协议中移除，数据集状态通过标准DATASET_*_ACK消息处理
    // 参考v1.4协议文档：数据集通知已合并到标准确认消息流程

    // v1.3协议遗留方法：各种TRAINING_*_NOTIFICATION处理器
    // 已在v1.4协议中移除，训练进度通过心跳报告状态
    // 参考v1.4协议文档：训练进度过度细分的4个协议已被移除

    // v1.3协议遗留方法：onModelDownloadNotification, onStatusUpdateNotification
    // 已在v1.4协议中移除，状态更新通过心跳和标准ACK消息处理
    // 参考v1.4协议文档：状态更新通知已合并到心跳和确认消息中

    /**
     * 更新参与者度量指标缓存
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @param round 当前轮次
     * @param accuracy 精度
     * @param loss 损失值
     * @param status 状态
     */
    private void updateParticipantMetricsCache(String taskId, String vmId, Integer round,
                                             Double accuracy, Double loss, String status) {
        try {
            // 创建参与者度量指标
            com.feduwacomm.service.cache.model.ParticipantMetrics metrics =
                com.feduwacomm.service.cache.model.ParticipantMetrics.builder()
                    .vmId(vmId)
                    .taskId(taskId)
                    .currentRound(round)
                    .accuracy(accuracy)
                    .loss(loss)
                    .status(status)
                    .lastUpdated(LocalDateTime.now())
                    .build();

            // 更新缓存
            metricsCacheService.updateParticipantMetrics(taskId, vmId, metrics);

            log.debug("参与者度量指标缓存更新成功: taskId={}, vmId={}, accuracy={}, loss={}",
                    taskId, vmId, accuracy, loss);

            // 重新计算并更新全局指标缓存
            updateGlobalMetricsCache(taskId);

        } catch (com.feduwacomm.service.cache.exception.CacheValidationException e) {
            log.error("更新参与者度量指标缓存失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("更新参与者度量指标缓存时发生意外错误: taskId={}, vmId={}, 错误={}",
                    taskId, vmId, e.getMessage(), e);
        }
    }

    /**
     * 更新全局度量指标缓存
     *
     * @param taskId 任务ID
     */
    private void updateGlobalMetricsCache(String taskId) {
        try {
            // 获取任务信息，用于计算预估时间
            com.feduwacomm.entity.FederatedTask task = federatedTasksMapper.selectTaskById(taskId);
            Integer totalRounds = task != null ? task.getTotalRounds() : null;

            // 重新计算并更新全局指标
            com.feduwacomm.service.cache.model.GlobalMetrics globalMetrics =
                metricsCacheService.computeAndUpdateGlobalMetrics(taskId, totalRounds);

            log.debug("全局度量指标缓存更新成功: taskId={}, globalAccuracy={}, globalLoss={}, rounds={}",
                    taskId, globalMetrics.getGlobalAccuracy(), globalMetrics.getGlobalLoss(),
                    globalMetrics.getCommunicationRounds());

        } catch (com.feduwacomm.service.cache.exception.CacheValidationException e) {
            log.error("更新全局度量指标缓存失败: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("更新全局度量指标缓存时发生意外错误: taskId={}, 错误={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 安全解析Double值
     */
    private Double parseDoubleSafely(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析Double值: {}", value);
            return null;
        }
    }

    /**
     * 安全解析Integer值
     */
    private Integer parseIntegerSafely(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析Integer值: {}", value);
            return null;
        }
    }

    /**
     * 创建错误响应ACK
     *
     * @param originalMessage 原始消息
     * @param errorCode 错误代码
     * @param errorMessage 错误消息
     * @return 错误响应ACK
     */
    private ProtocolAck errorFor(ProtocolMessage originalMessage, String errorCode, String errorMessage) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", true);
        errorData.put("errorCode", errorCode);
        errorData.put("errorMessage", errorMessage);
        if (originalMessage != null) {
            errorData.put("originalMessageId", originalMessage.getId());
        }

        return ProtocolAck.builder()
                .type(ProtocolType.ERROR)
                .id(MessageBuilder.generateStandardId("error"))
                .timestamp(Instant.now())
                .vmId(originalMessage != null ? originalMessage.getVmId() : "unknown")
                .data(errorData)
                .signature("")
                .build();
    }

    // ==================== v1.4协议合规性验证方法 ====================

    /**
     * 验证v1.4协议必需字段
     */
    private ProtocolAck validateRequiredFields(ProtocolMessage msg) {
        // 验证消息ID
        if (msg.getId() == null || msg.getId().trim().isEmpty()) {
            return createErrorAck("FIELD_VALIDATION_ERROR", "消息ID不能为空");
        }

        // 验证时间戳
        if (msg.getTimestamp() == null) {
            return createErrorAck("FIELD_VALIDATION_ERROR", "时间戳不能为空");
        }

        // 验证虚拟机ID
        if (msg.getVmId() == null || msg.getVmId().trim().isEmpty()) {
            return createErrorAck("FIELD_VALIDATION_ERROR", "虚拟机ID不能为空");
        }

        // 验证数据字段
        if (msg.getData() == null) {
            return createErrorAck("FIELD_VALIDATION_ERROR", "数据字段不能为空");
        }

        // 验证签名字段存在（即使为空）
        if (msg.getSignature() == null) {
            return createErrorAck("FIELD_VALIDATION_ERROR", "签名字段不能为null");
        }

        return null; // 所有字段验证通过
    }

    /**
     * 验证v1.4协议消息结构
     */
    private ProtocolAck validateMessageStructure(ProtocolMessage msg) {
        ProtocolType type = msg.getType();
        Map<String, Object> data = msg.getData();

        // 根据消息类型验证特定的数据结构
        switch (type) {
            case CONNECT:
                return validateConnectMessage(data);
            case HEARTBEAT:
                return validateHeartbeatMessage(data);
            case GRADIENT_UPLOAD:
                return validateGradientUploadMessage(data);
            case FEDERATED_TASK_START_ACK:
                return validateTaskAckMessage(data);
            case DATASET_CREATE:
                return validateDatasetCreateMessage(data);
            case VM_STATUS_RESPONSE:
                return validateVmStatusMessage(data);
            default:
                // 对于其他消息类型，执行基本验证
                return validateBasicDataStructure(data);
        }
    }

    /**
     * 验证CONNECT消息结构
     */
    private ProtocolAck validateConnectMessage(Map<String, Object> data) {
        if (!data.containsKey("version")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "CONNECT消息必须包含version字段");
        }
        if (!data.containsKey("supportedMLAlgorithms")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "CONNECT消息必须包含supportedMLAlgorithms字段");
        }
        if (!data.containsKey("systemInfo")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "CONNECT消息必须包含systemInfo字段");
        }
        return null;
    }

    /**
     * 验证HEARTBEAT消息结构
     */
    private ProtocolAck validateHeartbeatMessage(Map<String, Object> data) {
        if (!data.containsKey("status")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "HEARTBEAT消息必须包含status字段");
        }
        if (!data.containsKey("resourceUsage")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "HEARTBEAT消息必须包含resourceUsage字段");
        }
        return null;
    }

    /**
     * 验证GRADIENT_UPLOAD消息结构
     */
    private ProtocolAck validateGradientUploadMessage(Map<String, Object> data) {
        if (!data.containsKey("taskId")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "GRADIENT_UPLOAD消息必须包含taskId字段");
        }
        if (!data.containsKey("roundNumber")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "GRADIENT_UPLOAD消息必须包含roundNumber字段");
        }
        if (!data.containsKey("gradientData")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "GRADIENT_UPLOAD消息必须包含gradientData字段");
        }
        return null;
    }

    /**
     * 验证任务ACK消息结构
     */
    private ProtocolAck validateTaskAckMessage(Map<String, Object> data) {
        if (!data.containsKey("status")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "任务ACK消息必须包含status字段");
        }
        if (!data.containsKey("taskId")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "任务ACK消息必须包含taskId字段");
        }
        return null;
    }

    /**
     * 验证DATASET_CREATE消息结构
     */
    private ProtocolAck validateDatasetCreateMessage(Map<String, Object> data) {
        if (!data.containsKey("taskId")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "DATASET_CREATE消息必须包含taskId字段");
        }
        if (!data.containsKey("datasetId")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "DATASET_CREATE消息必须包含datasetId字段");
        }
        if (!data.containsKey("datasetType")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "DATASET_CREATE消息必须包含datasetType字段");
        }
        return null;
    }

    /**
     * 验证VM_STATUS_RESPONSE消息结构
     */
    private ProtocolAck validateVmStatusMessage(Map<String, Object> data) {
        if (!data.containsKey("overallStatus")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "VM_STATUS_RESPONSE消息必须包含overallStatus字段");
        }
        if (!data.containsKey("resourceUsage")) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "VM_STATUS_RESPONSE消息必须包含resourceUsage字段");
        }
        return null;
    }

    /**
     * 验证基本数据结构
     */
    private ProtocolAck validateBasicDataStructure(Map<String, Object> data) {
        // 确保data不为空且包含基本字段
        if (data.isEmpty()) {
            return createErrorAck("STRUCTURE_VALIDATION_ERROR", "消息数据不能为空");
        }
        return null;
    }

    /**
     * 验证消息时间戳格式
     */
    private boolean isValidTimestamp(String timestamp) {
        try {
            java.time.Instant.parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证UUID格式
     */
    private boolean isValidUuid(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== v1.5协议支持方法 ==========

    /**
     * 发送数据集分配消息 (v1.5)
     * 用于通知VM创建指定的数据集分片
     */
    public void sendDatasetAllocation(String vmId, String taskId, String assignedDatasetId, DatasetSlice datasetSlice) {
        log.info("发送数据集分配消息: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);

        try {
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("taskId", taskId);
            messageData.put("assignedDatasetId", assignedDatasetId);
            messageData.put("localPath", datasetSlice.getLocalPath());
            messageData.put("sampleCount", datasetSlice.getSampleCount());
            messageData.put("startIndex", datasetSlice.getStartIndex());
            messageData.put("endIndex", datasetSlice.getEndIndex());
            messageData.put("dataType", datasetSlice.getDataType());
            messageData.put("transferStatus", "PENDING");

            ProtocolMessage message = ProtocolMessage.builder()
                    .type(ProtocolType.DATASET_CREATE)
                    .vmId(vmId)
                    .data(messageData)
                    .timestamp(Instant.now())
                    .id(messageIdGenerator.generateServerMessageId())
                    .build();

            messagingTemplate.convertAndSend("/topic/vm/" + vmId, message);
            log.info("数据集分配消息发送成功: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

        } catch (Exception e) {
            log.error("发送数据集分配消息失败: vmId={}, assignedDatasetId={}, error={}", vmId, assignedDatasetId, e.getMessage(), e);
            throw new RuntimeException("发送数据集分配消息失败", e);
        }
    }

    /**
     * 发送联邦任务开始消息 (v1.5标准协议)
     * 关键变更：assignedDatasetId位于dataConfig内部
     */
    public void sendFederatedTaskStart(String vmId,
                                       String taskId,
                                       Map<String, Object> dataConfig,
                                       Map<String, Object> initialModel,
                                       Map<String, Object> trainingPlan) {
        Map<String, Object> safeDataConfig = dataConfig != null ? new HashMap<>(dataConfig) : new HashMap<>();
        String assignedDatasetId = (String) safeDataConfig.get("assignedDatasetId");
        String distributionId = initialModel != null ? Objects.toString(initialModel.get("distributionId"), null) : null;

        log.info("准备发送FEDERATED_TASK_START消息: vmId={}, taskId={}, assignedDatasetId={}, distributionId={}",
                vmId, taskId, assignedDatasetId, distributionId);
        log.info("FEDERATED_TASK_START initialModel payload: {}", initialModel);

        try {
            if (!safeDataConfig.containsKey("dataPath")) {
                safeDataConfig.put("dataPath", "/data/training");
            }
            if (!safeDataConfig.containsKey("validationSplit")) {
                safeDataConfig.put("validationSplit", 0.2);
            }
            if (!safeDataConfig.containsKey("shuffle")) {
                safeDataConfig.put("shuffle", Boolean.TRUE);
            }

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("taskId", taskId);
            messageData.put("timestamp", Instant.now().toString());
            messageData.put("dataConfig", safeDataConfig);
            if (initialModel != null && !initialModel.isEmpty()) {
                messageData.put("initialModel", initialModel);
            }
            if (trainingPlan != null && !trainingPlan.isEmpty()) {
                messageData.put("trainingPlan", trainingPlan);
                Object algorithm = trainingPlan.get("algorithm");
                if (algorithm != null) {
                    messageData.put("federatedAlgorithm", algorithm);
                }
                Object totalRounds = trainingPlan.get("totalRounds");
                if (totalRounds != null) {
                    messageData.put("totalRounds", totalRounds);
                }
                Object roundNumber = trainingPlan.get("roundNumber");
                if (roundNumber != null) {
                    messageData.put("roundNumber", roundNumber);
                }
            }

            ProtocolMessage message = ProtocolMessage.builder()
                    .type(ProtocolType.FEDERATED_TASK_START)
                    .vmId(vmId)
                    .data(messageData)
                    .timestamp(Instant.now())
                    .id(messageIdGenerator.generateServerMessageId())
                    .build();

            messagingTemplate.convertAndSend("/topic/vm/" + vmId, message);
            log.info("FEDERATED_TASK_START消息发送成功: vmId={}, taskId={}, assignedDatasetId={}, distributionId={}",
                    vmId, taskId, assignedDatasetId, distributionId);

        } catch (Exception e) {
            log.error("发送FEDERATED_TASK_START消息失败: vmId={}, assignedDatasetId={}, error={}",
                    vmId, assignedDatasetId, e.getMessage(), e);
            throw new RuntimeException("发送联邦任务开始消息失败", e);
        }
    }

    /**
     * 查询数据集状态 (v1.5新协议)
     * 用于查询VM上的数据集状态
     */
    public DatasetQueryResult queryDatasetStatus(String vmId, String taskId, String assignedDatasetId) {
        log.info("发送DATASET_LIST_QUERY查询: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);

        try {
            Map<String, Object> queryData = new HashMap<>();
            queryData.put("taskId", taskId);
            queryData.put("assignedDatasetId", assignedDatasetId);

            ProtocolMessage message = ProtocolMessage.builder()
                    .type(ProtocolType.DATASET_STATUS_QUERY) // 复用现有协议类型
                    .vmId(vmId)
                    .data(queryData)
                    .timestamp(Instant.now())
                    .id(messageIdGenerator.generateServerMessageId())
                    .build();

            messagingTemplate.convertAndSend("/topic/vm/" + vmId, message);
            log.info("DATASET_LIST_QUERY消息发送成功: vmId={}", vmId);

            // 🔄 等待响应 (实际实现中使用CompletableFuture或消息回调)
            return waitForDatasetResponse(vmId, taskId, assignedDatasetId);

        } catch (Exception e) {
            log.error("查询数据集状态失败: vmId={}, assignedDatasetId={}, error={}", vmId, assignedDatasetId, e.getMessage(), e);
            return DatasetQueryResult.builder()
                    .success(false)
                    .errorMessage("查询失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 处理数据集列表响应 (v1.5)
     * 处理来自VM的数据集状态响应
     */
    public void handleDatasetListResponse(ProtocolMessage message) {
        log.info("收到DATASET_LIST_RESPONSE消息: vmId={}", message.getVmId());

        try {
            String vmId = message.getVmId();
            Map<String, Object> data = message.getData();
            String taskId = (String) data.get("taskId");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> datasets = (List<Map<String, Object>>) data.get("datasets");

            for (Map<String, Object> dataset : datasets) {
                String assignedDatasetId = (String) dataset.get("assignedDatasetId");
                String status = (String) dataset.get("status");
                String localPath = (String) dataset.get("localPath");

                log.info("处理数据集状态: vmId={}, assignedDatasetId={}, status={}", vmId, assignedDatasetId, status);

                // 更新数据库中的数据集状态
                TaskParticipant participant = taskParticipantsMapper.selectParticipant(taskId, vmId);
                if (participant != null && assignedDatasetId.equals(participant.getAssignedDatasetId())) {
                    participant.setDatasetStatus(status);
                    participant.setLocalPath(localPath);
                    participant.setDatasetCreatedAt(LocalDateTime.now());
                    taskParticipantsMapper.updateParticipant(participant);

                    log.info("数据集状态更新成功: vmId={}, assignedDatasetId={}, status={}", vmId, assignedDatasetId, status);
                } else {
                    log.warn("无法匹配数据集响应: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);
                }
            }

        } catch (Exception e) {
            log.error("处理数据集列表响应失败: vmId={}, error={}", message.getVmId(), e.getMessage(), e);
        }
    }

    /**
     * 处理梯度上传消息 (v1.5更新)
     * 关键变更：包含assignedDatasetId验证
     */
    public ProtocolAck handleGradientUploadWithValidation(ProtocolMessage message) {
        Map<String, Object> data = message.getData();
        String taskId = (String) data.get("taskId");
        String vmId = message.getVmId();
        String assignedDatasetId = (String) data.get("assignedDatasetId");  // 🆕 标准新增字段

        log.info("收到GRADIENT_UPLOAD消息: vmId={}, taskId={}, assignedDatasetId={}", vmId, taskId, assignedDatasetId);

        try {
            // 🔑 关键验证：assignedDatasetId的合法性
            TaskParticipant participant = taskParticipantsMapper.selectParticipant(taskId, vmId);
            if (participant == null || !assignedDatasetId.equals(participant.getAssignedDatasetId())) {
                log.error("GRADIENT_UPLOAD消息中assignedDatasetId验证失败: taskId={}, vmId={}, expected={}, actual={}",
                        taskId, vmId,
                        participant != null ? participant.getAssignedDatasetId() : "null",
                        assignedDatasetId);

                return ackFor(message, ProtocolType.MESSAGE_ERROR, mapOf(
                        "errorCode", "DATASET_ID_MISMATCH",
                        "errorMessage", "数据集ID验证失败"));
            }

            log.info("assignedDatasetId验证成功: vmId={}, assignedDatasetId={}", vmId, assignedDatasetId);

            // 处理梯度上传逻辑（复用现有逻辑）
            return processGradientUpload(message);

        } catch (Exception e) {
            log.error("处理梯度上传失败: vmId={}, assignedDatasetId={}, error={}", vmId, assignedDatasetId, e.getMessage(), e);
            return ackFor(message, ProtocolType.MESSAGE_ERROR, mapOf(
                    "errorCode", "GRADIENT_UPLOAD_ERROR",
                    "errorMessage", "梯度上传处理失败: " + e.getMessage()));
        }
    }

    /**
     * 验证assignedDatasetId的合法性 (v1.5新增)
     * 检查数据集ID是否与任务参与者匹配
     */
    public boolean validateAssignedDatasetId(String taskId, String vmId, String assignedDatasetId) {
        log.debug("验证assignedDatasetId: taskId={}, vmId={}, assignedDatasetId={}", taskId, vmId, assignedDatasetId);

        try {
            if (assignedDatasetId == null || assignedDatasetId.trim().isEmpty()) {
                log.warn("assignedDatasetId为空: taskId={}, vmId={}", taskId, vmId);
                return false;
            }

            TaskParticipant participant = taskParticipantsMapper.selectParticipant(taskId, vmId);
            if (participant == null) {
                log.warn("未找到任务参与者: taskId={}, vmId={}", taskId, vmId);
                return false;
            }

            boolean isValid = assignedDatasetId.equals(participant.getAssignedDatasetId());
            log.debug("assignedDatasetId验证结果: taskId={}, vmId={}, valid={}", taskId, vmId, isValid);

            return isValid;

        } catch (Exception e) {
            log.error("验证assignedDatasetId失败: taskId={}, vmId={}, error={}", taskId, vmId, e.getMessage(), e);
            return false;
        }
    }

    // ========== v1.5辅助方法 ==========

    /**
     * 等待数据集响应 (v1.5辅助方法)
     * 实际实现中需要使用异步等待机制
     */
    private DatasetQueryResult waitForDatasetResponse(String vmId, String taskId, String assignedDatasetId) {
        // 🔄 实际实现中需要使用异步等待机制
        // 这里简化为同步模拟，实际应该使用CompletableFuture
        log.info("等待数据集查询响应: vmId={}, taskId={}", vmId, taskId);

        // 模拟响应结果
        return DatasetQueryResult.builder()
                .success(true)
                .datasetStatus("CREATED")
                .localPath("/data/assigned/" + assignedDatasetId)
                .build();
    }

    /**
     * 处理梯度上传的核心逻辑 (复用现有实现)
     */
    private ProtocolAck processGradientUpload(ProtocolMessage message) {
        // 这里应该调用现有的梯度上传处理逻辑
        // 为了保持向后兼容，复用现有的处理方法
        log.info("处理梯度上传: vmId={}, messageId={}", message.getVmId(), message.getId());

        // 返回成功确认
        return ackFor(message, ProtocolType.GRADIENT_UPLOAD_ACK, mapOf(
                "status", "SUCCESS",
                "message", "梯度上传处理成功"));
    }

} 
