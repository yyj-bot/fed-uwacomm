package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 虚拟机轮次模型结果响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModelVO {

    /**
     * 虚拟机轮次模型ID
     */
    private String vmRoundModelId;

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
     * 模型JSON
     */
    private Map<String, Object> modelJson;

    /**
     * 指标信息
     */
    private Map<String, Object> metrics;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;
}