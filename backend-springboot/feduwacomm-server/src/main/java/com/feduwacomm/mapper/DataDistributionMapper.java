package com.feduwacomm.mapper;

import com.feduwacomm.entity.DataDistribution;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据分发任务数据访问层
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Mapper
public interface DataDistributionMapper {

    /**
     * 插入数据分发任务
     */
    @Insert("INSERT INTO data_distributions (id, task_id, distribution_name, strategy, " +
            "status, config, created_at, started_at, completed_at, created_by) " +
            "VALUES (#{id}, #{taskId}, #{distributionName}, #{strategy}, " +
            "#{status}, #{config}, #{createdAt}, #{startedAt}, #{completedAt}, #{createdBy})")
    int insertDataDistribution(DataDistribution dataDistribution);

    /**
     * 根据ID查询分发任务
     */
    @Select("SELECT * FROM data_distributions WHERE id = #{id}")
    DataDistribution selectById(String id);

    /**
     * 根据任务ID查询分发任务列表
     */
    @Select("SELECT * FROM data_distributions WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<DataDistribution> selectByTaskId(String taskId);

    /**
     * 根据状态查询分发任务列表
     */
    @Select("SELECT * FROM data_distributions WHERE status = #{status} ORDER BY created_at DESC")
    List<DataDistribution> selectByStatus(String status);

    /**
     * 查询所有分发任务
     */
    @Select("SELECT * FROM data_distributions ORDER BY created_at DESC")
    List<DataDistribution> selectAll();

    /**
     * 分页查询分发任务
     */
    @Select("<script>" +
            "SELECT * FROM data_distributions " +
            "WHERE 1=1 " +
            "<if test='taskId != null and taskId != \"\"'>" +
            "AND task_id = #{taskId} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='strategy != null and strategy != \"\"'>" +
            "AND strategy = #{strategy} " +
            "</if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<DataDistribution> selectPagedList(@Param("taskId") String taskId,
                                          @Param("status") String status,
                                          @Param("strategy") String strategy,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    /**
     * 统计分发任务数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM data_distributions " +
            "WHERE 1=1 " +
            "<if test='taskId != null and taskId != \"\"'>" +
            "AND task_id = #{taskId} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='strategy != null and strategy != \"\"'>" +
            "AND strategy = #{strategy} " +
            "</if>" +
            "</script>")
    int countDataDistributions(@Param("taskId") String taskId,
                              @Param("status") String status,
                              @Param("strategy") String strategy);

    /**
     * 更新分发状态
     */
    @Update("UPDATE data_distributions SET status = #{status}, " +
            "started_at = #{startedAt}, completed_at = #{completedAt} " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") String id, 
                    @Param("status") String status,
                    @Param("startedAt") LocalDateTime startedAt,
                    @Param("completedAt") LocalDateTime completedAt);

    /**
     * 更新分发任务信息
     */
    @Update("UPDATE data_distributions SET distribution_name = #{distributionName}, " +
            "strategy = #{strategy}, config = #{config} WHERE id = #{id}")
    int updateDataDistribution(DataDistribution dataDistribution);

    /**
     * 删除分发任务
     */
    @Delete("DELETE FROM data_distributions WHERE id = #{id}")
    int deleteById(String id);

    /**
     * 根据任务ID删除所有分发任务
     */
    @Delete("DELETE FROM data_distributions WHERE task_id = #{taskId}")
    int deleteByTaskId(String taskId);

    /**
     * 统计任务的分发状态
     */
    @Select("SELECT status, COUNT(*) as count FROM data_distributions " +
            "WHERE task_id = #{taskId} GROUP BY status")
    List<java.util.Map<String, Object>> countStatusByTaskId(String taskId);

    /**
     * 查询超时的分发任务
     */
    @Select("SELECT * FROM data_distributions WHERE status = 'IN_PROGRESS' " +
            "AND started_at < DATE_SUB(NOW(), INTERVAL #{timeoutMinutes} MINUTE)")
    List<DataDistribution> selectTimeoutDistributions(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * 查询待处理的分发任务
     */
    @Select("SELECT * FROM data_distributions WHERE status = 'CREATED' " +
            "ORDER BY created_at ASC LIMIT #{limit}")
    List<DataDistribution> selectPendingDistributions(@Param("limit") int limit);

    /**
     * 根据创建者查询分发任务
     */
    @Select("SELECT * FROM data_distributions WHERE created_by = #{createdBy} " +
            "ORDER BY created_at DESC")
    List<DataDistribution> selectByCreatedBy(String createdBy);

    /**
     * 查询指定时间范围内的分发任务
     */
    @Select("SELECT * FROM data_distributions WHERE created_at >= #{startTime} " +
            "AND created_at <= #{endTime} ORDER BY created_at DESC")
    List<DataDistribution> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计分发策略使用情况
     */
    @Select("SELECT strategy, COUNT(*) as count FROM data_distributions GROUP BY strategy")
    List<java.util.Map<String, Object>> countByStrategy();
}