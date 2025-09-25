package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 可用虚拟机查询响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableVmsResponseVO {

    private Integer total;
    private List<AvailableVmVO> availableVms;
    private List<String> recommendedSelection;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableVmVO {
        private String vmId;
        private String name;
        private String status;
        private Map<String, Object> capabilities;
        private Double currentLoad;
        private LocalDateTime lastHeartbeat;
        private String ipAddress;
        private Integer port;
    }
}