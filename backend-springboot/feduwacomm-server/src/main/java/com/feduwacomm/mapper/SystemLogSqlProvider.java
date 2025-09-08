package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import org.apache.ibatis.annotations.Param;

public class SystemLogSqlProvider {

    public String selectByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder("SELECT * FROM system_logs WHERE 1=1");
        
        if (queryDTO.getLevel() != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (queryDTO.getCategory() != null) {
            sql.append(" AND logger LIKE CONCAT(#{category}, '%')");
        }
        
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%', #{keyword}, '%')");
        }
        
        if (queryDTO.getStartTime() != null) {
            sql.append(" AND timestamp >= #{startTime}");
        }
        
        if (queryDTO.getEndTime() != null) {
            sql.append(" AND timestamp <= #{endTime}");
        }
        
        // LogQueryDTO 没有 userId 字段，如需要可以添加
        
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%虚拟机ID: ', #{vmId}, '%')");
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%任务ID: ', #{taskId}, '%')");
        }
        
        sql.append(" ORDER BY timestamp DESC");
        
        if (queryDTO.getPage() != null && queryDTO.getSize() != null) {
            int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
            sql.append(" LIMIT ").append(offset).append(", ").append(queryDTO.getSize());
        }
        
        return sql.toString();
    }
    
    public String countByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM system_logs WHERE 1=1");
        
        if (queryDTO.getLevel() != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (queryDTO.getCategory() != null) {
            sql.append(" AND logger LIKE CONCAT(#{category}, '%')");
        }
        
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%', #{keyword}, '%')");
        }
        
        if (queryDTO.getStartTime() != null) {
            sql.append(" AND timestamp >= #{startTime}");
        }
        
        if (queryDTO.getEndTime() != null) {
            sql.append(" AND timestamp <= #{endTime}");
        }
        
        // LogQueryDTO 没有 userId 字段，如需要可以添加
        
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%虚拟机ID: ', #{vmId}, '%')");
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%任务ID: ', #{taskId}, '%')");
        }
        
        return sql.toString();
    }
    
    public String getLevelDistribution(@Param("startTime") String startTime,
                                     @Param("endTime") String endTime,
                                     @Param("loggerPattern") String loggerPattern) {
        StringBuilder sql = new StringBuilder(
            "SELECT level, COUNT(*) as count FROM system_logs WHERE 1=1"
        );
        
        if (startTime != null) {
            sql.append(" AND timestamp >= #{startTime}");
        }
        
        if (endTime != null) {
            sql.append(" AND timestamp <= #{endTime}");
        }
        
        if (loggerPattern != null) {
            sql.append(" AND logger LIKE #{loggerPattern}");
        }
        
        sql.append(" GROUP BY level");
        
        return sql.toString();
    }
    
    public String getCategoryDistribution(@Param("startTime") String startTime,
                                        @Param("endTime") String endTime,
                                        @Param("level") String level) {
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "CASE " +
            "  WHEN logger LIKE 'TASK.%' THEN 'TASK' " +
            "  WHEN logger LIKE 'VM.%' THEN 'VM' " +
            "  WHEN logger LIKE 'MODEL.%' THEN 'MODEL' " +
            "  ELSE 'SYSTEM' " +
            "END as category, COUNT(*) as count " +
            "FROM system_logs WHERE 1=1"
        );
        
        if (startTime != null) {
            sql.append(" AND timestamp >= #{startTime}");
        }
        
        if (endTime != null) {
            sql.append(" AND timestamp <= #{endTime}");
        }
        
        if (level != null) {
            sql.append(" AND level = #{level}");
        }
        
        sql.append(" GROUP BY category");
        
        return sql.toString();
    }
    
    public String deleteByCondition(@Param("level") String level,
                                  @Param("loggerPattern") String loggerPattern,
                                  @Param("retentionDays") Integer retentionDays) {
        StringBuilder sql = new StringBuilder("DELETE FROM system_logs WHERE 1=1");
        
        if (level != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (loggerPattern != null) {
            sql.append(" AND logger LIKE #{loggerPattern}");
        }
        
        if (retentionDays != null) {
            sql.append(" AND timestamp < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)");
        }
        
        return sql.toString();
    }
    
    public String countForCleanup(@Param("level") String level,
                                @Param("loggerPattern") String loggerPattern,
                                @Param("retentionDays") Integer retentionDays) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM system_logs WHERE 1=1");
        
        if (level != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (loggerPattern != null) {
            sql.append(" AND logger LIKE #{loggerPattern}");
        }
        
        if (retentionDays != null) {
            sql.append(" AND timestamp < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)");
        }
        
        return sql.toString();
    }
}