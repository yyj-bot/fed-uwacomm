package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多轮联邦学习数据集绑定快照
 * 记录单个VM在指定轮次使用的 assignedDatasetId 及其元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundDatasetBinding {

    /**
     * 虚拟机唯一标识
     */
    private String vmId;

    /**
     * 后端分配的数据集ID
     */
    private String assignedDatasetId;

    /**
     * 数据集当前状态
     */
    private String datasetStatus;

    /**
     * VM 端数据集本地路径
     */
    private String localPath;
}

