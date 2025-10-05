package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据连续性检查结果 (v1.5.1)
 * 用于验证虚拟机接收的数据是否存在间隙（不连续的索引）
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>自动检测数据索引的连续性</li>
 *   <li>精确定位所有间隙的位置和范围</li>
 *   <li>支持数据完整性验证和异常处理决策</li>
 * </ul>
 *
 * <p>使用场景：
 * 在DATASET_COMPLETE协议的sliceVerification中，
 * 描述数据接收的连续性状态，帮助后端判断是否需要重传。
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContinuityCheck {

    /**
     * 是否存在数据间隙
     * true: 存在不连续的索引区间
     * false: 所有接收的数据索引连续
     *
     * 示例：
     * - 接收[0-99, 100-199] → hasGaps=false（连续）
     * - 接收[0-99, 150-199] → hasGaps=true（缺失[100-149]）
     */
    @JsonProperty("hasGaps")
    private Boolean hasGaps;

    /**
     * 间隙范围列表
     * 包含所有检测到的数据间隙的详细信息
     *
     * 如果hasGaps=false，则此列表为空
     * 如果hasGaps=true，则包含一个或多个GapRange对象
     *
     * 示例：
     * 接收索引[0-99, 150-199, 300-399]，则gapRanges包含：
     * - GapRange{startIndex: 100, endIndex: 149}
     * - GapRange{startIndex: 200, endIndex: 299}
     */
    @JsonProperty("gapRanges")
    private List<GapRange> gapRanges;

    /**
     * 获取总间隙数量
     *
     * @return 间隙的个数
     */
    public int getGapCount() {
        return (gapRanges == null) ? 0 : gapRanges.size();
    }

    /**
     * 获取缺失样本总数
     *
     * @return 所有间隙包含的样本总数
     */
    public int getTotalMissingSamples() {
        if (gapRanges == null || gapRanges.isEmpty()) {
            return 0;
        }
        return gapRanges.stream()
                .mapToInt(GapRange::getGapSize)
                .sum();
    }

    /**
     * 验证连续性检查结果的一致性
     *
     * @return true if 检查结果有效
     */
    public boolean isValid() {
        if (hasGaps == null) {
            return false;
        }
        if (hasGaps) {
            // 如果声明有间隙，则gapRanges不能为空
            return gapRanges != null && !gapRanges.isEmpty();
        } else {
            // 如果声明无间隙，则gapRanges必须为空
            return gapRanges == null || gapRanges.isEmpty();
        }
    }
}