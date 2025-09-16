package com.feduwacomm.dto;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogExportFormat;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogExportDTO {

    private LogLevel level;
    
    private LogCategory category;
    
    private String vmId;
    
    private String taskId;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String keyword;
    
    @Builder.Default
    private LogExportFormat format = LogExportFormat.CSV;
    
    @Builder.Default
    private Boolean includeDetails = true;
}