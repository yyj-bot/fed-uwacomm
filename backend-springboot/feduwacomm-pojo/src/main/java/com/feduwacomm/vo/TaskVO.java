package com.feduwacomm.vo;

import com.feduwacomm.enums.FederatedAlgorithm;
import com.feduwacomm.enums.FederatedTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联邦学习任务基本信息响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskVO {

    private String taskId;
    private String taskName;
    private String taskType;
    private String status;
    private String algorithm;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer participantCount;
    private Integer currentRound;
    private Integer totalRounds;
    private Double progress;
    private String createdBy;
    private Double finalAccuracy; // 仅完成状态时有值
    private TaskOperationVO.InitialModelSummary initialModelSummary;
    private TaskOperationVO.ConfigSummaryVO configSummary;
}
