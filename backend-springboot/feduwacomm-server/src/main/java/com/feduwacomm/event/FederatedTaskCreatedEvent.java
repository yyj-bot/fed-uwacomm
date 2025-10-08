package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联邦学习任务创建完成事件
 * 当联邦学习任务创建完成时发布此事件，触发后续的数据分发流程
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 */
@Getter
public class FederatedTaskCreatedEvent extends ApplicationEvent {

    /**
     * 任务ID
     */
    private final String taskId;

    /**
     * 任务名称
     */
    private final String taskName;

    /**
     * 创建者用户ID
     */
    private final String createdBy;

    /**
     * 数据集ID（用于数据分发）
     */
    private final String datasetId;

    /**
     * 参与者VM ID列表
     */
    private final List<String> participantVmIds;

    /**
     * 分配策略
     */
    private final String distributionStrategy;

    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;

    /**
     * 构造函数
     *
     * @param source 事件源对象
     * @param taskId 任务ID
     * @param taskName 任务名称
     * @param createdBy 创建者
     * @param datasetId 数据集ID
     * @param participantVmIds 参与者VM ID列表
     * @param distributionStrategy 分配策略
     */
    public FederatedTaskCreatedEvent(Object source, String taskId, String taskName,
                                   String createdBy, String datasetId,
                                   List<String> participantVmIds, String distributionStrategy) {
        super(source);
        this.taskId = taskId;
        this.taskName = taskName;
        this.createdBy = createdBy;
        this.datasetId = datasetId;
        this.participantVmIds = participantVmIds;
        this.distributionStrategy = distributionStrategy;
        this.eventTime = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("FederatedTaskCreatedEvent{taskId='%s', taskName='%s', datasetId='%s', participantCount=%d, strategy='%s', eventTime=%s}",
                taskId, taskName, datasetId,
                participantVmIds != null ? participantVmIds.size() : 0,
                distributionStrategy, eventTime);
    }
}
