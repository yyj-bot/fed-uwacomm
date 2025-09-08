package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 训练数据删除响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataDeleteVO {

    private String datasetId;

    private LocalDateTime deletedAt;

    private String deletedBy;

    private Boolean fileDeleted;

    private Boolean metadataPreserved;
}