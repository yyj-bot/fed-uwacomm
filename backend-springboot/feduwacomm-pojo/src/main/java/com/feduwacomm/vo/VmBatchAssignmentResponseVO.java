package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 虚拟机批量分配响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmBatchAssignmentResponseVO {

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 目标用户ID
     */
    private String userId;

    /**
     * 目标用户名
     */
    private String username;

    /**
     * 分配的权限
     */
    private Set<String> permissions;

    /**
     * 批量分配状态
     */
    private String overallStatus;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 分配者ID
     */
    private String assignedBy;

    /**
     * 分配者用户名
     */
    private String assignedByUsername;

    /**
     * 成功分配数量
     */
    private Integer successCount;

    /**
     * 失败分配数量
     */
    private Integer failureCount;

    /**
     * 总数量
     */
    private Integer totalCount;

    /**
     * 详细结果列表
     */
    private List<BatchAssignmentDetail> details;

    /**
     * 批量分配备注
     */
    private String notes;

    /**
     * 批量分配详情内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchAssignmentDetail {
        private String vmId;
        private String vmName;
        private String status;
        private String errorMessage;
        private LocalDateTime processedAt;
    }
}