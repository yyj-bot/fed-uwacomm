package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 虚拟机轮次模型查询DTO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelQueryDTO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 轮数
     */
    private Integer roundNumber;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 页大小
     */
    private Integer size;
}