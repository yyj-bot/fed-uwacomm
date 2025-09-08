package com.feduwacomm.vo;

import com.feduwacomm.enums.LogCategory;
import com.feduwacomm.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogListVO {

    private String logId;
    private LogLevel level;
    private LogCategory category;
    private String vmId;
    private String taskId;
    private String message;
    private Map<String, Object> details;
    private LocalDateTime createdAt;
}