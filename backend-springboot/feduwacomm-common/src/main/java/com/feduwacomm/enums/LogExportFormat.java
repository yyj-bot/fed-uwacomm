package com.feduwacomm.enums;

public enum LogExportFormat {
    CSV("CSV", "逗号分隔值格式", "text/csv", ".csv"),
    JSON("JSON", "JSON格式", "application/json", ".json"),
    EXCEL("EXCEL", "Excel格式", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String code;
    private final String description;
    private final String mimeType;
    private final String extension;

    LogExportFormat(String code, String description, String mimeType, String extension) {
        this.code = code;
        this.description = description;
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public static LogExportFormat fromCode(String code) {
        if (code == null) {
            return CSV; // 默认返回CSV格式
        }
        for (LogExportFormat format : values()) {
            if (format.code.equalsIgnoreCase(code)) {
                return format;
            }
        }
        return CSV; // 无效代码时也返回默认CSV格式
    }
}