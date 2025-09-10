package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 模型分发请求DTO
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDistributionDTO {
    
    /**
     * 模型ID
     */
    @NotBlank(message = "模型ID不能为空")
    private String modelId;
    
    /**
     * 目标虚拟机ID列表
     */
    @NotEmpty(message = "目标虚拟机列表不能为空")
    private List<String> targetVmIds;
    
    /**
     * 是否验证分发完整性
     */
    @Builder.Default
    private Boolean verifyIntegrity = true;
    
    /**
     * 分发超时时间(秒)
     */
    @Builder.Default
    private Integer timeoutSeconds = 300;
    
    /**
     * 最大重试次数
     */
    @Builder.Default
    private Integer maxRetries = 3;
}