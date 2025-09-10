package com.feduwacomm.mapper;

import com.feduwacomm.entity.OrchestrationWorkflow;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 工作流编排Mapper
 */
@Mapper
public interface OrchestrationWorkflowMapper {

    /**
     * 插入工作流记录
     */
    @Insert("""
        INSERT INTO orchestration_workflows (
            id, task_id, workflow_name, status, current_stage, config,
            created_at, started_at, completed_at, created_by, updated_at
        ) VALUES (
            #{id}, #{taskId}, #{workflowName}, #{status}, #{currentStage}, #{config},
            #{createdAt}, #{startedAt}, #{completedAt}, #{createdBy}, #{updatedAt}
        )
    """)
    int insert(OrchestrationWorkflow workflow);

    /**
     * 更新工作流记录
     */
    @Update("""
        UPDATE orchestration_workflows SET
            workflow_name = #{workflowName},
            status = #{status},
            current_stage = #{currentStage},
            config = #{config},
            started_at = #{startedAt},
            completed_at = #{completedAt},
            updated_at = NOW()
        WHERE id = #{id}
    """)
    int update(OrchestrationWorkflow workflow);

    /**
     * 根据ID查询工作流
     */
    @Select("""
        SELECT id, task_id, workflow_name, status, current_stage, config,
               created_at, started_at, completed_at, created_by, updated_at
        FROM orchestration_workflows
        WHERE id = #{id}
    """)
    OrchestrationWorkflow selectById(String id);

    /**
     * 根据任务ID查询工作流
     */
    @Select("""
        SELECT id, task_id, workflow_name, status, current_stage, config,
               created_at, started_at, completed_at, created_by, updated_at
        FROM orchestration_workflows
        WHERE task_id = #{taskId}
        ORDER BY created_at DESC
        LIMIT 1
    """)
    OrchestrationWorkflow selectByTaskId(String taskId);

    /**
     * 查询指定状态的工作流列表
     */
    @Select("""
        SELECT id, task_id, workflow_name, status, current_stage, config,
               created_at, started_at, completed_at, created_by, updated_at
        FROM orchestration_workflows
        WHERE status = #{status}
        ORDER BY created_at DESC
    """)
    List<OrchestrationWorkflow> selectByStatus(String status);

    /**
     * 查询用户创建的工作流列表
     */
    @Select("""
        SELECT id, task_id, workflow_name, status, current_stage, config,
               created_at, started_at, completed_at, created_by, updated_at
        FROM orchestration_workflows
        WHERE created_by = #{createdBy}
        ORDER BY created_at DESC
    """)
    List<OrchestrationWorkflow> selectByCreatedBy(String createdBy);

    /**
     * 删除工作流记录
     */
    @Delete("DELETE FROM orchestration_workflows WHERE id = #{id}")
    int deleteById(String id);
}