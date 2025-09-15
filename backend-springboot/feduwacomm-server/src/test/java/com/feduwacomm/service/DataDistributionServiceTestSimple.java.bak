package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.entity.DataDistribution;
import com.feduwacomm.entity.DataDistributionDetail;
import com.feduwacomm.mapper.DataDistributionMapper;
import com.feduwacomm.mapper.DataDistributionDetailMapper;
import com.feduwacomm.service.impl.DataDistributionServiceImpl;
import com.feduwacomm.testdata.TestDataBuilder;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.DataDistributionTaskVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DataDistributionService简化单元测试
 * 重点测试核心数据分发功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("数据分发服务简化测试")
class DataDistributionServiceTestSimple {

    @Mock
    private DataDistributionMapper distributionMapper;

    @Mock
    private DataDistributionDetailMapper distributionDetailMapper;

    @Mock
    private UuidUtil uuidUtil;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DataDistributionServiceImpl dataDistributionService;

    private String testDistributionId;
    private String testTaskId;
    private String testCreatedBy;
    private DataDistributionDTO testDistributionDTO;
    private DataDistribution testDistribution;

    @BeforeEach
    void setUp() {
        testDistributionId = "dist-001";
        testTaskId = "task-001";
        testCreatedBy = "test-user";
        
        // 构建测试数据
        testDistributionDTO = DataDistributionDTO.builder()
                .taskId(testTaskId)
                .strategy("RANDOM")
                .datasetIds(Arrays.asList("dataset1", "dataset2"))
                .vmIds(Arrays.asList("vm1", "vm2", "vm3"))
                .build();
                
        testDistribution = createTestDistribution();
    }

    @Test
    @DisplayName("创建数据分发任务 - 成功创建")
    void testCreateDistributionTask_Success() {
        // Given
        when(uuidUtil.generateUuid()).thenReturn(testDistributionId);
        when(distributionMapper.insert(any(DataDistribution.class))).thenReturn(1);
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);

        // When
        DataDistributionTaskVO result = dataDistributionService.createDistributionTask(testDistributionDTO, testCreatedBy);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(testDistributionId, result.getDistributionId());
        assertEquals(testTaskId, result.getTaskId());
        assertEquals("CREATED", result.getStatus());
        
        verify(uuidUtil).generateUuid();
        verify(distributionMapper).insert(any(DataDistribution.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("开始数据分发 - 成功启动")
    void testStartDistribution_Success() {
        // Given
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionMapper.update(any(DataDistribution.class))).thenReturn(1);

        // When
        DataDistributionTaskVO result = dataDistributionService.startDistribution(testDistributionId, testCreatedBy);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(testDistributionId, result.getDistributionId());
        
        verify(distributionMapper).selectById(testDistributionId);
        verify(distributionMapper).update(any(DataDistribution.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("暂停数据分发 - 成功暂停")
    void testPauseDistribution_Success() {
        // Given
        testDistribution.setStatus("IN_PROGRESS");
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionMapper.update(any(DataDistribution.class))).thenReturn(1);

        // When
        DataDistributionTaskVO result = dataDistributionService.pauseDistribution(testDistributionId, testCreatedBy);

        // Then
        assertNotNull(result, "Result should not be null");
        verify(distributionMapper).update(any(DataDistribution.class));
    }

    @Test
    @DisplayName("获取分发任务详情 - 任务存在")
    void testGetDistributionTask_TaskExists() {
        // Given
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionDetailMapper.selectByDistributionId(testDistributionId))
                .thenReturn(Arrays.asList(createTestDistributionDetail()));

        // When
        DataDistributionTaskVO result = dataDistributionService.getDistributionTask(testDistributionId);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(testDistributionId, result.getDistributionId());
        assertEquals(testTaskId, result.getTaskId());
        
        verify(distributionMapper).selectById(testDistributionId);
        verify(distributionDetailMapper).selectByDistributionId(testDistributionId);
    }

    @Test
    @DisplayName("获取分发任务详情 - 任务不存在")
    void testGetDistributionTask_TaskNotExists() {
        // Given
        when(distributionMapper.selectById("nonexistent")).thenReturn(null);

        // When
        DataDistributionTaskVO result = dataDistributionService.getDistributionTask("nonexistent");

        // Then
        assertNull(result, "Result should be null for non-existent task");
        verify(distributionMapper).selectById("nonexistent");
    }

    @Test
    @DisplayName("获取任务的分发记录 - 存在记录")
    void testGetTaskDistributions_HasRecords() {
        // Given
        List<DataDistribution> distributions = Arrays.asList(testDistribution);
        when(distributionMapper.selectByTaskId(testTaskId)).thenReturn(distributions);
        when(distributionDetailMapper.selectByDistributionId(any()))
                .thenReturn(Arrays.asList(createTestDistributionDetail()));

        // When
        List<DataDistributionTaskVO> result = dataDistributionService.getTaskDistributions(testTaskId);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should have one distribution");
        assertEquals(testDistributionId, result.get(0).getDistributionId());
        
        verify(distributionMapper).selectByTaskId(testTaskId);
    }

    @Test
    @DisplayName("获取分页任务列表 - 基本分页")
    void testGetDistributionTasksPaged_BasicPaging() {
        // Given
        List<DataDistribution> distributions = Arrays.asList(testDistribution);
        when(distributionMapper.selectPaged(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(distributions);
        when(distributionMapper.countByConditions(any(), any(), any()))
                .thenReturn(1L);

        // When
        PageResult<DataDistributionTaskVO> result = dataDistributionService
                .getDistributionTasksPaged(testTaskId, "CREATED", "RANDOM", 1, 10);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1L, result.getTotal(), "Total should be 1");
        assertEquals(1, result.getRecords().size(), "Should have one record");
        
        verify(distributionMapper).selectPaged(testTaskId, "CREATED", "RANDOM", 0, 10);
        verify(distributionMapper).countByConditions(testTaskId, "CREATED", "RANDOM");
    }

    @Test
    @DisplayName("删除分发任务 - 成功删除")
    void testDeleteDistributionTask_Success() {
        // Given
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionMapper.deleteById(testDistributionId)).thenReturn(1);
        when(distributionDetailMapper.deleteByDistributionId(testDistributionId)).thenReturn(3);

        // When
        boolean result = dataDistributionService.deleteDistributionTask(testDistributionId, testCreatedBy);

        // Then
        assertTrue(result, "Delete should succeed");
        verify(distributionMapper).deleteById(testDistributionId);
        verify(distributionDetailMapper).deleteByDistributionId(testDistributionId);
    }

    @Test
    @DisplayName("获取分发进度 - 计算进度")
    void testGetDistributionProgress_CalculateProgress() {
        // Given
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionDetailMapper.selectByDistributionId(testDistributionId))
                .thenReturn(Arrays.asList(
                        createTestDistributionDetail("COMPLETED"),
                        createTestDistributionDetail("COMPLETED"),
                        createTestDistributionDetail("IN_PROGRESS")
                ));

        // When
        Double progress = dataDistributionService.getDistributionProgress(testDistributionId);

        // Then
        assertNotNull(progress, "Progress should not be null");
        assertTrue(progress >= 0.0 && progress <= 100.0, "Progress should be between 0 and 100");
        // 2/3完成 = 约66.67%
        assertEquals(66.67, progress, 0.1, "Progress should be approximately 66.67%");
    }

    @Test
    @DisplayName("验证分发完整性 - 完整性良好")
    void testVerifyDistributionIntegrity_IntegrityGood() {
        // Given
        when(distributionMapper.selectById(testDistributionId)).thenReturn(testDistribution);
        when(distributionDetailMapper.selectByDistributionId(testDistributionId))
                .thenReturn(Arrays.asList(
                        createTestDistributionDetail("COMPLETED"),
                        createTestDistributionDetail("COMPLETED")
                ));

        // When
        boolean integrity = dataDistributionService.verifyDistributionIntegrity(testDistributionId);

        // Then
        assertTrue(integrity, "Integrity verification should pass");
        verify(distributionMapper).selectById(testDistributionId);
        verify(distributionDetailMapper).selectByDistributionId(testDistributionId);
    }

    @Test
    @DisplayName("预览分发计划 - 生成预览")
    void testPreviewDistributionPlan_GeneratePreview() {
        // Given - 预览不需要持久化数据

        // When
        DataDistributionTaskVO preview = dataDistributionService.previewDistributionPlan(testDistributionDTO);

        // Then
        assertNotNull(preview, "Preview should not be null");
        assertEquals(testTaskId, preview.getTaskId());
        assertEquals("PREVIEW", preview.getStatus());
        assertNotNull(preview.getVmDataInfos(), "VM data infos should not be null");
        
        // 预览不应该调用insert方法
        verify(distributionMapper, never()).insert(any());
    }

    /**
     * 创建测试用的DataDistribution对象
     */
    private DataDistribution createTestDistribution() {
        return DataDistribution.builder()
                .id(testDistributionId)
                .taskId(testTaskId)
                .strategy("RANDOM")
                .status("CREATED")
                .totalFiles(100)
                .totalSize(1024L * 1024L) // 1MB
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(testCreatedBy)
                .build();
    }

    /**
     * 创建测试用的DataDistributionDetail对象
     */
    private DataDistributionDetail createTestDistributionDetail() {
        return createTestDistributionDetail("COMPLETED");
    }

    /**
     * 创建指定状态的测试用DataDistributionDetail对象
     */
    private DataDistributionDetail createTestDistributionDetail(String status) {
        return DataDistributionDetail.builder()
                .id("detail-" + System.nanoTime())
                .distributionId(testDistributionId)
                .vmId("vm1")
                .datasetId("dataset1")
                .fileCount(10)
                .fileSize(102400L) // 100KB
                .status(status)
                .distributedAt(LocalDateTime.now())
                .build();
    }
}