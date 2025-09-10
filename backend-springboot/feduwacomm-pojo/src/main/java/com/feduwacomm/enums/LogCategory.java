package com.feduwacomm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LogCategory {
    SYSTEM("SYSTEM", "系统级日志，如启动、关闭、配置变更等"),
    USER("USER", "用户操作日志，如登录、注册、权限变更等"),
    VM("VM", "虚拟机相关日志，如连接、断开、状态变更等"),
    TASK("TASK", "联邦学习任务日志，如任务创建、执行、完成等"),
    DATA("DATA", "数据管理日志，如数据上传、下载、处理等"),
    MODEL("MODEL", "模型管理日志，如模型上传、部署、评估等"),
    SECURITY("SECURITY", "安全相关日志，如认证失败、权限拒绝等"),
    PERFORMANCE("PERFORMANCE", "性能监控日志，如响应时间、资源使用等");

    private final String code;
    private final String description;

    LogCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LogCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LogCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid log category code: " + code);
    }
}