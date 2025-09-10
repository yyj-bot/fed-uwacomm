package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 工作流启动请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStartRequest {

    /**
     * 关联的任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 工作流名称
     */
    private String workflowName;

    /**
     * 初始模型生成策略
     */
    @NotNull(message = "初始模型策略不能为空")
    private InitialModelStrategy initialModelStrategy;

    /**
     * 数据分发策略
     */
    @NotNull(message = "数据分发策略不能为空")
    private DataDistributionStrategy dataDistributionStrategy;

    /**
     * 自定义配置参数
     */
    private Map<String, Object> customConfig;

    /**
     * 参与训练的虚拟机ID列表
     */
    private java.util.List<String> participantVmIds;

    /**
     * 初始模型策略枚举
     */
    public enum InitialModelStrategy {
        RANDOM("随机生成"),
        CUSTOM_UPLOAD("自定义上传");

        private final String description;

        InitialModelStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 数据分发策略枚举
     */
    public enum DataDistributionStrategy {
        BALANCED("均衡分发"),
        RANDOM("随机分发"),
        ROUND_ROBIN("轮询分发"),
        CUSTOM("自定义分发");

        private final String description;

        DataDistributionStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}