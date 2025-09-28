package com.feduwacomm.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 训练数据分析服务接口
 * 提供数据文件的分析和统计功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
public interface TrainingDataAnalysisService {

    /**
     * 数据统计结果
     */
    class DataStatistics {
        private final long rowCount;
        private final long columnCount;
        private final long fileSize;
        private final String encoding;
        private final String format;
        private final String delimiter;

        public DataStatistics(long rowCount, long columnCount, long fileSize,
                            String encoding, String format, String delimiter) {
            this.rowCount = rowCount;
            this.columnCount = columnCount;
            this.fileSize = fileSize;
            this.encoding = encoding;
            this.format = format;
            this.delimiter = delimiter;
        }

        public long getRowCount() { return rowCount; }
        public long getColumnCount() { return columnCount; }
        public long getFileSize() { return fileSize; }
        public String getEncoding() { return encoding; }
        public String getFormat() { return format; }
        public String getDelimiter() { return delimiter; }

        @Override
        public String toString() {
            return String.format("DataStatistics{rows=%d, columns=%d, size=%d bytes, format=%s, encoding=%s}",
                    rowCount, columnCount, fileSize, format, encoding);
        }
    }

    /**
     * 分析上传的文件
     *
     * @param file 上传的文件
     * @return 数据统计结果
     * @throws Exception 分析异常
     */
    DataStatistics analyzeFile(MultipartFile file) throws Exception;

    /**
     * 分析文本数据
     *
     * @param textData 文本数据
     * @param dataType 数据类型
     * @return 数据统计结果
     * @throws Exception 分析异常
     */
    DataStatistics analyzeText(String textData, String dataType) throws Exception;

    /**
     * 验证数据格式
     *
     * @param file 上传的文件
     * @param expectedType 期望的数据类型
     * @return 是否验证通过
     */
    boolean validateDataFormat(MultipartFile file, String expectedType);

    /**
     * 检测文件编码
     *
     * @param file 上传的文件
     * @return 文件编码
     */
    String detectEncoding(MultipartFile file);

    /**
     * 检测CSV分隔符
     *
     * @param file 上传的文件
     * @return 分隔符字符
     */
    String detectDelimiter(MultipartFile file);

    /**
     * 获取数据质量评分
     *
     * @param file 上传的文件
     * @return 质量评分 (0-100)
     */
    double getDataQualityScore(MultipartFile file);
}