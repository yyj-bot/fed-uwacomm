package com.feduwacomm.entity;

import com.feduwacomm.enums.DataType;
import com.feduwacomm.enums.DataStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 训练数据实体类
 * 对应数据库表 training_dataset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingData {

    private String id;

    private String vmId;

    private String name;

    private String description;

    private DataType dataType;

    private DataStatus status;

    private String filePath;

    private Long fileSize;

    private String fileFormat;

    private List<String> tags;

    private Map<String, Object> metadata;

    private LocalDateTime uploadTime;

    private String uploadedBy;

    private LocalDateTime updateTime;

    private String updatedBy;

    private Boolean isValid;

    private LocalDateTime validationTime;

    private Map<String, Object> validationResult;

    private Boolean isProcessed;

    private LocalDateTime processTime;

    private Map<String, Object> processResult;

    private Integer progress;

    private String errorMessage;
}