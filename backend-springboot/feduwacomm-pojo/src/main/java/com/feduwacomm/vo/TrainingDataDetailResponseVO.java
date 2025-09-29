package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 训练数据详情响应VO
 * 专门用于测试场景的详情响应类
 *
 * @author FedUWAComm Team
 * @version 1.4.0
 * @since 2025-09-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataDetailResponseVO {

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
     * 更新时间
     */
    private LocalDateTime updateTime;

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

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 数据维度
     */
    private String dimensions;

    /**
     * 样本数量
     */
    private Long sampleCount;

    /**
     * 特征数量
     */
    private Integer featureCount;

    /**
     * 数据标签
     */
    private String labels;

    /**
     * 预处理信息
     */
    private String preprocessInfo;

    /**
     * 数据统计信息
     */
    private Map<String, Object> statistics;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 访问权限
     */
    private String permissions;

    /**
     * 下载次数
     */
    private Long downloadCount;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 验证状态
     */
    private String validationStatus;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
}