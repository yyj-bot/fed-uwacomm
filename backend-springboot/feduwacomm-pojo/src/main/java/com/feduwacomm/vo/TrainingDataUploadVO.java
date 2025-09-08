package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 训练数据上传响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataUploadVO {

    private String datasetId;

    private String datasetDescription;

    private String datasetType;

    private String vmId;

    private String status;

    private LocalDateTime uploadTime;

    private String uploadedBy;

    private Integer progress;
}