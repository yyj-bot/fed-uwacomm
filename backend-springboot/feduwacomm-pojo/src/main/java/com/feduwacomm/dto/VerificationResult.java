package com.feduwacomm.dto;

import com.feduwacomm.enums.VerificationDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证结果 (v1.5.1)
 * 封装数据完整性验证的结果信息
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {
    /**
     * 虚拟机ID
     */
    private String vmId;

    /**
     * 是否通过验证
     */
    private Boolean isValid;

    /**
     * 样本数是否正确
     */
    private Boolean isSampleCountValid;

    /**
     * 索引范围是否正确
     */
    private Boolean isRangeValid;

    /**
     * 是否存在数据间隙
     */
    private Boolean hasGaps;

    /**
     * 缺失的索引列表
     */
    private List<Integer> missingIndices;

    /**
     * 数据间隙范围列表
     */
    private List<GapRange> gapRanges;

    /**
     * 缺失样本数
     */
    private Integer missingCount;

    /**
     * 缺失率（0.0-1.0）
     */
    private Double missingRate;

    /**
     * 期望样本数
     */
    private Integer expectedSamples;

    /**
     * 实际样本数
     */
    private Integer actualSamples;

    /**
     * 推荐的决策
     */
    private VerificationDecision recommendedDecision;

    /**
     * 验证消息
     */
    private String message;

    /**
     * 数据间隙范围
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapRange {
        /**
         * 间隙起始索引（包含）
         */
        private Integer startIndex;

        /**
         * 间隙结束索引（包含）
         */
        private Integer endIndex;

        /**
         * 间隙大小
         */
        public int getGapSize() {
            return endIndex - startIndex + 1;
        }
    }

    /**
     * 验证是否完全通过（无任何问题）
     */
    public boolean isPerfect() {
        return Boolean.TRUE.equals(isValid)
                && Boolean.TRUE.equals(isSampleCountValid)
                && Boolean.TRUE.equals(isRangeValid)
                && Boolean.FALSE.equals(hasGaps)
                && (missingIndices == null || missingIndices.isEmpty());
    }

    /**
     * 计算总的间隙大小
     */
    public int getTotalGapSize() {
        if (gapRanges == null || gapRanges.isEmpty()) {
            return 0;
        }
        return gapRanges.stream()
                .mapToInt(GapRange::getGapSize)
                .sum();
    }
}