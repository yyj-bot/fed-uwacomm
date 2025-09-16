package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 训练数据明细实体类
 * 对应数据库表：training_dataset_row
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataRow {

    /**
     * 数据行唯一标识(32位UUID)
     */
    private String id;

    /**
     * 所属数据集ID
     */
    private String datasetId;

    /**
     * 原始CSV行数据（JSON格式）
     */
    private Map<String, Object> rowData;

    /**
     * 插入时间
     */
    private LocalDateTime createdAt;
}