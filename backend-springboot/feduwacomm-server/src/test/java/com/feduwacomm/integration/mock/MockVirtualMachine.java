package com.feduwacomm.integration.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.utils.MessageBuilder;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.lang.reflect.Type;
import java.net.URI;
import java.security.SecureRandom;
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
    private String websocketUrl; // 保存WebSocket连接URL，用于重连
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledExecutorService messageExecutor;
    private final Object sessionLock = new Object(); // 会话同步锁

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

                if (data != null) {
                    sessionId = (String) data.get("sessionId");
                    accessToken = (String) data.get("accessToken");
                    String generatedVmId = (String) data.get("vmId");

                    if (generatedVmId != null) {
                        vmData.setVmId(generatedVmId);
                        registered = true;
                        return generatedVmId;
                    }
                }
            }
        }

        throw new RuntimeException("VM registration failed for " + vmData.getName());
    }

    /**
     * 建立WebSocket连接
     */
    public void connectWebSocket(String websocketUrl) throws Exception {
        try {
            // 保存WebSocket URL用于重连
            this.websocketUrl = websocketUrl;

            // 创建支持大消息的WebSocket客户端
            StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

            // 配置客户端消息缓冲区大小为1GB，匹配服务器配置
            webSocketClient.setUserProperties(Map.of(
                "org.apache.tomcat.websocket.textBufferSize", 1073741824,     // 1GB文本消息缓冲区
                "org.apache.tomcat.websocket.binaryBufferSize", 1073741824,   // 1GB二进制消息缓冲区
                "org.apache.tomcat.websocket.session.timeout", 600000         // 10分钟会话超时
            ));

            System.out.println("🔧 [" + vmData.getName() + "] 客户端消息缓冲区已配置为1GB");

            // 创建STOMP客户端
            WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            // STOMP会话处理器（带认证信息）
            // 认证信息通过STOMP CONNECT头部的Authorization Bearer传递
            StompSessionHandler sessionHandler = new MockStompSessionHandlerWithAuth(accessToken, vmData.getVmId());

            // 使用标准的WebSocket URL，不使用原生端点
            String wsUrl = websocketUrl;
            System.out.println("🔌 [" + vmData.getName() + "] 尝试WebSocket连接: " + wsUrl);
            System.out.println("🆔 [" + vmData.getName() + "] VM ID: " + vmData.getVmId());
            System.out.println("🔐 [" + vmData.getName() + "] Access Token: " + (accessToken != null && accessToken.length() > 20 ? accessToken.substring(0, 20) + "..." : accessToken));
            System.out.println("🔑 [" + vmData.getName() + "] 认证方式: WebSocket握手头部 + STOMP头部双重认证");
            System.out.println("✅ [" + vmData.getName() + "] VM 注册状态: " + (registered ? "已注册" : "未注册"));
            System.out.println("🕒 [" + vmData.getName() + "] 连接时间: " + new java.util.Date());
            System.out.println("⚙️  [" + vmData.getName() + "] 配置信息: CPU=" + vmData.getCpuCores() + "核, 内存=" + vmData.getMemoryMb() + "MB, GPU=" + vmData.getGpuCount() + "个");

            // 检查关键信息是否可用
            if (vmData.getVmId() == null || accessToken == null) {
                throw new IllegalStateException("VM必须先注册才能建立WebSocket连接，当前vmId=" + vmData.getVmId() + ", accessToken=" + (accessToken != null ? "存在" : "null"));
            }

            // 创建STOMP连接头部
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.setAcceptVersion("1.1", "1.2");
            connectHeaders.setHeartbeat(new long[]{0, 0});
            // 使用标准的Authorization Bearer头部传递JWT token
            if (accessToken != null) {
                connectHeaders.add("Authorization", "Bearer " + accessToken);
            }
            // 可选传递vmId
            if (vmData.getVmId() != null) {
                connectHeaders.add("vmId", vmData.getVmId());
            }

            // 创建WebSocket握手头部
            WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
            if (accessToken != null) {
                handshakeHeaders.add("Authorization", "Bearer " + accessToken);
            }
            if (vmData.getVmId() != null) {
                handshakeHeaders.add("X-VM-ID", vmData.getVmId());
            }

            System.out.println("🔄 [" + vmData.getName() + "] 开始建立STOMP连接...");
            try {
                System.out.println("📞 [" + vmData.getName() + "] 尝试连接到WebSocket端点: " + wsUrl);
                stompSession = stompClient.connect(wsUrl, handshakeHeaders, connectHeaders, sessionHandler).get();
                System.out.println("✅ [" + vmData.getName() + "] WebSocket连接成功！");
            } catch (Exception ex) {
                System.err.println("❌ [" + vmData.getName() + "] WebSocket连接失败: " + ex.getMessage());
                throw ex;
            }

            // 等待连接建立
            System.out.println("⏳ [" + vmData.getName() + "] 等待连接稳定...");
            Thread.sleep(1000);

            if (stompSession != null && stompSession.isConnected()) {
                System.out.println("📡 [" + vmData.getName() + "] 开始订阅消息队列...");
                // 订阅回复队列
                stompSession.subscribe("/user/queue/reply", new MockStompFrameHandler());
                System.out.println("✅ [" + vmData.getName() + "] 已订阅个人回复队列: /user/queue/reply");
                stompSession.subscribe("/topic/vm/" + vmData.getVmId(), new MockStompFrameHandler());
                System.out.println("✅ [" + vmData.getName() + "] 已订阅VM专题队列: /topic/vm/" + vmData.getVmId());

                // 发送CONNECT协议消息
                System.out.println("📤 [" + vmData.getName() + "] 发送CONNECT协议消息...");
                Map<String, Object> connectMessage = createProtocolMessage(ProtocolType.CONNECT);
                Map<String, Object> data = new HashMap<>();
                data.put("sessionId", sessionId);
                data.put("vmId", vmData.getVmId());
                data.put("capabilities", vmData.getCapabilities());
                data.put("supportedMLAlgorithms", Arrays.asList("FEDERATED_AVERAGING", "FEDERATED_PROXIMAL"));
                connectMessage.put("data", data);

                stompSession.send("/app/protocol", connectMessage);
                System.out.println("📤 [" + vmData.getName() + "] CONNECT消息发送完成，消息ID: " + connectMessage.get("id"));

                connected = true;
                System.out.println("🎉 [" + vmData.getName() + "] WebSocket连接完全建立！");

                // 启动心跳机制
                startHeartbeat();
                System.out.println("💓 [" + vmData.getName() + "] 心跳机制已启动");
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
            // 先取消已存在的心跳任务，避免重复启动
            if (!heartbeatExecutor.isTerminated()) {
                heartbeatExecutor.shutdownNow();
                heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
            }
            heartbeatExecutor.scheduleAtFixedRate(() -> {
                try {
                    sendHeartbeat();
                } catch (Exception e) {
                    // 记录错误但不中断心跳
                    System.err.println("💔 " + vmData.getName() + " 心跳发送失败: " + e.getMessage());
                }
            }, 10, 60, TimeUnit.SECONDS); // 对齐服务端心跳间隔60秒
        }
    }

    /**
     * 发送心跳消息
     */
    private void sendHeartbeat() throws Exception {
        try {
            // 使用同步锁检查连接状态
            synchronized (sessionLock) {
                if (!connected || stompSession == null || !stompSession.isConnected()) {
                    System.out.println("💔 " + vmData.getName() + " 心跳检测到连接断开，跳过本次心跳");
                    return;
                }
            }

            Map<String, Object> heartbeatMessage = createProtocolMessage(ProtocolType.HEARTBEAT);
            Map<String, Object> data = new HashMap<>();
            data.put("status", "ACTIVE");
            data.put("cpuUsage", 20 + Math.random() * 50);
            data.put("memoryUsage", 40 + Math.random() * 40);
            data.put("gpuUsage", vmData.getGpuCount() > 0 ? 30 + Math.random() * 50 : 0);
            heartbeatMessage.put("data", data);

            sendStompMessage(heartbeatMessage);
        } catch (Exception e) {
            System.err.println("⚠️ " + vmData.getName() + " 心跳发送失败: " + e.getMessage());
            synchronized (sessionLock) {
                connected = false; // 标记连接已断开
            }
        }
    }

    /**
     * 模拟训练轮次
     */
    public void simulateTrainingRound(String taskId, int round) throws Exception {
        try {
            // 根据VM性能调整训练时间
            int baseTrainingTime = vmData.getBaseTrainingTime();
            Thread.sleep(baseTrainingTime + (int)(Math.random() * 500));

            // 生成基于VM能力的训练结果
            TrainingMetrics metrics = generateTrainingMetrics(round);

            System.out.println("🔄 " + vmData.getName() + " 开始第" + round + "轮训练模拟...");

            Map<String, Object> modelUpload = createProtocolMessage(ProtocolType.MODEL_UPLOAD);

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

            // 使用正确的连接管理发送消息
            sendStompMessage(modelUpload);

            System.out.println("✅ " + vmData.getName() + " 第" + round + "轮训练完成，精度: " +
                String.format("%.3f", metrics.getAccuracy()) + ", 损失: " + String.format("%.3f", metrics.getLoss()));

        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " 第" + round + "轮训练失败: " + e.getMessage());
            throw e; // 重新抛出异常，让测试能够正确检测到错误
        }
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
     * 生成模拟模型参数 - 优化为小尺寸以避免WebSocket消息过大
     */
    private Map<String, Object> generateMockModelParameters() {
        Map<String, Object> parameters = new HashMap<>();

        // 临时使用小尺寸模型参数进行WebSocket连接稳定性测试
        // 减小到最小尺寸以诊断连接问题
        Map<String, Object> weights = new HashMap<>();
        weights.put("layer1", generateRandomMatrix(10, 20)); // 简化尺寸：10x20 = 200个参数
        weights.put("layer2", generateRandomMatrix(5, 10));  // 5x10 = 50个参数
        weights.put("output", generateRandomMatrix(3, 5));   // 3x5 = 15个参数
        parameters.put("weights", weights);

        // 相应减少偏置向量大小
        Map<String, Object> biases = new HashMap<>();
        biases.put("layer1", generateRandomVector(10)); // 10个偏置
        biases.put("layer2", generateRandomVector(5));  // 5个偏置
        biases.put("output", generateRandomVector(3));  // 3个偏置
        parameters.put("biases", biases);

        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        // 小尺寸参数量: 200 + 50 + 15 + 10 + 5 + 3 = 283个参数
        metadata.put("parameterCount", 283);
        metadata.put("modelSize", 2264); // 大约2KB
        metadata.put("checksum", "sha256:" + vmData.getVmId() + "_" + System.currentTimeMillis());
        parameters.put("metadata", metadata);

        return parameters;
    }

    /**
     * 生成随机矩阵（实际数值）
     */
    private double[][] generateRandomMatrix(int rows, int cols) {
        double[][] matrix = new double[rows][cols];
        Random random = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = random.nextGaussian() * 0.1; // 小的随机值
            }
        }
        return matrix;
    }

    /**
     * 生成随机向量（实际数值）
     */
    private double[] generateRandomVector(int size) {
        double[] vector = new double[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            vector[i] = random.nextGaussian() * 0.1; // 小的随机值
        }
        return vector;
    }

    /**
     * 创建协议消息的基本结构
     * 使用符合协议v1.4.1标准的客户端消息ID格式
     */
    private Map<String, Object> createProtocolMessage(ProtocolType type) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type.name()); // 使用字符串格式，真实环境中WebSocket只能传输字符串
        message.put("id", generateClientMessageId()); // 使用标准化的客户端ID格式
        message.put("timestamp", Instant.now().toString());
        message.put("vmId", vmData.getVmId());
        return message;
    }

    /**
     * 生成符合协议v1.4.1标准的客户端消息ID
     * 格式: client-{timestamp}-{random}
     * @return 标准化的客户端消息ID
     */
    private String generateClientMessageId() {
        long timestamp = System.currentTimeMillis(); // 13位Unix毫秒时间戳
        int random = new SecureRandom().nextInt(1000000); // 0-999999的随机数
        return String.format("client-%d-%06d", timestamp, random);
    }

    /**
     * 发送STOMP消息 - 确保WebSocket连接的强健性
     */
    private void sendStompMessage(Map<String, Object> message) throws Exception {
        String messageType = (String) message.get("type");
        String messageId = (String) message.get("id");
        int maxRetries = 3;

        System.out.println("📤 [" + vmData.getName() + "] 准备发送消息: " + messageType + " (ID: " + messageId + ")");

        // 计算消息大小
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(message);
            int messageSize = jsonMessage.getBytes("UTF-8").length;
            System.out.println("📊 [" + vmData.getName() + "] 消息大小: " + messageSize + " bytes (" + String.format("%.2f", messageSize / 1024.0) + " KB)");

            // 如果是MODEL_UPLOAD消息，额外打印模型参数信息
            if ("MODEL_UPLOAD".equals(messageType)) {
                Map<String, Object> data = (Map<String, Object>) message.get("data");
                if (data != null && data.containsKey("parameters")) {
                    Map<String, Object> parameters = (Map<String, Object>) data.get("parameters");
                    if (parameters != null && parameters.containsKey("metadata")) {
                        Map<String, Object> metadata = (Map<String, Object>) parameters.get("metadata");
                        if (metadata != null) {
                            System.out.println("🧠 [" + vmData.getName() + "] 模型参数量: " + metadata.get("parameterCount"));
                            System.out.println("💾 [" + vmData.getName() + "] 模型大小: " + metadata.get("modelSize") + " bytes");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ [" + vmData.getName() + "] 无法计算消息大小: " + e.getMessage());
        }

        // 使用同步锁避免并发冲突
        synchronized (sessionLock) {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    // 检查并确保WebSocket连接
                    if (stompSession == null || !stompSession.isConnected()) {
                        System.out.println("🔄 [" + vmData.getName() + "] WebSocket连接异常，尝试重连 (第" + attempt + "次)");
                        reconnectWebSocketRobust();
                    }

                    // 发送消息
                    if (stompSession != null && stompSession.isConnected()) {
                        System.out.println("🚀 [" + vmData.getName() + "] 正在发送 " + messageType + " 消息到 /app/protocol...");
                        long startTime = System.currentTimeMillis();
                        stompSession.send("/app/protocol", message);
                        long endTime = System.currentTimeMillis();
                        System.out.println("✅ [" + vmData.getName() + "] " + messageType + " 消息发送成功！用时: " + (endTime - startTime) + "ms");
                        return; // 成功发送，退出重试循环
                    } else {
                        throw new RuntimeException("WebSocket连接未建立");
                    }

                } catch (Exception e) {
                    System.err.println("❌ [" + vmData.getName() + "] " + messageType + " 消息发送失败 (尝试" + attempt + "/" + maxRetries + "): " + e.getMessage());
                    System.err.println("🔍 [" + vmData.getName() + "] 错误详情: " + e.getClass().getSimpleName());

                    if (e.getCause() != null) {
                        System.err.println("🔍 [" + vmData.getName() + "] 根本原因: " + e.getCause().getMessage());
                    }

                    if (attempt < maxRetries) {
                        // 等待后重试，增加随机延迟避免雷群效应
                        int baseDelay = 1000 * attempt;
                        int randomDelay = (int)(Math.random() * 1000);
                        System.out.println("⏳ [" + vmData.getName() + "] 等待 " + (baseDelay + randomDelay) + "ms 后重试...");
                        Thread.sleep(baseDelay + randomDelay);
                    } else {
                        // 最后一次重试失败，抛出异常
                        throw new Exception("WebSocket连接持续失败，无法发送消息: " + messageType);
                    }
                }
            }
        }
    }

    /**
     * 强健的WebSocket重连机制
     */
    private void reconnectWebSocketRobust() {
        try {
            // 关闭现有连接
            if (stompSession != null) {
                try {
                    stompSession.disconnect();
                } catch (Exception e) {
                    // 忽略断开连接时的异常
                }
                stompSession = null;
            }

            // 等待一段时间
            Thread.sleep(500);

            // 重新建立连接（使用保存的WebSocket URL）
            if (this.websocketUrl == null) {
                throw new RuntimeException("WebSocket URL未初始化，无法重连");
            }
            System.out.println("🔄 " + vmData.getName() + " 使用保存的URL重连: " + this.websocketUrl);
            connectWebSocket(this.websocketUrl);

            if (stompSession != null && stompSession.isConnected()) {
                System.out.println("✅ " + vmData.getName() + " WebSocket重连成功");
                connected = true;
                // 重新启动心跳机制
                startHeartbeat();
                System.out.println("💓 " + vmData.getName() + " 心跳机制已重启");
            } else {
                System.err.println("❌ " + vmData.getName() + " WebSocket重连失败");
                connected = false;
            }

        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " WebSocket重连异常: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * 带重试机制的STOMP消息发送
     */
    private void sendStompMessageWithRetry(Map<String, Object> message, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (stompSession != null && stompSession.isConnected()) {
                    stompSession.send("/app/protocol", message);
                    return; // 发送成功，退出重试循环
                } else {
                    System.out.println("⚠️ " + vmData.getName() + " WebSocket连接断开，尝试重连 (" + attempt + "/" + maxRetries + ")");
                    if (attempt < maxRetries) {
                        Thread.sleep(500); // 短暂等待后重试
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ " + vmData.getName() + " 发送消息失败 (尝试 " + attempt + "/" + maxRetries + "): " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500 * attempt); // 递增退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("消息发送失败，已达到最大重试次数: " + maxRetries);
    }

    /**
     * 确保WebSocket连接
     */
    private void ensureConnection() {
        if (stompSession == null || !stompSession.isConnected()) {
            System.out.println("🔄 " + vmData.getName() + " 检测到连接断开，尝试重连...");
            reconnectWebSocket();
        }
    }

    /**
     * 重新连接WebSocket
     */
    private void reconnectWebSocket() {
        try {
            // 先断开现有连接
            if (stompSession != null) {
                try {
                    stompSession.disconnect();
                } catch (Exception e) {
                    // 忽略断开连接时的异常
                }
            }

            // 等待一段时间后重连
            Thread.sleep(1000);

            // 模拟重连成功（在实际环境中，这里应该重新建立连接）
            connected = true;
            System.out.println("✅ " + vmData.getName() + " WebSocket重连成功");
            // 重新启动心跳机制
            startHeartbeat();
            System.out.println("💓 " + vmData.getName() + " 心跳机制已重启");

        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " WebSocket重连失败: " + e.getMessage());
            connected = false;
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
            System.out.println("STOMP连接已建立: " + vmData.getName());
            System.out.println("认证信息 - Token: " + (token != null && token.length() > 20 ? token.substring(0, 20) + "..." : token));
            System.out.println("认证信息 - VmId: " + vmId);

            // 连接建立后，认证信息已通过STOMP CONNECT帧的Authorization头部传递
            // 符合协议文档的最佳实践
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

                // 添加空值检查
                if (messageData == null) {
                    System.err.println("收到空的STOMP消息: " + vmData.getName());
                    return;
                }

                String type = (String) messageData.get("type");

                // 检查type字段是否为null
                if (type == null) {
                    System.err.println("收到STOMP消息但type字段为null，消息内容: " + messageData + " from VM: " + vmData.getName());
                    return;
                }

                System.out.println("收到STOMP消息: " + type + " from VM: " + vmData.getName());

                // 处理不同类型的消息 - 使用ProtocolType枚举比较
                if (isProtocolType(type, ProtocolType.CONNECT_ACK)) {
                    System.out.println("✅ " + vmData.getName() + " 连接确认");
                } else if (isProtocolType(type, ProtocolType.TRAINING_START_COMMAND_NOTIFICATION) ||
                           isProtocolType(type, ProtocolType.TRAINING_START)) {
                    System.out.println("📨 " + vmData.getName() + " 收到训练开始指令");
                    // 自动响应训练指令
                    handleTrainingCommand(messageData);
                } else if (isProtocolType(type, ProtocolType.GLOBAL_MODEL_UPDATE)) {
                    System.out.println("📨 " + vmData.getName() + " 收到全局模型更新");
                    // 处理全局模型更新
                    handleGlobalModelUpdate(messageData);
                } else if (isProtocolType(type, ProtocolType.TASK_START)) {
                    System.out.println("📨 " + vmData.getName() + " 收到任务启动指令");
                    // 处理任务启动
                    handleTaskStart(messageData);
                } else if (isProtocolType(type, ProtocolType.FEDERATED_TASK_START)) {
                    System.out.println("📨 " + vmData.getName() + " 收到联邦学习任务启动指令");
                    // 处理联邦学习任务启动
                    handleFederatedTaskStart(messageData);
                } else if (isProtocolType(type, ProtocolType.MODEL_UPDATE_ACK) ||
                           isProtocolType(type, ProtocolType.HEARTBEAT_ACK)) {
                    System.out.println("✅ " + vmData.getName() + " 收到确认消息: " + type);
                    // 对于ACK消息，只需要记录，不需要特殊处理
                } else {
                    System.out.println("📨 " + vmData.getName() + " 收到未知类型消息: " + type);
                    // 对于未知消息，尝试作为通用训练指令处理
                    if (messageData.containsKey("taskId")) {
                        System.out.println("🤖 " + vmData.getName() + " 尝试作为训练指令处理");
                        handleTrainingCommand(messageData);
                    }
                }
            } catch (Exception e) {
                System.err.println("处理STOMP消息失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查消息类型是否匹配协议枚举
     */
    private boolean isProtocolType(String messageType, ProtocolType protocolType) {
        return protocolType.name().equals(messageType);
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

    /**
     * 处理训练指令 - 自动响应后端训练请求
     */
    private void handleTrainingCommand(Map<String, Object> messageData) {
        try {
            // 从消息中提取训练参数
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data == null) return;

            String taskId = (String) data.get("taskId");
            Object roundObj = data.get("round");
            int round = roundObj instanceof Number ? ((Number) roundObj).intValue() : 1;

            System.out.println("🤖 " + vmData.getName() + " 开始自动训练响应: taskId=" + taskId + ", round=" + round);

            // 使用线程池异步执行训练，避免阻塞消息处理
            if (messageExecutor == null) {
                messageExecutor = Executors.newSingleThreadScheduledExecutor();
            }

            messageExecutor.submit(() -> {
                try {
                    simulateTrainingRound(taskId, round);
                } catch (Exception e) {
                    System.err.println("❌ " + vmData.getName() + " 自动训练响应失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " 处理训练指令失败: " + e.getMessage());
        }
    }

    /**
     * 处理全局模型更新
     */
    private void handleGlobalModelUpdate(Map<String, Object> messageData) {
        try {
            System.out.println("📥 " + vmData.getName() + " 处理全局模型更新");

            // 模拟接收和处理全局模型
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                Object modelVersion = data.get("version");
                System.out.println("✅ " + vmData.getName() + " 全局模型更新完成，版本: " + modelVersion);

                // 发送模型更新确认
                Map<String, Object> ackMessage = createProtocolMessage(ProtocolType.MODEL_UPDATE_ACK);
                Map<String, Object> ackData = new HashMap<>();
                ackData.put("vmId", vmData.getVmId());
                ackData.put("modelVersion", modelVersion);
                ackData.put("updateTime", Instant.now().toString());
                ackMessage.put("data", ackData);

                sendStompMessage(ackMessage);
            }
        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " 处理全局模型更新失败: " + e.getMessage());
        }
    }

    /**
     * 处理任务启动指令
     */
    private void handleTaskStart(Map<String, Object> messageData) {
        try {
            System.out.println("🚀 " + vmData.getName() + " 处理任务启动指令");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                String taskId = (String) data.get("taskId");
                System.out.println("✅ " + vmData.getName() + " 任务启动确认: " + taskId);

                // 发送任务启动确认
                Map<String, Object> startAck = createProtocolMessage(ProtocolType.TASK_START_ACK);
                Map<String, Object> startData = new HashMap<>();
                startData.put("vmId", vmData.getVmId());
                startData.put("taskId", taskId);
                startData.put("status", "READY");
                startData.put("readyTime", Instant.now().toString());
                startAck.put("data", startData);

                sendStompMessage(startAck);
            }
        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " 处理任务启动指令失败: " + e.getMessage());
        }
    }

    /**
     * 处理联邦学习任务启动指令
     */
    private void handleFederatedTaskStart(Map<String, Object> messageData) {
        try {
            System.out.println("🚀 " + vmData.getName() + " 处理联邦学习任务启动指令");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) messageData.get("data");
            if (data != null) {
                String taskId = (String) data.get("taskId");
                System.out.println("✅ " + vmData.getName() + " 联邦学习任务启动确认: " + taskId);

                // 发送任务准备就绪确认
                Map<String, Object> readyAck = createProtocolMessage(ProtocolType.TRAINING_START_ACK);
                Map<String, Object> readyData = new HashMap<>();
                readyData.put("vmId", vmData.getVmId());
                readyData.put("taskId", taskId);
                readyData.put("status", "READY_FOR_TRAINING");
                readyData.put("capabilities", vmData.getCapabilities());
                readyData.put("readyTime", Instant.now().toString());
                readyAck.put("data", readyData);

                sendStompMessage(readyAck);

                // 启动主动训练监控
                startActiveTrainingMonitoring(taskId);
            }
        } catch (Exception e) {
            System.err.println("❌ " + vmData.getName() + " 处理联邦学习任务启动指令失败: " + e.getMessage());
        }
    }

    /**
     * 启动主动训练监控 - 定期检查是否需要执行训练
     */
    private void startActiveTrainingMonitoring(String taskId) {
        System.out.println("🔍 " + vmData.getName() + " 启动主动训练监控: " + taskId);

        // 使用现有的消息执行器来定期检查训练状态
        if (messageExecutor == null) {
            messageExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        // 定期执行模拟训练轮次
        for (int round = 1; round <= 8; round++) { // 最多8轮训练
            final int currentRound = round;

            messageExecutor.submit(() -> {
                try {
                    // 等待一段时间模拟训练间隔
                    Thread.sleep(5000 * currentRound); // 每轮间隔递增

                    System.out.println("🎯 " + vmData.getName() + " 主动执行第" + currentRound + "轮训练");
                    simulateTrainingRound(taskId, currentRound);

                } catch (Exception e) {
                    System.err.println("❌ " + vmData.getName() + " 主动训练第" + currentRound + "轮失败: " + e.getMessage());
                }
            });
        }
    }
}