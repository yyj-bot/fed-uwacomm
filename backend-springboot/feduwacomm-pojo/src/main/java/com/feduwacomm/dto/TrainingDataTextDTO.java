package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 训练数据文本信息上传请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataTextDTO {

    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    private String vmId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String datasetDescription;

    private List<String> tags;

    private Map<String, Object> metadata;

    /**
     * 获取文本数据内容
     * 兼容性方法，映射到content字段
     */
    public String getTextData() {
        return this.content;
    }

    /**
     * 设置文本数据内容
     * 兼容性方法，映射到content字段
     */
    public void setTextData(String textData) {
        this.content = textData;
    }
}