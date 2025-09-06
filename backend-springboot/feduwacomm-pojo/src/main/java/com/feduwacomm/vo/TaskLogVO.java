package com.feduwacomm.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 联邦学习任务日志响应VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskLogVO {

    private String taskId;
    private Integer total;
    private Integer page;
    private Integer size;
    private List<LogEntry> logs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntry {
        private LocalDateTime timestamp;
        private String level;
        private String message;
        private String source;
        private Map<String, Object> details; // JSON格式详细信息
    }
}