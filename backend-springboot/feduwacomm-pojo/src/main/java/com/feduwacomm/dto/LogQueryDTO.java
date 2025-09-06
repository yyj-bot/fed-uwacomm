package com.feduwacomm.dto;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryDTO {

    private LogLevel level;
    
    private LogCategory category;
    
    private String vmId;
    
    private String taskId;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String keyword;
    
    @Builder.Default
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;
    
    @Builder.Default
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;
    
    @Builder.Default
    private String sort = "createdAt";
    
    @Builder.Default
    private String order = "desc";
    
    @Min(value = 1, message = "尾部行数必须大于0")
    @Max(value = 1000, message = "尾部行数不能超过1000")
    private Integer tail;
}