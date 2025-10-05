package com.feduwacomm.service;

import com.feduwacomm.dto.ContinuityCheck;
import com.feduwacomm.dto.GapRange;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.dto.SliceVerification;
import com.feduwacomm.dto.VerificationResult;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.enums.VerificationDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SliceVerificationService单元测试 (v1.5.1)
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-09-30
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SliceVerificationService单元测试")
class SliceVerificationServiceTest {

    @Autowired
    private SliceVerificationService sliceVerificationService;

    // ========== 1. 完整性验证测试 ==========

    @Test
    @DisplayName("测试完整验证 - 数据完全正确")
    void testVerifySliceVerification_Perfect() {
        // Given: VM1完全正确接收了所有数据
        SliceInfo expectedSliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy(AllocationStrategy.IID.getCode())
                .build();

        SliceVerification verification = SliceVerification.builder()
                .expectedStartIndex(0)
                .expectedEndIndex(1999)
                .expectedSamples(2000)
                .actualStartIndex(0)
                .actualEndIndex(1999)
                .actualSamples(2000)
                .isComplete(true)
                .missingIndices(Collections.emptyList())
                .continuityCheck(ContinuityCheck.builder()
                        .hasGaps(false)
                        .gapRanges(Collections.emptyList())
                        .build())
                .build();

        // When
        VerificationResult result = sliceVerificationService.verifySliceVerification(
                "vm-1", verification, expectedSliceInfo);

        // Then
        assertThat(result.getIsValid()).isTrue();
        assertThat(result.getIsSampleCountValid()).isTrue();
        assertThat(result.getIsRangeValid()).isTrue();
        assertThat(result.getHasGaps()).isFalse();
        assertThat(result.getMissingCount()).isEqualTo(0);
        assertThat(result.getMissingRate()).isEqualTo(0.0);
        assertThat(result.getRecommendedDecision()).isEqualTo(VerificationDecision.ACCEPT);
        assertThat(result.isPerfect()).isTrue();
    }

    @Test
    @DisplayName("测试验证失败 - 缺失部分数据（缺失率3%）")
    void testVerifySliceVerification_MinorMissing() {
        // Given: VM1缺失了60个样本（3%）
        SliceInfo expectedSliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy(AllocationStrategy.IID.getCode())
                .build();

        // 缺失索引: [100-159]
        List<Integer> missingIndices = Arrays.asList(
                100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
                110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
                120, 121, 122, 123, 124, 125, 126, 127, 128, 129,
                130, 131, 132, 133, 134, 135, 136, 137, 138, 139,
                140, 141, 142, 143, 144, 145, 146, 147, 148, 149,
                150, 151, 152, 153, 154, 155, 156, 157, 158, 159
        );

        SliceVerification verification = SliceVerification.builder()
                .expectedStartIndex(0)
                .expectedEndIndex(1999)
                .expectedSamples(2000)
                .actualStartIndex(0)
                .actualEndIndex(1999)
                .actualSamples(1940)  // 2000 - 60
                .isComplete(false)
                .missingIndices(missingIndices)
                .continuityCheck(ContinuityCheck.builder()
                        .hasGaps(true)
                        .gapRanges(Collections.singletonList(
                                GapRange.builder()
                                        .startIndex(100)
                                        .endIndex(159)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        VerificationResult result = sliceVerificationService.verifySliceVerification(
                "vm-1", verification, expectedSliceInfo);

        // Then
        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getMissingCount()).isEqualTo(60);
        assertThat(result.getMissingRate()).isEqualTo(0.03);  // 3%
        assertThat(result.getHasGaps()).isTrue();
        assertThat(result.getGapRanges()).hasSize(1);
        assertThat(result.getRecommendedDecision()).isEqualTo(VerificationDecision.RETRY_MISSING);
    }

    @Test
    @DisplayName("测试验证失败 - 缺失较多数据（缺失率10%）")
    void testVerifySliceVerification_ModerateMissing() {
        // Given: VM1缺失了200个样本（10%）
        SliceInfo expectedSliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy(AllocationStrategy.IID.getCode())
                .build();

        // 生成200个缺失索引
        List<Integer> missingIndices = new java.util.ArrayList<>();
        for (int i = 0; i < 200; i++) {
            missingIndices.add(i);
        }

        SliceVerification verification = SliceVerification.builder()
                .expectedStartIndex(0)
                .expectedEndIndex(1999)
                .expectedSamples(2000)
                .actualStartIndex(200)  // 从200开始
                .actualEndIndex(1999)
                .actualSamples(1800)  // 2000 - 200
                .isComplete(false)
                .missingIndices(missingIndices)
                .continuityCheck(ContinuityCheck.builder()
                        .hasGaps(true)
                        .gapRanges(Collections.singletonList(
                                GapRange.builder()
                                        .startIndex(0)
                                        .endIndex(199)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        VerificationResult result = sliceVerificationService.verifySliceVerification(
                "vm-1", verification, expectedSliceInfo);

        // Then
        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getMissingCount()).isEqualTo(200);
        assertThat(result.getMissingRate()).isEqualTo(0.10);  // 10%
        assertThat(result.getRecommendedDecision()).isEqualTo(VerificationDecision.EXCLUDE_VM);
    }

    @Test
    @DisplayName("测试验证失败 - 严重缺失（缺失率30%）")
    void testVerifySliceVerification_SevereMissing() {
        // Given: VM1缺失了600个样本（30%）
        SliceInfo expectedSliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy(AllocationStrategy.IID.getCode())
                .build();

        // 生成600个缺失索引
        List<Integer> missingIndices = new java.util.ArrayList<>();
        for (int i = 0; i < 600; i++) {
            missingIndices.add(i);
        }

        SliceVerification verification = SliceVerification.builder()
                .expectedStartIndex(0)
                .expectedEndIndex(1999)
                .expectedSamples(2000)
                .actualStartIndex(600)
                .actualEndIndex(1999)
                .actualSamples(1400)  // 2000 - 600
                .isComplete(false)
                .missingIndices(missingIndices)
                .continuityCheck(ContinuityCheck.builder()
                        .hasGaps(true)
                        .gapRanges(Collections.singletonList(
                                GapRange.builder()
                                        .startIndex(0)
                                        .endIndex(599)
                                        .build()
                        ))
                        .build())
                .build();

        // When
        VerificationResult result = sliceVerificationService.verifySliceVerification(
                "vm-1", verification, expectedSliceInfo);

        // Then
        assertThat(result.getIsValid()).isFalse();
        assertThat(result.getMissingCount()).isEqualTo(600);
        assertThat(result.getMissingRate()).isEqualTo(0.30);  // 30%
        assertThat(result.getRecommendedDecision()).isEqualTo(VerificationDecision.ABORT_TASK);
    }

    // ========== 2. 缺失索引检测测试 ==========

    @Test
    @DisplayName("测试查找缺失索引 - 无缺失")
    void testFindMissingIndices_NothingMissing() {
        // Given: 完整的索引列表
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        // When
        List<Integer> missing = sliceVerificationService.findMissingIndices(
                receivedIndices, 0, 9);

        // Then
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("测试查找缺失索引 - 中间缺失")
    void testFindMissingIndices_MiddleMissing() {
        // Given: 缺失[5-7]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 3, 4, 8, 9);

        // When
        List<Integer> missing = sliceVerificationService.findMissingIndices(
                receivedIndices, 0, 9);

        // Then
        assertThat(missing).containsExactly(5, 6, 7);
    }

    @Test
    @DisplayName("测试查找缺失索引 - 开头缺失")
    void testFindMissingIndices_StartMissing() {
        // Given: 缺失[0-2]
        List<Integer> receivedIndices = Arrays.asList(3, 4, 5, 6, 7, 8, 9);

        // When
        List<Integer> missing = sliceVerificationService.findMissingIndices(
                receivedIndices, 0, 9);

        // Then
        assertThat(missing).containsExactly(0, 1, 2);
    }

    @Test
    @DisplayName("测试查找缺失索引 - 末尾缺失")
    void testFindMissingIndices_EndMissing() {
        // Given: 缺失[7-9]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 3, 4, 5, 6);

        // When
        List<Integer> missing = sliceVerificationService.findMissingIndices(
                receivedIndices, 0, 9);

        // Then
        assertThat(missing).containsExactly(7, 8, 9);
    }

    // ========== 3. 数据间隙检测测试 ==========

    @Test
    @DisplayName("测试间隙检测 - 无间隙")
    void testHasDataGaps_NoGaps() {
        // Given: 连续索引
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 3, 4, 5);

        // When
        boolean hasGaps = sliceVerificationService.hasDataGaps(receivedIndices);

        // Then
        assertThat(hasGaps).isFalse();
    }

    @Test
    @DisplayName("测试间隙检测 - 存在单个间隙")
    void testHasDataGaps_SingleGap() {
        // Given: [0-2] gap [5-7]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 5, 6, 7);

        // When
        boolean hasGaps = sliceVerificationService.hasDataGaps(receivedIndices);

        // Then
        assertThat(hasGaps).isTrue();
    }

    @Test
    @DisplayName("测试间隙检测 - 存在多个间隙")
    void testHasDataGaps_MultipleGaps() {
        // Given: [0-1] gap [3-4] gap [7-8]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 3, 4, 7, 8);

        // When
        boolean hasGaps = sliceVerificationService.hasDataGaps(receivedIndices);

        // Then
        assertThat(hasGaps).isTrue();
    }

    @Test
    @DisplayName("测试查找间隙范围 - 单个间隙")
    void testFindGapRanges_SingleGap() {
        // Given: [0-2] gap [5-7]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 2, 5, 6, 7);

        // When
        List<VerificationResult.GapRange> gaps = sliceVerificationService.findGapRanges(receivedIndices);

        // Then
        assertThat(gaps).hasSize(1);
        assertThat(gaps.get(0).getStartIndex()).isEqualTo(3);
        assertThat(gaps.get(0).getEndIndex()).isEqualTo(4);
        assertThat(gaps.get(0).getGapSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("测试查找间隙范围 - 多个间隙")
    void testFindGapRanges_MultipleGaps() {
        // Given: [0-1] gap [3-4] gap [7-9]
        List<Integer> receivedIndices = Arrays.asList(0, 1, 3, 4, 7, 8, 9);

        // When
        List<VerificationResult.GapRange> gaps = sliceVerificationService.findGapRanges(receivedIndices);

        // Then
        assertThat(gaps).hasSize(2);
        // Gap 1: [2-2]
        assertThat(gaps.get(0).getStartIndex()).isEqualTo(2);
        assertThat(gaps.get(0).getEndIndex()).isEqualTo(2);
        assertThat(gaps.get(0).getGapSize()).isEqualTo(1);
        // Gap 2: [5-6]
        assertThat(gaps.get(1).getStartIndex()).isEqualTo(5);
        assertThat(gaps.get(1).getEndIndex()).isEqualTo(6);
        assertThat(gaps.get(1).getGapSize()).isEqualTo(2);
    }

    // ========== 4. 决策测试 ==========

    @Test
    @DisplayName("测试决策 - 完整数据应接受")
    void testDecideOnVerificationFailure_Accept() {
        // Given: 完全正确的验证结果
        VerificationResult result = VerificationResult.builder()
                .vmId("vm-1")
                .isValid(true)
                .missingCount(0)
                .missingRate(0.0)
                .expectedSamples(2000)
                .build();

        // When
        VerificationDecision decision = sliceVerificationService.decideOnVerificationFailure("vm-1", result);

        // Then
        assertThat(decision).isEqualTo(VerificationDecision.ACCEPT);
    }

    @Test
    @DisplayName("测试决策 - 缺失率4%应重传")
    void testDecideOnVerificationFailure_Retry() {
        // Given: 缺失率4%
        VerificationResult result = VerificationResult.builder()
                .vmId("vm-1")
                .isValid(false)
                .missingCount(80)
                .missingRate(0.04)
                .expectedSamples(2000)
                .build();

        // When
        VerificationDecision decision = sliceVerificationService.decideOnVerificationFailure("vm-1", result);

        // Then
        assertThat(decision).isEqualTo(VerificationDecision.RETRY_MISSING);
    }

    @Test
    @DisplayName("测试决策 - 缺失率15%应排除VM")
    void testDecideOnVerificationFailure_Exclude() {
        // Given: 缺失率15%
        VerificationResult result = VerificationResult.builder()
                .vmId("vm-1")
                .isValid(false)
                .missingCount(300)
                .missingRate(0.15)
                .expectedSamples(2000)
                .build();

        // When
        VerificationDecision decision = sliceVerificationService.decideOnVerificationFailure("vm-1", result);

        // Then
        assertThat(decision).isEqualTo(VerificationDecision.EXCLUDE_VM);
    }

    @Test
    @DisplayName("测试决策 - 缺失率25%应中止任务")
    void testDecideOnVerificationFailure_Abort() {
        // Given: 缺失率25%
        VerificationResult result = VerificationResult.builder()
                .vmId("vm-1")
                .isValid(false)
                .missingCount(500)
                .missingRate(0.25)
                .expectedSamples(2000)
                .build();

        // When
        VerificationDecision decision = sliceVerificationService.decideOnVerificationFailure("vm-1", result);

        // Then
        assertThat(decision).isEqualTo(VerificationDecision.ABORT_TASK);
    }

    // ========== 5. 边界条件测试 ==========

    @Test
    @DisplayName("测试参数验证 - vmId为空")
    void testVerifySliceVerification_NullVmId() {
        SliceInfo sliceInfo = SliceInfo.builder().build();
        SliceVerification verification = SliceVerification.builder().build();

        assertThatThrownBy(() ->
                sliceVerificationService.verifySliceVerification(null, verification, sliceInfo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vmId不能为空");
    }

    @Test
    @DisplayName("测试参数验证 - verification为空")
    void testVerifySliceVerification_NullVerification() {
        SliceInfo sliceInfo = SliceInfo.builder().build();

        assertThatThrownBy(() ->
                sliceVerificationService.verifySliceVerification("vm-1", null, sliceInfo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verification不能为空");
    }

    @Test
    @DisplayName("测试参数验证 - expectedSliceInfo为空")
    void testVerifySliceVerification_NullSliceInfo() {
        SliceVerification verification = SliceVerification.builder().build();

        assertThatThrownBy(() ->
                sliceVerificationService.verifySliceVerification("vm-1", verification, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedSliceInfo不能为空");
    }

    @Test
    @DisplayName("测试缺失率计算")
    void testCalculateMissingRate() {
        assertThat(sliceVerificationService.calculateMissingRate(0, 100)).isEqualTo(0.0);
        assertThat(sliceVerificationService.calculateMissingRate(5, 100)).isEqualTo(0.05);
        assertThat(sliceVerificationService.calculateMissingRate(50, 100)).isEqualTo(0.5);
        assertThat(sliceVerificationService.calculateMissingRate(100, 100)).isEqualTo(1.0);
    }
}