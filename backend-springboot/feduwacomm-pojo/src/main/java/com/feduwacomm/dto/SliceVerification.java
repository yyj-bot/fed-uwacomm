package com.feduwacomm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据切片完整性验证 (v1.5.1)
 * 用于虚拟机向后端报告数据接收的完整性状态
 *
 * <p>v1.5.1核心特性：
 * <ul>
 *   <li>自动验证接收数据与预期切片的一致性</li>
 *   <li>精确检测缺失的数据索引</li>
 *   <li>检测数据连续性和间隙</li>
 *   <li>支持后端异常处理决策（重传/排除/中止）</li>
 * </ul>
 *
 * <p>使用场景：
 * 在DATASET_COMPLETE协议中，虚拟机完成数据接收后，
 * 生成SliceVerification报告并发送给后端，用于验证数据完整性。
 *
 * <p>验证流程：
 * 1. VM接收DATASET_CREATE时保存expectedStartIndex/endIndex/samples
 * 2. VM在接收数据过程中跟踪actualStartIndex/endIndex/samples
 * 3. VM检测missingIndices和continuityCheck
 * 4. VM生成SliceVerification并发送DATASET_COMPLETE
 * 5. 后端根据isComplete和missingIndices决策处理
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliceVerification {

    /**
     * 预期起始索引（全局索引）
     * 从DATASET_CREATE的sliceInfo.startIndex获取
     *
     * 示例：VM1分配[0-1999]，则expectedStartIndex=0
     */
    @JsonProperty("expectedStartIndex")
    private Integer expectedStartIndex;

    /**
     * 预期结束索引（全局索引）
     * 从DATASET_CREATE的sliceInfo.endIndex获取
     *
     * 示例：VM1分配[0-1999]，则expectedEndIndex=1999
     */
    @JsonProperty("expectedEndIndex")
    private Integer expectedEndIndex;

    /**
     * 预期样本数量
     * 从DATASET_CREATE的sliceInfo.sliceSamples获取
     *
     * 示例：VM1分配[0-1999]，则expectedSamples=2000
     */
    @JsonProperty("expectedSamples")
    private Integer expectedSamples;

    /**
     * 实际接收的起始索引（全局索引）
     * VM实际接收到的数据中的最小索引
     *
     * 示例：VM1实际接收到索引[0-1999]，则actualStartIndex=0
     *       如果部分缺失，实际接收[10-1999]，则actualStartIndex=10
     */
    @JsonProperty("actualStartIndex")
    private Integer actualStartIndex;

    /**
     * 实际接收的结束索引（全局索引）
     * VM实际接收到的数据中的最大索引
     *
     * 示例：VM1实际接收到索引[0-1999]，则actualEndIndex=1999
     *       如果部分缺失，实际接收[0-1990]，则actualEndIndex=1990
     */
    @JsonProperty("actualEndIndex")
    private Integer actualEndIndex;

    /**
     * 实际接收的样本数量
     * VM实际接收到的数据条数
     *
     * 示例：VM1预期2000条，实际接收1950条，则actualSamples=1950
     */
    @JsonProperty("actualSamples")
    private Integer actualSamples;

    /**
     * 数据是否完整
     * true: 所有预期数据都已接收，无缺失，无间隙
     * false: 存在缺失数据或数据间隙
     *
     * 判定条件：
     * isComplete = (actualSamples == expectedSamples) &&
     *              (actualStartIndex == expectedStartIndex) &&
     *              (actualEndIndex == expectedEndIndex) &&
     *              (missingIndices.isEmpty()) &&
     *              (!continuityCheck.hasGaps)
     */
    @JsonProperty("isComplete")
    private Boolean isComplete;

    /**
     * 缺失的数据索引列表（全局索引）
     * 包含所有预期但未接收到的数据索引
     *
     * 示例：VM1预期[0-1999]，实际接收[0-99, 150-1999]
     *       则missingIndices=[100, 101, ..., 149]
     *
     * 如果isComplete=true，则此列表为空
     */
    @JsonProperty("missingIndices")
    private List<Integer> missingIndices;

    /**
     * 数据连续性检查结果
     * 描述接收数据中是否存在间隙
     *
     * 示例：VM1预期[0-1999]，实际接收[0-99, 150-1999]
     *       则continuityCheck = {hasGaps: true, gapRanges: [{startIndex: 100, endIndex: 149}]}
     */
    @JsonProperty("continuityCheck")
    private ContinuityCheck continuityCheck;

    /**
     * 获取缺失数据数量
     *
     * @return 缺失的样本数
     */
    public int getMissingCount() {
        return (missingIndices == null) ? 0 : missingIndices.size();
    }

    /**
     * 获取缺失率
     *
     * @return 缺失样本占预期样本的百分比 (0.0 - 1.0)
     */
    public double getMissingRate() {
        if (expectedSamples == null || expectedSamples == 0) {
            return 0.0;
        }
        return (double) getMissingCount() / expectedSamples;
    }

    /**
     * 验证完整性报告的一致性
     *
     * @return true if 验证报告有效
     */
    public boolean isValid() {
        // 必需字段检查
        if (expectedStartIndex == null || expectedEndIndex == null || expectedSamples == null ||
            actualStartIndex == null || actualEndIndex == null || actualSamples == null ||
            isComplete == null) {
            return false;
        }

        // 预期范围合理性检查
        if (expectedStartIndex < 0 || expectedEndIndex < expectedStartIndex) {
            return false;
        }
        if (expectedSamples != (expectedEndIndex - expectedStartIndex + 1)) {
            return false;
        }

        // isComplete标志一致性检查
        if (isComplete) {
            // 如果声明完整，则不应有缺失和间隙
            boolean hasNoMissing = (missingIndices == null || missingIndices.isEmpty());
            boolean hasNoGaps = (continuityCheck == null ||
                                 !continuityCheck.getHasGaps() ||
                                 continuityCheck.getGapRanges() == null ||
                                 continuityCheck.getGapRanges().isEmpty());
            boolean samplesMatch = actualSamples.equals(expectedSamples);
            boolean rangeMatch = actualStartIndex.equals(expectedStartIndex) &&
                                 actualEndIndex.equals(expectedEndIndex);

            return hasNoMissing && hasNoGaps && samplesMatch && rangeMatch;
        }

        return true;
    }

    /**
     * 判断是否需要重传数据
     * 基于缺失率判断
     *
     * @param threshold 缺失率阈值 (0.0 - 1.0)，默认0.05 (5%)
     * @return true if 缺失率低于阈值，建议重传
     */
    public boolean shouldRetry(double threshold) {
        return !isComplete && getMissingRate() < threshold;
    }

    /**
     * 判断是否应排除该虚拟机
     * 基于缺失率判断
     *
     * @param minThreshold 最小缺失率阈值，默认0.05 (5%)
     * @param maxThreshold 最大缺失率阈值，默认0.20 (20%)
     * @return true if 缺失率在[minThreshold, maxThreshold)之间，建议排除VM
     */
    public boolean shouldExcludeVm(double minThreshold, double maxThreshold) {
        double rate = getMissingRate();
        return !isComplete && rate >= minThreshold && rate < maxThreshold;
    }

    /**
     * 判断是否应中止任务
     * 基于缺失率判断
     *
     * @param threshold 缺失率阈值，默认0.20 (20%)
     * @return true if 缺失率超过阈值，建议中止任务
     */
    public boolean shouldAbortTask(double threshold) {
        return !isComplete && getMissingRate() >= threshold;
    }
}