package com.feduwacomm.service.impl;

import com.feduwacomm.service.InitialModelGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 初始模型分发重试执行器
 * 由重试调度触发，调用业务服务执行实际分发。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialModelDistributionRetryExecutor {

    private final InitialModelGenerationService initialModelGenerationService;

    /**
     * 执行初始模型分发重试。
     *
     * @param modelId 模型ID
     * @param attempt 当前重试次数
     * @param context 重试上下文
     */
    public void retry(String modelId, int attempt, Map<String, Object> context) {
        if (!StringUtils.hasText(modelId)) {
            log.warn("初始模型分发重试缺少模型ID");
            return;
        }
        if (context == null) {
            log.warn("初始模型分发重试缺少上下文: modelId={}", modelId);
            return;
        }

        String taskId = (String) context.get("taskId");
        @SuppressWarnings("unchecked")
        List<String> failedVmIds = (List<String>) context.get("failedVmIds");
        String orchestrationId = (String) context.get("orchestrationId");

        if (!StringUtils.hasText(taskId)) {
            log.warn("初始模型分发重试缺少任务ID: modelId={}", modelId);
            return;
        }
        if (CollectionUtils.isEmpty(failedVmIds)) {
            log.warn("初始模型分发重试缺少目标虚拟机: modelId={}, taskId={}", modelId, taskId);
            return;
        }

        try {
            initialModelGenerationService.retryInitialModelDistribution(
                    modelId,
                    taskId,
                    failedVmIds,
                    orchestrationId,
                    attempt
            );
        } catch (Exception ex) {
            log.error("执行初始模型分发重试失败: modelId={}, taskId={}, attempt={}", modelId, taskId, attempt, ex);
            throw ex;
        }
    }
}
