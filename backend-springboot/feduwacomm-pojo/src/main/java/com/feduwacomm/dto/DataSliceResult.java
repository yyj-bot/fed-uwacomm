package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据切片结果 (v1.5.1)
 * 表示数据切分服务为单个虚拟机生成的切片结果
 *
 * <p>使用场景：
 * DataSlicingService.sliceDataset() 方法的返回类型，
 * 将vmId和对应的SliceInfo绑定在一起，便于后续数据分发。
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSliceResult {

    /**
     * 虚拟机ID
     * 该切片分配的目标虚拟机
     */
    private String vmId;

    /**
     * 切片元数据信息
     * 描述该VM应接收的数据范围和上下文
     */
    private SliceInfo sliceInfo;

    /**
     * 分配的数据集ID（可选）
     * 后端生成的assignedDatasetId，用于唯一标识该VM的数据集
     */
    private String assignedDatasetId;
}