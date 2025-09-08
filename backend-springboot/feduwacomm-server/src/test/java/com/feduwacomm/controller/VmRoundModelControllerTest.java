package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.VmRoundModelBestQueryDTO;
import com.feduwacomm.dto.VmRoundModelQueryDTO;
import com.feduwacomm.dto.VmRoundModelTrendQueryDTO;
import com.feduwacomm.service.VmRoundModelService;
import com.feduwacomm.vo.VmRoundModelBestVO;
import com.feduwacomm.vo.VmRoundModelTrendVO;
import com.feduwacomm.vo.VmRoundModelVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VM轮次模型控制器测试类
 * 测试本地模型查询、分析等功能
 */
@SpringBootTest
@AutoConfigureMockMvc
public class VmRoundModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VmRoundModelService vmRoundModelService;

    @Autowired
    private ObjectMapper objectMapper;

    private VmRoundModelVO sampleVmRoundModelVO;
    private VmRoundModelTrendVO sampleTrendVO;
    private VmRoundModelBestVO sampleBestVO;
    private PageResult<VmRoundModelVO> samplePageResult;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(vmRoundModelService);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 准备VmRoundModelVO
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.85);
        metrics.put("loss", 0.25);
        metrics.put("f1_score", 0.82);

        Map<String, Object> modelInfo = new HashMap<>();
        modelInfo.put("modelSize", 1024000);
        modelInfo.put("parameters", 50000);
        modelInfo.put("layers", 5);

        sampleVmRoundModelVO = new VmRoundModelVO();
        sampleVmRoundModelVO.setVmRoundModelId("vm-round-model-001");
        sampleVmRoundModelVO.setTaskId("task-001");
        sampleVmRoundModelVO.setRoundNumber(1);
        sampleVmRoundModelVO.setVmId("vm-001");
        sampleVmRoundModelVO.setModelJson(modelInfo);
        sampleVmRoundModelVO.setMetrics(metrics);
        sampleVmRoundModelVO.setCreatedAt(LocalDateTime.now());

        // 准备分页结果
        List<VmRoundModelVO> records = Arrays.asList(sampleVmRoundModelVO);
        samplePageResult = PageResult.<VmRoundModelVO>builder()
                .records(records)
                .total(1L)
                .pages(1L)
                .current(1L)
                .size(10L)
                .build();

        // 准备趋势数据
        sampleTrendVO = new VmRoundModelTrendVO();
        sampleTrendVO.setTaskId("task-001");
        sampleTrendVO.setVmId("vm-001");
        sampleTrendVO.setMetric("accuracy");
        // 创建趋势点
        List<VmRoundModelTrendVO.TrendPoint> trendPoints = Arrays.asList(
                new VmRoundModelTrendVO.TrendPoint(1, new java.math.BigDecimal("0.75")),
                new VmRoundModelTrendVO.TrendPoint(2, new java.math.BigDecimal("0.80")),
                new VmRoundModelTrendVO.TrendPoint(3, new java.math.BigDecimal("0.85"))
        );
        sampleTrendVO.setTrend(trendPoints);

        // 准备最佳/离群结果
        sampleBestVO = new VmRoundModelBestVO();
        sampleBestVO.setTaskId("task-001");
        sampleBestVO.setMetric("accuracy");
        sampleBestVO.setType("best");
        VmRoundModelBestVO.BestResult bestResult = new VmRoundModelBestVO.BestResult();
        bestResult.setVmRoundModelId("vm-round-model-001");
        bestResult.setRoundNumber(1);
        bestResult.setVmId("vm-001");
        bestResult.setValue(new java.math.BigDecimal("0.95"));
        sampleBestVO.setResult(bestResult);
    }

    /**
     * 测试本地模型结果分页查询 - 成功场景
     */
    @Test
    void testQueryVmRoundModels_Success() throws Exception {
        // Mock服务层方法
        when(vmRoundModelService.queryVmRoundModels(any(VmRoundModelQueryDTO.class)))
                .thenReturn(samplePageResult);

        mockMvc.perform(get("/api/model/vm-round-models")
                        .param("taskId", "task-001")
                        .param("roundNumber", "1")
                        .param("vmId", "vm-001")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].vmRoundModelId").value("vm-round-model-001"))
                .andExpect(jsonPath("$.data.records[0].taskId").value("task-001"))
                .andExpect(jsonPath("$.data.records[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data.records[0].vmId").value("vm-001"))
                .andExpect(jsonPath("$.data.records[0].metrics.accuracy").value(0.85));

        // 验证服务调用
        verify(vmRoundModelService).queryVmRoundModels(any(VmRoundModelQueryDTO.class));
    }

    /**
     * 测试本地模型结果分页查询 - 使用默认分页参数
     */
    @Test
    void testQueryVmRoundModels_DefaultPagination() throws Exception {
        when(vmRoundModelService.queryVmRoundModels(any(VmRoundModelQueryDTO.class)))
                .thenReturn(samplePageResult);

        mockMvc.perform(get("/api/model/vm-round-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"));

        // 验证默认分页参数的使用
        verify(vmRoundModelService).queryVmRoundModels(argThat(dto -> 
                dto.getPage() == 1 && dto.getSize() == 10));
    }

    /**
     * 测试本地模型结果分页查询 - 服务异常
     */
    @Test
    void testQueryVmRoundModels_ServiceException() throws Exception {
        // Mock服务层抛出异常
        when(vmRoundModelService.queryVmRoundModels(any(VmRoundModelQueryDTO.class)))
                .thenThrow(new RuntimeException("数据库连接异常"));

        mockMvc.perform(get("/api/model/vm-round-models")
                        .param("taskId", "task-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("查询失败: 数据库连接异常"));

        verify(vmRoundModelService).queryVmRoundModels(any(VmRoundModelQueryDTO.class));
    }

    /**
     * 测试本地模型结果详情查询 - 成功场景
     */
    @Test
    void testGetVmRoundModelById_Success() throws Exception {
        String vmRoundModelId = "vm-round-model-001";
        
        when(vmRoundModelService.getVmRoundModelById(vmRoundModelId))
                .thenReturn(sampleVmRoundModelVO);

        mockMvc.perform(get("/api/model/vm-round-models/{vmRoundModelId}", vmRoundModelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.vmRoundModelId").value(vmRoundModelId))
                .andExpect(jsonPath("$.data.taskId").value("task-001"))
                .andExpect(jsonPath("$.data.metrics.accuracy").value(0.85))
                .andExpect(jsonPath("$.data.metrics.loss").value(0.25));

        verify(vmRoundModelService).getVmRoundModelById(vmRoundModelId);
    }

    /**
     * 测试本地模型结果详情查询 - 未找到
     */
    @Test
    void testGetVmRoundModelById_NotFound() throws Exception {
        String vmRoundModelId = "non-existent-id";
        
        when(vmRoundModelService.getVmRoundModelById(vmRoundModelId))
                .thenReturn(null);

        mockMvc.perform(get("/api/model/vm-round-models/{vmRoundModelId}", vmRoundModelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("未找到指定的模型结果"));

        verify(vmRoundModelService).getVmRoundModelById(vmRoundModelId);
    }

    /**
     * 测试本地模型结果详情查询 - 服务异常
     */
    @Test
    void testGetVmRoundModelById_ServiceException() throws Exception {
        String vmRoundModelId = "vm-round-model-001";
        
        when(vmRoundModelService.getVmRoundModelById(vmRoundModelId))
                .thenThrow(new RuntimeException("数据库查询异常"));

        mockMvc.perform(get("/api/model/vm-round-models/{vmRoundModelId}", vmRoundModelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("查询失败: 数据库查询异常"));

        verify(vmRoundModelService).getVmRoundModelById(vmRoundModelId);
    }

    /**
     * 测试本地模型训练指标趋势 - 成功场景
     */
    @Test
    void testGetVmRoundModelTrend_Success() throws Exception {
        when(vmRoundModelService.getVmRoundModelTrend(any(VmRoundModelTrendQueryDTO.class)))
                .thenReturn(sampleTrendVO);

        mockMvc.perform(get("/api/model/vm-round-models/metrics/trend")
                        .param("taskId", "task-001")
                        .param("vmId", "vm-001")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.taskId").value("task-001"))
                .andExpect(jsonPath("$.data.vmId").value("vm-001"))
                .andExpect(jsonPath("$.data.metric").value("accuracy"))
                .andExpect(jsonPath("$.data.trend").isArray())
                .andExpect(jsonPath("$.data.trend[0].roundNumber").value(1))
                .andExpect(jsonPath("$.data.trend[0].value").value(0.75));

        verify(vmRoundModelService).getVmRoundModelTrend(any(VmRoundModelTrendQueryDTO.class));
    }

    /**
     * 测试本地模型训练指标趋势 - 不支持的指标类型
     */
    @Test
    void testGetVmRoundModelTrend_InvalidMetric() throws Exception {
        mockMvc.perform(get("/api/model/vm-round-models/metrics/trend")
                        .param("taskId", "task-001")
                        .param("vmId", "vm-001")
                        .param("metric", "invalid-metric"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的指标类型: invalid-metric"));

        // 服务不应该被调用
        verify(vmRoundModelService, never()).getVmRoundModelTrend(any(VmRoundModelTrendQueryDTO.class));
    }

    /**
     * 测试本地模型训练指标趋势 - 支持的指标类型
     */
    @Test
    void testGetVmRoundModelTrend_ValidMetrics() throws Exception {
        when(vmRoundModelService.getVmRoundModelTrend(any(VmRoundModelTrendQueryDTO.class)))
                .thenReturn(sampleTrendVO);

        // 测试accuracy指标
        mockMvc.perform(get("/api/model/vm-round-models/metrics/trend")
                        .param("taskId", "task-001")
                        .param("vmId", "vm-001")
                        .param("metric", "accuracy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 测试loss指标
        mockMvc.perform(get("/api/model/vm-round-models/metrics/trend")
                        .param("taskId", "task-001")
                        .param("vmId", "vm-001")
                        .param("metric", "loss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(vmRoundModelService, times(2)).getVmRoundModelTrend(any(VmRoundModelTrendQueryDTO.class));
    }

    /**
     * 测试本地模型最佳/离群查询 - 成功场景
     */
    @Test
    void testGetVmRoundModelBest_Success() throws Exception {
        when(vmRoundModelService.getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class)))
                .thenReturn(sampleBestVO);

        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "accuracy")
                        .param("type", "best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("查询成功"))
                .andExpect(jsonPath("$.data.taskId").value("task-001"))
                .andExpect(jsonPath("$.data.metric").value("accuracy"))
                .andExpect(jsonPath("$.data.type").value("best"))
                .andExpect(jsonPath("$.data.result.value").value(0.95))
                .andExpect(jsonPath("$.data.result.vmRoundModelId").value("vm-round-model-001"));

        verify(vmRoundModelService).getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class));
    }

    /**
     * 测试本地模型最佳/离群查询 - 不支持的指标类型
     */
    @Test
    void testGetVmRoundModelBest_InvalidMetric() throws Exception {
        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "invalid-metric")
                        .param("type", "best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的指标类型: invalid-metric"));

        verify(vmRoundModelService, never()).getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class));
    }

    /**
     * 测试本地模型最佳/离群查询 - 不支持的查询类型
     */
    @Test
    void testGetVmRoundModelBest_InvalidType() throws Exception {
        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "accuracy")
                        .param("type", "invalid-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的查询类型: invalid-type"));

        verify(vmRoundModelService, never()).getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class));
    }

    /**
     * 测试本地模型最佳/离群查询 - 支持的查询类型
     */
    @Test
    void testGetVmRoundModelBest_ValidTypes() throws Exception {
        when(vmRoundModelService.getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class)))
                .thenReturn(sampleBestVO);

        // 测试best类型
        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "accuracy")
                        .param("type", "best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 测试outlier类型
        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "accuracy")
                        .param("type", "outlier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(vmRoundModelService, times(2)).getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class));
    }

    /**
     * 测试本地模型最佳/离群查询 - 未找到结果
     */
    @Test
    void testGetVmRoundModelBest_NotFound() throws Exception {
        when(vmRoundModelService.getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class)))
                .thenReturn(null);

        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001")
                        .param("metric", "accuracy")
                        .param("type", "best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("未找到符合条件的模型结果"));

        verify(vmRoundModelService).getVmRoundModelBest(any(VmRoundModelBestQueryDTO.class));
    }

    /**
     * 测试不支持的HTTP方法
     */
    @Test
    void testUnsupportedHttpMethods() throws Exception {
        // 分页查询只支持GET
        mockMvc.perform(post("/api/model/vm-round-models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // 详情查询只支持GET
        mockMvc.perform(put("/api/model/vm-round-models/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // 趋势查询只支持GET
        mockMvc.perform(delete("/api/model/vm-round-models/metrics/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // 最佳/离群查询只支持GET
        mockMvc.perform(patch("/api/model/vm-round-models/metrics/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试缺少必需参数
     */
    @Test
    void testMissingRequiredParams() throws Exception {
        // 趋势查询缺少必需参数
        mockMvc.perform(get("/api/model/vm-round-models/metrics/trend")
                        .param("taskId", "task-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)); // 全局异常处理器返回500

        // 最佳/离群查询缺少必需参数
        mockMvc.perform(get("/api/model/vm-round-models/metrics/best")
                        .param("taskId", "task-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)); // 全局异常处理器返回500
    }

    /**
     * 测试CORS头信息
     */
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/model/vm-round-models")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }
}