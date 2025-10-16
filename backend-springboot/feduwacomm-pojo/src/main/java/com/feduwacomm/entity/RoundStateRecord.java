package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * round_states 表对应实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundStateRecord {

    private Long id;

    private String taskId;

    private Integer roundNumber;

    /**
     * 轮次状态，使用 RoundState.name() 持久化
     */
    private String state;

    private Integer participantCount;

    private Integer completedParticipants;

    private Integer gradientUploadsReceived;

    private Integer modelBroadcastsAcked;

    /**
     * 轮次数据集绑定快照(JSON字符串)，记录vmId->assignedDatasetId等信息
     */
    private String datasetBindings;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
