package com.feduwacomm.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联邦学习任务创建事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FederatedTaskCreatedEvent {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 创建者
     */
    private String createdBy;
    
    /**
     * 创建时间戳
     */
    private Long timestamp;
    
    public FederatedTaskCreatedEvent(String taskId, String taskName, String createdBy) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.createdBy = createdBy;
        this.timestamp = System.currentTimeMillis();
    }
}