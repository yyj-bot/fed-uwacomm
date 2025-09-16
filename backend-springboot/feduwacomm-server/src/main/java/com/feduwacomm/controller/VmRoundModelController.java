package com.feduwacomm.controller;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.VmRoundModelBestQueryDTO;
import com.feduwacomm.dto.VmRoundModelQueryDTO;
import com.feduwacomm.dto.VmRoundModelTrendQueryDTO;
import com.feduwacomm.service.VmRoundModelService;
import com.feduwacomm.vo.VmRoundModelBestVO;
import com.feduwacomm.vo.VmRoundModelTrendVO;
import com.feduwacomm.vo.VmRoundModelVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 虚拟机轮次模型结果控制器
 * 提供本地模型查询、分析等功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/model/vm-round-models")
@CrossOrigin(origins = "*")
public class VmRoundModelController {

    private static final Logger logger = LoggerFactory.getLogger(VmRoundModelController.class);

    @Autowired
    private VmRoundModelService vmRoundModelService;

    /**
     * 本地模型结果分页查询
     * 按任务/轮次/虚拟机筛选本地模型结果，分页返回
     *
     * @param taskId 任务ID
     * @param roundNumber 轮数
     * @param vmId 虚拟机ID
     * @param page 页码，默认1
     * @param size 页大小，默认10
     * @return 分页查询结果
     */
    @GetMapping
    public Result<PageResult<VmRoundModelVO>> queryVmRoundModels(
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) Integer roundNumber,
            @RequestParam(required = false) String vmId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        logger.info("分页查询本地模型结果: taskId={}, roundNumber={}, vmId={}, page={}, size={}",
                taskId, roundNumber, vmId, page, size);

        try {
            VmRoundModelQueryDTO queryDTO = new VmRoundModelQueryDTO();
            queryDTO.setTaskId(taskId);
            queryDTO.setRoundNumber(roundNumber);
            queryDTO.setVmId(vmId);
            queryDTO.setPage(page);
            queryDTO.setSize(size);

            PageResult<VmRoundModelVO> result = vmRoundModelService.queryVmRoundModels(queryDTO);
            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("分页查询本地模型结果失败", e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 本地模型结果详情查询
     * 查询单台虚拟机单轮的本地模型及度量
     *
     * @param vmRoundModelId 虚拟机轮次模型ID
     * @return 模型结果详情
     */
    @GetMapping("/{vmRoundModelId}")
    public Result<VmRoundModelVO> getVmRoundModelById(@PathVariable String vmRoundModelId) {
        logger.info("查询本地模型结果详情: vmRoundModelId={}", vmRoundModelId);

        try {
            VmRoundModelVO result = vmRoundModelService.getVmRoundModelById(vmRoundModelId);
            if (result == null) {
                return Result.failure(404, "未找到指定的模型结果");
            }

            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("查询本地模型结果详情失败: vmRoundModelId={}", vmRoundModelId, e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 本地模型训练指标趋势
     * 查询某虚拟机在某任务下的训练指标趋势
     *
     * @param taskId 任务ID
     * @param vmId 虚拟机ID
     * @param metric 指标名称
     * @return 趋势数据
     */
    @GetMapping("/metrics/trend")
    public Result<VmRoundModelTrendVO> getVmRoundModelTrend(
            @RequestParam String taskId,
            @RequestParam String vmId,
            @RequestParam String metric) {

        logger.info("查询本地模型训练指标趋势: taskId={}, vmId={}, metric={}", taskId, vmId, metric);

        try {
            // 验证指标类型
            if (!isValidMetric(metric)) {
                return Result.failure(400, "不支持的指标类型: " + metric);
            }

            VmRoundModelTrendQueryDTO queryDTO = new VmRoundModelTrendQueryDTO();
            queryDTO.setTaskId(taskId);
            queryDTO.setVmId(vmId);
            queryDTO.setMetric(metric);

            VmRoundModelTrendVO result = vmRoundModelService.getVmRoundModelTrend(queryDTO);
            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("查询本地模型训练指标趋势失败: taskId={}, vmId={}, metric={}",
                    taskId, vmId, metric, e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 本地模型最佳/离群查询
     * 查询最佳/离群本地模型
     *
     * @param taskId 任务ID
     * @param metric 指标名称
     * @param type 类型（best/outlier）
     * @return 最佳/离群结果
     */
    @GetMapping("/metrics/best")
    public Result<VmRoundModelBestVO> getVmRoundModelBest(
            @RequestParam String taskId,
            @RequestParam String metric,
            @RequestParam String type) {

        logger.info("查询本地模型最佳/离群结果: taskId={}, metric={}, type={}", taskId, metric, type);

        try {
            // 验证指标类型
            if (!isValidMetric(metric)) {
                return Result.failure(400, "不支持的指标类型: " + metric);
            }

            // 验证查询类型
            if (!isValidType(type)) {
                return Result.failure(400, "不支持的查询类型: " + type);
            }

            VmRoundModelBestQueryDTO queryDTO = new VmRoundModelBestQueryDTO();
            queryDTO.setTaskId(taskId);
            queryDTO.setMetric(metric);
            queryDTO.setType(type);

            VmRoundModelBestVO result = vmRoundModelService.getVmRoundModelBest(queryDTO);
            if (result == null) {
                return Result.failure(404, "未找到符合条件的模型结果");
            }

            return Result.success("查询成功", result);

        } catch (Exception e) {
            logger.error("查询本地模型最佳/离群结果失败: taskId={}, metric={}, type={}",
                    taskId, metric, type, e);
            return Result.failure(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 验证指标类型是否有效
     *
     * @param metric 指标名称
     * @return 是否有效
     */
    private boolean isValidMetric(String metric) {
        return "accuracy".equals(metric) || "loss".equals(metric);
    }

    /**
     * 验证查询类型是否有效
     *
     * @param type 查询类型
     * @return 是否有效
     */
    private boolean isValidType(String type) {
        return "best".equals(type) || "outlier".equals(type);
    }
}