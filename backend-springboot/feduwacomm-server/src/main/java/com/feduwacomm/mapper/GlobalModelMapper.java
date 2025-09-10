package com.feduwacomm.mapper;

import com.feduwacomm.entity.GlobalModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 全局模型数据访问层
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface GlobalModelMapper {

    /**
     * 插入全局模型记录
     */
    int insertGlobalModel(GlobalModel globalModel);

    /**
     * 更新全局模型记录
     */
    int updateGlobalModel(GlobalModel globalModel);

    /**
     * 更新全局模型状态
     */
    int updateStatus(@Param("id") String id, 
                     @Param("status") String status,
                     @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 更新聚合完成信息
     */
    int updateAggregationComplete(@Param("id") String id,
                                  @Param("globalLoss") BigDecimal globalLoss,
                                  @Param("globalAccuracy") BigDecimal globalAccuracy,
                                  @Param("status") String status,
                                  @Param("completedAt") LocalDateTime completedAt,
                                  @Param("aggregationDuration") Long aggregationDuration);

    /**
     * 根据ID查询全局模型
     */
    GlobalModel selectById(@Param("id") String id);

    /**
     * 根据任务ID和轮次查询全局模型
     */
    GlobalModel selectByTaskIdAndRound(@Param("taskId") String taskId, 
                                       @Param("roundNumber") Integer roundNumber);

    /**
     * 查询任务的所有全局模型
     */
    List<GlobalModel> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 查询最新的全局模型
     */
    GlobalModel selectLatestByTaskId(@Param("taskId") String taskId);

    /**
     * 查询待聚合的模型记录
     */
    List<GlobalModel> selectPendingAggregation();

    /**
     * 查询指定状态的全局模型
     */
    List<GlobalModel> selectByStatus(@Param("status") String status);

    /**
     * 删除全局模型记录
     */
    int deleteById(@Param("id") String id);

    /**
     * 按任务ID删除全局模型记录
     */
    int deleteByTaskId(@Param("taskId") String taskId);

    /**
     * 统计任务的全局模型数量
     */
    int countByTaskId(@Param("taskId") String taskId);

    /**
     * 查询任务的聚合历史
     */
    List<GlobalModel> selectAggregationHistory(@Param("taskId") String taskId,
                                               @Param("limit") Integer limit);
}