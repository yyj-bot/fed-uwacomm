package com.feduwacomm.mapper;

import com.feduwacomm.dto.LogQueryDTO;
import org.apache.ibatis.annotations.Param;

public class VmRuntimeLogSqlProvider {

    public String selectByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder("SELECT * FROM vm_runtime_logs WHERE 1=1");
        
        if (queryDTO.getLevel() != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (queryDTO.getCategory() != null) {
            sql.append(" AND category = #{category}");
        }
        
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(" AND vm_id = #{vmId}");
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(" AND task_id = #{taskId}");
        }
        
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%', #{keyword}, '%')");
        }
        
        if (queryDTO.getStartTime() != null) {
            sql.append(" AND created_at >= #{startTime}");
        }
        
        if (queryDTO.getEndTime() != null) {
            sql.append(" AND created_at <= #{endTime}");
        }
        
        sql.append(" ORDER BY created_at DESC");
        
        if (queryDTO.getPage() != null && queryDTO.getSize() != null) {
            int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
            sql.append(" LIMIT ").append(offset).append(", ").append(queryDTO.getSize());
        }
        
        return sql.toString();
    }
    
    public String countByCondition(LogQueryDTO queryDTO) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vm_runtime_logs WHERE 1=1");
        
        if (queryDTO.getLevel() != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (queryDTO.getCategory() != null) {
            sql.append(" AND category = #{category}");
        }
        
        if (queryDTO.getVmId() != null && !queryDTO.getVmId().isEmpty()) {
            sql.append(" AND vm_id = #{vmId}");
        }
        
        if (queryDTO.getTaskId() != null && !queryDTO.getTaskId().isEmpty()) {
            sql.append(" AND task_id = #{taskId}");
        }
        
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
            sql.append(" AND message LIKE CONCAT('%', #{keyword}, '%')");
        }
        
        if (queryDTO.getStartTime() != null) {
            sql.append(" AND created_at >= #{startTime}");
        }
        
        if (queryDTO.getEndTime() != null) {
            sql.append(" AND created_at <= #{endTime}");
        }
        
        return sql.toString();
    }
    
    public String deleteByCondition(@Param("level") String level,
                                  @Param("category") String category,
                                  @Param("vmId") String vmId,
                                  @Param("taskId") String taskId,
                                  @Param("retentionDays") Integer retentionDays) {
        StringBuilder sql = new StringBuilder("DELETE FROM vm_runtime_logs WHERE 1=1");
        
        if (level != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (category != null) {
            sql.append(" AND category = #{category}");
        }
        
        if (vmId != null && !vmId.isEmpty()) {
            sql.append(" AND vm_id = #{vmId}");
        }
        
        if (taskId != null && !taskId.isEmpty()) {
            sql.append(" AND task_id = #{taskId}");
        }
        
        if (retentionDays != null) {
            sql.append(" AND created_at < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)");
        }
        
        return sql.toString();
    }
    
    public String countForCleanup(@Param("level") String level,
                                @Param("category") String category,
                                @Param("vmId") String vmId,
                                @Param("taskId") String taskId,
                                @Param("retentionDays") Integer retentionDays) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vm_runtime_logs WHERE 1=1");
        
        if (level != null) {
            sql.append(" AND level = #{level}");
        }
        
        if (category != null) {
            sql.append(" AND category = #{category}");
        }
        
        if (vmId != null && !vmId.isEmpty()) {
            sql.append(" AND vm_id = #{vmId}");
        }
        
        if (taskId != null && !taskId.isEmpty()) {
            sql.append(" AND task_id = #{taskId}");
        }
        
        if (retentionDays != null) {
            sql.append(" AND created_at < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)");
        }
        
        return sql.toString();
    }
}