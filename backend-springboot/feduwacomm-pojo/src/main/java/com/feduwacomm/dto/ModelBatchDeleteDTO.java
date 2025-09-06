package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量模型删除请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelBatchDeleteDTO {

    /**
     * 模型ID列表，必填
     */
    @NotNull(message = "模型ID列表不能为空")
    @Size(min = 1, message = "至少需要删除一个模型")
    private List<String> modelIds;

    /**
     * 强制删除，可选
     */
    private Boolean force = false;

    /**
     * 是否删除文件，可选
     */
    private Boolean deleteFile = true;
}