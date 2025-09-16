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

        LogQueryDTO queryDTO = LogQueryDTO.builder()
                .level(level != null ? LogLevel.valueOf(level) : null)
                .category(category != null ? LogCategory.valueOf(category) : null)
                .vmId(vmId)
                .taskId(taskId)
                .keyword(keyword)
                .startTime(startTime != null ? LocalDateTime.parse(startTime) : null)
                .endTime(endTime != null ? LocalDateTime.parse(endTime) : null)
                .build();

        LogStatisticsVO statistics = logService.getStatistics(queryDTO);
        return Result.success(statistics);
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
        
        public static RealtimeLogVOBuilder builder() {
            return new RealtimeLogVOBuilder();
        }
        
        public static class RealtimeLogVOBuilder {
            private List<LogListVO> logs;
            private Integer totalCount;
            
            public RealtimeLogVOBuilder logs(List<LogListVO> logs) {
                this.logs = logs;
                return this;
            }
            
            public RealtimeLogVOBuilder totalCount(Integer totalCount) {
                this.totalCount = totalCount;
                return this;
            }
            
            public RealtimeLogVO build() {
                RealtimeLogVO vo = new RealtimeLogVO();
                vo.logs = this.logs;
                vo.totalCount = this.totalCount;
                return vo;
            }
        }
        
        public List<LogListVO> getLogs() { return logs; }
        public void setLogs(List<LogListVO> logs) { this.logs = logs; }
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    }
}