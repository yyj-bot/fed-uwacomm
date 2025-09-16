package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练数据统计查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataStatisticsDTO {

    private String dataType;

    private String startDate;

    private String endDate;
}