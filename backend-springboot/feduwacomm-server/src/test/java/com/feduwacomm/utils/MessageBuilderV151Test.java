package com.feduwacomm.utils;

import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试MessageBuilder v1.5.1协议支持
 * 验证dataConfig字段的正确性
 */
class MessageBuilderV151Test {

    @Test
    void testBuildTrainingStartMessage_WithDataConfig() {
        // 准备测试数据
        String vmId = "vm-test-001";
        String taskId = "task-test-123";
        int roundNumber = 1;
        String mlAlgorithm = "FEDERATED_AVERAGING";
        Map<String, Object> hyperparameters = Map.of(
            "learningRate", 0.01,
            "batchSize", 32,
            "epochs", 100,
            "timeout", 300
        );
        Map<String, Object> globalModel = Map.of(
            "modelId", "global-1",
            "version", "v1.0",
            "downloadUrl", "/api/models/global-1"
        );
        String message = "请开始本地ML训练任务";
        String assignedDatasetId = "dataset-test-123";
        String dataPath = "/data/assigned/dataset-test-123";

        // 调用方法
        ProtocolMessage result = MessageBuilder.buildTrainingStartMessage(
            vmId, taskId, roundNumber, mlAlgorithm,
            hyperparameters, globalModel, message,
            assignedDatasetId, dataPath
        );

        // 验证基本属性
        assertNotNull(result, "返回的消息不应为null");
        assertEquals(ProtocolType.FEDERATED_TASK_START, result.getType(), "消息类型应为FEDERATED_TASK_START");
        assertEquals(vmId, result.getVmId(), "vmId应该正确");
        assertNotNull(result.getId(), "消息ID不应为null");
        assertNotNull(result.getTimestamp(), "时间戳不应为null");
        assertNotNull(result.getSignature(), "签名字段不应为null");

        // 验证data字段
        Map<String, Object> data = result.getData();
        assertNotNull(data, "data不应为null");
        assertEquals(taskId, data.get("taskId"), "taskId应该正确");
        assertEquals(roundNumber, data.get("roundNumber"), "roundNumber应该正确");
        assertEquals(mlAlgorithm, data.get("mlAlgorithm"), "mlAlgorithm应该正确");
        assertEquals(message, data.get("message"), "message应该正确");

        // 验证v1.5.1新增的dataConfig字段
        assertTrue(data.containsKey("dataConfig"), "应包含dataConfig字段");

        @SuppressWarnings("unchecked")
        Map<String, Object> dataConfig = (Map<String, Object>) data.get("dataConfig");
        assertNotNull(dataConfig, "dataConfig不应为null");
        assertEquals(assignedDatasetId, dataConfig.get("assignedDatasetId"),
            "dataConfig.assignedDatasetId应该正确");
        assertEquals(dataPath, dataConfig.get("dataPath"),
            "dataConfig.dataPath应该正确");

        // 验证hyperparameters
        assertTrue(data.containsKey("hyperparameters"), "应包含hyperparameters字段");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultHyperparameters = (Map<String, Object>) data.get("hyperparameters");
        assertEquals(0.01, resultHyperparameters.get("learningRate"));
        assertEquals(32, resultHyperparameters.get("batchSize"));

        // 验证globalModel
        assertTrue(data.containsKey("globalModel"), "应包含globalModel字段");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultGlobalModel = (Map<String, Object>) data.get("globalModel");
        assertEquals("global-1", resultGlobalModel.get("modelId"));
    }

    @Test
    void testBuildTrainingStartMessage_DataConfigNotNull() {
        // 测试即使传入null值，dataConfig也应该被创建（虽然实际使用中不应传null）
        ProtocolMessage result = MessageBuilder.buildTrainingStartMessage(
            "vm-001", "task-001", 1, "FEDERATED_AVERAGING",
            Map.of(), Map.of(), "test",
            "dataset-001", "/data/001"
        );

        Map<String, Object> data = result.getData();
        assertTrue(data.containsKey("dataConfig"), "dataConfig字段必须存在");

        @SuppressWarnings("unchecked")
        Map<String, Object> dataConfig = (Map<String, Object>) data.get("dataConfig");
        assertNotNull(dataConfig, "dataConfig不应为null");
        assertEquals("dataset-001", dataConfig.get("assignedDatasetId"));
        assertEquals("/data/001", dataConfig.get("dataPath"));
    }
}
