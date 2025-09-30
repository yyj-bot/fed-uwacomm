package com.feduwacomm.service;

import com.feduwacomm.dto.BatchRange;
import com.feduwacomm.dto.DataSliceResult;
import com.feduwacomm.dto.SliceInfo;
import com.feduwacomm.enums.AllocationStrategy;
import com.feduwacomm.mapper.TrainingDatasetRowMapper;
import com.feduwacomm.service.impl.DataDistributionServiceImpl;
import com.feduwacomm.utils.MessageIdGenerator;
import com.feduwacomm.utils.MessageBuilder;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 数据分发服务增强功能单元测试 (v1.5.1)
 * 测试BatchRange计算和双重索引添加功能
 *
 * @author FedUWAComm Team
 * @version 1.5.1
 * @since 2025-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataDistributionService v1.5.1增强功能测试")
class DataDistributionServiceEnhancedTest {

    @Mock
    private com.feduwacomm.mapper.DataDistributionMapper dataDistributionMapper;

    @Mock
    private com.feduwacomm.mapper.DataDistributionDetailMapper dataDistributionDetailMapper;

    @Mock
    private com.feduwacomm.mapper.VmInstancesMapper vmInstancesMapper;

    @Mock
    private com.feduwacomm.mapper.TrainingDatasetMapper trainingDatasetMapper;

    @Mock
    private com.feduwacomm.mapper.TaskParticipantsMapper taskParticipantsMapper;

    @Mock
    private com.feduwacomm.mapper.FederatedTasksMapper federatedTasksMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UuidUtil uuidUtil;

    @Mock
    private DataSlicingService dataSlicingService;

    @Mock
    private TrainingDatasetRowMapper trainingDatasetRowMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageIdGenerator messageIdGenerator;

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private com.feduwacomm.service.impl.RatioDataSlicingServiceImpl ratioDataSlicingService;

    private DataDistributionService dataDistributionService;

    @BeforeEach
    void setUp() {
        dataDistributionService = new DataDistributionServiceImpl(
                dataDistributionMapper,
                dataDistributionDetailMapper,
                vmInstancesMapper,
                trainingDatasetMapper,
                taskParticipantsMapper,
                federatedTasksMapper,
                eventPublisher,
                objectMapper,
                uuidUtil,
                dataSlicingService,
                trainingDatasetRowMapper,
                messagingTemplate,
                messageIdGenerator,
                messageBuilder,
                ratioDataSlicingService
        );
    }

    // ========== BatchRange计算测试 ==========

    @Test
    @DisplayName("测试BatchRange计算 - 完整批次")
    void testCalculateBatchRange_CompleteBatch() {
        // Given: VM1接收[0-1999]的数据，batchSize=500
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When: 计算第一个批次[0-499]
        BatchRange batchRange = dataDistributionService.calculateBatchRange(sliceInfo, 0, 500);

        // Then
        assertThat(batchRange.getLocalStartIndex()).isEqualTo(0);
        assertThat(batchRange.getLocalEndIndex()).isEqualTo(499);
        assertThat(batchRange.getGlobalStartIndex()).isEqualTo(0);
        assertThat(batchRange.getGlobalEndIndex()).isEqualTo(499);
        assertThat(batchRange.getBatchSize()).isEqualTo(500);
        assertThat(batchRange.isValid()).isTrue();
        assertThat(batchRange.verifyIndexRelation(0)).isTrue();
    }

    @Test
    @DisplayName("测试BatchRange计算 - 最后一个不完整批次")
    void testCalculateBatchRange_LastIncompleteBatch() {
        // Given: VM1接收[0-1999]的数据，batchSize=500
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When: 计算最后一个批次（batchIndex=3，应该是[1500-1999]）
        BatchRange batchRange = dataDistributionService.calculateBatchRange(sliceInfo, 3, 500);

        // Then
        assertThat(batchRange.getLocalStartIndex()).isEqualTo(1500);
        assertThat(batchRange.getLocalEndIndex()).isEqualTo(1999); // 不满500，只有500个样本
        assertThat(batchRange.getGlobalStartIndex()).isEqualTo(1500);
        assertThat(batchRange.getGlobalEndIndex()).isEqualTo(1999);
        assertThat(batchRange.getBatchSize()).isEqualTo(500);
        assertThat(batchRange.isValid()).isTrue();
    }

    @Test
    @DisplayName("测试BatchRange计算 - VM2的批次（startIndex=2000）")
    void testCalculateBatchRange_VM2WithOffset() {
        // Given: VM2接收[2000-3999]的数据，batchSize=500
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(2000)
                .endIndex(3999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(2)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When: 计算第一个批次
        BatchRange batchRange = dataDistributionService.calculateBatchRange(sliceInfo, 0, 500);

        // Then: 本地索引从0开始，全局索引从2000开始
        assertThat(batchRange.getLocalStartIndex()).isEqualTo(0);
        assertThat(batchRange.getLocalEndIndex()).isEqualTo(499);
        assertThat(batchRange.getGlobalStartIndex()).isEqualTo(2000); // 全局索引偏移2000
        assertThat(batchRange.getGlobalEndIndex()).isEqualTo(2499);
        assertThat(batchRange.verifyIndexRelation(2000)).isTrue();
    }

    @Test
    @DisplayName("测试BatchRange计算 - 批次大小为1")
    void testCalculateBatchRange_BatchSizeOne() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(99)
                .sliceSamples(100)
                .totalSamples(100)
                .sliceIndex(1)
                .totalSlices(1)
                .allocationStrategy("IID")
                .build();

        // When: 批次大小为1
        BatchRange batchRange = dataDistributionService.calculateBatchRange(sliceInfo, 50, 1);

        // Then
        assertThat(batchRange.getLocalStartIndex()).isEqualTo(50);
        assertThat(batchRange.getLocalEndIndex()).isEqualTo(50);
        assertThat(batchRange.getGlobalStartIndex()).isEqualTo(50);
        assertThat(batchRange.getGlobalEndIndex()).isEqualTo(50);
        assertThat(batchRange.getBatchSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试BatchRange计算 - 异常情况：批次索引超出范围")
    void testCalculateBatchRange_ExceptionBatchIndexOutOfRange() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When & Then: batchIndex=5, batchSize=500会超出[0-1999]范围
        assertThatThrownBy(() -> dataDistributionService.calculateBatchRange(sliceInfo, 5, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("批次索引超出范围");
    }

    @Test
    @DisplayName("测试BatchRange计算 - 异常情况：SliceInfo无效")
    void testCalculateBatchRange_ExceptionInvalidSliceInfo() {
        // Given: SliceInfo无效（endIndex < startIndex）
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(2000)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When & Then
        assertThatThrownBy(() -> dataDistributionService.calculateBatchRange(sliceInfo, 0, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sliceInfo无效");
    }

    @Test
    @DisplayName("测试BatchRange计算 - 异常情况：batchSize非法")
    void testCalculateBatchRange_ExceptionInvalidBatchSize() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When & Then: batchSize <= 0
        assertThatThrownBy(() -> dataDistributionService.calculateBatchRange(sliceInfo, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize必须大于0");

        assertThatThrownBy(() -> dataDistributionService.calculateBatchRange(sliceInfo, 0, -10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize必须大于0");
    }

    // ========== 双重索引添加测试 ==========

    @Test
    @DisplayName("测试双重索引生成 - VM1（startIndex=0）")
    void testAddDualIndices_VM1() {
        // Given: VM1的切片信息，startIndex=0
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // 创建批次数据（batchStartIndex=0，3行数据）
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("feature1", i * 1.0);
            row.put("feature2", i * 2.0);
            rows.add(row);
        }

        // When: 为批次添加双重索引
        dataDistributionService.addDualIndices(rows, 0, sliceInfo);

        // Then: 验证每一行的索引
        assertThat(rows.get(0).get("localIndex")).isEqualTo(0);
        assertThat(rows.get(0).get("globalIndex")).isEqualTo(0);

        assertThat(rows.get(1).get("localIndex")).isEqualTo(1);
        assertThat(rows.get(1).get("globalIndex")).isEqualTo(1);

        assertThat(rows.get(2).get("localIndex")).isEqualTo(2);
        assertThat(rows.get(2).get("globalIndex")).isEqualTo(2);
    }

    @Test
    @DisplayName("测试双重索引生成 - VM2（startIndex=2000）")
    void testAddDualIndices_VM2() {
        // Given: VM2的切片信息，startIndex=2000
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(2000)
                .endIndex(3999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(2)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // 创建批次数据（batchStartIndex=0，3行数据）
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("feature1", i * 1.0);
            rows.add(row);
        }

        // When
        dataDistributionService.addDualIndices(rows, 0, sliceInfo);

        // Then: 本地索引从0开始，全局索引从2000开始
        assertThat(rows.get(0).get("localIndex")).isEqualTo(0);
        assertThat(rows.get(0).get("globalIndex")).isEqualTo(2000);

        assertThat(rows.get(1).get("localIndex")).isEqualTo(1);
        assertThat(rows.get(1).get("globalIndex")).isEqualTo(2001);

        assertThat(rows.get(2).get("localIndex")).isEqualTo(2);
        assertThat(rows.get(2).get("globalIndex")).isEqualTo(2002);
    }

    @Test
    @DisplayName("测试双重索引生成 - 第二个批次（batchStartIndex=500）")
    void testAddDualIndices_SecondBatch() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(2000)
                .endIndex(3999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(2)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // 第二个批次，batchStartIndex=500
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            rows.add(new HashMap<>());
        }

        // When
        dataDistributionService.addDualIndices(rows, 500, sliceInfo);

        // Then: 本地索引从500开始，全局索引从2500开始
        assertThat(rows.get(0).get("localIndex")).isEqualTo(500);
        assertThat(rows.get(0).get("globalIndex")).isEqualTo(2500);

        assertThat(rows.get(1).get("localIndex")).isEqualTo(501);
        assertThat(rows.get(1).get("globalIndex")).isEqualTo(2501);

        assertThat(rows.get(2).get("localIndex")).isEqualTo(502);
        assertThat(rows.get(2).get("globalIndex")).isEqualTo(2502);
    }

    @Test
    @DisplayName("测试双重索引生成 - 空行列表")
    void testAddDualIndices_EmptyRows() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        List<Map<String, Object>> rows = new ArrayList<>();

        // When & Then: 空列表不应该抛出异常
        assertThatCode(() -> dataDistributionService.addDualIndices(rows, 0, sliceInfo))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("测试双重索引生成 - 异常情况：SliceInfo为null")
    void testAddDualIndices_ExceptionNullSliceInfo() {
        // Given
        List<Map<String, Object>> rows = Arrays.asList(new HashMap<>());

        // When & Then
        assertThatThrownBy(() -> dataDistributionService.addDualIndices(rows, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sliceInfo不能为空");
    }

    @Test
    @DisplayName("测试双重索引生成 - 异常情况：rows为null")
    void testAddDualIndices_ExceptionNullRows() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(0)
                .endIndex(1999)
                .sliceSamples(2000)
                .totalSamples(10000)
                .sliceIndex(1)
                .totalSlices(5)
                .allocationStrategy("IID")
                .build();

        // When & Then
        assertThatThrownBy(() -> dataDistributionService.addDualIndices(null, 0, sliceInfo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rows不能为空");
    }

    @Test
    @DisplayName("测试双重索引生成 - 验证索引关系一致性")
    void testAddDualIndices_VerifyIndexRelation() {
        // Given
        SliceInfo sliceInfo = SliceInfo.builder()
                .startIndex(5000)
                .endIndex(5999)
                .sliceSamples(1000)
                .totalSamples(10000)
                .sliceIndex(3)
                .totalSlices(10)
                .allocationStrategy("IID")
                .build();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rows.add(new HashMap<>());
        }

        // When
        dataDistributionService.addDualIndices(rows, 100, sliceInfo);

        // Then: 验证所有行的索引关系
        for (int i = 0; i < rows.size(); i++) {
            int localIndex = (Integer) rows.get(i).get("localIndex");
            int globalIndex = (Integer) rows.get(i).get("globalIndex");

            // 验证：globalIndex = startIndex + localIndex
            assertThat(globalIndex).isEqualTo(sliceInfo.getStartIndex() + localIndex);
            assertThat(globalIndex).isEqualTo(sliceInfo.toGlobalIndex(localIndex));
        }
    }
}