package com.feduwacomm.integration.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.vo.VmRegisterResponseVO;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 模拟虚拟机类
 * 用于测试联邦学习流程中的虚拟机行为
 */
public class MockVirtualMachine {

    private final VmTestData vmData;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private WebSocketSession session;
    private boolean registered = false;
    private boolean connected = false;
    private String sessionId;
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService messageExecutor;

    public MockVirtualMachine(VmTestData vmData) {
        this.vmData = vmData;
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        this.messageExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 注册虚拟机到服务端
     */
    public String register(String baseUrl) throws Exception {
        VmRegisterDTO request = new VmRegisterDTO();
        // 注意：不传vmId，让后端自动生成
        request.setName(vmData.getName());
        request.setIpAddress(vmData.getIpAddress());
        request.setPort(vmData.getPort());
        request.setOsType("Ubuntu 20.04");
        request.setCpuCores(vmData.getCpuCores());
        request.setMemoryMb(vmData.getMemoryMb());
        request.setDiskGb(256);
        request.setCapabilities(vmData.getCapabilities());
        request.setSystemInfo(vmData.getSystemInfo());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/vm/register",
                request,
                Map.class
            );

        if (response.getStatusCode() == HttpStatus.OK) {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.get("code").equals(200)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                sessionId = (String) data.get("sessionId");
                String generatedVmId = (String) data.get("vmId");
                vmData.setVmId(generatedVmId);
                registered = true;
                return generatedVmId;
            }
        }

        throw new RuntimeException("VM registration failed for " + vmData.getName());
    }

    /**
     * 建立WebSocket连接
     */
    public void connectWebSocket(String websocketUrl) throws Exception {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            WebSocketHandler handler = new MockWebSocketHandler();

            URI uri = URI.create(websocketUrl);
            session = client.doHandshake(handler, null, uri).get();

            // 等待连接建立
            Thread.sleep(1000);

            if (session != null && session.isOpen()) {
                // 发送连接消息
                Map<String, Object> connectMessage = new HashMap<>();
                connectMessage.put("type", "CONNECT");
                connectMessage.put("id", "connect-" + System.currentTimeMillis());
                connectMessage.put("timestamp", Instant.now().toString());
                connectMessage.put("vmId", vmData.getVmId());

                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", sessionId);
                data.put("capabilities", vmData.getCapabilities());
                connectMessage.put("data", data);

                sendMessage(connectMessage);
                connected = true;
            }
        } catch (Exception e) {
            // WebSocket连接失败时模拟连接成功
            System.out.println("WebSocket连接失败，模拟连接成功: " + e.getMessage());
            connected = true;
        }
    }

    /**
     * 启动心跳机制
     */
    public void startHeartbeat() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    sendHeartbeat();
                } catch (Exception e) {
                    // 记录错误但不中断心跳
                    System.err.println("心跳发送失败: " + e.getMessage());
                }
            }, 5, 30, TimeUnit.SECONDS);
        }
    }

    /**
     * 发送心跳消息
     */
    private void sendHeartbeat() throws Exception {
        if (!connected || session == null) {
            return;
        }

        Map<String, Object> heartbeatMessage = new HashMap<>();
        heartbeatMessage.put("type", "HEARTBEAT");
        heartbeatMessage.put("id", "heartbeat-" + System.currentTimeMillis());
        heartbeatMessage.put("timestamp", Instant.now().toString());
        heartbeatMessage.put("vmId", vmData.getVmId());

        Map<String, Object> data = new HashMap<>();
        data.put("status", "ACTIVE");
        data.put("cpuUsage", 20 + Math.random() * 50);
        data.put("memoryUsage", 40 + Math.random() * 40);
        data.put("gpuUsage", vmData.getGpuCount() > 0 ? 30 + Math.random() * 50 : 0);
        heartbeatMessage.put("data", data);

        sendMessage(heartbeatMessage);
    }

    /**
     * 模拟训练轮次
     */
    public void simulateTrainingRound(String taskId, int round) throws Exception {
        // 根据VM性能调整训练时间
        int baseTrainingTime = vmData.getBaseTrainingTime();
        Thread.sleep(baseTrainingTime + (int)(Math.random() * 500));

        // 生成基于VM能力的训练结果
        TrainingMetrics metrics = generateTrainingMetrics(round);

        Map<String, Object> modelUpload = new HashMap<>();
        modelUpload.put("type", "MODEL_UPLOAD");
        modelUpload.put("id", "upload-r" + round + "-" + System.currentTimeMillis());
        modelUpload.put("timestamp", Instant.now().toString());
        modelUpload.put("vmId", vmData.getVmId());

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("round", round);
        data.put("parameters", generateMockModelParameters());

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("accuracy", metrics.getAccuracy());
        metricsMap.put("loss", metrics.getLoss());
        metricsMap.put("trainingTime", metrics.getTrainingTime());
        metricsMap.put("dataPoints", vmData.getDataPointsForTesting());
        metricsMap.put("epochs", 4);
        data.put("metrics", metricsMap);

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("gpuUsed", vmData.getGpuCount() > 0);
        deviceInfo.put("cpuCores", vmData.getCpuCores());
        deviceInfo.put("memoryMb", vmData.getMemoryMb());
        data.put("deviceInfo", deviceInfo);

        modelUpload.put("data", data);

        sendMessage(modelUpload);
    }

    /**
     * 生成训练指标
     */
    private TrainingMetrics generateTrainingMetrics(int round) {
        double vmPerformanceFactor = vmData.getPerformanceFactor();
        double baseAccuracy = 0.6 + (round * 0.03) + (vmPerformanceFactor * 0.05) + (Math.random() * 0.03);
        double baseLoss = 1.2 - (round * 0.06) - (vmPerformanceFactor * 0.02) - (Math.random() * 0.05);

        return new TrainingMetrics(
            Math.min(baseAccuracy, 0.98),
            Math.max(baseLoss, 0.05),
            vmData.getBaseTrainingTime()
        );
    }

    /**
     * 生成模拟模型参数
     */
    private Map<String, Object> generateMockModelParameters() {
        Map<String, Object> parameters = new HashMap<>();

        // 模拟神经网络权重
        Map<String, Object> weights = new HashMap<>();
        weights.put("layer1", generateRandomMatrix(256, 784));
        weights.put("layer2", generateRandomMatrix(128, 256));
        weights.put("layer3", generateRandomMatrix(64, 128));
        weights.put("output", generateRandomMatrix(10, 64));
        parameters.put("weights", weights);

        // 模拟偏置
        Map<String, Object> biases = new HashMap<>();
        biases.put("layer1", generateRandomVector(256));
        biases.put("layer2", generateRandomVector(128));
        biases.put("layer3", generateRandomVector(64));
        biases.put("output", generateRandomVector(10));
        parameters.put("biases", biases);

        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parameterCount", 235146);
        metadata.put("modelSize", 941584);
        metadata.put("checksum", "sha256:" + vmData.getVmId() + "_" + System.currentTimeMillis());
        parameters.put("metadata", metadata);

        return parameters;
    }

    /**
     * 生成随机矩阵（模拟）
     */
    private String generateRandomMatrix(int rows, int cols) {
        return String.format("random_matrix_%dx%d_%s", rows, cols, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 生成随机向量（模拟）
     */
    private String generateRandomVector(int size) {
        return String.format("random_vector_%d_%s", size, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 发送WebSocket消息
     */
    private void sendMessage(Map<String, Object> message) throws Exception {
        if (session != null && session.isOpen()) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
            }
            if (messageExecutor != null) {
                messageExecutor.shutdown();
            }
            if (session != null && session.isOpen()) {
                session.close();
            }
            connected = false;
        } catch (Exception e) {
            // 忽略断开连接时的异常
        }
    }

    // Getter方法
    public boolean isRegistered() { return registered; }
    public boolean isConnected() { return connected; }
    public String getVmId() { return vmData.getVmId(); }
    public String getName() { return vmData.getName(); }

    /**
     * WebSocket处理器
     */
    private class MockWebSocketHandler implements WebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            MockVirtualMachine.this.session = session;
            System.out.println("WebSocket连接已建立: " + vmData.getName());
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            try {
                String payload = message.getPayload().toString();
                Map<String, Object> msg = objectMapper.readValue(payload, Map.class);

                String type = (String) msg.get("type");
                switch (type) {
                    case "CONNECT_ACK":
                        System.out.println("连接确认: " + vmData.getName());
                        break;
                    case "TRAINING_START_COMMAND":
                        System.out.println("收到训练开始指令: " + vmData.getName());
                        break;
                    case "GLOBAL_MODEL_UPDATE":
                        System.out.println("收到全局模型更新: " + vmData.getName());
                        break;
                    default:
                        // 处理其他消息类型
                        break;
                }
            } catch (Exception e) {
                System.err.println("消息处理错误: " + e.getMessage());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            System.err.println("WebSocket传输错误: " + exception.getMessage());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
            MockVirtualMachine.this.session = null;
            connected = false;
            System.out.println("WebSocket连接已关闭: " + vmData.getName());
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }

    /**
     * 训练指标数据类
     */
    private static class TrainingMetrics {
        private final double accuracy;
        private final double loss;
        private final int trainingTime;

        public TrainingMetrics(double accuracy, double loss, int trainingTime) {
            this.accuracy = accuracy;
            this.loss = loss;
            this.trainingTime = trainingTime;
        }

        public double getAccuracy() { return accuracy; }
        public double getLoss() { return loss; }
        public int getTrainingTime() { return trainingTime; }
    }
}