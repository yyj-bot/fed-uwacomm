package com.feduwacomm.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 轮次完成事件
 * 当一轮训练完成（所有客户端完成或超时）时触发
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class RoundCompleteEvent extends ApplicationEvent {

    private final String taskId;
    private final Integer completedRound;
    private final Integer participantCount;
    private final Integer expectedParticipants;
    private final LocalDateTime completeTime;
    private final String completionReason;
    private final Map<String, BigDecimal> globalMetrics;

    public RoundCompleteEvent(Object source, String taskId, Integer completedRound, 
                             Integer participantCount, Integer expectedParticipants,
                             String completionReason) {
        super(source);
        this.taskId = taskId;
        this.completedRound = completedRound;
        this.participantCount = participantCount;
        this.expectedParticipants = expectedParticipants;
        this.completeTime = LocalDateTime.now();
        this.completionReason = completionReason;
        this.globalMetrics = null;
    }

    public RoundCompleteEvent(Object source, String taskId, Integer completedRound,
                             Integer participantCount, Integer expectedParticipants,
                             String completionReason, Map<String, BigDecimal> globalMetrics) {
        super(source);
        this.taskId = taskId;
        this.completedRound = completedRound;
        this.participantCount = participantCount;
        this.expectedParticipants = expectedParticipants;
        this.completeTime = LocalDateTime.now();
        this.completionReason = completionReason;
        this.globalMetrics = globalMetrics;
    }

    @Override
    public String toString() {
        return String.format("RoundCompleteEvent{taskId='%s', round=%d, participants=%d/%d, reason='%s', time=%s}", 
                taskId, completedRound, participantCount, expectedParticipants, completionReason, completeTime);
    }
}