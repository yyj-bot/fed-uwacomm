package com.feduwacomm.model.dto.initial;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 初始模型分发请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModelDistributeRequest {

    /**
     * 指定分发的初始模型ID（可选，不填则使用任务绑定的模型）
     */
    @Size(max = 32, message = "模型ID长度不能超过32个字符")
    private String modelId;

    /**
     * 指定分发的目标虚拟机ID列表（可选，不填则默认任务参与者）
     */
    private List<@Size(max = 32, message = "虚拟机ID长度不能超过32个字符") String> targetVmIds;

    /**
     * 是否校验文件完整性
     */
    private Boolean verifyIntegrity;

    /**
     * 分发超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;
}
