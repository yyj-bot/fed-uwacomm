package com.feduwacomm.service;

import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.mapper.TrainingDatasetMapper;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.service.impl.IIDDataSlicingServiceImpl;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 数据切分服务单元测试 (v1.5.1)
 * 测试IID数据分配策略的正确性
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataSlicingService 单元测试")
class DataSlicingServiceTest {

    @Mock
    private TrainingDatasetMapper trainingDatasetMapper;

    @Mock
    private TrainingDatasetRowMapper trainingDatasetRowMapper;

    @Mock
    private UuidUtil uuidUtil;

    private com.feduwacomm.service.impl.RatioDataSlicingServiceImpl ratioDataSlicingService;
    private DataSlicingService dataSlicingService;

    @BeforeEach
    void setUp() {
        // 创建真实的RatioDataSlicingServiceImpl实例，因为IID复用其逻辑
        ratioDataSlicingService = new com.feduwacomm.service.impl.RatioDataSlicingServiceImpl(
                trainingDatasetMapper,
                trainingDatasetRowMapper,
                uuidUtil
        );

        dataSlicingService = new IIDDataSlicingServiceImpl(
                trainingDatasetMapper,
                trainingDatasetRowMapper,
                uuidUtil,
                ratioDataSlicingService
        );

        // Mock UUID生成 (lenient for tests that don't use UUID)
        lenient().when(uuidUtil.generateUuid())
                .thenReturn("uuid-1")
                .thenReturn("uuid-2")
                .thenReturn("uuid-3")
                .thenReturn("uuid-4")
                .thenReturn("uuid-5");
    }

    @Test
    @DisplayName("测试IID均匀分配 - 5个VM，10000样本")
    void testIIDUniformDistribution_5VMs_10000Samples() {
        // Given
        String datasetId = "dataset-001";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2", "vm-3", "vm-4", "vm-5");
        int totalSamples = 10000;

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(totalSamples);

        // When
        List<DataSliceResult> results = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then
        assertThat(results).hasSize(5);

        // 验证每个VM分配2000个样本
        for (int i = 0; i < 5; i++) {
            DataSliceResult result = results.get(i);
            assertThat(result.getVmId()).isEqualTo(vmIds.get(i));
            assertThat(result.getSliceInfo().getSliceSamples()).isEqualTo(2000);
            assertThat(result.getSliceInfo().getStartIndex()).isEqualTo(i * 2000);
            assertThat(result.getSliceInfo().getEndIndex()).isEqualTo((i + 1) * 2000 - 1);
            assertThat(result.getSliceInfo().getTotalSamples()).isEqualTo(totalSamples);
            assertThat(result.getSliceInfo().getSliceIndex()).isEqualTo(i + 1);
            assertThat(result.getSliceInfo().getTotalSlices()).isEqualTo(5);
            assertThat(result.getSliceInfo().getAllocationStrategy()).isEqualTo("IID");
        }

        // 验证覆盖完整性
        verifyFullCoverage(results, totalSamples);
    }

    @Test
    @DisplayName("测试IID余数处理 - 5个VM，10003样本")
    void testIIDRemainderHandling_5VMs_10003Samples() {
        // Given
        String datasetId = "dataset-002";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2", "vm-3", "vm-4", "vm-5");
        int totalSamples = 10003;

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(totalSamples);

        // When
        List<DataSliceResult> results = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then
        assertThat(results).hasSize(5);

        // 前3个VM应该分配2001个样本，后2个VM分配2000个样本
        assertThat(results.get(0).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(results.get(1).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(results.get(2).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(results.get(3).getSliceInfo().getSliceSamples()).isEqualTo(2000);
        assertThat(results.get(4).getSliceInfo().getSliceSamples()).isEqualTo(2000);

        // 验证索引连续性
        assertThat(results.get(0).getSliceInfo().getStartIndex()).isEqualTo(0);
        assertThat(results.get(0).getSliceInfo().getEndIndex()).isEqualTo(2000);
        assertThat(results.get(1).getSliceInfo().getStartIndex()).isEqualTo(2001);
        assertThat(results.get(1).getSliceInfo().getEndIndex()).isEqualTo(4001);
        assertThat(results.get(2).getSliceInfo().getStartIndex()).isEqualTo(4002);
        assertThat(results.get(2).getSliceInfo().getEndIndex()).isEqualTo(6002);
        assertThat(results.get(3).getSliceInfo().getStartIndex()).isEqualTo(6003);
        assertThat(results.get(3).getSliceInfo().getEndIndex()).isEqualTo(8002);
        assertThat(results.get(4).getSliceInfo().getStartIndex()).isEqualTo(8003);
        assertThat(results.get(4).getSliceInfo().getEndIndex()).isEqualTo(10002);

        // 验证覆盖完整性
        verifyFullCoverage(results, totalSamples);
    }

    @Test
    @DisplayName("测试边界条件 - 1个VM")
    void testBoundaryCase_SingleVM() {
        // Given
        String datasetId = "dataset-003";
        List<String> vmIds = Arrays.asList("vm-1");
        int totalSamples = 5000;

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(totalSamples);

        // When
        List<DataSliceResult> results = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSliceInfo().getSliceSamples()).isEqualTo(5000);
        assertThat(results.get(0).getSliceInfo().getStartIndex()).isEqualTo(0);
        assertThat(results.get(0).getSliceInfo().getEndIndex()).isEqualTo(4999);
    }

    @Test
    @DisplayName("测试边界条件 - VM数量大于样本数")
    void testBoundaryCase_MoreVMsThanSamples() {
        // Given
        String datasetId = "dataset-004";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2", "vm-3", "vm-4", "vm-5");
        int totalSamples = 3;

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(totalSamples);

        // When
        List<DataSliceResult> results = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then
        // 只有前3个VM应该分配到数据
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getSliceInfo().getSliceSamples()).isEqualTo(1);
        assertThat(results.get(1).getSliceInfo().getSliceSamples()).isEqualTo(1);
        assertThat(results.get(2).getSliceInfo().getSliceSamples()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试异常情况 - 空数据集")
    void testExceptionCase_EmptyDataset() {
        // Given
        String datasetId = "dataset-005";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2");

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        ))
                .isInstanceOf(DataSlicingService.DataSlicingException.class)
                .hasMessageContaining("数据集为空");
    }

    @Test
    @DisplayName("测试异常情况 - VM列表为空")
    void testExceptionCase_EmptyVmList() {
        // Given
        String datasetId = "dataset-006";
        List<String> vmIds = Arrays.asList();

        // When & Then
        assertThatThrownBy(() -> dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vmIds列表不能为空");
    }

    @Test
    @DisplayName("测试异常情况 - 数据集ID为空")
    void testExceptionCase_NullDatasetId() {
        // Given
        List<String> vmIds = Arrays.asList("vm-1", "vm-2");

        // When & Then
        assertThatThrownBy(() -> dataSlicingService.sliceDataset(
                null, vmIds, AllocationStrategy.IID
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datasetId不能为空");
    }

    @Test
    @DisplayName("测试SliceInfo生成")
    void testGenerateSliceInfo() {
        // When
        SliceInfo sliceInfo = dataSlicingService.generateSliceInfo(
                1, 0, 1999, 10000, 5, AllocationStrategy.IID
        );

        // Then
        assertThat(sliceInfo.getSliceIndex()).isEqualTo(1);
        assertThat(sliceInfo.getStartIndex()).isEqualTo(0);
        assertThat(sliceInfo.getEndIndex()).isEqualTo(1999);
        assertThat(sliceInfo.getSliceSamples()).isEqualTo(2000);
        assertThat(sliceInfo.getTotalSamples()).isEqualTo(10000);
        assertThat(sliceInfo.getTotalSlices()).isEqualTo(5);
        assertThat(sliceInfo.getAllocationStrategy()).isEqualTo("IID");
        assertThat(sliceInfo.isValid()).isTrue();
    }

    @Test
    @DisplayName("测试切片覆盖验证 - 有效切片")
    void testValidateSlicesCoverage_ValidSlices() {
        // Given
        List<DataSliceResult> results = Arrays.asList(
                createValidSliceResult("vm-1", 0, 1999, 6000),
                createValidSliceResult("vm-2", 2000, 3999, 6000),
                createValidSliceResult("vm-3", 4000, 5999, 6000)
        );

        // When & Then
        assertThatCode(() -> dataSlicingService.validateSlicesCoverage(results, 6000))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("测试切片覆盖验证 - 检测索引遗漏")
    void testValidateSlicesCoverage_DetectMissingIndices() {
        // Given - 缺失[2000-2999]
        List<DataSliceResult> results = Arrays.asList(
                createValidSliceResult("vm-1", 0, 1999, 4000),
                createValidSliceResult("vm-2", 3000, 3999, 4000)
        );

        // When & Then
        assertThatThrownBy(() -> dataSlicingService.validateSlicesCoverage(results, 4000))
                .isInstanceOf(DataSlicingService.DataSlicingException.class)
                .hasMessageContaining("切片覆盖不完整");
    }

    @Test
    @DisplayName("测试切片覆盖验证 - 检测索引重叠")
    void testValidateSlicesCoverage_DetectOverlap() {
        // Given - [1500-2000] 重叠
        List<DataSliceResult> results = Arrays.asList(
                createValidSliceResult("vm-1", 0, 2000, 4000),
                createValidSliceResult("vm-2", 1500, 3999, 4000)
        );

        // When & Then
        assertThatThrownBy(() -> dataSlicingService.validateSlicesCoverage(results, 4000))
                .isInstanceOf(DataSlicingService.DataSlicingException.class)
                .hasMessageContaining("索引重叠");
    }

    @Test
    @DisplayName("测试assignedDatasetId生成")
    void testAssignedDatasetIdGeneration() {
        // Given
        String datasetId = "dataset-007";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2");
        int totalSamples = 1000;

        when(trainingDatasetRowMapper.countByDataset(datasetId)).thenReturn(totalSamples);

        // When
        List<DataSliceResult> results = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then
        assertThat(results.get(0).getAssignedDatasetId()).isNotNull();
        assertThat(results.get(1).getAssignedDatasetId()).isNotNull();
        assertThat(results.get(0).getAssignedDatasetId())
                .isNotEqualTo(results.get(1).getAssignedDatasetId());
    }

    // ========== 辅助方法 ==========

    private void verifyFullCoverage(List<DataSliceResult> results, int totalSamples) {
        Set<Integer> coveredIndices = results.stream()
                .flatMap(result -> {
                    SliceInfo info = result.getSliceInfo();
                    return java.util.stream.IntStream
                            .rangeClosed(info.getStartIndex(), info.getEndIndex())
                            .boxed();
                })
                .collect(Collectors.toSet());

        assertThat(coveredIndices).hasSize(totalSamples);
        assertThat(coveredIndices).contains(0, totalSamples - 1);
    }

    private DataSliceResult createSliceResult(String vmId, int startIndex, int endIndex) {
        int sliceSamples = endIndex - startIndex + 1;
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(startIndex)
                .endIndex(endIndex)
                .sliceSamples(sliceSamples)
                .totalSamples(sliceSamples) // 设置有效值避免验证失败
                .sliceIndex(1)
                .totalSlices(1)
                .allocationStrategy("IID")
                .build();

        return DataSliceResult.builder()
                .vmId(vmId)
                .sliceInfo(sliceInfo)
                .build();
    }

    private DataSliceResult createValidSliceResult(String vmId, int startIndex, int endIndex, int totalSamples) {
        int sliceSamples = endIndex - startIndex + 1;
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(startIndex)
                .endIndex(endIndex)
                .sliceSamples(sliceSamples)
                .totalSamples(totalSamples) // 使用真实的totalSamples
                .sliceIndex(1)
                .totalSlices(2)
                .allocationStrategy("IID")
                .build();

        return DataSliceResult.builder()
                .vmId(vmId)
                .sliceInfo(sliceInfo)
                .build();
    }
}