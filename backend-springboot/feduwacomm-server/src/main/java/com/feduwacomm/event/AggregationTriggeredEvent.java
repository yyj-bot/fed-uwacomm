package com.feduwacomm.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聚合触发事件
 * 当满足聚合条件时触发，启动聚合处理流程
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class AggregationTriggeredEvent extends ApplicationEvent {

    private final String taskId;
    private final Integer roundNumber;
    private final String algorithm;
    private final List<String> participantVmIds;
    private final String triggerReason;
    private final LocalDateTime triggerTime;
    private final boolean isTimeoutTriggered;

    public AggregationTriggeredEvent(Object source, String taskId, Integer roundNumber, 
                                   String algorithm, List<String> participantVmIds, 
                                   String triggerReason) {
        super(source);
        this.taskId = taskId;
        this.roundNumber = roundNumber;
        this.algorithm = algorithm;
        this.participantVmIds = participantVmIds != null ? List.copyOf(participantVmIds) : List.of();
        this.triggerReason = triggerReason;
        this.triggerTime = LocalDateTime.now();
        this.isTimeoutTriggered = triggerReason != null && triggerReason.contains("TIMEOUT");
    }

    public int getParticipantCount() {
        return participantVmIds.size();
    }

    @Override
    public String toString() {
        return String.format("AggregationTriggeredEvent{taskId='%s', round=%d, algorithm='%s', participants=%d, reason='%s', time=%s}", 
                taskId, roundNumber, algorithm, getParticipantCount(), triggerReason, triggerTime);
    }
}