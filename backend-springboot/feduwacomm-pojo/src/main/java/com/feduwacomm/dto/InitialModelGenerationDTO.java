package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 初始模型生成请求DTO
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelGenerationDTO {
    
    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;
    
    /**
     * 模型类型 (如: CNN, LSTM, TRANSFORMER等)
     */
    @NotBlank(message = "模型类型不能为空")
    private String modelType;
    
    /**
     * 生成方式: RANDOM-随机生成, CUSTOM_UPLOAD-自定义上传
     */
    @NotBlank(message = "生成方式不能为空")
    private String generationMethod;
    
    /**
     * 架构参数
     * 对于随机生成: 包含layer数量、节点数、激活函数等
     * 对于自定义上传: 包含验证参数
     */
    private Map<String, Object> architectureParams;
    
    /**
     * 是否立即分发到虚拟机
     */
    @Builder.Default
    private Boolean autoDistribute = false;
    
    /**
     * 目标虚拟机ID列表(如果指定了autoDistribute=true)
     */
    private java.util.List<String> targetVmIds;
}