package com.feduwacomm.service.impl;

import com.feduwacomm.service.TrainingDataAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 训练数据分析服务实现
 * 提供文件分析、统计和验证功能
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Service
public class TrainingDataAnalysisServiceImpl implements TrainingDataAnalysisService {

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("csv", "txt", "json", "xml");
    private static final List<String> COMMON_DELIMITERS = Arrays.asList(",", ";", "\t", "|");
    private static final int SAMPLE_SIZE = 1000; // 分析前1000行

    @Override
    public DataStatistics analyzeFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            String filename = file.getOriginalFilename();
            String format = getFileExtension(filename);
            long fileSize = file.getSize();

            // 检测编码
            String encoding = detectEncoding(file);

            // 根据文件类型进行具体分析
            switch (format.toLowerCase()) {
                case "csv":
                    return analyzeCsvFile(file, encoding, fileSize, format);
                case "txt":
                    return analyzeTextFile(file, encoding, fileSize, format);
                case "json":
                    return analyzeJsonFile(file, encoding, fileSize, format);
                default:
                    return analyzeGenericFile(file, encoding, fileSize, format);
            }

        } catch (Exception e) {
            log.error("文件分析失败: filename={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            throw new Exception("文件分析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public DataStatistics analyzeText(String textData, String dataType) throws Exception {
        if (textData == null || textData.trim().isEmpty()) {
            throw new IllegalArgumentException("文本数据不能为空");
        }

        try {
            long textLength = textData.length();
            String[] lines = textData.split("\r?\n");
            long rowCount = lines.length;

            long columnCount = 0;
            String delimiter = ",";

            // 分析列数
            if (rowCount > 0) {
                if ("csv".equalsIgnoreCase(dataType)) {
                    delimiter = detectDelimiterFromText(textData);
                    columnCount = lines[0].split(Pattern.quote(delimiter)).length;
                } else {
                    // 对于其他类型，估算平均单词数作为列数
                    columnCount = (long) Arrays.stream(lines)
                            .limit(Math.min(10, lines.length))
                            .mapToInt(line -> line.split("\\s+").length)
                            .average()
                            .orElse(1.0);
                }
            }

            log.info("文本数据分析完成: rows={}, columns={}, size={} chars, type={}",
                    rowCount, columnCount, textLength, dataType);

            return new DataStatistics(rowCount, columnCount, textLength,
                    "UTF-8", dataType, delimiter);

        } catch (Exception e) {
            log.error("文本数据分析失败: type={}, error={}", dataType, e.getMessage(), e);
            throw new Exception("文本数据分析失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateDataFormat(MultipartFile file, String expectedType) {
        if (file == null || expectedType == null) {
            return false;
        }

        try {
            String filename = file.getOriginalFilename();
            String actualFormat = getFileExtension(filename);

            // 检查文件扩展名
            if (!expectedType.toLowerCase().equals(actualFormat.toLowerCase())) {
                log.warn("文件格式不匹配: expected={}, actual={}", expectedType, actualFormat);
                return false;
            }

            // 检查文件大小（不能超过100MB）
            if (file.getSize() > 100 * 1024 * 1024) {
                log.warn("文件大小超过限制: size={} bytes", file.getSize());
                return false;
            }

            // 检查文件内容
            return validateFileContent(file, expectedType);

        } catch (Exception e) {
            log.error("文件格式验证失败: filename={}, error={}", file.getOriginalFilename(), e.getMessage());
            return false;
        }
    }

    @Override
    public String detectEncoding(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead = inputStream.read(buffer);

            if (bytesRead > 0) {
                // 检测BOM
                if (bytesRead >= 3 && buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB && buffer[2] == (byte) 0xBF) {
                    return "UTF-8";
                }
                if (bytesRead >= 2 && buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xFE) {
                    return "UTF-16LE";
                }
                if (bytesRead >= 2 && buffer[0] == (byte) 0xFE && buffer[1] == (byte) 0xFF) {
                    return "UTF-16BE";
                }

                // 尝试不同编码解码
                for (String encoding : Arrays.asList("UTF-8", "GBK", "GB2312", "ISO-8859-1")) {
                    try {
                        Charset charset = Charset.forName(encoding);
                        String decoded = new String(buffer, 0, bytesRead, charset);
                        if (isValidString(decoded)) {
                            return encoding;
                        }
                    } catch (Exception e) {
                        // 忽略编码错误，继续尝试下一个
                    }
                }
            }

        } catch (Exception e) {
            log.warn("编码检测失败，使用默认编码: {}", e.getMessage());
        }

        return "UTF-8"; // 默认编码
    }

    @Override
    public String detectDelimiter(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), detectEncoding(file)))) {

            String firstLine = reader.readLine();
            if (firstLine != null) {
                return detectDelimiterFromText(firstLine);
            }

        } catch (Exception e) {
            log.warn("分隔符检测失败: {}", e.getMessage());
        }

        return ","; // 默认分隔符
    }

    @Override
    public double getDataQualityScore(MultipartFile file) {
        try {
            double score = 100.0;

            // 检查文件大小 (太小或太大都扣分)
            long size = file.getSize();
            if (size < 1024) { // 小于1KB
                score -= 20;
            } else if (size > 50 * 1024 * 1024) { // 大于50MB
                score -= 10;
            }

            // 检查编码一致性
            String encoding = detectEncoding(file);
            if (!"UTF-8".equals(encoding)) {
                score -= 5;
            }

            // 检查格式
            String format = getFileExtension(file.getOriginalFilename());
            if (!SUPPORTED_FORMATS.contains(format.toLowerCase())) {
                score -= 15;
            }

            // 检查内容质量
            if ("csv".equalsIgnoreCase(format)) {
                score -= checkCsvQuality(file);
            }

            return Math.max(0, Math.min(100, score));

        } catch (Exception e) {
            log.warn("数据质量评分失败: {}", e.getMessage());
            return 60.0; // 默认评分
        }
    }

    /**
     * 分析CSV文件
     */
    private DataStatistics analyzeCsvFile(MultipartFile file, String encoding, long fileSize, String format) throws Exception {
        String delimiter = detectDelimiter(file);
        long rowCount = 0;
        long columnCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), encoding))) {

            String line;
            long sampleBytes = 0; // 采样数据的字节数

            // 先读取样本数据确定列数和平均行长度
            while ((line = reader.readLine()) != null && rowCount < SAMPLE_SIZE) {
                if (rowCount == 0) {
                    // 第一行确定列数
                    columnCount = line.split(Pattern.quote(delimiter)).length;
                }
                rowCount++;
                sampleBytes += line.getBytes(encoding).length + System.lineSeparator().getBytes(encoding).length;
            }

            // 如果文件还有更多行，继续读取剩余行数进行精确统计
            if (rowCount == SAMPLE_SIZE) {
                // 继续读取剩余行数，确保完全准确的统计
                while ((line = reader.readLine()) != null) {
                    rowCount++;
                }
                log.debug("完整文件统计: 总行数={}", rowCount);
            }

        }

        log.info("CSV文件分析完成: rows={}, columns={}, delimiter='{}', encoding={}",
                rowCount, columnCount, delimiter, encoding);

        return new DataStatistics(rowCount, columnCount, fileSize, encoding, format, delimiter);
    }

    /**
     * 分析文本文件
     */
    private DataStatistics analyzeTextFile(MultipartFile file, String encoding, long fileSize, String format) throws Exception {
        long rowCount = 0;
        long avgWordsPerLine = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), encoding))) {

            String line;
            long totalWords = 0;

            while ((line = reader.readLine()) != null && rowCount < SAMPLE_SIZE) {
                rowCount++;
                totalWords += line.split("\\s+").length;
            }

            if (rowCount > 0) {
                avgWordsPerLine = totalWords / rowCount;
            }

            // 估算总行数
            if (rowCount == SAMPLE_SIZE) {
                double avgLineLength = (double) fileSize / rowCount;
                rowCount = (long) (fileSize / avgLineLength);
            }
        }

        log.info("文本文件分析完成: rows={}, avgWords={}, encoding={}", rowCount, avgWordsPerLine, encoding);

        return new DataStatistics(rowCount, avgWordsPerLine, fileSize, encoding, format, "\\s+");
    }

    /**
     * 分析JSON文件
     */
    private DataStatistics analyzeJsonFile(MultipartFile file, String encoding, long fileSize, String format) throws Exception {
        // 简单的JSON分析，统计对象数量
        long objectCount = 0;
        long estimatedFields = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), encoding))) {

            String content = reader.lines().reduce("", String::concat);

            // 简单统计大括号对数来估算对象数量
            long openBraces = content.chars().filter(ch -> ch == '{').count();
            long closeBraces = content.chars().filter(ch -> ch == '}').count();
            objectCount = Math.min(openBraces, closeBraces);

            // 估算字段数
            long colonCount = content.chars().filter(ch -> ch == ':').count();
            estimatedFields = objectCount > 0 ? colonCount / objectCount : 0;
        }

        log.info("JSON文件分析完成: objects={}, avgFields={}, encoding={}", objectCount, estimatedFields, encoding);

        return new DataStatistics(objectCount, estimatedFields, fileSize, encoding, format, ":");
    }

    /**
     * 分析通用文件
     */
    private DataStatistics analyzeGenericFile(MultipartFile file, String encoding, long fileSize, String format) throws Exception {
        long lineCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), encoding))) {

            while (reader.readLine() != null && lineCount < SAMPLE_SIZE) {
                lineCount++;
            }

            // 估算总行数
            if (lineCount == SAMPLE_SIZE) {
                double avgLineLength = (double) fileSize / lineCount;
                lineCount = (long) (fileSize / avgLineLength);
            }
        }

        log.info("通用文件分析完成: lines={}, encoding={}, format={}", lineCount, encoding, format);

        return new DataStatistics(lineCount, 1, fileSize, encoding, format, "");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 从文本中检测分隔符
     */
    private String detectDelimiterFromText(String text) {
        if (text == null || text.isEmpty()) {
            return ",";
        }

        String firstLine = text.split("\r?\n")[0];
        int maxCount = 0;
        String bestDelimiter = ",";

        for (String delimiter : COMMON_DELIMITERS) {
            int count = firstLine.split(Pattern.quote(delimiter)).length;
            if (count > maxCount) {
                maxCount = count;
                bestDelimiter = delimiter;
            }
        }

        return bestDelimiter;
    }

    /**
     * 验证文件内容
     */
    private boolean validateFileContent(MultipartFile file, String expectedType) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), detectEncoding(file)))) {

            String firstLine = reader.readLine();
            if (firstLine == null) {
                return false;
            }

            switch (expectedType.toLowerCase()) {
                case "csv":
                    return firstLine.contains(",") || firstLine.contains(";") || firstLine.contains("\t");
                case "json":
                    return firstLine.trim().startsWith("{") || firstLine.trim().startsWith("[");
                case "xml":
                    return firstLine.trim().startsWith("<");
                default:
                    return true; // 对于其他类型，只要有内容就认为有效
            }

        } catch (Exception e) {
            log.warn("文件内容验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查字符串是否有效
     */
    private boolean isValidString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 检查是否包含过多的无效字符
        long invalidChars = str.chars()
                .filter(ch -> ch == 0xFFFD || (ch < 32 && ch != 9 && ch != 10 && ch != 13))
                .count();

        return invalidChars < str.length() * 0.1; // 无效字符不超过10%
    }

    /**
     * 检查CSV质量
     */
    private double checkCsvQuality(MultipartFile file) {
        double penalty = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), detectEncoding(file)))) {

            String delimiter = detectDelimiter(file);
            String line;
            int lineNumber = 0;
            int expectedColumns = -1;
            int inconsistentLines = 0;

            while ((line = reader.readLine()) != null && lineNumber < 100) { // 检查前100行
                lineNumber++;
                String[] fields = line.split(Pattern.quote(delimiter));

                if (expectedColumns == -1) {
                    expectedColumns = fields.length;
                } else if (fields.length != expectedColumns) {
                    inconsistentLines++;
                }
            }

            // 如果列数不一致的行数超过5%，扣分
            if (lineNumber > 0 && (double) inconsistentLines / lineNumber > 0.05) {
                penalty += 10;
            }

        } catch (Exception e) {
            penalty += 5;
        }

        return penalty;
    }
}