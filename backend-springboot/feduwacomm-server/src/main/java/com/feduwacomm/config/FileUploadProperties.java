package com.feduwacomm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.feduwacomm.constants.SystemConstants.*;

/**
 * 文件上传配置属性类
 *
 * 管理系统中所有文件上传和存储相关的配置项
 *
 * @author FedUWAComm Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feduwacomm.file")
public class FileUploadProperties {

    /**
     * 上传配置
     */
    private Upload upload = new Upload();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * 日志导出配置
     */
    private LogExport logExport = new LogExport();

    @Data
    public static class Upload {
        /**
         * 模型文件最大大小
         * 默认: 100MB
         */
        private DataSize maxModelFileSize = DataSize.ofMegabytes(100);

        /**
         * 训练数据文件最大大小
         * 默认: 200MB
         */
        private DataSize maxTrainingDataFileSize = DataSize.ofMegabytes(200);

        /**
         * 普通文件最大大小
         * 默认: 50MB
         */
        private DataSize maxGeneralFileSize = DataSize.ofMegabytes(50);

        /**
         * 批量上传最大文件数
         * 默认: 10
         */
        private int maxBatchFiles = 10;

        /**
         * 允许的文件扩展名（训练数据）
         */
        private String[] allowedTrainingDataExtensions = {
            ".csv", ".json", ".txt", ".xlsx", ".xls"
        };

        /**
         * 允许的文件扩展名（模型文件）
         */
        private String[] allowedModelExtensions = {
            ".pkl", ".h5", ".pb", ".onnx", ".pt", ".pth"
        };

        /**
         * 是否启用文件类型检查
         * 默认: true
         */
        private boolean enableTypeCheck = true;

        /**
         * 是否启用病毒扫描
         * 默认: false
         */
        private boolean enableVirusScan = false;

        /**
         * 获取模型文件大小限制（MB）
         * @return 大小限制（MB）
         */
        public long getMaxModelFileSizeMB() {
            return maxModelFileSize.toMegabytes();
        }

        /**
         * 获取训练数据文件大小限制（MB）
         * @return 大小限制（MB）
         */
        public long getMaxTrainingDataFileSizeMB() {
            return maxTrainingDataFileSize.toMegabytes();
        }

        /**
         * 获取普通文件大小限制（MB）
         * @return 大小限制（MB）
         */
        public long getMaxGeneralFileSizeMB() {
            return maxGeneralFileSize.toMegabytes();
        }
    }

    @Data
    public static class Storage {
        /**
         * 基础存储路径
         * 默认: ./data
         */
        private String basePath = "./data";

        /**
         * 模型文件存储路径
         * 默认: models
         */
        private String modelPath = "models";

        /**
         * 训练数据存储路径
         * 默认: training-data
         */
        private String trainingDataPath = "training-data";

        /**
         * 临时文件存储路径
         * 默认: temp
         */
        private String tempPath = "temp";

        /**
         * 备份文件存储路径
         * 默认: backup
         */
        private String backupPath = "backup";

        /**
         * 文件保留天数
         * 默认: 30
         */
        private int retentionDays = 30;

        /**
         * 是否启用自动清理
         * 默认: true
         */
        private boolean enableAutoCleanup = true;

        /**
         * 获取完整的模型存储路径
         * @return 模型存储路径
         */
        public Path getModelStoragePath() {
            return Paths.get(basePath, modelPath);
        }

        /**
         * 获取完整的训练数据存储路径
         * @return 训练数据存储路径
         */
        public Path getTrainingDataStoragePath() {
            return Paths.get(basePath, trainingDataPath);
        }

        /**
         * 获取完整的临时文件存储路径
         * @return 临时文件存储路径
         */
        public Path getTempStoragePath() {
            return Paths.get(basePath, tempPath);
        }

        /**
         * 获取完整的备份文件存储路径
         * @return 备份文件存储路径
         */
        public Path getBackupStoragePath() {
            return Paths.get(basePath, backupPath);
        }
    }

    @Data
    public static class LogExport {
        /**
         * 日志导出基础路径
         * 默认: /tmp/log-exports
         */
        private String basePath = Paths.get(DEFAULT_TEMP_DIR, LOG_EXPORT_DIR).toString();

        /**
         * 日志文件保留天数
         * 默认: 7
         */
        private int retentionDays = DEFAULT_LOG_RETENTION_DAYS;

        /**
         * 单次导出最大记录数
         * 默认: 10000
         */
        private int maxRecordsPerExport = 10000;

        /**
         * 导出文件最大大小
         * 默认: 50MB
         */
        private DataSize maxExportFileSize = DataSize.ofMegabytes(50);

        /**
         * 支持的导出格式
         */
        private String[] supportedFormats = {"CSV", "JSON", "EXCEL"};

        /**
         * 是否启用压缩
         * 默认: true
         */
        private boolean enableCompression = true;

        /**
         * 获取日志导出路径
         * @return 日志导出路径
         */
        public Path getLogExportPath() {
            return Paths.get(basePath);
        }

        /**
         * 获取导出文件大小限制（MB）
         * @return 大小限制（MB）
         */
        public long getMaxExportFileSizeMB() {
            return maxExportFileSize.toMegabytes();
        }
    }

    /**
     * 验证文件大小是否符合限制
     * @param fileSize 文件大小（字节）
     * @param fileType 文件类型
     * @return 是否符合限制
     */
    public boolean isFileSizeValid(long fileSize, FileType fileType) {
        DataSize maxSize = switch (fileType) {
            case MODEL -> upload.maxModelFileSize;
            case TRAINING_DATA -> upload.maxTrainingDataFileSize;
            case GENERAL -> upload.maxGeneralFileSize;
        };
        return fileSize <= maxSize.toBytes();
    }

    /**
     * 获取文件大小限制错误消息
     * @param fileType 文件类型
     * @return 错误消息
     */
    public String getFileSizeLimitErrorMessage(FileType fileType) {
        long maxSizeMB = switch (fileType) {
            case MODEL -> upload.getMaxModelFileSizeMB();
            case TRAINING_DATA -> upload.getMaxTrainingDataFileSizeMB();
            case GENERAL -> upload.getMaxGeneralFileSizeMB();
        };
        return String.format(FILE_SIZE_LIMIT_ERROR_TEMPLATE, maxSizeMB);
    }

    /**
     * 文件类型枚举
     */
    public enum FileType {
        MODEL,
        TRAINING_DATA,
        GENERAL
    }
}