package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.VmRoundModelBestQueryDTO;
import com.feduwacomm.dto.VmRoundModelQueryDTO;
import com.feduwacomm.dto.VmRoundModelTrendQueryDTO;
import com.feduwacomm.entity.VmRoundModel;
import com.feduwacomm.mapper.VmRoundModelMapper;
import com.feduwacomm.service.VmRoundModelService;
import com.feduwacomm.vo.VmRoundModelBestVO;
import com.feduwacomm.vo.VmRoundModelTrendVO;
import com.feduwacomm.vo.VmRoundModelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 虚拟机轮次模型结果服务实现类
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
public class VmRoundModelServiceImpl implements VmRoundModelService {

    private static final Logger logger = LoggerFactory.getLogger(VmRoundModelServiceImpl.class);

    @Autowired
    private VmRoundModelMapper vmRoundModelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageResult<VmRoundModelVO> queryVmRoundModels(VmRoundModelQueryDTO queryDTO) {
        logger.info("分页查询虚拟机轮次模型结果: {}", queryDTO);

        // 设置默认值
        Integer page = queryDTO.getPage() != null ? queryDTO.getPage() : 1;
        Integer size = queryDTO.getSize() != null ? queryDTO.getSize() : 10;
        Integer offset = (page - 1) * size;

        // 查询数据
        List<VmRoundModel> models = vmRoundModelMapper.selectByPage(
                queryDTO.getTaskId(),
                queryDTO.getRoundNumber(),
                queryDTO.getVmId(),
                offset,
                size
        );

        // 统计总数
        Long total = vmRoundModelMapper.countByConditions(
                queryDTO.getTaskId(),
                queryDTO.getRoundNumber(),
                queryDTO.getVmId()
        );

        // 转换为VO
        List<VmRoundModelVO> voList = models.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, total, page.longValue(), size.longValue());
    }

    @Override
    public VmRoundModelVO getVmRoundModelById(String vmRoundModelId) {
        logger.info("查询虚拟机轮次模型结果详情: {}", vmRoundModelId);

        VmRoundModel model = vmRoundModelMapper.selectById(vmRoundModelId);
        if (model == null) {
            return null;
        }

        return convertToVO(model);
    }

    @Override
    public VmRoundModelTrendVO getVmRoundModelTrend(VmRoundModelTrendQueryDTO queryDTO) {
        logger.info("查询虚拟机轮次模型指标趋势: {}", queryDTO);

        List<VmRoundModelMapper.VmRoundModelTrendPoint> trendPoints =
                vmRoundModelMapper.selectTrendByTaskAndVm(
                        queryDTO.getTaskId(),
                        queryDTO.getVmId(),
                        queryDTO.getMetric()
                );

        // 转换为VO
        List<VmRoundModelTrendVO.TrendPoint> trend = trendPoints.stream()
                .map(point -> new VmRoundModelTrendVO.TrendPoint(
                        point.getRoundNumber(),
                        point.getValue()
                ))
                .collect(Collectors.toList());

        return new VmRoundModelTrendVO(
                queryDTO.getTaskId(),
                queryDTO.getVmId(),
                queryDTO.getMetric(),
                trend
        );
    }

    @Override
    public VmRoundModelBestVO getVmRoundModelBest(VmRoundModelBestQueryDTO queryDTO) {
        logger.info("查询虚拟机轮次模型最佳/离群结果: {}", queryDTO);

        VmRoundModelMapper.VmRoundModelBestResult bestResult;

        if ("best".equals(queryDTO.getType())) {
            bestResult = vmRoundModelMapper.selectBestByTaskAndMetric(
                    queryDTO.getTaskId(),
                    queryDTO.getMetric()
            );
        } else if ("outlier".equals(queryDTO.getType())) {
            bestResult = vmRoundModelMapper.selectOutlierByTaskAndMetric(
                    queryDTO.getTaskId(),
                    queryDTO.getMetric()
            );
        } else {
            logger.warn("不支持的查询类型: {}", queryDTO.getType());
            return null;
        }

        if (bestResult == null) {
            return null;
        }

        // 转换为VO
        VmRoundModelBestVO.BestResult result = new VmRoundModelBestVO.BestResult(
                bestResult.getVmRoundModelId(),
                bestResult.getRoundNumber(),
                bestResult.getVmId(),
                bestResult.getValue()
        );

        return new VmRoundModelBestVO(
                queryDTO.getTaskId(),
                queryDTO.getMetric(),
                queryDTO.getType(),
                result
        );
    }

    /**
     * 将实体转换为VO
     *
     * @param model 实体对象
     * @return VO对象
     */
    private VmRoundModelVO convertToVO(VmRoundModel model) {
        VmRoundModelVO vo = new VmRoundModelVO();
        vo.setVmRoundModelId(model.getId());
        vo.setTaskId(model.getTaskId());
        vo.setRoundNumber(model.getRoundNumber());
        vo.setVmId(model.getVmId());
        vo.setCreatedAt(model.getCreatedAt());

        // 解析JSON参数为modelJson
        if (model.getParameters() != null) {
            try {
                Map<String, Object> modelJson = objectMapper.readValue(
                        model.getParameters(),
                        new TypeReference<Map<String, Object>>() {}
                );
                vo.setModelJson(modelJson);
            } catch (Exception e) {
                logger.warn("解析模型参数JSON失败: {}", e.getMessage());
                vo.setModelJson(new HashMap<>());
            }
        } else {
            vo.setModelJson(new HashMap<>());
        }

        // 构建指标信息
        Map<String, Object> metrics = new HashMap<>();
        if (model.getAccuracy() != null) {
            metrics.put("accuracy", model.getAccuracy());
        }
        if (model.getLoss() != null) {
            metrics.put("loss", model.getLoss());
        }
        vo.setMetrics(metrics);

        return vo;
    }
}