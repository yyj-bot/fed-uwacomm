package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 虚拟机批量取消分配响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmBatchUnassignmentResponseVO {

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
     * 批量取消分配状态
     */
    private String overallStatus;

    /**
     * 取消分配时间
     */
    private LocalDateTime unassignedAt;

    /**
     * 取消分配者ID
     */
    private String unassignedBy;

    /**
     * 取消分配者用户名
     */
    private String unassignedByUsername;

    /**
     * 成功取消分配数量
     */
    private Integer successCount;

    /**
     * 失败取消分配数量
     */
    private Integer failureCount;

    /**
     * 总数量
     */
    private Integer totalCount;

    /**
     * 详细结果列表
     */
    private List<BatchUnassignmentDetail> details;

    /**
     * 批量操作备注
     */
    private String notes;

    /**
     * 批量取消分配详情内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchUnassignmentDetail {
        private String vmId;
        private String vmName;
        private String status;
        private String errorMessage;
        private LocalDateTime originalAssignedAt;
        private LocalDateTime processedAt;
    }
}