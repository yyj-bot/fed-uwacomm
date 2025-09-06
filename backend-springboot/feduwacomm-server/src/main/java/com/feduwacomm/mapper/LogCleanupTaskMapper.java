package com.feduwacomm.mapper;

import com.feduwacomm.entity.LogCleanupTask;
import com.feduwacomm.enums.LogCleanupStrategy;
import com.feduwacomm.enums.TaskStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogCleanupTaskMapper {

    int insert(LogCleanupTask cleanupTask);

    LogCleanupTask selectById(@Param("cleanupId") String cleanupId);

    int updateStatus(@Param("cleanupId") String cleanupId,
                    @Param("status") TaskStatus status,
                    @Param("progress") Integer progress,
                    @Param("deletedRecords") Long deletedRecords,
                    @Param("freedSpace") Long freedSpace,
                    @Param("completedAt") LocalDateTime completedAt,
                    @Param("errorMessage") String errorMessage);

    int updateProgress(@Param("cleanupId") String cleanupId,
                      @Param("progress") Integer progress,
                      @Param("deletedRecords") Long deletedRecords,
                      @Param("freedSpace") Long freedSpace);

    List<LogCleanupTask> selectByStatus(@Param("status") TaskStatus status,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    long countByStatus(@Param("status") TaskStatus status);

    List<LogCleanupTask> selectByStrategy(@Param("strategy") LogCleanupStrategy strategy,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    long countByStrategy(@Param("strategy") LogCleanupStrategy strategy);

    List<LogCleanupTask> selectByCreatedBy(@Param("createdBy") String createdBy,
                                          @Param("status") TaskStatus status,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    long countByCreatedBy(@Param("createdBy") String createdBy,
                         @Param("status") TaskStatus status);

    int deleteOldTasks(@Param("retentionDays") Integer retentionDays);
}