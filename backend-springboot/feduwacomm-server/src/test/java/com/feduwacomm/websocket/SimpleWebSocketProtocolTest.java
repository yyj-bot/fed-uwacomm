package com.feduwacomm.websocket;

import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * 简单的WebSocket协议测试
 * 测试协议消息创建和基本功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket协议基础功能测试")
class SimpleWebSocketProtocolTest {

    @Test
    @DisplayName("测试ProtocolMessage创建和基本属性")
    void testProtocolMessageCreation() {
        // Given
        Map<String, Object> data = Map.of(
            "taskId", "task-001",
            "round", 5,
            "vmId", "vm-001"
        );

        // When
        ProtocolMessage message = ProtocolMessage.builder()
            .type(ProtocolType.GRADIENT_UPLOAD)
            .vmId("vm-001")
            .data(data)
            .timestamp(Instant.now().toString())
            .build();

        // Then
        assertThat(message).isNotNull();
        assertThat(message.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD);
        assertThat(message.getVmId()).isEqualTo("vm-001");
        assertThat(message.getData()).containsEntry("taskId", "task-001");
        assertThat(message.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("测试ProtocolAck创建和基本属性")
    void testProtocolAckCreation() {
        // Given
        Map<String, Object> ackData = Map.of(
            "status", "RECEIVED",
            "taskId", "task-001",
            "timestamp", Instant.now().toString()
        );

        // When
        ProtocolAck ack = ProtocolAck.builder()
            .type(ProtocolType.GRADIENT_UPLOAD_ACK)
            .data(ackData)
            .build();

        // Then
        assertThat(ack).isNotNull();
        assertThat(ack.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD_ACK);
        assertThat(ack.getData()).containsEntry("status", "RECEIVED");
        assertThat(ack.getData()).containsEntry("taskId", "task-001");
    }

    @Test
    @DisplayName("测试所有新增的协议类型枚举值")
    void testNewProtocolTypes() {
        // 验证第一阶段：核心协议
        assertThat(ProtocolType.GRADIENT_UPLOAD).isNotNull();
        assertThat(ProtocolType.GRADIENT_UPLOAD_ACK).isNotNull();
        assertThat(ProtocolType.AGGREGATION_START).isNotNull();
        assertThat(ProtocolType.AGGREGATION_START_ACK).isNotNull();
        assertThat(ProtocolType.AGGREGATION_COMPLETE).isNotNull();
        assertThat(ProtocolType.AGGREGATION_COMPLETE_ACK).isNotNull();
        assertThat(ProtocolType.GLOBAL_MODEL_BROADCAST).isNotNull();
        assertThat(ProtocolType.GLOBAL_MODEL_BROADCAST_ACK).isNotNull();

        // 验证第二阶段：轮次管理协议
        assertThat(ProtocolType.ROUND_START).isNotNull();
        assertThat(ProtocolType.ROUND_START_ACK).isNotNull();
        assertThat(ProtocolType.ROUND_COMPLETE).isNotNull();
        assertThat(ProtocolType.ROUND_COMPLETE_ACK).isNotNull();
        assertThat(ProtocolType.MODEL_TYPE_NEGOTIATION).isNotNull();
        assertThat(ProtocolType.MODEL_TYPE_NEGOTIATION_ACK).isNotNull();

        // 验证第三阶段：增强功能协议
        assertThat(ProtocolType.ALGORITHM_CONFIG).isNotNull();
        assertThat(ProtocolType.ALGORITHM_CONFIG_ACK).isNotNull();
        assertThat(ProtocolType.GRADIENT_UPLOAD_PREPARE).isNotNull();
        assertThat(ProtocolType.GRADIENT_UPLOAD_PREPARE_ACK).isNotNull();
        assertThat(ProtocolType.AGGREGATION_NOTIFICATION).isNotNull();
        assertThat(ProtocolType.STRATEGY_SWITCH_NOTIFICATION).isNotNull();
        assertThat(ProtocolType.STRATEGY_SWITCH_ACK).isNotNull();
    }

    @Test
    @DisplayName("测试协议消息数据结构")
    void testProtocolMessageDataStructures() {
        // 测试GRADIENT_UPLOAD消息结构
        Map<String, Object> gradientUploadData = createGradientUploadData();
        ProtocolMessage gradientMsg = createProtocolMessage(ProtocolType.GRADIENT_UPLOAD, gradientUploadData);

        assertThat(gradientMsg.getData()).containsKey("taskId");
        assertThat(gradientMsg.getData()).containsKey("round");
        assertThat(gradientMsg.getData()).containsKey("training_result");

        // 测试ALGORITHM_CONFIG消息结构
        Map<String, Object> algorithmConfigData = createAlgorithmConfigData();
        ProtocolMessage algorithmMsg = createProtocolMessage(ProtocolType.ALGORITHM_CONFIG, algorithmConfigData);

        assertThat(algorithmMsg.getData()).containsKey("algorithm");
        assertThat(algorithmMsg.getData()).containsKey("parameters");
    }

    @Test
    @DisplayName("测试协议消息序列化兼容性")
    void testProtocolMessageSerialization() {
        // Given
        ProtocolMessage originalMessage = createProtocolMessage(
            ProtocolType.GRADIENT_UPLOAD,
            createGradientUploadData()
        );

        // When & Then - 验证消息可以被正确构造和访问
        assertThat(originalMessage.getType()).isEqualTo(ProtocolType.GRADIENT_UPLOAD);
        assertThat(originalMessage.getVmId()).isEqualTo("vm-001");
        assertThat(originalMessage.getData()).isInstanceOf(Map.class);
        assertThat(originalMessage.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("测试错误处理协议")
    void testErrorProtocols() {
        // Given
        Map<String, Object> errorData = Map.of(
            "errorType", "PROCESSING_ERROR",
            "errorMessage", "协议处理失败",
            "taskId", "task-001"
        );

        // When
        ProtocolMessage errorMsg = createProtocolMessage(ProtocolType.ERROR, errorData);

        // Then
        assertThat(errorMsg.getType()).isEqualTo(ProtocolType.ERROR);
        assertThat(errorMsg.getData()).containsEntry("errorType", "PROCESSING_ERROR");
        assertThat(errorMsg.getData()).containsEntry("errorMessage", "协议处理失败");
    }

    @Test
    @DisplayName("测试状态查询和响应协议")
    void testStatusProtocols() {
        // 测试状态查询
        Map<String, Object> queryData = Map.of(
            "taskId", "task-001",
            "queryType", "AGGREGATION_STATUS"
        );

        ProtocolMessage queryMsg = createProtocolMessage(ProtocolType.STATUS_QUERY, queryData);
        assertThat(queryMsg.getData()).containsEntry("queryType", "AGGREGATION_STATUS");

        // 测试状态响应
        Map<String, Object> responseData = Map.of(
            "status", "IN_PROGRESS",
            "taskId", "task-001",
            "progress", 0.75
        );

        ProtocolMessage responseMsg = createProtocolMessage(ProtocolType.STATUS_RESPONSE, responseData);
        assertThat(responseMsg.getData()).containsEntry("status", "IN_PROGRESS");
        assertThat(responseMsg.getData()).containsEntry("progress", 0.75);
    }

    @Test
    @DisplayName("测试数据集操作协议")
    void testDatasetProtocols() {
        // 测试数据集创建
        Map<String, Object> createData = Map.of(
            "datasetId", "dataset-001",
            "columns", Arrays.asList("feature1", "feature2", "feature3", "label"),
            "expectedRows", 10000
        );

        ProtocolMessage createMsg = createProtocolMessage(ProtocolType.DATASET_CREATE, createData);
        assertThat(createMsg.getData()).containsEntry("datasetId", "dataset-001");
        assertThat(createMsg.getData()).containsKey("columns");

        // 测试数据集完成
        Map<String, Object> completeData = Map.of(
            "datasetId", "dataset-001",
            "actualRows", 9876,
            "status", "COMPLETED"
        );

        ProtocolMessage completeMsg = createProtocolMessage(ProtocolType.DATASET_COMPLETE, completeData);
        assertThat(completeMsg.getData()).containsEntry("actualRows", 9876);
        assertThat(completeMsg.getData()).containsEntry("status", "COMPLETED");
    }

    // =============== 辅助方法 ===============

    private ProtocolMessage createProtocolMessage(ProtocolType type, Map<String, Object> data) {
        return ProtocolMessage.builder()
            .type(type)
            .vmId("vm-001")
            .data(data)
            .timestamp(Instant.now().toString())
            .build();
    }

    private Map<String, Object> createGradientUploadData() {
        return Map.of(
            "taskId", "task-001",
            "round", 5,
            "training_result", Map.of(
                "model_parameters", Map.of(
                    "feature_importances_", Arrays.asList(0.25, 0.30, 0.20, 0.25),
                    "n_estimators", 100
                ),
                "training_metadata", Map.of(
                    "samples_count", 1500,
                    "local_accuracy", 0.87,
                    "algorithm", "RandomForest",
                    "framework", "sklearn"
                )
            )
        );
    }

    private Map<String, Object> createAlgorithmConfigData() {
        return Map.of(
            "taskId", "task-001",
            "algorithm", "FedAvg",
            "parameters", Map.of(
                "learningRate", 0.01,
                "momentum", 0.9,
                "batchSize", 32
            )
        );
    }
}