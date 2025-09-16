package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟机轮次模型最佳/离群查询DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelBestQueryDTO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 指标名称
     */
    private String metric;

    /**
     * 类型（best/outlier）
     */
    private String type;
}