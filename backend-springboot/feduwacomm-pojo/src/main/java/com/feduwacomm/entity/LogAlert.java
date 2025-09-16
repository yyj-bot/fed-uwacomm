package com.feduwacomm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAlert {
    
    private String alertId;
    private String name;
    private String type;
    private String condition;
    private String status;
    private String severity;
    private String message;
    private Integer triggerCount;
    private LocalDateTime lastTriggered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}