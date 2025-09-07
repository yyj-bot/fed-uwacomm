package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.VmRoundModelBestQueryDTO;
import com.feduwacomm.dto.VmRoundModelQueryDTO;
import com.feduwacomm.dto.VmRoundModelTrendQueryDTO;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.mapper.VmRoundModelMapper;
import com.feduwacomm.service.impl.VmRoundModelServiceImpl;
import com.feduwacomm.vo.VmRoundModelBestVO;
import com.feduwacomm.vo.VmRoundModelTrendVO;
import com.feduwacomm.vo.VmRoundModelVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VmRoundModelService单元测试类
 * 测试虚拟机轮次模型管理服务的各种功能
 */
@ExtendWith(MockitoExtension.class)
public class VmRoundModelServiceTest {

    @Mock
    private VmRoundModelMapper vmRoundModelMapper;

    @InjectMocks
    private VmRoundModelServiceImpl vmRoundModelService;

    private VmRoundModelQueryDTO queryDTO;
    private VmRoundModelTrendQueryDTO trendQueryDTO;
    private VmRoundModelBestQueryDTO bestQueryDTO;
    private VmRoundModel vmRoundModel;
    private VmRoundModelVO vmRoundModelVO;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(vmRoundModelMapper);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 准备查询DTO
        queryDTO = new VmRoundModelQueryDTO();
        queryDTO.setTaskId("task-001");
        queryDTO.setRoundNumber(1);
        queryDTO.setVmId("vm-001");
        queryDTO.setPage(1);
        queryDTO.setSize(10);

        // 准备趋势查询DTO
        trendQueryDTO = new VmRoundModelTrendQueryDTO();
        trendQueryDTO.setTaskId("task-001");
        trendQueryDTO.setVmId("vm-001");
        trendQueryDTO.setMetric("accuracy");

        // 准备最佳结果查询DTO
        bestQueryDTO = new VmRoundModelBestQueryDTO();
        bestQueryDTO.setTaskId("task-001");
        bestQueryDTO.setMetric("accuracy");
        bestQueryDTO.setType("best");

        // 准备模型实体 - 根据实际Entity字段修复
        vmRoundModel = new VmRoundModel();
        vmRoundModel.setId("vm-round-model-001");
        vmRoundModel.setTaskId("task-001");
        vmRoundModel.setRoundNumber(1);
        vmRoundModel.setVmId("vm-001");
        vmRoundModel.setAccuracy(new BigDecimal("0.85"));
        vmRoundModel.setLoss(new BigDecimal("0.25"));
        vmRoundModel.setParameters("{\"modelSize\": 1024000, \"parameters\": 50000}");
        vmRoundModel.setCreatedAt(LocalDateTime.now());

        // 准备VO
        Map<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("modelSize", 1024000);
        modelInfo.put("parameters", 50000);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.85);
        metrics.put("loss", 0.25);

        vmRoundModelVO = new VmRoundModelVO();
        vmRoundModelVO.setVmRoundModelId("vm-round-model-001");
        vmRoundModelVO.setTaskId("task-001");
        vmRoundModelVO.setRoundNumber(1);
        vmRoundModelVO.setVmId("vm-001");
        vmRoundModelVO.setModelJson(modelInfo);
        vmRoundModelVO.setMetrics(metrics);
        vmRoundModelVO.setCreatedAt(LocalDateTime.now());
    }

    /**
     * 测试分页查询虚拟机轮次模型 - 成功场景
     */
    @Test
    void testQueryVmRoundModels_Success() {
        // 准备mock数据
        List<VmRoundModel> models = Arrays.asList(vmRoundModel);
        when(vmRoundModelMapper.selectByPage(anyString(), any(), anyString(), any(), any())).thenReturn(models);
        when(vmRoundModelMapper.countByConditions(anyString(), any(), anyString())).thenReturn(1L);

        // 执行测试
        PageResult<VmRoundModelVO> result = vmRoundModelService.queryVmRoundModels(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPages());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertEquals(1, result.getRecords().size());

        VmRoundModelVO resultVO = result.getRecords().get(0);
        assertEquals(vmRoundModel.getId(), resultVO.getVmRoundModelId());
        assertEquals(vmRoundModel.getTaskId(), resultVO.getTaskId());
        assertEquals(vmRoundModel.getVmId(), resultVO.getVmId());

        // 验证mock调用
        verify(vmRoundModelMapper).selectByPage(anyString(), any(), anyString(), any(), any());
        verify(vmRoundModelMapper).countByConditions(anyString(), any(), anyString());
    }

    /**
     * 测试分页查询虚拟机轮次模型 - 空结果
     */
    @Test
    void testQueryVmRoundModels_EmptyResult() {
        // 准备mock数据
        when(vmRoundModelMapper.selectByPage(anyString(), any(), anyString(), any(), any())).thenReturn(Collections.emptyList());
        when(vmRoundModelMapper.countByConditions(anyString(), any(), anyString())).thenReturn(0L);

        // 执行测试
        PageResult<VmRoundModelVO> result = vmRoundModelService.queryVmRoundModels(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(0L, result.getPages());
        assertEquals(1L, result.getCurrent());
        assertEquals(10L, result.getSize());
        assertTrue(result.getRecords().isEmpty());

        // 验证mock调用
        verify(vmRoundModelMapper).selectByPage(anyString(), any(), anyString(), any(), any());
        verify(vmRoundModelMapper).countByConditions(anyString(), any(), anyString());
    }

    /**
     * 测试根据ID查询模型详情 - 成功场景
     */
    @Test
    void testGetVmRoundModelById_Success() {
        String modelId = "vm-round-model-001";
        
        // 准备mock数据
        when(vmRoundModelMapper.selectById(modelId)).thenReturn(vmRoundModel);

        // 执行测试
        VmRoundModelVO result = vmRoundModelService.getVmRoundModelById(modelId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmRoundModel.getId(), result.getVmRoundModelId());
        assertEquals(vmRoundModel.getTaskId(), result.getTaskId());
        assertEquals(vmRoundModel.getVmId(), result.getVmId());
        assertEquals(vmRoundModel.getRoundNumber(), result.getRoundNumber());

        // 验证mock调用
        verify(vmRoundModelMapper).selectById(modelId);
    }

    /**
     * 测试根据ID查询模型详情 - 模型不存在
     */
    @Test
    void testGetVmRoundModelById_NotFound() {
        String modelId = "non-existent-model";
        
        // 准备mock数据
        when(vmRoundModelMapper.selectById(modelId)).thenReturn(null);

        // 执行测试
        VmRoundModelVO result = vmRoundModelService.getVmRoundModelById(modelId);
        
        // 验证结果
        assertNull(result);

        // 验证mock调用
        verify(vmRoundModelMapper).selectById(modelId);
    }

    /**
     * 测试查询模型指标趋势 - 成功场景
     */
    @Test
    void testGetVmRoundModelTrend_Success() {
        // 准备趋势数据
        List<VmRoundModelMapper.VmRoundModelTrendPoint> trendData = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            VmRoundModelMapper.VmRoundModelTrendPoint point = new VmRoundModelMapper.VmRoundModelTrendPoint();
            point.setRoundNumber(i);
            point.setValue(new BigDecimal(String.valueOf(0.70 + i * 0.05)));
            trendData.add(point);
        }

        // 准备mock数据
        when(vmRoundModelMapper.selectTrendByTaskAndVm(anyString(), anyString(), anyString())).thenReturn(trendData);

        // 执行测试
        VmRoundModelTrendVO result = vmRoundModelService.getVmRoundModelTrend(trendQueryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(trendQueryDTO.getTaskId(), result.getTaskId());
        assertEquals(trendQueryDTO.getVmId(), result.getVmId());
        assertEquals(trendQueryDTO.getMetric(), result.getMetric());
        assertEquals(3, result.getTrend().size());
        
        // 验证趋势数据
        assertEquals(1, result.getTrend().get(0).getRoundNumber());
        assertEquals(new BigDecimal("0.75"), result.getTrend().get(0).getValue());
        assertEquals(3, result.getTrend().get(2).getRoundNumber());
        assertEquals(new BigDecimal("0.85"), result.getTrend().get(2).getValue());

        // 验证mock调用
        verify(vmRoundModelMapper).selectTrendByTaskAndVm(anyString(), anyString(), anyString());
    }

    /**
     * 测试查询模型指标趋势 - 空结果
     */
    @Test
    void testGetVmRoundModelTrend_EmptyResult() {
        // 准备mock数据
        when(vmRoundModelMapper.selectTrendByTaskAndVm(anyString(), anyString(), anyString())).thenReturn(Collections.emptyList());

        // 执行测试
        VmRoundModelTrendVO result = vmRoundModelService.getVmRoundModelTrend(trendQueryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(trendQueryDTO.getTaskId(), result.getTaskId());
        assertEquals(trendQueryDTO.getVmId(), result.getVmId());
        assertEquals(trendQueryDTO.getMetric(), result.getMetric());
        assertTrue(result.getTrend().isEmpty());

        // 验证mock调用
        verify(vmRoundModelMapper).selectTrendByTaskAndVm(anyString(), anyString(), anyString());
    }

    /**
     * 测试查询最佳/离群模型 - 成功场景
     */
    @Test
    void testGetVmRoundModelBest_Success() {
        // 准备最佳结果数据
        VmRoundModelMapper.VmRoundModelBestResult bestResult = new VmRoundModelMapper.VmRoundModelBestResult();
        bestResult.setVmRoundModelId("vm-round-model-001");
        bestResult.setRoundNumber(3);
        bestResult.setVmId("vm-001");
        bestResult.setValue(new BigDecimal("0.95"));

        // 准备mock数据
        when(vmRoundModelMapper.selectBestByTaskAndMetric(anyString(), anyString())).thenReturn(bestResult);

        // 执行测试
        VmRoundModelBestVO result = vmRoundModelService.getVmRoundModelBest(bestQueryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(bestQueryDTO.getTaskId(), result.getTaskId());
        assertEquals(bestQueryDTO.getMetric(), result.getMetric());
        assertEquals(bestQueryDTO.getType(), result.getType());
        assertNotNull(result.getResult());
        assertEquals("vm-round-model-001", result.getResult().getVmRoundModelId());
        assertEquals(3, result.getResult().getRoundNumber());
        assertEquals("vm-001", result.getResult().getVmId());
        assertEquals(new BigDecimal("0.95"), result.getResult().getValue());

        // 验证mock调用
        verify(vmRoundModelMapper).selectBestByTaskAndMetric(anyString(), anyString());
    }

    /**
     * 测试查询最佳/离群模型 - 无结果
     */
    @Test
    void testGetVmRoundModelBest_NoResult() {
        // 准备mock数据
        when(vmRoundModelMapper.selectBestByTaskAndMetric(anyString(), anyString())).thenReturn(null);

        // 执行测试
        VmRoundModelBestVO result = vmRoundModelService.getVmRoundModelBest(bestQueryDTO);

        // 验证结果
        assertNull(result);

        // 验证mock调用
        verify(vmRoundModelMapper).selectBestByTaskAndMetric(anyString(), anyString());
    }
}