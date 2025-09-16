package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 训练数据删除请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataDeleteDTO {

    private String reason;

    private Boolean deleteFile = true;

    private Boolean deleteMetadata = false;
}