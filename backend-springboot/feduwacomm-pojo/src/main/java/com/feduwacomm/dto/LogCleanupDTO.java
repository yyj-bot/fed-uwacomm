package com.feduwacomm.dto;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogCleanupStrategy;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogCleanupDTO {

    @NotNull(message = "清理策略不能为空")
    private LogCleanupStrategy strategy;
    
    @Min(value = 1, message = "保留天数必须大于0")
    private Integer retentionDays;
    
    private LogLevel level;
    
    @Min(value = 1, message = "最大大小必须大于0")
    private Integer maxSizeGB;
    
    private LogCategory category;
    
    private String vmId;
    
    private String taskId;
    
    @Builder.Default
    private Boolean dryRun = false;
}