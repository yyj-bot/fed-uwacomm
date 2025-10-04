package com.feduwacomm.integration;

import com.feduwacomm.dto.BatchRange;
import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.service.DataDistributionService;
import com.feduwacomm.service.DataSlicingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 数据切分集成测试 (v1.5.1)
 * 测试从数据切分到数据分发的完整流程
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("数据切分集成测试")
class DataSlicingIntegrationTest {

    @Autowired
    private DataSlicingService dataSlicingService;

    @Autowired
    private DataDistributionService dataDistributionService;

    @MockBean
    private TrainingDatasetRowMapper trainingDatasetRowMapper;

    @BeforeEach
    void setUp() {
        // Mock数据集大小
        // dataset-integration-test: 10000 samples
        when(trainingDatasetRowMapper.countByDataset("dataset-integration-test"))
                .thenReturn(10000);

        // dataset-coverage-test: 1000 samples
        when(trainingDatasetRowMapper.countByDataset("dataset-coverage-test"))
                .thenReturn(1000);

        // dataset-remainder-test: 10003 samples (有余数)
        when(trainingDatasetRowMapper.countByDataset("dataset-remainder-test"))
                .thenReturn(10003);
    }

    @Test
    @DisplayName("集成测试：完整的数据切分和分发流程")
    void testCompleteDataSlicingAndDistributionFlow() {
        // ========== 阶段1：数据切分 ==========

        // Given: 模拟5个VM，数据集有10000个样本
        String datasetId = "dataset-integration-test";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2", "vm-3", "vm-4", "vm-5");

        // When: 使用IID策略切分数据
        List<DataSliceResult> sliceResults = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then: 验证切分结果
        assertThat(sliceResults).hasSize(5);

        // 验证每个VM的切片信息
        for (int i = 0; i < 5; i++) {
            DataSliceResult result = sliceResults.get(i);
            assertThat(result.getVmId()).isEqualTo(vmIds.get(i));
            assertThat(result.getAssignedDatasetId()).isNotNull();
            assertThat(result.getSliceInfo()).isNotNull();
            assertThat(result.getSliceInfo().isValid()).isTrue();
        }

        // 验证切片覆盖完整性（无重叠、无遗漏）
        Set<Integer> allIndices = new HashSet<>();
        for (DataSliceResult result : sliceResults) {
            SliceInfo info = result.getSliceInfo();
            for (int i = info.getStartIndex(); i <= info.getEndIndex(); i++) {
                assertThat(allIndices.add(i))
                        .as("索引%d不应该重复", i)
                        .isTrue();
            }
        }
        assertThat(allIndices).hasSize(10000);

        // ========== 阶段2：为第一个VM计算BatchRange ==========

        DataSliceResult vm1Slice = sliceResults.get(0);
        SliceInfo vm1SliceInfo = vm1Slice.getSliceInfo();

        // 计算第一个批次（batchSize=500）
        BatchRange batch0 = dataDistributionService.calculateBatchRange(vm1SliceInfo, 0, 500);
        assertThat(batch0.getLocalStartIndex()).isEqualTo(0);
        assertThat(batch0.getLocalEndIndex()).isEqualTo(499);
        assertThat(batch0.getGlobalStartIndex()).isEqualTo(vm1SliceInfo.getStartIndex());
        assertThat(batch0.getGlobalEndIndex()).isEqualTo(vm1SliceInfo.getStartIndex() + 499);

        // 计算第二个批次
        BatchRange batch1 = dataDistributionService.calculateBatchRange(vm1SliceInfo, 1, 500);
        assertThat(batch1.getLocalStartIndex()).isEqualTo(500);
        assertThat(batch1.getLocalEndIndex()).isEqualTo(999);

        // 计算最后一个批次（不完整）
        int lastBatchIndex = vm1SliceInfo.getSliceSamples() / 500;
        BatchRange lastBatch = dataDistributionService.calculateBatchRange(
                vm1SliceInfo, lastBatchIndex - 1, 500);
        assertThat(lastBatch.getLocalEndIndex()).isEqualTo(vm1SliceInfo.getSliceSamples() - 1);

        // ========== 阶段3：为数据行添加双重索引 ==========

        // 模拟第一个批次的数据行
        List<Map<String, Object>> batch0Rows = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("feature1", i * 1.0);
            row.put("feature2", i * 2.0);
            batch0Rows.add(row);
        }

        // 添加双重索引
        dataDistributionService.addDualIndices(batch0Rows, 0, vm1SliceInfo);

        // 验证索引
        for (int i = 0; i < batch0Rows.size(); i++) {
            Map<String, Object> row = batch0Rows.get(i);
            assertThat(row.get("localIndex")).isEqualTo(i);
            assertThat(row.get("globalIndex")).isEqualTo(vm1SliceInfo.getStartIndex() + i);
        }

        // ========== 阶段4：测试VM2的偏移处理 ==========

        DataSliceResult vm2Slice = sliceResults.get(1);
        SliceInfo vm2SliceInfo = vm2Slice.getSliceInfo();

        // VM2的startIndex应该是2000
        assertThat(vm2SliceInfo.getStartIndex()).isEqualTo(2000);

        // 为VM2计算第一个批次
        BatchRange vm2Batch0 = dataDistributionService.calculateBatchRange(vm2SliceInfo, 0, 500);
        assertThat(vm2Batch0.getLocalStartIndex()).isEqualTo(0);
        assertThat(vm2Batch0.getGlobalStartIndex()).isEqualTo(2000);
        assertThat(vm2Batch0.getGlobalEndIndex()).isEqualTo(2499);

        // 为VM2的数据添加双重索引
        List<Map<String, Object>> vm2Rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            vm2Rows.add(new HashMap<>());
        }
        dataDistributionService.addDualIndices(vm2Rows, 0, vm2SliceInfo);

        // 验证VM2的全局索引从2000开始
        assertThat(vm2Rows.get(0).get("globalIndex")).isEqualTo(2000);
        assertThat(vm2Rows.get(1).get("globalIndex")).isEqualTo(2001);
        assertThat(vm2Rows.get(2).get("globalIndex")).isEqualTo(2002);

        // ========== 阶段5：验证索引转换的双向性 ==========

        for (DataSliceResult result : sliceResults) {
            SliceInfo info = result.getSliceInfo();

            // 验证所有本地索引都能正确转换为全局索引
            for (int localIndex = 0; localIndex < info.getSliceSamples(); localIndex++) {
                int globalIndex = info.toGlobalIndex(localIndex);
                assertThat(globalIndex).isBetween(info.getStartIndex(), info.getEndIndex());

                // 反向验证：全局索引能否转换回本地索引
                int reversedLocalIndex = info.toLocalIndex(globalIndex);
                assertThat(reversedLocalIndex).isEqualTo(localIndex);
            }
        }

        System.out.println("✅ 集成测试通过：数据切分和分发流程完整且正确");
    }

    @Test
    @DisplayName("集成测试：分布式数据覆盖验证")
    void testDistributedDataCoverageValidation() {
        // Given: 3个VM，数据集有1000个样本
        String datasetId = "dataset-coverage-test";
        List<String> vmIds = Arrays.asList("vm-alpha", "vm-beta", "vm-gamma");

        // When: 切分数据
        List<DataSliceResult> sliceResults = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then: 模拟每个VM接收数据并验证全局覆盖
        Map<String, Set<Integer>> vmReceivedIndices = new HashMap<>();

        for (DataSliceResult result : sliceResults) {
            String vmId = result.getVmId();
            SliceInfo sliceInfo = result.getSliceInfo();
            Set<Integer> receivedIndices = new HashSet<>();

            // 模拟VM按批次接收数据
            int batchSize = 100;
            int totalBatches = (sliceInfo.getSliceSamples() + batchSize - 1) / batchSize;

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                // 计算批次范围
                BatchRange batchRange = dataDistributionService.calculateBatchRange(
                        sliceInfo, batchIndex, batchSize);

                // 模拟接收该批次的所有样本
                for (int globalIndex = batchRange.getGlobalStartIndex();
                     globalIndex <= batchRange.getGlobalEndIndex();
                     globalIndex++) {
                    receivedIndices.add(globalIndex);
                }
            }

            // 验证VM接收的数据范围
            assertThat(receivedIndices).hasSize(sliceInfo.getSliceSamples());
            assertThat(receivedIndices).containsAll(
                    Arrays.asList(sliceInfo.getStartIndex(), sliceInfo.getEndIndex()));

            vmReceivedIndices.put(vmId, receivedIndices);
        }

        // 验证所有VM合并后的数据覆盖了完整的数据集
        Set<Integer> allReceivedIndices = new HashSet<>();
        vmReceivedIndices.values().forEach(allReceivedIndices::addAll);

        assertThat(allReceivedIndices).hasSize(1000);
        assertThat(allReceivedIndices).contains(0, 999);

        // 验证没有重复接收
        int totalReceived = vmReceivedIndices.values().stream()
                .mapToInt(Set::size)
                .sum();
        assertThat(totalReceived).isEqualTo(1000);

        System.out.println("✅ 分布式数据覆盖验证通过：无遗漏、无重复");
    }

    @Test
    @DisplayName("集成测试：余数处理和边界条件")
    void testRemainderHandlingAndBoundaryConditions() {
        // Given: 5个VM，10003个样本（有余数）
        String datasetId = "dataset-remainder-test";
        List<String> vmIds = Arrays.asList("vm-1", "vm-2", "vm-3", "vm-4", "vm-5");

        // When
        List<DataSliceResult> sliceResults = dataSlicingService.sliceDataset(
                datasetId, vmIds, AllocationStrategy.IID
        );

        // Then: 验证余数处理
        // 前3个VM应该得到2001个样本，后2个VM得到2000个样本
        assertThat(sliceResults.get(0).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(sliceResults.get(1).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(sliceResults.get(2).getSliceInfo().getSliceSamples()).isEqualTo(2001);
        assertThat(sliceResults.get(3).getSliceInfo().getSliceSamples()).isEqualTo(2000);
        assertThat(sliceResults.get(4).getSliceInfo().getSliceSamples()).isEqualTo(2000);

        // 验证索引连续性
        for (int i = 1; i < sliceResults.size(); i++) {
            SliceInfo prev = sliceResults.get(i - 1).getSliceInfo();
            SliceInfo curr = sliceResults.get(i).getSliceInfo();

            // 当前切片的startIndex应该等于前一个切片的endIndex + 1
            assertThat(curr.getStartIndex()).isEqualTo(prev.getEndIndex() + 1);
        }

        // 验证最后一个批次的边界处理
        DataSliceResult lastVm = sliceResults.get(4);
        SliceInfo lastSliceInfo = lastVm.getSliceInfo();
        int lastBatchIndex = (lastSliceInfo.getSliceSamples() - 1) / 500;

        BatchRange lastBatch = dataDistributionService.calculateBatchRange(
                lastSliceInfo, lastBatchIndex, 500);

        // 最后一个批次应该正确处理不满500的情况
        assertThat(lastBatch.getGlobalEndIndex()).isEqualTo(10002); // 最后一个索引

        System.out.println("✅ 余数处理和边界条件测试通过");
    }
}