package com.feduwacomm.mapper;

import com.feduwacomm.entity.TaskParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务参与者数据访问层
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface TaskParticipantsMapper {

    /**
     * 插入新的任务参与者
     */
    int insertParticipant(TaskParticipant participant);

    /**
     * 更新参与者信息
     */
    int updateParticipant(TaskParticipant participant);

    /**
     * 更新参与者状态和轮次信息
     */
    int updateParticipantStatus(@Param("taskId") String taskId,
                               @Param("vmId") String vmId,
                               @Param("status") String status,
                               @Param("currentEpoch") Integer currentEpoch,
                               @Param("lastHeartbeat") LocalDateTime lastHeartbeat);

    /**
     * 更新参与者状态、轮次信息和训练度量指标
     */
    int updateParticipantWithMetrics(@Param("taskId") String taskId,
                                   @Param("vmId") String vmId,
                                   @Param("status") String status,
                                   @Param("currentEpoch") Integer currentEpoch,
                                   @Param("accuracy") Double accuracy,
                                   @Param("loss") Double loss,
                                   @Param("lastHeartbeat") LocalDateTime lastHeartbeat);

    /**
     * 统计指定任务和轮次中已完成训练的参与者数量
     */
    int countCompletedParticipants(@Param("taskId") String taskId,
                                   @Param("currentEpoch") Integer currentEpoch);

    /**
     * 统计指定任务的总参与者数量
     */
    int countTotalParticipants(@Param("taskId") String taskId);

    /**
     * 统计指定任务中状态为活跃的参与者数量
     */
    int countActiveParticipants(@Param("taskId") String taskId);

    /**
     * 查询指定任务的所有参与者
     */
    List<TaskParticipant> selectParticipantsByTaskId(@Param("taskId") String taskId);

    /**
     * 查询指定任务和轮次中未完成训练的参与者
     */
    List<TaskParticipant> selectIncompleteParticipants(@Param("taskId") String taskId,
                                                       @Param("currentEpoch") Integer currentEpoch);

    /**
     * 重置指定任务所有参与者的轮次状态（准备新一轮训练）
     */
    int resetParticipantsForNewRound(@Param("taskId") String taskId,
                                     @Param("newEpoch") Integer newEpoch);

    /**
     * 删除指定任务的所有参与者
     */
    int deleteParticipantsByTaskId(@Param("taskId") String taskId);

    /**
     * 根据任务ID和VM ID查询参与者
     */
    TaskParticipant selectParticipant(@Param("taskId") String taskId,
                                      @Param("vmId") String vmId);

    /**
     * 获取指定任务中参与者实际完成的最新轮次
     */
    Integer getLatestCompletedRound(@Param("taskId") String taskId);

    /**
     * 调试方法：获取参与者状态详情用于诊断
     */
    List<Map<String, Object>> getParticipantStatusDetails(@Param("taskId") String taskId);
}