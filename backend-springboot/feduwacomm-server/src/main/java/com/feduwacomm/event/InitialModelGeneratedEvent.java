package com.feduwacomm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 初始模型生成完成事件
 * 当初始模型生成完成（包括随机生成和自定义上传）时发布此事件
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Getter
public class InitialModelGeneratedEvent extends ApplicationEvent {
    
    /**
     * 模型ID
     */
    private final String modelId;
    
    /**
     * 任务ID
     */
    private final String taskId;
    
    /**
     * 编排ID（如果有）
     */
    private final String orchestrationId;
    
    /**
     * 模型类型
     */
    private final String modelType;
    
    /**
     * 生成方式
     */
    private final String generationMethod;
    
    /**
     * 模型大小（字节）
     */
    private final Long modelSize;
    
    /**
     * 文件路径
     */
    private final String filePath;
    
    /**
     * 校验和
     */
    private final String checksum;
    
    /**
     * 架构参数
     */
    private final Map<String, Object> architectureParams;
    
    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;
    
    /**
     * 创建者
     */
    private final String createdBy;
    
    public InitialModelGeneratedEvent(Object source, String modelId, String taskId, 
                                    String orchestrationId, String modelType, String generationMethod, 
                                    Long modelSize, String filePath, String checksum,
                                    Map<String, Object> architectureParams, String createdBy) {
        super(source);
        this.modelId = modelId;
        this.taskId = taskId;
        this.orchestrationId = orchestrationId;
        this.modelType = modelType;
        this.generationMethod = generationMethod;
        this.modelSize = modelSize;
        this.filePath = filePath;
        this.checksum = checksum;
        this.architectureParams = architectureParams;
        this.createdBy = createdBy;
        this.eventTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return String.format("InitialModelGeneratedEvent{modelId='%s', taskId='%s', modelType='%s', generationMethod='%s', eventTime=%s}", 
                modelId, taskId, modelType, generationMethod, eventTime);
    }
}