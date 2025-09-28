package com.feduwacomm.mock;

import com.feduwacomm.service.DigitalSignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mock数据生成器
 * 为测试环境提供标准化的模拟数据生成
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 * @since 2025-09-28
 */
@Slf4j
@Component
public class MockDataGenerator {

    @Autowired(required = false)
    private DigitalSignatureService digitalSignatureService;

    private final SecureRandom random = new SecureRandom();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * 生成模拟VM ID
     */
    public String generateVmId() {
        long timestamp = System.currentTimeMillis() / 1000; // 秒级时间戳
        int randomSuffix = random.nextInt(999999); // 6位随机数
        return String.format("vm-%d-%06d", timestamp, randomSuffix);
    }

    /**
     * 生成模拟客户端ID
     */
    public String generateClientId() {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        int randomSuffix = random.nextInt(9999); // 4位随机数
        return String.format("client-%s-%04d", timestamp, randomSuffix);
    }

    /**
     * 生成模拟任务ID
     */
    public String generateTaskId() {
        return "task-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成模拟数据集ID
     */
    public String generateDatasetId() {
        return "dataset-" + UUID.randomUUID().toString().substring(0, 12);
    }

    /**
     * 生成模拟模型ID
     */
    public String generateModelId() {
        return "model-" + UUID.randomUUID().toString().substring(0, 10);
    }

    /**
     * 生成真实的数字签名（如果服务可用）
     */
    public String generateSignature(String messageType, String messageId, String vmId, String messageData) {
        if (digitalSignatureService != null) {
            try {
                return digitalSignatureService.signWebSocketMessage(messageType, messageId, vmId, messageData);
            } catch (Exception e) {
                log.warn("数字签名生成失败，使用Mock签名: {}", e.getMessage());
            }
        }

        // 降级到Mock签名
        return generateMockSignature(messageType, vmId, messageData);
    }

    /**
     * 生成Mock签名（用于测试环境）
     */
    public String generateMockSignature(String messageType, String vmId, String data) {
        String sigData = String.format("%s:%s:%s:%d", messageType, vmId, data, System.currentTimeMillis());
        return "mock_sig_" + Math.abs(sigData.hashCode());
    }

    /**
     * 生成小尺寸模型参数（适合WebSocket传输）
     */
    public Map<String, Object> generateSmallModelParameters() {
        Map<String, Object> parameters = new HashMap<>();

        // 权重矩阵 - 小尺寸
        Map<String, Object> weights = new HashMap<>();
        weights.put("input_layer", generateRandomMatrix(5, 10));   // 50个参数
        weights.put("hidden_layer", generateRandomMatrix(10, 8)); // 80个参数
        weights.put("output_layer", generateRandomMatrix(8, 3));  // 24个参数
        parameters.put("weights", weights);

        // 偏置向量
        Map<String, Object> biases = new HashMap<>();
        biases.put("input_layer", generateRandomVector(10));   // 10个偏置
        biases.put("hidden_layer", generateRandomVector(8));  // 8个偏置
        biases.put("output_layer", generateRandomVector(3));  // 3个偏置
        parameters.put("biases", biases);

        // 元数据
        parameters.put("model_type", "neural_network");
        parameters.put("total_parameters", 175); // 154 + 21
        parameters.put("architecture", "5-10-8-3");
        parameters.put("generated_at", LocalDateTime.now().toString());

        log.debug("生成小尺寸模型参数: {} 个参数", parameters.get("total_parameters"));
        return parameters;
    }

    /**
     * 生成中等尺寸模型参数
     */
    public Map<String, Object> generateMediumModelParameters() {
        Map<String, Object> parameters = new HashMap<>();

        Map<String, Object> weights = new HashMap<>();
        weights.put("layer1", generateRandomMatrix(20, 50));  // 1000个参数
        weights.put("layer2", generateRandomMatrix(50, 30)); // 1500个参数
        weights.put("layer3", generateRandomMatrix(30, 10)); // 300个参数
        parameters.put("weights", weights);

        Map<String, Object> biases = new HashMap<>();
        biases.put("layer1", generateRandomVector(50));  // 50个偏置
        biases.put("layer2", generateRandomVector(30)); // 30个偏置
        biases.put("layer3", generateRandomVector(10)); // 10个偏置
        parameters.put("biases", biases);

        parameters.put("model_type", "deep_neural_network");
        parameters.put("total_parameters", 2890); // 2800 + 90
        parameters.put("architecture", "20-50-30-10");

        return parameters;
    }

    /**
     * 生成随机矩阵
     */
    public double[][] generateRandomMatrix(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (random.nextGaussian() * 0.1); // 小范围随机数
            }
        }
        return matrix;
    }

    /**
     * 生成随机向量
     */
    public double[] generateRandomVector(int size) {
        double[] vector = new double[size];
        for (int i = 0; i < size; i++) {
            vector[i] = (random.nextGaussian() * 0.1);
        }
        return vector;
    }

    /**
     * 生成模拟训练数据
     */
    public List<Map<String, Object>> generateTrainingData(int sampleCount, int featureCount) {
        List<Map<String, Object>> data = new ArrayList<>();

        for (int i = 0; i < sampleCount; i++) {
            Map<String, Object> sample = new HashMap<>();

            // 特征向量
            double[] features = generateRandomVector(featureCount);
            sample.put("features", features);

            // 模拟标签（0或1的二分类）
            sample.put("label", random.nextBoolean() ? 1 : 0);

            // 样本ID
            sample.put("sample_id", "sample_" + i);

            data.add(sample);
        }

        log.debug("生成模拟训练数据: {} 个样本，{} 个特征", sampleCount, featureCount);
        return data;
    }

    /**
     * 生成模拟性能指标
     */
    public Map<String, Object> generatePerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("accuracy", 0.85 + random.nextDouble() * 0.1); // 85%-95%
        metrics.put("precision", 0.80 + random.nextDouble() * 0.15); // 80%-95%
        metrics.put("recall", 0.75 + random.nextDouble() * 0.2); // 75%-95%
        metrics.put("f1_score", 0.78 + random.nextDouble() * 0.17); // 78%-95%
        metrics.put("loss", random.nextDouble() * 0.5); // 0-0.5

        metrics.put("training_time_ms", 1000 + random.nextInt(5000)); // 1-6秒
        metrics.put("inference_time_ms", 10 + random.nextInt(90)); // 10-100毫秒

        metrics.put("memory_usage_mb", 50 + random.nextInt(200)); // 50-250MB
        metrics.put("cpu_usage_percent", 20 + random.nextInt(60)); // 20%-80%

        return metrics;
    }

    /**
     * 生成模拟VM状态信息
     */
    public Map<String, Object> generateVmStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("vm_id", generateVmId());
        status.put("status", randomChoice(Arrays.asList("ONLINE", "TRAINING", "IDLE", "UPDATING")));
        status.put("cpu_usage", 10 + random.nextInt(70)); // 10%-80%
        status.put("memory_usage", 20 + random.nextInt(60)); // 20%-80%
        status.put("disk_usage", 30 + random.nextInt(50)); // 30%-80%
        status.put("network_latency_ms", 5 + random.nextInt(45)); // 5-50ms
        status.put("last_heartbeat", LocalDateTime.now());
        status.put("model_version", "v" + (1 + random.nextInt(10)) + "." + random.nextInt(100));

        return status;
    }

    /**
     * 生成模拟错误信息
     */
    public Map<String, Object> generateErrorInfo(String errorType) {
        Map<String, Object> error = new HashMap<>();

        error.put("error_id", "error_" + UUID.randomUUID().toString().substring(0, 8));
        error.put("error_type", errorType);
        error.put("timestamp", LocalDateTime.now());

        switch (errorType.toLowerCase()) {
            case "network":
                error.put("message", "网络连接超时");
                error.put("details", "Connection timeout after 30 seconds");
                error.put("severity", "MEDIUM");
                break;
            case "model":
                error.put("message", "模型训练失败");
                error.put("details", "Loss diverged after epoch 15");
                error.put("severity", "HIGH");
                break;
            case "data":
                error.put("message", "数据格式错误");
                error.put("details", "Invalid CSV format: missing columns");
                error.put("severity", "LOW");
                break;
            default:
                error.put("message", "未知错误");
                error.put("details", "Unexpected error occurred");
                error.put("severity", "MEDIUM");
        }

        return error;
    }

    /**
     * 从列表中随机选择一个元素
     */
    public <T> T randomChoice(List<T> choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(random.nextInt(choices.size()));
    }

    /**
     * 生成指定范围的随机整数
     */
    public int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    /**
     * 生成指定范围的随机双精度数
     */
    public double randomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 生成模拟配置信息
     */
    public Map<String, Object> generateMockConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("mock_mode", true);
        config.put("environment", "test");
        config.put("generator_version", "1.0.0");
        config.put("generated_at", LocalDateTime.now());
        config.put("seed", random.nextLong());

        return config;
    }
}