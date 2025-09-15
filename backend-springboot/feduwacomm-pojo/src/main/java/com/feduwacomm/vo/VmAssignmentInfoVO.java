package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 虚拟机分配信息VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmAssignmentInfoVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机名称
     */
    private String vmName;

    /**
     * 虚拟机基本信息
     */
    private VmBasicInfo vmInfo;

    /**
     * 是否已分配
     */
    private Boolean isAssigned;

    /**
     * 已分配用户列表
     */
    private List<AssignedUser> assignedUsers;

    /**
     * 总分配用户数
     */
    private Integer totalAssignedUsers;

    /**
     * 虚拟机创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 虚拟机基本信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VmBasicInfo {
        private String ipAddress;
        private Integer port;
        private String osType;
        private String status;
        private String connectionStatus;
        private LocalDateTime lastHeartbeat;
    }

    /**
     * 已分配用户信息内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignedUser {
        private String userId;
        private String username;
        private String email;
        private Set<String> permissions;
        private LocalDateTime assignedAt;
        private String assignedByUsername;
        private String notes;
    }
}