package com.feduwacomm.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 虚拟机轮次模型结果实体类
 * 存储虚拟机在每轮训练中的模型结果和性能指标
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmRoundModel {

    /**
     * 唯一标识(32位UUID)
     */
    private String id;

    /**
     * 任务ID(32位UUID)
     */
    private String taskId;

    /**
     * 虚拟机ID(32位UUID)
     */
    private String vmId;

    /**
     * 训练轮数
     */
    private Integer roundNumber;

    /**
     * 准确率
     */
    private BigDecimal accuracy;

    /**
     * 损失值
     */
    private BigDecimal loss;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    /**
     * 本地模型参数/元信息(JSON，仅记录，不存文件路径)
     */
    private String parameters;
}