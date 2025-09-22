package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.entity.SystemLog;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SystemLogMapper {

    @Insert("INSERT INTO system_logs (id, timestamp, level, logger, message, thread, " +
            "user_id, username, request_uri, client_ip, vm_id, task_id, details, environment, exception) " +
            "VALUES (#{id}, #{timestamp}, #{level}, #{logger}, #{message}, #{thread}, " +
            "#{userId}, #{username}, #{requestUri}, #{clientIp}, #{vmId}, #{taskId}, #{details}, #{environment}, #{exception})")
    int insert(SystemLog systemLog);

    @Select("SELECT * FROM system_logs WHERE id = #{logId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "timestamp", column = "timestamp"), 
        @Result(property = "level", column = "level"),
        @Result(property = "logger", column = "logger"),
        @Result(property = "message", column = "message"),
        @Result(property = "thread", column = "thread"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "requestUri", column = "request_uri"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "details", column = "details"),
        @Result(property = "environment", column = "environment"),
        @Result(property = "exception", column = "exception"),
        @Result(property = "createdAt", column = "created_at")
    })
    SystemLog selectById(@Param("logId") String logId);

    @SelectProvider(type = SystemLogSqlProvider.class, method = "selectByCondition")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "timestamp", column = "timestamp"), 
        @Result(property = "level", column = "level"),
        @Result(property = "logger", column = "logger"),
        @Result(property = "message", column = "message"),
        @Result(property = "thread", column = "thread"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "requestUri", column = "request_uri"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "details", column = "details"),
        @Result(property = "environment", column = "environment"),
        @Result(property = "exception", column = "exception"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<SystemLog> selectByCondition(LogQueryDTO queryDTO);

    @SelectProvider(type = SystemLogSqlProvider.class, method = "countByCondition")
    long countByCondition(LogQueryDTO queryDTO);

    @Select("SELECT * FROM system_logs " +
            "WHERE (#{level} IS NULL OR level = #{level}) " +
            "AND (#{loggerPattern} IS NULL OR logger LIKE #{loggerPattern}) " +
            "ORDER BY timestamp DESC " +
            "LIMIT #{tail}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "timestamp", column = "timestamp"), 
        @Result(property = "level", column = "level"),
        @Result(property = "logger", column = "logger"),
        @Result(property = "message", column = "message"),
        @Result(property = "thread", column = "thread"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "requestUri", column = "request_uri"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "details", column = "details"),
        @Result(property = "environment", column = "environment"),
        @Result(property = "exception", column = "exception"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<SystemLog> selectRealtimeLogs(@Param("level") String level, 
                                       @Param("loggerPattern") String loggerPattern,
                                       @Param("tail") Integer tail);

    @Select("SELECT COUNT(*) FROM system_logs " +
            "WHERE level = #{level} " +
            "AND timestamp BETWEEN #{startTime} AND #{endTime}")
    long countByLevel(@Param("level") String level, 
                     @Param("startTime") LocalDateTime startTime, 
                     @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM system_logs " +
            "WHERE logger LIKE #{loggerPattern} " +
            "AND timestamp BETWEEN #{startTime} AND #{endTime}")
    long countByCategory(@Param("loggerPattern") String loggerPattern, 
                        @Param("startTime") LocalDateTime startTime, 
                        @Param("endTime") LocalDateTime endTime);

    @Select("SELECT DATE_FORMAT(timestamp, '%H') as hour, COUNT(*) as count " +
            "FROM system_logs " +
            "WHERE timestamp BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY hour " +
            "ORDER BY hour")
    List<Map<String, Object>> countByHour(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Select("SELECT DATE_FORMAT(timestamp, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM system_logs " +
            "WHERE level = 'ERROR' " +
            "AND timestamp BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY date " +
            "ORDER BY date")
    List<Map<String, Object>> countErrorTrend(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @SelectProvider(type = SystemLogSqlProvider.class, method = "getLevelDistribution")
    List<Map<String, Object>> getLevelDistribution(@Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime,
                                                   @Param("loggerPattern") String loggerPattern);

    @SelectProvider(type = SystemLogSqlProvider.class, method = "getCategoryDistribution")
    List<Map<String, Object>> getCategoryDistribution(@Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime,
                                                      @Param("level") String level);

    @DeleteProvider(type = SystemLogSqlProvider.class, method = "deleteByCondition")
    int deleteByCondition(@Param("level") String level,
                         @Param("loggerPattern") String loggerPattern,
                         @Param("retentionDays") Integer retentionDays);

    @SelectProvider(type = SystemLogSqlProvider.class, method = "countForCleanup")
    long countForCleanup(@Param("level") String level,
                        @Param("loggerPattern") String loggerPattern,
                        @Param("retentionDays") Integer retentionDays);

    @Select("SELECT * FROM system_logs " +
            "WHERE level = 'ERROR' " +
            "AND timestamp >= DATE_SUB(NOW(), INTERVAL #{timeRange} HOUR) " +
            "ORDER BY timestamp DESC " +
            "LIMIT #{limit}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "timestamp", column = "timestamp"), 
        @Result(property = "level", column = "level"),
        @Result(property = "logger", column = "logger"),
        @Result(property = "message", column = "message"),
        @Result(property = "thread", column = "thread"),
        @Result(property = "userId", column = "user_id"),
        @Result(property = "username", column = "username"),
        @Result(property = "requestUri", column = "request_uri"),
        @Result(property = "clientIp", column = "client_ip"),
        @Result(property = "vmId", column = "vm_id"),
        @Result(property = "taskId", column = "task_id"),
        @Result(property = "details", column = "details"),
        @Result(property = "environment", column = "environment"),
        @Result(property = "exception", column = "exception"),
        @Result(property = "createdAt", column = "created_at")
    })
    List<SystemLog> selectRecentErrors(@Param("timeRange") Integer timeRange, 
                                      @Param("limit") Integer limit);
}