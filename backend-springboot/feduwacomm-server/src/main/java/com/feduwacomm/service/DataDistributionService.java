package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.DataDistributionDTO;
import com.feduwacomm.dto.DatasetSlice;
import com.feduwacomm.dto.DatasetAllocationValidation;
import com.feduwacomm.vo.DataDistributionTaskVO;

import java.util.List;

/**
 * 数据分发服务接口
 * 负责训练数据在虚拟机间的智能分发和管理
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface DataDistributionService {

    /**
     * 创建数据分发任务
     * 根据指定策略将数据集分发到目标虚拟机
     *
     * @param distributionDTO 分发配置
     * @param createdBy 创建者ID
     * @return 分发任务信息
     */
    DataDistributionTaskVO createDistributionTask(DataDistributionDTO distributionDTO, String createdBy);

    /**
     * 开始执行数据分发
     *
     * @param distributionId 分发任务ID
     * @param startedBy 启动者ID
     * @return 更新后的任务信息
     */
    DataDistributionTaskVO startDistribution(String distributionId, String startedBy);

    /**
     * 暂停数据分发
     *
     * @param distributionId 分发任务ID
     * @param pausedBy 暂停者ID
     * @return 更新后的任务信息
     */
    DataDistributionTaskVO pauseDistribution(String distributionId, String pausedBy);

    /**
     * 恢复数据分发
     *
     * @param distributionId 分发任务ID
     * @param resumedBy 恢复者ID
     * @return 更新后的任务信息
     */
    DataDistributionTaskVO resumeDistribution(String distributionId, String resumedBy);

    /**
     * 停止数据分发
     *
     * @param distributionId 分发任务ID
     * @param stoppedBy 停止者ID
     * @return 更新后的任务信息
     */
    DataDistributionTaskVO stopDistribution(String distributionId, String stoppedBy);

    /**
     * 取消数据分发
     *
     * @param distributionId 分发任务ID
     * @param cancelledBy 取消者ID
     * @return 更新后的任务信息
     */
    DataDistributionTaskVO cancelDistribution(String distributionId, String cancelledBy);

    /**
     * 获取分发任务详情
     *
     * @param distributionId 分发任务ID
     * @return 任务详细信息
     */
    DataDistributionTaskVO getDistributionTask(String distributionId);

    /**
     * 获取任务的所有分发记录
     *
     * @param taskId 任务ID
     * @return 分发任务列表
     */
    List<DataDistributionTaskVO> getTaskDistributions(String taskId);

    /**
     * 获取分页分发任务列表
     *
     * @param taskId 任务ID（可选）
     * @param status 状态过滤（可选）
     * @param strategy 策略过滤（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页任务列表
     */
    PageResult<DataDistributionTaskVO> getDistributionTasksPaged(String taskId, String status, 
                                                               String strategy, Integer page, Integer size);

    /**
     * 重新分发失败的数据
     *
     * @param distributionId 分发任务ID
     * @param redistributedBy 重新分发者ID
     * @return 重新分发的任务信息
     */
    DataDistributionTaskVO redistributeFailedData(String distributionId, String redistributedBy);

    /**
     * 删除分发任务
     *
     * @param distributionId 分发任务ID
     * @param deletedBy 删除者ID
     * @return 是否删除成功
     */
    boolean deleteDistributionTask(String distributionId, String deletedBy);

    /**
     * 验证分发数据完整性
     *
     * @param distributionId 分发任务ID
     * @return 验证结果
     */
    boolean verifyDistributionIntegrity(String distributionId);

    /**
     * 获取分发进度
     *
     * @param distributionId 分发任务ID
     * @return 进度信息（0-100）
     */
    Double getDistributionProgress(String distributionId);

    /**
     * 获取虚拟机数据分发详情
     *
     * @param distributionId 分发任务ID
     * @param vmId 虚拟机ID
     * @return 虚拟机数据详情
     */
    List<DataDistributionTaskVO.VmDataInfo> getVmDataDetails(String distributionId, String vmId);

    /**
     * 获取数据分布分析
     *
     * @param distributionId 分发任务ID
     * @return 数据分布分析结果
     */
    DataDistributionTaskVO.DataBalanceAnalysis getDataBalanceAnalysis(String distributionId);

    /**
     * 获取分发质量评估
     *
     * @param distributionId 分发任务ID
     * @return 质量评估结果
     */
    DataDistributionTaskVO.QualityAssessment getQualityAssessment(String distributionId);

    /**
     * 清理过期的分发任务
     *
     * @param daysOld 清理多少天前的已完成任务
     * @return 清理的任务数量
     */
    int cleanupExpiredDistributions(int daysOld);

    /**
     * 获取分发统计信息
     *
     * @param taskId 任务ID（可选）
     * @return 统计信息
     */
    DataDistributionTaskVO.DistributionStatistics getDistributionStatistics(String taskId);

    /**
     * 重新平衡数据分发
     * 用于优化现有分发的数据平衡性
     *
     * @param distributionId 分发任务ID
     * @param rebalancedBy 重新平衡者ID
     * @return 重新平衡后的任务信息
     */
    DataDistributionTaskVO rebalanceDataDistribution(String distributionId, String rebalancedBy);

    /**
     * 导出分发报告
     *
     * @param distributionId 分发任务ID
     * @param format 导出格式（JSON, CSV, EXCEL）
     * @return 报告文件路径
     */
    String exportDistributionReport(String distributionId, String format);

    /**
     * 批量创建分发任务
     *
     * @param distributionDTOs 分发配置列表
     * @param createdBy 创建者ID
     * @return 创建的任务列表
     */
    List<DataDistributionTaskVO> batchCreateDistributions(List<DataDistributionDTO> distributionDTOs, String createdBy);

    /**
     * 预览分发计划
     * 在实际执行前预览分发策略的效果
     *
     * @param distributionDTO 分发配置
     * @return 分发计划预览
     */
    DataDistributionTaskVO previewDistributionPlan(DataDistributionDTO distributionDTO);

    /**
     * 获取推荐的分发策略
     * 基于数据特征和虚拟机资源推荐最优分发策略
     *
     * @param taskId 任务ID
     * @param datasetIds 数据集ID列表
     * @param vmIds 虚拟机ID列表
     * @return 推荐的分发策略配置
     */
    DataDistributionDTO getRecommendedDistributionStrategy(String taskId, List<String> datasetIds, List<String> vmIds);

    /**
     * 监控分发性能
     *
     * @param distributionId 分发任务ID
     * @return 实时性能指标
     */
    DataDistributionTaskVO.DistributionStatistics getDistributionPerformanceMetrics(String distributionId);

    // ========== v1.5数据集分片和assignedDatasetId管理功能 ==========

    /**
     * 生成数据集分片 (v1.5)
     * 🎯 实现目标：根据参与者数量和数据分布策略分配数据集
     *
     * @param originalDatasetPath 原始数据集路径
     * @param vmId 虚拟机ID
     * @return 数据集分片信息
     */
    DatasetSlice generateDatasetSlice(String originalDatasetPath, String vmId);

    /**
     * IID数据分配策略 (v1.5)
     * 使用独立同分布算法分配数据
     *
     * @param originalDatasetPath 原始数据集路径
     * @param vmIndex 虚拟机索引
     * @param totalVms 总虚拟机数量
     * @return 分配的数据集分片
     */
    DatasetSlice performIIDAllocation(String originalDatasetPath, int vmIndex, int totalVms);

    /**
     * 验证数据集分配结果 (v1.5)
     * 检查所有分配是否正确和完整
     *
     * @param taskId 任务ID
     * @return 验证结果
     */
    DatasetAllocationValidation validateAllocation(String taskId);

    // ========== v1.5.1数据集切片增强功能 ==========

    /**
     * 使用切片信息分发数据集 (v1.5.1)
     * 集成DataSlicingService，为每个VM生成包含SliceInfo的数据分发配置
     *
     * @param taskId 任务ID
     * @param datasetId 原始数据集ID
     * @param vmIds 虚拟机ID列表
     * @param strategy 分配策略（IID/NON_IID）
     * @return 数据切片结果列表（包含vmId, assignedDatasetId, sliceInfo）
     */
    java.util.List<com.feduwacomm.dto.DataSliceResult> distributeDatasetWithSlice(
            String taskId,
            String datasetId,
            java.util.List<String> vmIds,
            com.feduwacomm.enums.AllocationStrategy strategy);

    /**
     * 计算批次范围 (v1.5.1)
     * 根据SliceInfo和批次信息计算双重索引范围
     *
     * @param sliceInfo 切片信息
     * @param batchIndex 批次索引（从0开始）
     * @param batchSize 批次大小
     * @return 批次范围（包含localStartIndex, localEndIndex, globalStartIndex, globalEndIndex）
     */
    com.feduwacomm.dto.BatchRange calculateBatchRange(
            com.feduwacomm.dto.SliceInfo sliceInfo,
            int batchIndex,
            int batchSize);

    /**
     * 为数据行添加双重索引 (v1.5.1)
     * 为批次中的每一行添加localIndex和globalIndex字段
     *
     * @param rows 数据行列表（Map格式）
     * @param batchStartIndex 批次起始索引（本地索引）
     * @param sliceInfo 切片信息（用于计算globalIndex）
     */
    void addDualIndices(
            java.util.List<java.util.Map<String, Object>> rows,
            int batchStartIndex,
            com.feduwacomm.dto.SliceInfo sliceInfo);
}