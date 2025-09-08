package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 训练数据更新响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataUpdateVO {

    private String datasetId;

    private LocalDateTime updatedAt;

    private String updatedBy;
}