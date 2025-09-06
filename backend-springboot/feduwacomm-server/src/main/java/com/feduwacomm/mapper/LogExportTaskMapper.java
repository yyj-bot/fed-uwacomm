package com.feduwacomm.mapper;

import com.feduwacomm.entity.LogExportTask;
import com.feduwacomm.enums.TaskStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogExportTaskMapper {

    int insert(LogExportTask exportTask);

    LogExportTask selectById(@Param("exportId") String exportId);

    int updateStatus(@Param("exportId") String exportId, 
                    @Param("status") TaskStatus status, 
                    @Param("progress") Integer progress,
                    @Param("processedRecords") Long processedRecords,
                    @Param("fileSize") Long fileSize,
                    @Param("completedAt") LocalDateTime completedAt,
                    @Param("errorMessage") String errorMessage);

    int updateProgress(@Param("exportId") String exportId, 
                      @Param("progress") Integer progress,
                      @Param("processedRecords") Long processedRecords);

    List<LogExportTask> selectByStatus(@Param("status") TaskStatus status,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);

    long countByStatus(@Param("status") TaskStatus status);

    List<LogExportTask> selectByCreatedBy(@Param("createdBy") String createdBy,
                                         @Param("status") TaskStatus status,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    long countByCreatedBy(@Param("createdBy") String createdBy,
                         @Param("status") TaskStatus status);

    int deleteExpiredTasks(@Param("expirationTime") LocalDateTime expirationTime);

    List<LogExportTask> selectExpiredTasks(@Param("expirationTime") LocalDateTime expirationTime);
}