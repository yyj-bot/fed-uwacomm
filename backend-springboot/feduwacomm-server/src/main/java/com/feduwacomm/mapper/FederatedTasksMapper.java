package com.feduwacomm.mapper;

import com.feduwacomm.entity.FederatedTask;
import com.feduwacomm.entity.TaskParticipant;
import com.feduwacomm.dto.TaskQueryDTO;
import com.feduwacomm.dto.TaskLogQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FederatedTasksMapper {

    // 任务CRUD操作
    int insertTask(FederatedTask task);
    
    int updateTask(FederatedTask task);
    
    int updateTaskStatus(@Param("id") String id,
                         @Param("status") String status,
                         @Param("timestamp") LocalDateTime timestamp);

    /**
     * 带版本控制的任务状态更新（乐观锁）
     * @param id 任务ID
     * @param status 新状态
     * @param timestamp 更新时间
     * @param expectedVersion 期望的版本号
     * @return 更新行数，如果为0表示版本冲突
     */
    int updateTaskStatusWithVersion(@Param("id") String id,
                                   @Param("status") String status,
                                   @Param("timestamp") LocalDateTime timestamp,
                                   @Param("expectedVersion") Long expectedVersion);
    
    int updateTaskProgress(@Param("id") String id,
                          @Param("currentRound") Integer currentRound,
                          @Param("status") String status);
    
    int deleteTask(@Param("id") String id);
    
    FederatedTask selectTaskById(@Param("id") String id);
    
    List<FederatedTask> selectTasksByQuery(TaskQueryDTO queryDTO);
    
    int countTasksByQuery(TaskQueryDTO queryDTO);
    
    // 参与者管理
    int insertParticipant(TaskParticipant participant);
    
    int updateParticipant(TaskParticipant participant);
    
    int updateParticipantStatus(@Param("taskId") String taskId,
                               @Param("vmId") String vmId,
                               @Param("status") String status,
                               @Param("lastHeartbeat") LocalDateTime lastHeartbeat);
    
    int deleteParticipantsByTaskId(@Param("taskId") String taskId);
    
    List<TaskParticipant> selectParticipantsByTaskId(@Param("taskId") String taskId);
    
    // 任务日志管理已移至统一的 LogService
    
    // 统计查询
    int countTasksByStatus(@Param("status") String status);
    
    List<Map<String, Object>> selectTaskStatsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    // v1.3 新增：检查VM占用情况
    int countTasksByVmIdAndStatuses(@Param("vmId") String vmId, @Param("statuses") List<String> statuses);

    // v1.4 新增：轮次状态和VM确认跟踪清理
    int deleteRoundStatesByTaskId(@Param("taskId") String taskId);

    int deleteVmAckTrackingByTaskId(@Param("taskId") String taskId);
    
    // 兼容旧方法
    @Deprecated
    int upsertTask(@Param("id") String id,
                   @Param("name") String name,
                   @Param("algorithm") String algorithm,
                   @Param("status") String status,
                   @Param("totalRounds") Integer totalRounds,
                   @Param("currentRound") Integer currentRound,
                   @Param("config") String configJson);

    @Deprecated
    int updateProgress(@Param("id") String id,
                       @Param("currentRound") Integer currentRound,
                       @Param("status") String status);

    @Deprecated
    int updateStatus(@Param("id") String id, @Param("status") String status);

    @Deprecated
    Map<String, Object> selectById(@Param("id") String id);
} 