package com.feduwacomm.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聚合完成事件
 * 当模型聚合完成时触发，用于分发全局模型和状态更新
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class AggregationCompletedEvent extends ApplicationEvent {

    private final String taskId;
    private final Integer roundNumber;
    private final String globalModelId;
    private final boolean success;
    private final String errorMessage;
    private final Map<String, BigDecimal> globalMetrics;
    private final Integer participantCount;
    private final Long aggregationDuration;
    private final String algorithm;
    private final LocalDateTime completedTime;

    public AggregationCompletedEvent(Object source, String taskId, Integer roundNumber, 
                                   String globalModelId, boolean success, String errorMessage,
                                   Map<String, BigDecimal> globalMetrics, Integer participantCount,
                                   Long aggregationDuration, String algorithm) {
        super(source);
        this.taskId = taskId;
        this.roundNumber = roundNumber;
        this.globalModelId = globalModelId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.globalMetrics = globalMetrics;
        this.participantCount = participantCount;
        this.aggregationDuration = aggregationDuration;
        this.algorithm = algorithm;
        this.completedTime = LocalDateTime.now();
    }

    public static AggregationCompletedEvent success(Object source, String taskId, Integer roundNumber,
                                                   String globalModelId, Map<String, BigDecimal> globalMetrics,
                                                   Integer participantCount, Long aggregationDuration, String algorithm) {
        return new AggregationCompletedEvent(source, taskId, roundNumber, globalModelId, true, null,
                globalMetrics, participantCount, aggregationDuration, algorithm);
    }

    public static AggregationCompletedEvent failure(Object source, String taskId, Integer roundNumber,
                                                   String errorMessage, Integer participantCount,
                                                   Long aggregationDuration, String algorithm) {
        return new AggregationCompletedEvent(source, taskId, roundNumber, null, false, errorMessage,
                null, participantCount, aggregationDuration, algorithm);
    }

    @Override
    public String toString() {
        return String.format("AggregationCompletedEvent{taskId='%s', round=%d, success=%s, participants=%d, duration=%dms, algorithm='%s', time=%s}", 
                taskId, roundNumber, success, participantCount, aggregationDuration, algorithm, completedTime);
    }
}