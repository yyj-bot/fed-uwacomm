package com.feduwacomm.listener;

import com.feduwacomm.event.FederatedTaskCreatedEvent;
import com.feduwacomm.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务事件监听器
 * 处理任务创建相关的事件，自动触发数据分发流程
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederatedTaskEventListener {

    private final LogService logService;

    /**
     * 处理联邦学习任务启动事件（v1.5.1修正版）
     * 任务启动时触发数据切片和分发流程
     *
     * @param event 任务启动事件
     */
    @EventListener
    // @Async  // 暂时禁用异步，用于调试
    public void handleTaskStarted(com.feduwacomm.event.FederatedTaskStartedEvent event) {
        System.out.println("🔥🔥🔥 FederatedTaskEventListener.handleTaskStarted() 被调用");
        log.info("🎯 处理联邦学习任务启动事件 (v1.5.1修正版): {}", event);
        System.out.println("🔥🔥🔥 事件内容: taskId=" + event.getTaskId() + ", datasetId=" + event.getDatasetId());

        try {
            String taskId = event.getTaskId();
            String datasetId = event.getDatasetId();
            List<String> participantVmIds = event.getParticipantVmIds();
            String distributionStrategy = event.getDistributionStrategy();

            Map<String, Object> logDetails = new HashMap<>();
            logDetails.put("taskId", taskId);
            logDetails.put("datasetId", datasetId);
            logDetails.put("participantVmIds", participantVmIds);
            logDetails.put("distributionStrategy", distributionStrategy);
            logDetails.put("eventTime", event.getEventTime());

            logService.logTask(taskId, "INFO",
                    "任务启动事件已触发，等待工作流阶段执行数据分发",
                    "FederatedTaskEventListener", null, logDetails);

        } catch (Exception e) {
            log.error("❌ 处理任务启动事件失败（工作流驱动模式）: taskId={}, error={}",
                    event.getTaskId(), e.getMessage(), e);
        }
    }
}
