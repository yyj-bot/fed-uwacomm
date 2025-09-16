package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量模型上传响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBatchUploadResponseVO {

    /**
     * 成功数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failedCount;

    /**
     * 模型列表
     */
    private List<ModelBatchUploadResultVO> models;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelBatchUploadResultVO {
        /**
         * 模型ID
         */
        private String modelId;

        /**
         * 状态
         */
        private String status;

        /**
         * 消息
         */
        private String message;
    }
}