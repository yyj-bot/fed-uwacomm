package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 虚拟机分配概况VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAssignmentOverviewVO {

    /**
     * 总虚拟机数量
     */
    private Integer totalVmCount;

    /**
     * 已分配虚拟机数量
     */
    private Integer assignedVmCount;

    /**
     * 未分配虚拟机数量
     */
    private Integer unassignedVmCount;

    /**
     * 在线虚拟机数量
     */
    private Integer onlineVmCount;

    /**
     * 离线虚拟机数量
     */
    private Integer offlineVmCount;

    /**
     * 总用户数量
     */
    private Integer totalUserCount;

    /**
     * 有虚拟机分配的用户数量
     */
    private Integer usersWithVmCount;

    /**
     * 总分配关系数量
     */
    private Integer totalAssignmentCount;

    /**
     * 虚拟机状态统计
     */
    private Map<String, Integer> vmStatusStats;

    /**
     * 用户权限统计
     */
    private Map<String, Integer> permissionStats;

    /**
     * 最近分配记录
     */
    private List<RecentAssignment> recentAssignments;

    /**
     * 虚拟机利用率统计
     */
    private VmUtilizationStats utilizationStats;

    /**
     * 最近分配记录内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentAssignment {
        private String vmId;
        private String vmName;
        private String userId;
        private String username;
        private String assignedByUsername;
        private String assignedAt;
        private String operation; // ASSIGN, UNASSIGN, UPDATE_PERMISSION
    }

    /**
     * 虚拟机利用率统计内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmUtilizationStats {
        private Double averageAssignmentsPerVm;
        private Double averageVmsPerUser;
        private Integer maxAssignmentsPerVm;
        private Integer maxVmsPerUser;
        private Double vmUtilizationRate; // 已分配虚拟机占总虚拟机的比例
    }
}