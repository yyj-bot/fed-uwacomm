package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据集切片元数据 (v1.5.1)
 * 用于精确描述分配给虚拟机的数据集切片信息
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>精确的切片范围定义（startIndex - endIndex）</li>
 *   <li>完整的切片上下文（当前切片在总切片中的位置）</li>
 *   <li>支持IID和Non-IID分配策略</li>
 *   <li>向后兼容v1.5（可选字段）</li>
 * </ul>
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliceInfo {

    /**
     * 切片起始索引（全局索引）
     * 在原始数据集中的起始位置（从0开始）
     *
     * 示例：对于10000样本，VM1分配[0-1999]，则startIndex=0
     */
    @JsonProperty("startIndex")
    private Integer startIndex;

    /**
     * 切片结束索引（全局索引，包含）
     * 在原始数据集中的结束位置
     *
     * 示例：对于10000样本，VM1分配[0-1999]，则endIndex=1999
     */
    @JsonProperty("endIndex")
    private Integer endIndex;

    /**
     * 切片样本数量
     * 该切片包含的样本总数
     *
     * 计算公式：sliceSamples = endIndex - startIndex + 1
     * 示例：[0-1999] 包含2000个样本
     */
    @JsonProperty("sliceSamples")
    private Integer sliceSamples;

    /**
     * 原始数据集总样本数
     * 用于验证切片完整性
     *
     * 示例：10000样本分5个VM，totalSamples=10000
     */
    @JsonProperty("totalSamples")
    private Integer totalSamples;

    /**
     * 切片编号（从1开始）
     * 当前切片在所有切片中的序号
     *
     * 示例：5个VM，当前VM分配第1个切片，则sliceIndex=1
     */
    @JsonProperty("sliceIndex")
    private Integer sliceIndex;

    /**
     * 总切片数量
     * 原始数据集被切分的总份数
     *
     * 示例：5个VM参与，则totalSlices=5
     */
    @JsonProperty("totalSlices")
    private Integer totalSlices;

    /**
     * 分配策略
     * 数据切片采用的策略类型
     *
     * 可选值：
     * - "IID": 独立同分布，数据均匀随机分配
     * - "NON_IID": 非独立同分布，按特定规则聚类分配
     */
    @JsonProperty("allocationStrategy")
    private String allocationStrategy;

    /**
     * 验证切片信息的完整性
     *
     * @return true if 切片信息有效
     */
    public boolean isValid() {
        if (startIndex == null || endIndex == null || sliceSamples == null) {
            return false;
        }
        if (startIndex < 0 || endIndex < startIndex) {
            return false;
        }
        if (sliceSamples != (endIndex - startIndex + 1)) {
            return false;
        }
        if (totalSamples != null && endIndex >= totalSamples) {
            return false;
        }
        if (sliceIndex != null && totalSlices != null) {
            if (sliceIndex < 1 || sliceIndex > totalSlices) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查指定的全局索引是否在当前切片范围内
     *
     * @param globalIndex 全局索引
     * @return true if 索引在切片范围内
     */
    public boolean containsGlobalIndex(int globalIndex) {
        return globalIndex >= startIndex && globalIndex <= endIndex;
    }

    /**
     * 将全局索引转换为本地索引
     *
     * @param globalIndex 全局索引
     * @return 本地索引（从0开始），如果全局索引不在范围内则返回-1
     */
    public int toLocalIndex(int globalIndex) {
        if (!containsGlobalIndex(globalIndex)) {
            return -1;
        }
        return globalIndex - startIndex;
    }

    /**
     * 将本地索引转换为全局索引
     *
     * @param localIndex 本地索引（从0开始）
     * @return 全局索引
     */
    public int toGlobalIndex(int localIndex) {
        return startIndex + localIndex;
    }
}