package com.feduwacomm.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 模型上传事件
 * 当客户端上传本地模型时触发，用于检查是否需要开始聚合
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
public class ModelUploadEvent extends ApplicationEvent {

    private final String taskId;
    private final Integer roundNumber;
    private final String vmId;
    private final LocalDateTime uploadTime;
    private final Long modelSize;
    private final Double accuracy;
    private final Double loss;

    public ModelUploadEvent(Object source, String taskId, Integer roundNumber, String vmId) {
        super(source);
        this.taskId = taskId;
        this.roundNumber = roundNumber;
        this.vmId = vmId;
        this.uploadTime = LocalDateTime.now();
        this.modelSize = null;
        this.accuracy = null;
        this.loss = null;
    }

    public ModelUploadEvent(Object source, String taskId, Integer roundNumber, String vmId,
                           Long modelSize, Double accuracy, Double loss) {
        super(source);
        this.taskId = taskId;
        this.roundNumber = roundNumber;
        this.vmId = vmId;
        this.uploadTime = LocalDateTime.now();
        this.modelSize = modelSize;
        this.accuracy = accuracy;
        this.loss = loss;
    }

    @Override
    public String toString() {
        return String.format("ModelUploadEvent{taskId='%s', round=%d, vmId='%s', time=%s}", 
                taskId, roundNumber, vmId, uploadTime);
    }
}