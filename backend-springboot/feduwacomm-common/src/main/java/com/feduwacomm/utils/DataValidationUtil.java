package com.feduwacomm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据验证工具类
 * 提供CSV和文本数据的格式验证功能
 */
public class DataValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(DataValidationUtil.class);

    // 数值类型正则表达式
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    // 时间戳格式正则表达式 (ISO 8601)
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(Z|[+-]\\d{2}:\\d{2})?$");

    // 频率范围 (Hz)
    private static final Pattern FREQUENCY_PATTERN = Pattern.compile("^[1-9]\\d{2,5}$"); // 100Hz - 999999Hz

    // 幅度范围 (dB)
    private static final Pattern AMPLITUDE_PATTERN = Pattern.compile("^-?\\d{1,3}(\\.\\d{1,2})?$"); // -999.99 to 999.99

    // 相位范围 (度)
    private static final Pattern PHASE_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,2})?$"); // 0 to 360

    // 深度范围 (米)
    private static final Pattern DEPTH_PATTERN = Pattern.compile("^\\d{1,4}(\\.\\d{1,2})?$"); // 0 to 9999.99

    // 温度范围 (摄氏度)
    private static final Pattern TEMPERATURE_PATTERN = Pattern.compile("^-?\\d{1,2}(\\.\\d{1,2})?$"); // -99.99 to 99.99

    // 盐度范围 (ppt)
    private static final Pattern SALINITY_PATTERN = Pattern.compile("^\\d{1,2}(\\.\\d{1,2})?$"); // 0 to 99.99

    // 噪声水平范围 (dB)
    private static final Pattern NOISE_LEVEL_PATTERN = Pattern.compile("^-?\\d{1,3}(\\.\\d{1,2})?$"); // -999.99 to 999.99

    // CSV行格式验证 (基本格式检查)
    private static final Pattern CSV_ROW_PATTERN = Pattern.compile("^[^,\"\\n\\r]*(?:\"[^\"]*\"[^,\"\\n\\r]*)*(?:,[^,\"\\n\\r]*(?:\"[^\"]*\"[^,\"\\n\\r]*)*)*$");

    // 文本内容安全性验证
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\s\\n\\r\\t]*$");

    // 恶意脚本检测
    private static final Pattern[] MALICIOUS_PATTERNS = {
        Pattern.compile("(?i)<script[^>]*>.*?</script>", Pattern.DOTALL),
        Pattern.compile("(?i)javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)<object[^>]*>", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 验证CSV文件内容的有效性
     */
    public static ValidationResult validateCsvContent(String csvContent) {
        log.debug("开始验证CSV内容，长度: {}", csvContent.length());

        ValidationResult result = new ValidationResult();

        if (csvContent == null || csvContent.trim().isEmpty()) {
            result.addError("CSV内容不能为空");
            return result;
        }

        // 检查恶意内容
        if (containsMaliciousContent(csvContent)) {
            result.addError("CSV内容包含潜在的恶意代码");
            return result;
        }

        String[] lines = csvContent.split("\\r?\\n");
        if (lines.length < 2) {
            result.addError("CSV文件至少需要包含表头和一行数据");
            return result;
        }

        // 验证表头
        String header = lines[0];
        if (!validateCsvHeader(header)) {
            result.addWarning("CSV表头格式可能不标准");
        }

        // 验证数据行
        int validRows = 0;
        int totalRows = lines.length - 1; // 排除表头

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            if (validateCsvRow(line)) {
                validRows++;
            } else {
                result.addWarning("第" + (i + 1) + "行数据格式无效: " + line.substring(0, Math.min(50, line.length())));
            }
        }

        result.setTotalRows(totalRows);
        result.setValidRows(validRows);
        result.setValidationRate((double) validRows / totalRows);

        log.debug("CSV验证完成，有效行: {}/{}", validRows, totalRows);
        return result;
    }

    /**
     * 验证文本内容的有效性
     */
    public static ValidationResult validateTextContent(String textContent) {
        log.debug("开始验证文本内容，长度: {}", textContent.length());

        ValidationResult result = new ValidationResult();

        if (textContent == null || textContent.trim().isEmpty()) {
            result.addError("文本内容不能为空");
            return result;
        }

        // 检查长度限制
        if (textContent.length() > 1000000) { // 1MB 限制
            result.addError("文本内容长度超过限制 (1MB)");
            return result;
        }

        // 检查恶意内容
        if (containsMaliciousContent(textContent)) {
            result.addError("文本内容包含潜在的恶意代码");
            return result;
        }

        // 检查字符安全性
        if (!SAFE_TEXT_PATTERN.matcher(textContent).matches()) {
            result.addWarning("文本包含特殊字符，请确认内容安全性");
        }

        result.setValid(true);
        log.debug("文本验证完成");
        return result;
    }

    /**
     * 验证水声数据行的特定字段
     */
    public static ValidationResult validateAcousticDataRow(Map<String, String> rowData) {
        ValidationResult result = new ValidationResult();

        // 验证时间戳
        String timestamp = rowData.get("timestamp");
        if (timestamp != null && !TIMESTAMP_PATTERN.matcher(timestamp).matches()) {
            result.addWarning("时间戳格式不标准: " + timestamp);
        }

        // 验证频率
        String frequency = rowData.get("frequency_hz");
        if (frequency != null && !FREQUENCY_PATTERN.matcher(frequency).matches()) {
            result.addWarning("频率值不在有效范围: " + frequency);
        }

        // 验证幅度
        String amplitude = rowData.get("amplitude_db");
        if (amplitude != null && !AMPLITUDE_PATTERN.matcher(amplitude).matches()) {
            result.addWarning("幅度值格式无效: " + amplitude);
        }

        // 验证相位
        String phase = rowData.get("phase_deg");
        if (phase != null) {
            if (!PHASE_PATTERN.matcher(phase).matches()) {
                result.addWarning("相位值格式无效: " + phase);
            } else {
                double phaseValue = Double.parseDouble(phase);
                if (phaseValue < 0 || phaseValue > 360) {
                    result.addWarning("相位值超出范围 (0-360): " + phase);
                }
            }
        }

        // 验证深度
        String depth = rowData.get("depth_m");
        if (depth != null && !DEPTH_PATTERN.matcher(depth).matches()) {
            result.addWarning("深度值格式无效: " + depth);
        }

        // 验证温度
        String temperature = rowData.get("temperature_c");
        if (temperature != null && !TEMPERATURE_PATTERN.matcher(temperature).matches()) {
            result.addWarning("温度值格式无效: " + temperature);
        }

        // 验证盐度
        String salinity = rowData.get("salinity_ppt");
        if (salinity != null && !SALINITY_PATTERN.matcher(salinity).matches()) {
            result.addWarning("盐度值格式无效: " + salinity);
        }

        // 验证噪声水平
        String noiseLevel = rowData.get("noise_level");
        if (noiseLevel != null && !NOISE_LEVEL_PATTERN.matcher(noiseLevel).matches()) {
            result.addWarning("噪声水平值格式无效: " + noiseLevel);
        }

        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    /**
     * 检查是否包含恶意内容
     */
    private static boolean containsMaliciousContent(String content) {
        String lowerContent = content.toLowerCase();

        // 检查恶意脚本模式
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("检测到潜在恶意内容: {}", pattern.pattern());
                return true;
            }
        }

        // 检查SQL注入模式
        String[] sqlKeywords = {"drop", "delete", "insert", "update", "select", "union", "exec", "execute"};
        for (String keyword : sqlKeywords) {
            if (lowerContent.contains(keyword + " ") || lowerContent.contains(" " + keyword)) {
                log.warn("检测到潜在SQL注入关键词: {}", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * 验证CSV表头格式
     */
    private static boolean validateCsvHeader(String header) {
        if (header == null || header.trim().isEmpty()) {
            return false;
        }

        // 基本格式检查
        return CSV_ROW_PATTERN.matcher(header).matches();
    }

    /**
     * 验证CSV数据行格式
     */
    private static boolean validateCsvRow(String row) {
        if (row == null || row.trim().isEmpty()) {
            return false;
        }

        // 基本格式检查
        return CSV_ROW_PATTERN.matcher(row).matches();
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private int totalRows = 0;
        private int validRows = 0;
        private double validationRate = 1.0;

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

        public int getValidRows() { return validRows; }
        public void setValidRows(int validRows) { this.validRows = validRows; }

        public double getValidationRate() { return validationRate; }
        public void setValidationRate(double validationRate) { this.validationRate = validationRate; }

        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
}