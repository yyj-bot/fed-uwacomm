package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * v1.5数据集分片DTO
 * 表示为单个VM分配的数据集分片信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetSlice {

    /**
     * 分配的数据集ID
     */
    private String assignedDatasetId;

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 本地存储路径
     */
    private String localPath;

    /**
     * 数据样本数量
     */
    private int sampleCount;

    /**
     * 数据集开始索引
     */
    private int startIndex;

    /**
     * 数据集结束索引
     */
    private int endIndex;

    /**
     * 分片数据内容（实际数据或引用）
     */
    private Object sliceData;

    /**
     * 分片创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 分片元数据
     */
    private String metadata;

    /**
     * 数据集类型
     */
    private String dataType;

    /**
     * 是否已传输到VM
     */
    private boolean transferred;

    /**
     * 传输状态
     */
    private String transferStatus;

    /**
     * 便利方法：获取分片大小
     */
    public int getSliceSize() {
        return endIndex - startIndex;
    }
}