package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟机轮次模型趋势查询DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelTrendQueryDTO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 指标名称
     */
    private String metric;
}