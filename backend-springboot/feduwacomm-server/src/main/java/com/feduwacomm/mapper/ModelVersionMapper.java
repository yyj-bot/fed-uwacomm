package com.feduwacomm.mapper;

import com.feduwacomm.entity.ModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模型版本数据访问层接口
 * 提供模型版本管理相关的数据访问功能
 */
@Mapper
public interface ModelVersionMapper {

    // 基础查询操作
    /**
     * 根据ID查询模型版本
     */
    ModelVersion selectById(@Param("id") String id);

    /**
     * 根据任务ID查询模型版本列表
     */
    List<ModelVersion> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID和轮次查询模型版本
     */
    ModelVersion selectByTaskIdAndRound(@Param("taskId") String taskId, 
                                       @Param("roundNumber") Integer roundNumber);

    // 分页查询
    /**
     * 分页查询模型版本列表
     */
    List<ModelVersion> selectByPage(@Param("offset") int offset, @Param("limit") int limit,
                                   @Param("taskId") String taskId, @Param("roundNumber") Integer roundNumber,
                                   @Param("status") String status, @Param("sort") String sort,
                                   @Param("order") String order);

    /**
     * 条件查询模型版本总数
     */
    int countByCondition(@Param("taskId") String taskId, @Param("roundNumber") Integer roundNumber,
                        @Param("status") String status);

    // 基础操作
    /**
     * 插入模型版本
     */
    int insert(ModelVersion modelVersion);

    /**
     * 更新模型版本
     */
    int update(ModelVersion modelVersion);

    /**
     * 更新模型状态
     */
    int updateStatus(@Param("id") String id, @Param("status") String status,
                    @Param("updatedBy") String updatedBy);

    /**
     * 更新模型评估指标
     */
    int updateMetrics(@Param("id") String id, @Param("accuracy") String accuracy,
                     @Param("loss") String loss, @Param("metrics") String metrics);

    /**
     * 删除模型版本
     */
    int deleteById(@Param("id") String id);

    /**
     * 批量删除模型版本
     */
    int deleteByIds(@Param("ids") List<String> ids);

    // 统计查询
    /**
     * 统计总模型数量
     */
    int countAll();

    /**
     * 根据任务ID统计模型数量
     */
    int countByTaskId(@Param("taskId") String taskId);

    /**
     * 根据状态统计模型数量
     */
    int countByStatus(@Param("status") String status);

    /**
     * 查询最佳准确率的模型
     */
    ModelVersion selectBestAccuracyByTaskId(@Param("taskId") String taskId);

    /**
     * 查询任务的最新轮次
     */
    Integer selectMaxRoundByTaskId(@Param("taskId") String taskId);

    /**
     * 查询准确率趋势数据
     */
    List<ModelVersion> selectAccuracyTrendByTaskId(@Param("taskId") String taskId);

    /**
     * 查询上传趋势统计
     */
    List<Object> selectUploadTrendByDateRange(@Param("startDate") String startDate,
                                             @Param("endDate") String endDate,
                                             @Param("taskId") String taskId);

    /**
     * 计算平均准确率
     */
    String selectAverageAccuracy(@Param("taskId") String taskId);

    /**
     * 计算平均损失值
     */
    String selectAverageLoss(@Param("taskId") String taskId);

    // 部署相关查询
    /**
     * 查询已部署的模型版本
     */
    List<ModelVersion> selectDeployedModels(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询可回滚的模型版本
     */
    List<ModelVersion> selectRollbackCandidates(@Param("taskId") String taskId,
                                               @Param("currentModelId") String currentModelId);
}