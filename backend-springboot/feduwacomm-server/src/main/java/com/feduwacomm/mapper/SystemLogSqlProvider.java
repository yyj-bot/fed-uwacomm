package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import org.apache.ibatis.annotations.Param;

public class SystemLogSqlProvider {

    /**
     * 将驼峰命名的字段名映射为数据库的下划线命名
     */
    private String mapSortField(String field) {
        switch (field) {
            case "createdAt":
                return "created_at";
            case "userId":
                return "user_id";
            case "requestUri":
                return "request_uri";
            case "clientIp":
                return "client_ip";
            case "vmId":
                return "vm_id";
            case "taskId":
                return "task_id";
            default:
                return field; // timestamp, level, logger, message, thread, environment, exception等保持不变
        }
    }

    public String selectByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder();
        
        // 优化：对于大数据量查询，使用索引友好的查询结构
        // 优先使用时间范围过滤，这样可以利用timestamp索引
        sql.append("SELECT /*+ USE_INDEX(system_logs, idx_timestamp) */ ");
        sql.append("id, timestamp, level, logger, message, thread, ");
        sql.append("user_id, username, request_uri, client_ip, vm_id, task_id, ");
        sql.append("details, environment, exception, created_at ");
        sql.append("FROM system_logs ");
        
        // 构建WHERE子句，优化查询条件顺序
        boolean hasWhere = false;
        
        // 优先使用时间范围条件（通常有索引）
        if (queryDTO.getStartTime() != null || queryDTO.getEndTime() != null) {
            sql.append("WHERE ");
            hasWhere = true;
            
            if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
                sql.append("timestamp BETWEEN #{startTime} AND #{endTime}");
            } else if (queryDTO.getStartTime() != null) {
                sql.append("timestamp >= #{startTime}");
            } else {
                sql.append("timestamp <= #{endTime}");
            }
        }
        
        // 添加高选择性条件
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("vm_id = #{vmId}");
            hasWhere = true;
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("task_id = #{taskId}");
            hasWhere = true;
        }
        
        if (queryDTO.getLevel() != null) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("level = #{level}");
            hasWhere = true;
        }
        
        // 类别条件（严格按照文档定义：SYSTEM, USER, VM, TASK, DATA, MODEL, SECURITY, PERFORMANCE）
        if (queryDTO.getCategory() != null) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("logger LIKE CONCAT(#{category}, '.%')");
            hasWhere = true;
        }
        
        // 关键词搜索（最后处理，因为LIKE查询代价较高）
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            // 对于大数据量，可以考虑全文索引
            sql.append("(message LIKE CONCAT('%', #{keyword}, '%') OR details LIKE CONCAT('%', #{keyword}, '%'))");
            hasWhere = true;
        }
        
        // 如果没有任何过滤条件，添加默认时间限制以避免全表扫描
        if (!hasWhere) {
            sql.append("WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
        }
        
        // 排序优化：使用索引排序，映射驼峰命名到下划线命名
        String sortField = queryDTO.getSort() != null ? mapSortField(queryDTO.getSort()) : "timestamp";
        String sortOrder = "desc".equalsIgnoreCase(queryDTO.getOrder()) ? "DESC" : "ASC";
        sql.append(" ORDER BY ").append(sortField).append(" ").append(sortOrder);
        
        // 分页优化：对于大偏移量使用子查询优化
        if (queryDTO.getPage() != null && queryDTO.getSize() != null) {
            int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
            int size = queryDTO.getSize();
            
            if (offset > 10000) {
                // 大偏移量优化：使用索引扫描而不是OFFSET
                sql = new StringBuilder("SELECT * FROM (")
                     .append(sql.toString())
                     .append(") t LIMIT ").append(size);
            } else {
                sql.append(" LIMIT ").append(offset).append(", ").append(size);
            }
        } else {
            // 默认限制返回数量，防止意外的大结果集
            sql.append(" LIMIT 1000");
        }
        
        return sql.toString();
    }
    
    public String countByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder();
        
        // 对于计数查询，同样优化条件顺序
        sql.append("SELECT COUNT(*) FROM system_logs ");
        
        boolean hasWhere = false;
        
        // 优先使用时间范围条件（通常有索引）
        if (queryDTO.getStartTime() != null || queryDTO.getEndTime() != null) {
            sql.append("WHERE ");
            hasWhere = true;
            
            if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
                sql.append("timestamp BETWEEN #{startTime} AND #{endTime}");
            } else if (queryDTO.getStartTime() != null) {
                sql.append("timestamp >= #{startTime}");
            } else {
                sql.append("timestamp <= #{endTime}");
            }
        }
        
        // 高选择性条件
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("vm_id = #{vmId}");
            hasWhere = true;
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("task_id = #{taskId}");
            hasWhere = true;
        }
        
        if (queryDTO.getLevel() != null) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("level = #{level}");
            hasWhere = true;
        }
        
        // 类别条件（严格按照文档定义：SYSTEM, USER, VM, TASK, DATA, MODEL, SECURITY, PERFORMANCE）
        if (queryDTO.getCategory() != null) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("logger LIKE CONCAT(#{category}, '.%')");
            hasWhere = true;
        }
        
        // 关键词搜索（计数时也要包含）
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(hasWhere ? " AND " : "WHERE ");
            sql.append("(message LIKE CONCAT('%', #{keyword}, '%') OR details LIKE CONCAT('%', #{keyword}, '%'))");
            hasWhere = true;
        }
        
        // 防止全表计数
        if (!hasWhere) {
            sql.append("WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
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