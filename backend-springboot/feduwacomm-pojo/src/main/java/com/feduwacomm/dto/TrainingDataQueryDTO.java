package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 训练数据查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataQueryDTO {

    private Integer page = 1;

    private Integer size = 20;

    private String vmId;

    private String dataType;

    private String status;

    private String keyword;

    private String startDate;

    private String endDate;

    private List<String> tags;
}