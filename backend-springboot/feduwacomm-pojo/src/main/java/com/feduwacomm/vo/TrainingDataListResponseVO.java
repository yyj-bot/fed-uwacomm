package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练数据列表响应VO
 * 专门用于测试场景的响应包装类
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 * @since 2025-09-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataListResponseVO {

    /**
     * 数据总数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 数据列表
     */
    private List<TrainingDataItemResponseVO> dataList;

    /**
     * 响应时间
     */
    private LocalDateTime responseTime;

    /**
     * 训练数据项响应VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrainingDataItemResponseVO {

        /**
         * 数据ID
         */
        private String id;

        /**
         * 数据名称
         */
        private String name;

        /**
         * 数据类型
         */
        private String type;

        /**
         * 数据大小（字节）
         */
        private Long size;

        /**
         * 数据状态
         */
        private String status;

        /**
         * 上传时间
         */
        private LocalDateTime uploadTime;

        /**
         * 上传者
         */
        private String uploader;

        /**
         * 数据描述
         */
        private String description;

        /**
         * 数据路径
         */
        private String path;

        /**
         * 数据格式
         */
        private String format;

        /**
         * 数据版本
         */
        private String version;
    }
}