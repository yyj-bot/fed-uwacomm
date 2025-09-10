package com.feduwacomm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 虚拟机更新响应VO
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
public class VmUpdateResponseVO {

    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 虚拟机名称
     */
    private String name;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 更新的字段列表
     */
    private String[] updatedFields;

    /**
     * 是否需要重启虚拟机以应用更改
     */
    private Boolean requiresRestart;

    /**
     * 更新操作的备注
     */
    private String message;
}