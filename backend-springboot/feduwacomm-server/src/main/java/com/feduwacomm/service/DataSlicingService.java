package com.feduwacomm.service;

import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.enums.AllocationStrategy;

import java.util.List;

/**
 * 数据切分服务接口 (v1.5.1)
 * 负责将原始数据集按照指定策略切分并分配给虚拟机
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>支持IID（独立同分布）和NON_IID（非独立同分布）策略</li>
 *   <li>精确的切片元数据生成（SliceInfo）</li>
 *   <li>切片完整性验证（无重叠、无遗漏）</li>
 *   <li>灵活的切分算法扩展机制</li>
 * </ul>
 *
 * <p>使用场景：
 * 在联邦学习任务启动时，DataSlicingService将原始数据集切分成多个切片，
 * 每个切片分配给一个虚拟机，确保数据分配的准确性和完整性。
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
public interface DataSlicingService {

    /**
     * 切分数据集并分配给虚拟机
     * 根据指定的分配策略，将数据集切分成多个切片
     *
     * @param datasetId 原始数据集ID
     * @param vmIds     虚拟机ID列表（切分份数 = vmIds.size()）
     * @param strategy  分配策略（IID 或 NON_IID）
     * @return 数据切片结果列表，每个结果包含vmId和对应的SliceInfo
     * @throws IllegalArgumentException if 参数无效（如vmIds为空，datasetId不存在）
     * @throws DataSlicingException if 切分过程中发生错误
     */
    List<DataSliceResult> sliceDataset(String datasetId, List<String> vmIds, AllocationStrategy strategy);

    /**
     * 生成切片元数据信息
     * 根据切片索引和范围生成完整的SliceInfo对象
     *
     * @param sliceIndex   切片编号（从1开始）
     * @param startIndex   切片起始索引（全局索引，从0开始）
     * @param endIndex     切片结束索引（全局索引，包含）
     * @param totalSamples 原始数据集总样本数
     * @param totalSlices  总切片数（虚拟机数量）
     * @param strategy     分配策略
     * @return SliceInfo对象
     */
    SliceInfo generateSliceInfo(int sliceIndex, int startIndex, int endIndex,
                                 int totalSamples, int totalSlices,
                                 AllocationStrategy strategy);

    /**
     * 验证切片覆盖完整性
     * 检查所有切片是否完整覆盖原始数据集，无重叠、无遗漏
     *
     * @param sliceResults 切片结果列表
     * @param totalSamples 原始数据集总样本数
     * @throws DataSlicingException if 切片验证失败（存在重叠或遗漏）
     */
    void validateSlicesCoverage(List<DataSliceResult> sliceResults, int totalSamples);

    /**
     * 获取数据集的样本总数
     * 查询指定数据集的样本数量，用于切分计算
     *
     * @param datasetId 数据集ID
     * @return 样本总数
     * @throws IllegalArgumentException if 数据集不存在
     */
    int getDatasetSize(String datasetId);

    /**
     * 数据切分异常
     * 在切分过程中发生错误时抛出
     */
    class DataSlicingException extends RuntimeException {
        public DataSlicingException(String message) {
            super(message);
        }

        public DataSlicingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}