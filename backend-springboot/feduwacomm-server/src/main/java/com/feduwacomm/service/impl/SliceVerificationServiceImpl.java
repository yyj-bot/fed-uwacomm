package com.feduwacomm.service.impl;

import com.feduwacomm.dto.ContinuityCheck;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.dto.SliceVerification;
import com.feduwacomm.dto.VerificationResult;
import com.feduwacomm.enums.VerificationDecision;
import com.feduwacomm.service.SliceVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据切片验证服务实现 (v1.5.1)
 * 实现数据完整性验证的核心逻辑
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 */
@Slf4j
@Service
public class SliceVerificationServiceImpl implements SliceVerificationService {

    @Override
    public VerificationResult verifySliceVerification(String vmId,
                                                       SliceVerification verification,
                                                       SliceInfo expectedSliceInfo) {
        // 参数验证
        if (vmId == null || vmId.isEmpty()) {
            throw new IllegalArgumentException("vmId不能为空");
        }
        if (verification == null) {
            throw new IllegalArgumentException("verification不能为空");
        }
        if (expectedSliceInfo == null) {
            throw new IllegalArgumentException("expectedSliceInfo不能为空");
        }

        log.debug("开始验证VM数据完整性: vmId={}, expectedSamples={}",
                vmId, expectedSliceInfo.getSliceSamples());

        // 1. 验证样本数
        boolean sampleCountValid = isSampleCountValid(verification, expectedSliceInfo);

        // 2. 验证索引范围
        boolean rangeValid = isRangeValid(verification, expectedSliceInfo);

        // 3. 获取缺失索引（VM已经计算好了）
        List<Integer> missingIndices = verification.getMissingIndices();
        if (missingIndices == null) {
            missingIndices = Collections.emptyList();
        }

        // 4. 检测数据间隙（从continuityCheck获取）
        List<VerificationResult.GapRange> gapRanges = extractGapRanges(verification.getContinuityCheck());
        boolean hasGaps = !gapRanges.isEmpty();

        // 5. 计算缺失率
        int missingCount = missingIndices.size();
        double missingRate = calculateMissingRate(missingCount, expectedSliceInfo.getSliceSamples());

        // 6. 判断是否通过验证
        boolean isValid = sampleCountValid && rangeValid && missingCount == 0 && !hasGaps;

        // 7. 构建验证结果
        VerificationResult result = VerificationResult.builder()
                .vmId(vmId)
                .isValid(isValid)
                .isSampleCountValid(sampleCountValid)
                .isRangeValid(rangeValid)
                .hasGaps(hasGaps)
                .missingIndices(missingIndices)
                .gapRanges(gapRanges)
                .missingCount(missingCount)
                .missingRate(missingRate)
                .expectedSamples(expectedSliceInfo.getSliceSamples())
                .actualSamples(verification.getActualSamples())
                .build();

        // 8. 推荐决策
        if (isValid) {
            result.setRecommendedDecision(VerificationDecision.ACCEPT);
            result.setMessage("数据验证通过，无任何问题");
        } else {
            VerificationDecision decision = decideOnVerificationFailure(vmId, result);
            result.setRecommendedDecision(decision);
            result.setMessage(buildFailureMessage(result));
        }

        log.info("VM数据完整性验证完成: vmId={}, isValid={}, missingRate={}, decision={}",
                vmId, isValid, String.format("%.2f%%", missingRate * 100), result.getRecommendedDecision());

        return result;
    }

    @Override
    public List<Integer> findMissingIndices(List<Integer> receivedIndices,
                                             int startIndex,
                                             int endIndex) {
        if (receivedIndices == null) {
            throw new IllegalArgumentException("receivedIndices不能为空");
        }
        if (startIndex > endIndex) {
            throw new IllegalArgumentException("startIndex不能大于endIndex");
        }

        // 转换为Set以提高查找效率
        Set<Integer> receivedSet = new HashSet<>(receivedIndices);
        List<Integer> missing = new ArrayList<>();

        // 遍历预期范围，找出缺失的索引
        for (int i = startIndex; i <= endIndex; i++) {
            if (!receivedSet.contains(i)) {
                missing.add(i);
            }
        }

        log.debug("缺失索引检测完成: 预期范围=[{}, {}], 缺失数量={}",
                startIndex, endIndex, missing.size());

        return missing;
    }

    @Override
    public boolean hasDataGaps(List<Integer> receivedIndices) {
        if (receivedIndices == null || receivedIndices.isEmpty()) {
            return false;
        }

        // 排序
        List<Integer> sorted = new ArrayList<>(receivedIndices);
        Collections.sort(sorted);

        // 检查是否连续
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i) - sorted.get(i - 1) > 1) {
                return true;  // 发现间隙
            }
        }

        return false;  // 连续，无间隙
    }

    @Override
    public List<VerificationResult.GapRange> findGapRanges(List<Integer> receivedIndices) {
        if (receivedIndices == null || receivedIndices.isEmpty()) {
            return Collections.emptyList();
        }

        // 排序
        List<Integer> sorted = new ArrayList<>(receivedIndices);
        Collections.sort(sorted);

        List<VerificationResult.GapRange> gaps = new ArrayList<>();

        // 查找所有间隙
        for (int i = 1; i < sorted.size(); i++) {
            int prev = sorted.get(i - 1);
            int curr = sorted.get(i);

            if (curr - prev > 1) {
                // 发现间隙：[prev+1, curr-1]
                VerificationResult.GapRange gap = VerificationResult.GapRange.builder()
                        .startIndex(prev + 1)
                        .endIndex(curr - 1)
                        .build();
                gaps.add(gap);
            }
        }

        if (!gaps.isEmpty()) {
            log.debug("检测到{}个数据间隙，总间隙大小={}",
                    gaps.size(),
                    gaps.stream().mapToInt(VerificationResult.GapRange::getGapSize).sum());
        }

        return gaps;
    }

    @Override
    public VerificationDecision decideOnVerificationFailure(String vmId,
                                                             VerificationResult verificationResult) {
        if (verificationResult == null) {
            throw new IllegalArgumentException("verificationResult不能为空");
        }

        // 如果验证通过，直接接受
        if (Boolean.TRUE.equals(verificationResult.getIsValid())) {
            return VerificationDecision.ACCEPT;
        }

        // 根据缺失率决策
        double missingRate = verificationResult.getMissingRate();

        VerificationDecision decision;
        if (missingRate < 0.05) {
            // 缺失率 < 5%，可以重传
            decision = VerificationDecision.RETRY_MISSING;
            log.warn("VM数据缺失率较低: vmId={}, missingRate={}, 建议重传缺失数据",
                    vmId, String.format("%.2f%%", missingRate * 100));
        } else if (missingRate < 0.20) {
            // 缺失率 5%-20%，排除该VM
            decision = VerificationDecision.EXCLUDE_VM;
            log.warn("VM数据缺失率较高: vmId={}, missingRate={}, 建议排除该VM",
                    vmId, String.format("%.2f%%", missingRate * 100));
        } else {
            // 缺失率 >= 20%，中止任务
            decision = VerificationDecision.ABORT_TASK;
            log.error("VM数据缺失率过高: vmId={}, missingRate={}, 建议中止任务",
                    vmId, String.format("%.2f%%", missingRate * 100));
        }

        return decision;
    }

    @Override
    public double calculateMissingRate(int missingCount, int expectedSamples) {
        if (expectedSamples <= 0) {
            throw new IllegalArgumentException("expectedSamples必须大于0");
        }
        if (missingCount < 0) {
            throw new IllegalArgumentException("missingCount不能为负数");
        }

        return (double) missingCount / expectedSamples;
    }

    @Override
    public boolean isSampleCountValid(SliceVerification verification, SliceInfo expectedSliceInfo) {
        if (verification == null || expectedSliceInfo == null) {
            return false;
        }

        Integer actualSamples = verification.getActualSamples();
        Integer expectedSamples = expectedSliceInfo.getSliceSamples();

        boolean isValid = actualSamples != null
                && expectedSamples != null
                && actualSamples.equals(expectedSamples);

        if (!isValid) {
            log.warn("样本数验证失败: expected={}, actual={}", expectedSamples, actualSamples);
        }

        return isValid;
    }

    @Override
    public boolean isRangeValid(SliceVerification verification, SliceInfo expectedSliceInfo) {
        if (verification == null || expectedSliceInfo == null) {
            return false;
        }

        Integer actualStart = verification.getActualStartIndex();
        Integer actualEnd = verification.getActualEndIndex();
        Integer expectedStart = expectedSliceInfo.getStartIndex();
        Integer expectedEnd = expectedSliceInfo.getEndIndex();

        boolean isValid = actualStart != null
                && actualEnd != null
                && expectedStart != null
                && expectedEnd != null
                && actualStart.equals(expectedStart)
                && actualEnd.equals(expectedEnd);

        if (!isValid) {
            log.warn("索引范围验证失败: expected=[{}, {}], actual=[{}, {}]",
                    expectedStart, expectedEnd, actualStart, actualEnd);
        }

        return isValid;
    }

    /**
     * 从ContinuityCheck提取GapRange列表
     */
    private List<VerificationResult.GapRange> extractGapRanges(ContinuityCheck continuityCheck) {
        if (continuityCheck == null || continuityCheck.getGapRanges() == null) {
            return Collections.emptyList();
        }

        // 转换ContinuityCheck.GapRange为VerificationResult.GapRange
        return continuityCheck.getGapRanges().stream()
                .map(gap -> VerificationResult.GapRange.builder()
                        .startIndex(gap.getStartIndex())
                        .endIndex(gap.getEndIndex())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 构建验证失败消息
     */
    private String buildFailureMessage(VerificationResult result) {
        StringBuilder message = new StringBuilder("数据验证失败: ");

        if (Boolean.FALSE.equals(result.getIsSampleCountValid())) {
            message.append(String.format("样本数不匹配(期望%d, 实际%d); ",
                    result.getExpectedSamples(), result.getActualSamples()));
        }

        if (Boolean.FALSE.equals(result.getIsRangeValid())) {
            message.append("索引范围不匹配; ");
        }

        if (result.getMissingCount() > 0) {
            message.append(String.format("缺失%d个样本(%.2f%%); ",
                    result.getMissingCount(), result.getMissingRate() * 100));
        }

        if (Boolean.TRUE.equals(result.getHasGaps())) {
            message.append(String.format("存在%d个数据间隙; ", result.getGapRanges().size()));
        }

        return message.toString();
    }
}