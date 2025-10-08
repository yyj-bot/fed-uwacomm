package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据间隙范围 (v1.5.1)
 * 用于描述数据接收过程中检测到的缺失数据范围
 *
 * <p>使用场景：
 * 在数据完整性验证中，如果检测到数据不连续，
 * 使用GapRange描述缺失的索引区间。
 *
 * <p>示例：
 * 如果VM接收了索引[0-99, 150-299]，缺失[100-149]，
 * 则会生成一个GapRange{startIndex: 100, endIndex: 149}
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapRange {

    /**
     * 间隙起始索引（全局索引）
     * 缺失数据的起始位置
     */
    @JsonProperty("startIndex")
    private Integer startIndex;

    /**
     * 间隙结束索引（全局索引，包含）
     * 缺失数据的结束位置
     */
    @JsonProperty("endIndex")
    private Integer endIndex;

    /**
     * 获取间隙大小
     *
     * @return 缺失的样本数量
     */
    public int getGapSize() {
        if (startIndex == null || endIndex == null) {
            return 0;
        }
        return endIndex - startIndex + 1;
    }

    /**
     * 验证间隙范围的有效性
     *
     * @return true if 间隙范围有效
     */
    public boolean isValid() {
        if (startIndex == null || endIndex == null) {
            return false;
        }
        return startIndex >= 0 && endIndex >= startIndex;
    }

    @Override
    public String toString() {
        return "[" + startIndex + "-" + endIndex + "]";
    }
}