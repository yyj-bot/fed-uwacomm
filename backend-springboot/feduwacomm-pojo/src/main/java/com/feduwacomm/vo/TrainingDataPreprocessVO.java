package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 训练数据预处理响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataPreprocessVO {

    private String datasetId;

    private String taskId;

    private String status;

    private List<String> methods;

    private LocalDateTime startedAt;

    private Integer estimatedTime;
}