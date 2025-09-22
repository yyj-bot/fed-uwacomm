package com.feduwacomm.entity;

import com.feduwacomm.enums.ModelType;
import com.feduwacomm.enums.GenerationMethod;
import com.feduwacomm.enums.InitialModelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 初始模型实体类
 * 对应数据库表: initial_models
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitialModel {
    
    /**
     * 初始模型唯一标识(32位UUID)
     */
    private String id;
    
    /**
     * 关联任务ID(32位UUID)
     */
    private String taskId;
    
    /**
     * 模型类型
     */
    private ModelType modelType;

    /**
     * 生成方式: RANDOM-随机生成, CUSTOM_UPLOAD-自定义上传
     */
    private GenerationMethod generationMethod;
    
    /**
     * 模型大小(字节)
     */
    private Long modelSize;
    
    /**
     * 架构参数(JSON格式)
     */
    private String architectureParams;
    
    /**
     * 文件存储路径
     */
    private String filePath;
    
    /**
     * 文件校验和
     */
    private String checksum;
    
    /**
     * 状态: GENERATING-生成中, READY-就绪, DISTRIBUTED-已分发, FAILED-失败
     */
    private InitialModelStatus status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建者ID(32位UUID)
     */
    private String createdBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}