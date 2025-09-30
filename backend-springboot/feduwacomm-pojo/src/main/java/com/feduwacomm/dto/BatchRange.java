package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批次范围信息 (v1.5.1)
 * 用于描述单个数据传输批次的双重索引范围
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>双重索引系统：本地索引 + 全局索引</li>
 *   <li>本地索引：VM切片内的相对位置（从0开始）</li>
 *   <li>全局索引：原始数据集中的绝对位置</li>
 *   <li>索引关系：globalIndex = localIndex + sliceInfo.startIndex</li>
 * </ul>
 *
 * <p>使用场景：
 * 在DATASET_APPEND_ROWS协议中，描述当前批次数据的索引范围，
 * 便于VM验证数据连续性和完整性。
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRange {

    /**
     * 批次本地起始索引
     * 在VM接收的切片内的起始位置（从0开始）
     *
     * 示例：VM1接收[0-1999]切片，第1个批次（200条），localStartIndex=0
     */
    @JsonProperty("localStartIndex")
    private Integer localStartIndex;

    /**
     * 批次本地结束索引（包含）
     * 在VM接收的切片内的结束位置
     *
     * 示例：VM1接收[0-1999]切片，第1个批次（200条），localEndIndex=199
     */
    @JsonProperty("localEndIndex")
    private Integer localEndIndex;

    /**
     * 批次全局起始索引
     * 在原始数据集中的起始位置（从0开始）
     *
     * 示例：VM1切片startIndex=0，第1个批次，globalStartIndex=0
     */
    @JsonProperty("globalStartIndex")
    private Integer globalStartIndex;

    /**
     * 批次全局结束索引（包含）
     * 在原始数据集中的结束位置
     *
     * 示例：VM1切片startIndex=0，第1个批次（200条），globalEndIndex=199
     */
    @JsonProperty("globalEndIndex")
    private Integer globalEndIndex;

    /**
     * 获取批次大小
     *
     * @return 批次包含的样本数量
     */
    public int getBatchSize() {
        if (localStartIndex == null || localEndIndex == null) {
            return 0;
        }
        return localEndIndex - localStartIndex + 1;
    }

    /**
     * 验证批次范围的完整性
     *
     * @return true if 批次范围有效
     */
    public boolean isValid() {
        if (localStartIndex == null || localEndIndex == null ||
            globalStartIndex == null || globalEndIndex == null) {
            return false;
        }
        if (localStartIndex < 0 || localEndIndex < localStartIndex) {
            return false;
        }
        if (globalStartIndex < 0 || globalEndIndex < globalStartIndex) {
            return false;
        }
        // 验证本地和全局索引的大小一致性
        int localSize = localEndIndex - localStartIndex + 1;
        int globalSize = globalEndIndex - globalStartIndex + 1;
        return localSize == globalSize;
    }

    /**
     * 验证双重索引关系是否正确
     *
     * @param sliceStartIndex 切片的起始索引
     * @return true if 索引关系符合 globalIndex = localIndex + sliceStartIndex
     */
    public boolean verifyIndexRelation(int sliceStartIndex) {
        if (!isValid()) {
            return false;
        }
        return globalStartIndex == (localStartIndex + sliceStartIndex) &&
               globalEndIndex == (localEndIndex + sliceStartIndex);
    }

    /**
     * 检查指定的本地索引是否在当前批次范围内
     *
     * @param localIndex 本地索引
     * @return true if 索引在批次范围内
     */
    public boolean containsLocalIndex(int localIndex) {
        return localIndex >= localStartIndex && localIndex <= localEndIndex;
    }

    /**
     * 检查指定的全局索引是否在当前批次范围内
     *
     * @param globalIndex 全局索引
     * @return true if 索引在批次范围内
     */
    public boolean containsGlobalIndex(int globalIndex) {
        return globalIndex >= globalStartIndex && globalIndex <= globalEndIndex;
    }
}