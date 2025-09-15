package com.feduwacomm.mapper;

import com.feduwacomm.entity.WorkflowStageExecution;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 工作流阶段执行记录Mapper
 */
@Mapper
public interface WorkflowStageExecutionMapper {

    /**
     * 插入阶段执行记录
     */
    @Insert("""
        INSERT INTO workflow_stage_executions (
            id, orchestration_id, stage_name, status, started_at, completed_at,
            input_data, output_data, error_message, created_at
        ) VALUES (
            #{id}, #{orchestrationId}, #{stageName}, #{status}, #{startedAt}, #{completedAt},
            #{inputData}, #{outputData}, #{errorMessage}, #{createdAt}
        )
    """)
    int insert(WorkflowStageExecution execution);

    /**
     * 更新阶段执行记录
     */
    @Update("""
        UPDATE workflow_stage_executions SET
            status = #{status},
            started_at = #{startedAt},
            completed_at = #{completedAt},
            output_data = #{outputData},
            error_message = #{errorMessage}
        WHERE id = #{id}
    """)
    int update(WorkflowStageExecution execution);

    /**
     * 根据工作流ID查询阶段执行记录
     */
    @Select("""
        SELECT id, orchestration_id, stage_name, status, started_at, completed_at,
               input_data, output_data, error_message, created_at
        FROM workflow_stage_executions
        WHERE orchestration_id = #{orchestrationId}
        ORDER BY created_at ASC
    """)
    List<WorkflowStageExecution> selectByOrchestrationId(String orchestrationId);

    /**
     * 根据ID查询阶段执行记录
     */
    @Select("""
        SELECT id, orchestration_id, stage_name, status, started_at, completed_at,
               input_data, output_data, error_message, created_at
        FROM workflow_stage_executions
        WHERE id = #{id}
    """)
    WorkflowStageExecution selectById(String id);

    /**
     * 查询工作流最新的阶段执行记录
     */
    @Select("""
        SELECT id, orchestration_id, stage_name, status, started_at, completed_at,
               input_data, output_data, error_message, created_at
        FROM workflow_stage_executions
        WHERE orchestration_id = #{orchestrationId}
        ORDER BY created_at DESC
        LIMIT 1
    """)
    WorkflowStageExecution selectLatestByOrchestrationId(String orchestrationId);

    /**
     * 统计各状态的阶段数量
     */
    @Select("""
        SELECT status, COUNT(*) as count
        FROM workflow_stage_executions
        WHERE orchestration_id = #{orchestrationId}
        GROUP BY status
    """)
    @MapKey("status")
    java.util.Map<String, Integer> countByStatus(String orchestrationId);
}