package com.feduwacomm.mapper;

import com.feduwacomm.entity.VmRoundModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VmRoundModelsMapper {

    int upsertRoundModel(@Param("id") String id,
                          @Param("taskId") String taskId,
                          @Param("vmId") String vmId,
                          @Param("roundNumber") Integer roundNumber,
                          @Param("accuracy") Double accuracy,
                          @Param("loss") Double loss,
                          @Param("parameters") String parametersJson);

    /**
     * 根据任务ID和轮次查询所有本地模型（用于聚合）
     */
    List<VmRoundModel> selectByTaskIdAndRound(@Param("taskId") String taskId, 
                                              @Param("roundNumber") Integer roundNumber);

    /**
     * 统计指定任务和轮次的已上传模型数量
     */
    int countReadyModels(@Param("taskId") String taskId, 
                         @Param("roundNumber") Integer roundNumber);

    /**
     * 查询待聚合的模型记录（按创建时间排序）
     */
    List<VmRoundModel> selectPendingAggregation(@Param("limit") Integer limit);

    /**
     * 根据任务ID查询所有轮次模型
     */
    List<VmRoundModel> selectByTaskId(@Param("taskId") String taskId);

    /**
     * 查询指定任务的最新轮次号
     */
    Integer selectLatestRoundByTaskId(@Param("taskId") String taskId);

    /**
     * 根据ID查询模型详情
     */
    VmRoundModel selectById(@Param("id") String id);

    /**
     * 查询指定VM在任务中的所有轮次模型
     */
    List<VmRoundModel> selectByTaskIdAndVmId(@Param("taskId") String taskId, 
                                             @Param("vmId") String vmId);

    /**
     * 删除指定任务的所有轮次模型
     */
    int deleteByTaskId(@Param("taskId") String taskId);

    /**
     * 查询任务的参与客户端列表
     */
    List<String> selectParticipantVmIds(@Param("taskId") String taskId);

    /**
     * 查询任务指定轮次的参与客户端数量
     */
    int countParticipantsInRound(@Param("taskId") String taskId, 
                                 @Param("roundNumber") Integer roundNumber);
} 