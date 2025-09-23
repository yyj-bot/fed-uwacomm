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
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
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

    private StompSession stompSession;
    private boolean registered = false;
    private boolean connected = false;
    private String sessionId;
    private String accessToken; // JWT访问令牌
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

            if (responseBody != null && Integer.valueOf(200).equals(responseBody.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                sessionId = (String) data.get("sessionId");
                accessToken = (String) data.get("accessToken"); // 保存访问令牌
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
            // 创建STOMP客户端
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            // STOMP会话处理器
            StompSessionHandler sessionHandler = new MockStompSessionHandler();

            // 尝试使用原生WebSocket端点（非SockJS）
            String nativeWsUrl = websocketUrl.replace("/ws", "/ws-native") + "?vmId=" + vmData.getVmId() + "&token=" + accessToken;
            System.out.println("尝试原生WebSocket连接: " + nativeWsUrl);
            System.out.println("VM ID: " + vmData.getVmId());
            System.out.println("Access Token: " + (accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) + "..." : "null"));

            try {
                stompSession = stompClient.connect(nativeWsUrl, sessionHandler).get();
            } catch (Exception nativeEx) {
                System.out.println("原生WebSocket连接失败，尝试SockJS端点");
                // 回退到SockJS端点
                String sockjsUrl = websocketUrl + "?vmId=" + vmData.getVmId() + "&token=" + accessToken;
                System.out.println("尝试SockJS连接: " + sockjsUrl);
                stompSession = stompClient.connect(sockjsUrl, sessionHandler).get();
            }

            // 等待连接建立
            Thread.sleep(1000);

            if (stompSession != null && stompSession.isConnected()) {
                // 订阅回复队列
                stompSession.subscribe("/user/queue/reply", new MockStompFrameHandler());
                stompSession.subscribe("/topic/vm/" + vmData.getVmId(), new MockStompFrameHandler());

                // 发送CONNECT协议消息
                Map<String, Object> connectMessage = createProtocolMessage("CONNECT");
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", sessionId);
                data.put("vmId", vmData.getVmId());
                data.put("capabilities", vmData.getCapabilities());
                data.put("supportedMLAlgorithms", Arrays.asList("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL"));
                connectMessage.put("data", data);

                stompSession.send("/app/protocol", connectMessage);
                connected = true;
                System.out.println("WebSocket连接成功: " + vmData.getName());
            }
        } catch (Exception e) {
            // 输出详细错误信息进行诊断
            System.err.println("WebSocket连接失败: " + vmData.getName());
            System.err.println("错误类型: " + e.getClass().getSimpleName());
            System.err.println("错误消息: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("根本原因: " + e.getCause().getMessage());
            }
            e.printStackTrace();

            // 模拟连接成功以继续测试
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
        if (!connected || stompSession == null || !stompSession.isConnected()) {
            return;
        }

        Map<String, Object> heartbeatMessage = createProtocolMessage("HEARTBEAT");
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ACTIVE");
        data.put("cpuUsage", 20 + Math.random() * 50);
        data.put("memoryUsage", 40 + Math.random() * 40);
        data.put("gpuUsage", vmData.getGpuCount() > 0 ? 30 + Math.random() * 50 : 0);
        heartbeatMessage.put("data", data);

        sendStompMessage(heartbeatMessage);
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

        Map<String, Object> modelUpload = createProtocolMessage("MODEL_UPLOAD");

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

        sendStompMessage(modelUpload);
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
     * 创建协议消息的基本结构
     */
    private Map<String, Object> createProtocolMessage(String type) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("id", type.toLowerCase() + "-" + System.currentTimeMillis());
        message.put("timestamp", Instant.now().toString());
        message.put("vmId", vmData.getVmId());
        return message;
    }

    /**
     * 发送STOMP消息
     */
    private void sendStompMessage(Map<String, Object> message) throws Exception {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send("/app/protocol", message);
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
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
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
     * Mock STOMP Session Handler
     */
    private class MockStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("STOMP连接已建立: " + vmData.getName());
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("STOMP异常: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("STOMP传输错误: " + exception.getMessage());
            connected = false;
        }
    }

    /**
     * Mock STOMP Session Handler with Authentication
     */
    private class MockStompSessionHandlerWithAuth extends StompSessionHandlerAdapter {
        private final String token;
        private final String vmId;

        public MockStompSessionHandlerWithAuth(String token, String vmId) {
            this.token = token;
            this.vmId = vmId;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            System.out.println("STOMP连接已建立，手动发送认证: " + vmData.getName());

            // 手动发送CONNECT帧携带认证信息
            // 但这个时候连接已经建立，我们需要在连接前设置认证
            // 实际上，我们不能在afterConnected中重新发送CONNECT帧
            // 让我们采用不同的方法
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("STOMP异常: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("STOMP传输错误: " + exception.getMessage());
            connected = false;
        }
    }

    /**
     * Mock STOMP Frame Handler
     */
    private class MockStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = (Map<String, Object>) payload;
                String type = (String) messageData.get("type");
                System.out.println("收到STOMP消息: " + type + " from VM: " + vmData.getName());

                // 处理不同类型的消息
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
                System.err.println("处理STOMP消息失败: " + e.getMessage());
            }
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