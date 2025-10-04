package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * v1.5数据集上传结果
 * 用于返回数据集上传和预处理的结果信息
 *
 * @author FedUWAComm Team
 * @version 1.5.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetUploadResult {

    /**
     * 数据集ID (后端生成的UUID)
     */
    private String datasetId;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 数据集元信息
     */
    private DatasetMetadata metadata;

    /**
     * 处理状态消息
     */
    private String message;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;

    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 数据集元信息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetMetadata {
        /**
         * 列数
         */
        private int columnCount;

        /**
         * 行数
         */
        private int rowCount;

        /**
         * 文件大小（字节）
         */
        private long fileSize;

        /**
         * 文件格式
         */
        private String fileFormat;

        /**
         * 数据类型
         */
        private String dataType;

        /**
         * 特征列名列表
         */
        private java.util.List<String> featureColumns;

        /**
         * 标签列名
         */
        private String labelColumn;

        /**
         * 样本分布信息
         */
        private java.util.Map<String, Object> sampleDistribution;

        /**
         * 转换为JSON字符串
         */
        public String toJson() {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(this);
            } catch (Exception e) {
                return "{}";
            }
        }

        /**
         * 从JSON字符串创建
         */
        public static DatasetMetadata fromJson(String json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(json, DatasetMetadata.class);
            } catch (Exception e) {
                return new DatasetMetadata();
            }
        }
    }

    /**
     * 创建成功结果
     */
    public static DatasetUploadResult success(String datasetId, String filePath, DatasetMetadata metadata) {
        return DatasetUploadResult.builder()
            .datasetId(datasetId)
            .filePath(filePath)
            .metadata(metadata)
            .success(true)
            .message("数据集上传和预处理完成")
            .uploadedAt(LocalDateTime.now())
            .processedAt(LocalDateTime.now())
            .build();
    }

    /**
     * 创建失败结果
     */
    public static DatasetUploadResult failure(String errorMessage) {
        return DatasetUploadResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .uploadedAt(LocalDateTime.now())
            .build();
    }
}