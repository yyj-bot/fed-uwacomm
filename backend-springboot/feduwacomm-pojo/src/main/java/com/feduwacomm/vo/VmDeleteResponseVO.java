package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机删除响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmDeleteResponseVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机名称
     */
    private String name;

    /**
     * 删除时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;

    /**
     * 是否强制删除
     */
    private Boolean force;

    /**
     * 删除操作影响的资源
     */
    @Builder
    @Data
    public static class DeletedResources {
        /**
         * 删除的任务数量
         */
        private Integer tasks;

        /**
         * 删除的模型数量
         */
        private Integer models;

        /**
         * 删除的数据集数量
         */
        private Integer datasets;

        /**
         * 删除的日志数量
         */
        private Integer logs;
    }

    /**
     * 删除的相关资源统计
     */
    private DeletedResources deletedResources;

    /**
     * 删除操作的备注
     */
    private String message;

    /**
     * 警告信息（如果有）
     */
    private String[] warnings;
}