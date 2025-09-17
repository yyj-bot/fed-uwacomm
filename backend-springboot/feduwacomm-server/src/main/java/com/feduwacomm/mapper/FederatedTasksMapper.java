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
    
    int updateTaskProgress(@Param("id") String id,
                          @Param("currentRound") Integer currentRound,
                          @Param("progress") Double progress,
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