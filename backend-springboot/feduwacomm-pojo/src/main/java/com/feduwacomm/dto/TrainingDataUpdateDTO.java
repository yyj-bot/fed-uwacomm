package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 训练数据更新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataUpdateDTO {

    private String datasetDescription;

    private List<String> tags;

    private Map<String, Object> metadata;
}