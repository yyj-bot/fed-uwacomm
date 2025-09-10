package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.entity.VmRuntimeLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface VmRuntimeLogMapper {

    @Insert("INSERT INTO vm_runtime_logs (id, level, category, vm_id, task_id, message, details, created_at) " +
            "VALUES (#{id}, #{level}, #{category}, #{vmId}, #{taskId}, #{message}, #{details}, #{createdAt})")
    int insert(VmRuntimeLog vmRuntimeLog);

    @Select("SELECT * FROM vm_runtime_logs WHERE id = #{logId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "level", column = "level"),
        @Result(property = "category", column = "category"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "message", column = "message"),
        @Result(property = "details", column = "details"),
        @Result(property = "createdAt", column = "created_at")
    })
    VmRuntimeLog selectById(@Param("logId") String logId);

    @SelectProvider(type = VmRuntimeLogSqlProvider.class, method = "selectByCondition")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "level", column = "level"),
        @Result(property = "category", column = "category"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "message", column = "message"),
        @Result(property = "details", column = "details"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<VmRuntimeLog> selectByCondition(LogQueryDTO queryDTO);

    @SelectProvider(type = VmRuntimeLogSqlProvider.class, method = "countByCondition")
    long countByCondition(LogQueryDTO queryDTO);

    @Select("SELECT * FROM vm_runtime_logs " +
            "WHERE (#{level} IS NULL OR level = #{level}) " +
            "AND (#{category} IS NULL OR category = #{category}) " +
            "AND (#{vmId} IS NULL OR vm_id = #{vmId}) " +
            "AND (#{taskId} IS NULL OR task_id = #{taskId}) " +
            "ORDER BY created_at DESC " +
            "LIMIT #{tail}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "level", column = "level"),
        @Result(property = "category", column = "category"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "message", column = "message"),
        @Result(property = "details", column = "details"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<VmRuntimeLog> selectRealtimeLogs(@Param("level") String level,
                                          @Param("category") String category,
                                          @Param("vmId") String vmId,
                                          @Param("taskId") String taskId,
                                          @Param("tail") Integer tail);

    @Select("SELECT COUNT(*) FROM vm_runtime_logs " +
            "WHERE level = #{level} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime}")
    long countByLevel(@Param("level") String level, 
                     @Param("startTime") LocalDateTime startTime, 
                     @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM vm_runtime_logs " +
            "WHERE category = #{category} " +
            "AND created_at BETWEEN #{startTime} AND #{endTime}")
    long countByCategory(@Param("category") String category, 
                        @Param("startTime") LocalDateTime startTime, 
                        @Param("endTime") LocalDateTime endTime);

    @Select("SELECT level, COUNT(*) as count FROM vm_runtime_logs " +
            "WHERE created_at BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY level")
    List<Map<String, Object>> getLevelDistribution(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);

    @Select("SELECT category, COUNT(*) as count FROM vm_runtime_logs " +
            "WHERE created_at BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY category")
    List<Map<String, Object>> getCategoryDistribution(@Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') as hour, COUNT(*) as count " +
            "FROM vm_runtime_logs " +
            "WHERE created_at BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY hour " +
            "ORDER BY hour")
    List<Map<String, Object>> countByHour(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);

    @SelectProvider(type = VmRuntimeLogSqlProvider.class, method = "deleteByCondition")
    int deleteByCondition(@Param("level") String level,
                         @Param("category") String category,
                         @Param("vmId") String vmId,
                         @Param("taskId") String taskId,
                         @Param("retentionDays") Integer retentionDays);

    @SelectProvider(type = VmRuntimeLogSqlProvider.class, method = "countForCleanup")
    long countForCleanup(@Param("level") String level,
                        @Param("category") String category,
                        @Param("vmId") String vmId,
                        @Param("taskId") String taskId,
                        @Param("retentionDays") Integer retentionDays);

    @Select("SELECT * FROM vm_runtime_logs " +
            "WHERE level = 'ERROR' " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{timeRange} HOUR) " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "level", column = "level"),
        @Result(property = "category", column = "category"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "message", column = "message"),
        @Result(property = "details", column = "details"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<VmRuntimeLog> selectRecentErrors(@Param("timeRange") Integer timeRange, 
                                         @Param("limit") Integer limit);
}