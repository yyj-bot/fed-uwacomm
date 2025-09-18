package com.feduwacomm.controller;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.Result;
import com.feduwacomm.dto.LogCleanupDTO;
import com.feduwacomm.dto.LogConfigDTO;
import com.feduwacomm.dto.LogExportDTO;
import com.feduwacomm.dto.LogQueryDTO;
import com.feduwacomm.service.LogService;
import com.feduwacomm.vo.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import com.feduwacomm.enums.LogLevel;
import com.feduwacomm.enums.LogCategory;

@RestController
@RequestMapping("/api/log")
@CrossOrigin(origins = "*")
public class LogController {
    
    @Autowired
    private LogService logService;

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<LogListVO>> queryLogs(@Valid LogQueryDTO queryDTO) {
        PageResult<LogListVO> pageResult = logService.queryLogs(queryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/detail/{logId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogDetailVO> getLogDetail(@PathVariable String logId) {
        LogDetailVO logDetail = logService.getLogDetail(logId);
        return Result.success(logDetail);
    }

    @GetMapping("/realtime")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RealtimeLogVO> getRealtimeLogs(@Valid LogQueryDTO queryDTO) {
        List<LogListVO> logs = logService.getRealtimeLogs(queryDTO);
        RealtimeLogVO realtimeLog = RealtimeLogVO.builder()
                .logs(logs)
                .totalCount(logs.size())
                .lastUpdateTime(LocalDateTime.now())
                .build();
        return Result.success(realtimeLog);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogStatisticsVO> getStatistics(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String vmId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            // 参数验证和转换
            LogLevel logLevel = null;
            if (level != null && !level.trim().isEmpty()) {
                try {
                    logLevel = LogLevel.valueOf(level.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Result.error("无效的日志级别: " + level + "，支持的级别: DEBUG, INFO, WARN, ERROR");
                }
            }

            LogCategory logCategory = null;
            if (category != null && !category.trim().isEmpty()) {
                try {
                    logCategory = LogCategory.valueOf(category.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return Result.error("无效的日志类别: " + category + "，支持的类别: SYSTEM, USER, VM, TASK, DATA, MODEL, SECURITY, PERFORMANCE");
                }
            }

            LocalDateTime startDateTime = null;
            if (startTime != null && !startTime.trim().isEmpty()) {
                try {
                    startDateTime = LocalDateTime.parse(startTime.trim());
                } catch (Exception e) {
                    return Result.error("无效的开始时间格式: " + startTime + "，请使用ISO格式如: 2024-01-01T00:00:00");
                }
            }

            LocalDateTime endDateTime = null;
            if (endTime != null && !endTime.trim().isEmpty()) {
                try {
                    endDateTime = LocalDateTime.parse(endTime.trim());
                } catch (Exception e) {
                    return Result.error("无效的结束时间格式: " + endTime + "，请使用ISO格式如: 2024-01-01T23:59:59");
                }
            }

            // 时间范围验证
            if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
                return Result.error("开始时间不能晚于结束时间");
            }

            LogQueryDTO queryDTO = LogQueryDTO.builder()
                    .level(logLevel)
                    .category(logCategory)
                    .vmId(vmId != null ? vmId.trim() : null)
                    .taskId(taskId != null ? taskId.trim() : null)
                    .keyword(keyword != null ? keyword.trim() : null)
                    .startTime(startDateTime)
                    .endTime(endDateTime)
                    .build();

            LogStatisticsVO statistics = logService.getStatistics(queryDTO);
            return Result.success(statistics);

        } catch (Exception e) {
            return Result.error("获取统计数据失败: " + e.getMessage());
        }
    }


    @PostMapping("/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadLogs(@RequestBody @Valid LogExportDTO exportDTO) {
        try {
            byte[] fileContent = logService.generateLogFile(exportDTO);
            String filename = generateFilename(exportDTO);

            // 根据格式设置正确的Content-Type
            String contentType = "text/csv; charset=UTF-8";
            if (exportDTO.getFormat() != null) {
                switch (exportDTO.getFormat()) {
                    case JSON -> contentType = "application/json; charset=UTF-8";
                    case EXCEL -> contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=UTF-8";
                    default -> contentType = "text/csv; charset=UTF-8";
                }
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", contentType)
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String generateFilename(LogExportDTO exportDTO) {
        StringBuilder filename = new StringBuilder("logs_");
        if (exportDTO.getLevel() != null) {
            filename.append(exportDTO.getLevel().name().toLowerCase()).append("_");
        }
        if (exportDTO.getCategory() != null) {
            filename.append(exportDTO.getCategory().name().toLowerCase()).append("_");
        }
        filename.append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        String extension = ".csv";
        if (exportDTO.getFormat() != null) {
            switch (exportDTO.getFormat()) {
                case JSON -> extension = ".json";
                case EXCEL -> extension = ".xlsx";
                default -> extension = ".csv";
            }
        }
        return filename.toString() + extension;
    }


    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogCleanupTaskVO> cleanupLogs(@RequestBody @Valid LogCleanupDTO cleanupDTO) {
        LogCleanupTaskVO cleanupTask = logService.createCleanupTask(cleanupDTO);
        return Result.success(cleanupTask);
    }

    @GetMapping("/cleanup/status/{cleanupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogCleanupTaskVO> getCleanupStatus(@PathVariable String cleanupId) {
        LogCleanupTaskVO cleanupTask = logService.getCleanupStatus(cleanupId);
        return Result.success(cleanupTask);
    }

    @GetMapping("/cleanup/history")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<LogCleanupTaskVO>> getCleanupHistory(@RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer size,
                                                                 @RequestParam(required = false) String status) {
        PageResult<LogCleanupTaskVO> pageResult = logService.getCleanupHistory(page, size, status);
        return Result.success(pageResult);
    }

    @GetMapping("/monitor/system")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogMonitorVO> getSystemMonitor() {
        LogMonitorVO monitor = logService.getSystemMonitor();
        return Result.success(monitor);
    }

    @GetMapping("/monitor/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogMonitorVO> getLogMonitor(@RequestParam(defaultValue = "1h") String timeRange,
                                             @RequestParam(required = false) String level) {
        LogMonitorVO monitor = logService.getLogMonitor(timeRange, level);
        return Result.success(monitor);
    }

    @GetMapping("/monitor/performance")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogMonitorVO> getPerformanceMonitor(@RequestParam(defaultValue = "1h") String timeRange,
                                                     @RequestParam(required = false) String endpoint) {
        LogMonitorVO monitor = logService.getPerformanceMonitor(timeRange, endpoint);
        return Result.success(monitor);
    }

    @GetMapping("/monitor/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogMonitorVO> getAlerts() {
        LogMonitorVO monitor = logService.getAlerts();
        return Result.success(monitor);
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<LogConfigVO> getLogConfig() {
        LogConfigVO config = logService.getLogConfig();
        return Result.success(config);
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateLogConfig(@RequestBody @Valid LogConfigDTO configDTO) {
        logService.updateLogConfig(configDTO);
        return Result.success();
    }

    public static class RealtimeLogVO {
        private List<LogListVO> logs;
        private Integer totalCount;
        private LocalDateTime lastUpdateTime;

        public static RealtimeLogVOBuilder builder() {
            return new RealtimeLogVOBuilder();
        }

        public static class RealtimeLogVOBuilder {
            private List<LogListVO> logs;
            private Integer totalCount;
            private LocalDateTime lastUpdateTime;

            public RealtimeLogVOBuilder logs(List<LogListVO> logs) {
                this.logs = logs;
                return this;
            }

            public RealtimeLogVOBuilder totalCount(Integer totalCount) {
                this.totalCount = totalCount;
                return this;
            }

            public RealtimeLogVOBuilder lastUpdateTime(LocalDateTime lastUpdateTime) {
                this.lastUpdateTime = lastUpdateTime;
                return this;
            }

            public RealtimeLogVO build() {
                RealtimeLogVO vo = new RealtimeLogVO();
                vo.logs = this.logs;
                vo.totalCount = this.totalCount;
                vo.lastUpdateTime = this.lastUpdateTime;
                return vo;
            }
        }

        public List<LogListVO> getLogs() { return logs; }
        public void setLogs(List<LogListVO> logs) { this.logs = logs; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }
}