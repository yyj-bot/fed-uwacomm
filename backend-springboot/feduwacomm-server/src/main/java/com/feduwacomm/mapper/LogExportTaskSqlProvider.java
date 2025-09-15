package com.feduwacomm.mapper;

import org.apache.ibatis.annotations.Param;

public class LogExportTaskSqlProvider {

    public String selectByCondition(@Param("status") String status,
                                  @Param("createdBy") String createdBy,
                                  @Param("page") Integer page,
                                  @Param("size") Integer size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM log_export_tasks WHERE 1=1");
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = #{status}");
        }
        
        if (createdBy != null && !createdBy.isEmpty()) {
            sql.append(" AND created_by = #{createdBy}");
        }
        
        sql.append(" ORDER BY created_at DESC");
        
        if (page != null && size != null) {
            int offset = (page - 1) * size;
            sql.append(" LIMIT ").append(offset).append(", ").append(size);
        }
        
        return sql.toString();
    }
    
    public String countByCondition(@Param("status") String status,
                                 @Param("createdBy") String createdBy) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM log_export_tasks WHERE 1=1");
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = #{status}");
        }
        
        if (createdBy != null && !createdBy.isEmpty()) {
            sql.append(" AND created_by = #{createdBy}");
        }
        
        return sql.toString();
    }
}