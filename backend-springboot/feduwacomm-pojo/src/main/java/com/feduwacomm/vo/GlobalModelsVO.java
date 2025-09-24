package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 全局模型列表响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalModelsVO {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 全局模型列表
     */
    private List<GlobalModelInfo> models;

    /**
     * 全局模型信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalModelInfo {
        /**
         * 模型ID
         */
        private String modelId;

        /**
         * 轮次
         */
        private Integer round;

        /**
         * 聚合方法
         */
        private String aggregationMethod;

        /**
         * 模型参数
         */
        private Map<String, Object> parameters;

        /**
         * 模型性能指标
         */
        private Map<String, Object> metrics;

        /**
         * 参与聚合的VM数量
         */
        private Integer participantCount;

        /**
         * 创建时间
         */
        private LocalDateTime createdAt;

        /**
         * 模型文件路径
         */
        private String modelPath;

        /**
         * 模型版本
         */
        private String version;
    }
}