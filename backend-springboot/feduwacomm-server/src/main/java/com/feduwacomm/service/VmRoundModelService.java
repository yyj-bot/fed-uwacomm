package com.feduwacomm.service;

import com.feduwacomm.dto.VmRoundModelBestQueryDTO;
import com.feduwacomm.dto.VmRoundModelQueryDTO;
import com.feduwacomm.dto.VmRoundModelTrendQueryDTO;
import com.feduwacomm.vo.VmRoundModelBestVO;
import com.feduwacomm.vo.VmRoundModelTrendVO;
import com.feduwacomm.vo.VmRoundModelVO;
import com.feduwacomm.common.PageResult;

/**
 * 虚拟机轮次模型结果服务接口
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface VmRoundModelService {

    /**
     * 分页查询虚拟机轮次模型结果
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<VmRoundModelVO> queryVmRoundModels(VmRoundModelQueryDTO queryDTO);

    /**
     * 根据ID查询虚拟机轮次模型结果详情
     *
     * @param vmRoundModelId 模型结果ID
     * @return 模型结果详情
     */
    VmRoundModelVO getVmRoundModelById(String vmRoundModelId);

    /**
     * 查询虚拟机轮次模型指标趋势
     *
     * @param queryDTO 查询条件
     * @return 趋势数据
     */
    VmRoundModelTrendVO getVmRoundModelTrend(VmRoundModelTrendQueryDTO queryDTO);

    /**
     * 查询虚拟机轮次模型最佳/离群结果
     *
     * @param queryDTO 查询条件
     * @return 最佳/离群结果
     */
    VmRoundModelBestVO getVmRoundModelBest(VmRoundModelBestQueryDTO queryDTO);
}