package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 训练数据导出响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataExportVO {

    private String taskId;

    private String status;

    private String format;

    private LocalDateTime startedAt;

    private Integer estimatedTime;

    private String downloadUrl;
}