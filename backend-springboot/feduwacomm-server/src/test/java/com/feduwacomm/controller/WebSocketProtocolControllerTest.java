package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.dto.ProtocolType;
import com.feduwacomm.service.WebSocketProtocolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocket协议控制器测试类
 * 测试WebSocket消息处理和路由功能
 * 使用单元测试方式，避免Spring上下文加载问题
 */
@ExtendWith(MockitoExtension.class)
public class WebSocketProtocolControllerTest {

    @Mock
    private WebSocketProtocolService protocolService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketProtocolController controller;
    private ProtocolMessage sampleMessage;
    private ProtocolAck sampleAck;
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(protocolService, messagingTemplate);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // v1.3: 准备协议消息，使用新格式
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("version", "1.0.0");
        messageData.put("supportedMLAlgorithms", java.util.List.of("RandomForest", "SVM", "NeuralNetwork"));
        messageData.put("systemInfo", Map.of(
                "os", "Ubuntu 20.04",
                "python", "3.8.10",
                "memory", "4GB"
        ));
        messageData.put("computeCapabilities", Map.of(
                "maxBatchSize", 1024,
                "parallelProcessing", true
        ));

        sampleMessage = ProtocolMessage.builder()
                .type(ProtocolType.CONNECT)
                .id("msg-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(messageData)
                .signature("test-signature")
                .build();

        // 准备协议ACK
        Map<String, Object> ackData = new HashMap<>();
        ackData.put("status", "ack");
        ackData.put("code", 200);

        sampleAck = ProtocolAck.builder()
                .type(ProtocolType.CONNECT_ACK)
                .id("ack-001")
                .timestamp(Instant.now())
                .vmId("vm-001")
                .data(ackData)
                .signature("ack-signature")
                .build();

        // Mock Principal
        mockPrincipal = mock(Principal.class);
        lenient().when(mockPrincipal.getName()).thenReturn("test-user");
    }

    /**
     * 测试协议消息处理 - 成功场景，有用户Principal
     */
    @Test
    void testOnProtocol_WithPrincipal_Success() {
        // Mock服务层返回ACK
        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // 调用控制器方法
        controller.onProtocol(sampleMessage, mockPrincipal);

        // 验证服务层调用
        verify(protocolService).handle(eq(sampleMessage));

        // 验证点对点消息发送
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/reply"),
                eq(sampleAck)
        );

        // 验证VM专属topic消息发送
        verify(messagingTemplate).convertAndSend(
                eq("/topic/vm/vm-001"),
                eq(sampleAck)
        );
    }

    /**
     * 测试协议消息处理 - 无用户Principal
     */
    @Test
    void testOnProtocol_WithoutPrincipal_Success() {
        // Mock服务层返回ACK
        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // 调用控制器方法，不传递Principal
        controller.onProtocol(sampleMessage, null);

        // 验证服务层调用
        verify(protocolService).handle(eq(sampleMessage));

        // 验证不发送点对点消息
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());

        // 验证VM专属topic消息发送
        verify(messagingTemplate).convertAndSend(
                eq("/topic/vm/vm-001"),
                eq(sampleAck)
        );
    }

    /**
     * 测试协议消息处理 - 消息为null
     */
    @Test
    void testOnProtocol_NullMessage() {
        // Mock服务层返回ACK
        when(protocolService.handle(any())).thenReturn(sampleAck);

        // 调用控制器方法，传递null消息
        controller.onProtocol(null, mockPrincipal);

        // 验证服务层调用
        verify(protocolService).handle(null);

        // 验证点对点消息发送
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/reply"),
                eq(sampleAck)
        );

        // 验证不发送VM专属topic消息（因为message为null）
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ProtocolAck.class));
    }

    /**
     * 测试协议消息处理 - 消息没有vmId
     */
    @Test
    void testOnProtocol_MessageWithoutVmId() {
        // 准备没有vmId的消息
        ProtocolMessage messageWithoutVmId = ProtocolMessage.builder()
                .type(ProtocolType.HEARTBEAT)
                .id("msg-002")
                .timestamp(Instant.now())
                .vmId(null)  // 没有vmId
                .data(new HashMap<>())
                .build();

        // Mock服务层返回ACK
        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // 调用控制器方法
        controller.onProtocol(messageWithoutVmId, mockPrincipal);

        // 验证服务层调用
        verify(protocolService).handle(eq(messageWithoutVmId));

        // 验证点对点消息发送
        verify(messagingTemplate).convertAndSendToUser(
                eq("test-user"),
                eq("/queue/reply"),
                eq(sampleAck)
        );

        // 验证不发送VM专属topic消息（因为vmId为null）
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/vm/null"), any(ProtocolAck.class));
    }

    /**
     * 测试不同协议类型的处理
     */
    @Test
    void testOnProtocol_DifferentProtocolTypes() {
        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // 测试不同的协议类型
        ProtocolType[] protocolTypes = {
                ProtocolType.CONNECT,
                ProtocolType.HEARTBEAT,
                ProtocolType.FEDERATED_TASK_START,
                ProtocolType.GRADIENT_UPLOAD,
                ProtocolType.VM_STATUS_QUERY,
                ProtocolType.DATASET_CREATE
        };

        for (ProtocolType type : protocolTypes) {
            // 创建不同类型的消息
            ProtocolMessage message = ProtocolMessage.builder()
                    .type(type)
                    .id("msg-" + type.name())
                    .timestamp(Instant.now())
                    .vmId("vm-test")
                    .data(new HashMap<>())
                    .build();

            // 调用控制器方法
            controller.onProtocol(message, mockPrincipal);
        }

        // 验证服务层被调用了6次
        verify(protocolService, times(6)).handle(any(ProtocolMessage.class));

        // 验证消息发送被调用了6次（每次发送两个消息：用户和VM topic）
        verify(messagingTemplate, times(6)).convertAndSendToUser(anyString(), anyString(), any());
        verify(messagingTemplate, times(6)).convertAndSend(anyString(), any(ProtocolAck.class));
    }

    /**
     * 测试服务异常处理
     */
    @Test
    void testOnProtocol_ServiceException() {
        // Mock服务层抛出异常
        when(protocolService.handle(any(ProtocolMessage.class)))
                .thenThrow(new RuntimeException("协议处理异常"));

        // 调用控制器方法应该不会抛出异常（异常在服务层处理）
        assertDoesNotThrow(() -> {
            controller.onProtocol(sampleMessage, mockPrincipal);
        });

        // 验证服务层调用
        verify(protocolService).handle(eq(sampleMessage));

        // 由于服务层抛出异常，不应该发送任何消息
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ProtocolAck.class));
    }

    /**
     * 测试消息模板异常处理
     */
    @Test
    void testOnProtocol_MessagingTemplateException() {
        // Mock服务层正常返回
        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // Mock消息模板抛出异常
        doThrow(new RuntimeException("消息发送异常"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // 调用控制器方法应该不会抛出异常
        assertDoesNotThrow(() -> {
            controller.onProtocol(sampleMessage, mockPrincipal);
        });

        // 验证服务层调用
        verify(protocolService).handle(eq(sampleMessage));

        // 验证至少尝试发送用户消息
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
    }

    /**
     * 测试消息数据完整性
     */
    @Test
    void testOnProtocol_MessageDataIntegrity() {
        // 准备包含完整数据的消息
        Map<String, Object> complexData = new HashMap<>();
        complexData.put("trainingRound", 5);
        complexData.put("accuracy", 0.85);
        complexData.put("loss", 0.25);
        complexData.put("metadata", Map.of("batchSize", 32, "epochs", 10));

        ProtocolMessage complexMessage = ProtocolMessage.builder()
                .type(ProtocolType.ROUND_START)
                .id("training-msg-001")
                .timestamp(Instant.now())
                .vmId("vm-complex")
                .data(complexData)
                .signature("complex-signature")
                .build();

        // Mock服务层返回对应的ACK
        ProtocolAck complexAck = ProtocolAck.builder()
                .type(ProtocolType.ROUND_START)
                .id("training-ack-001")
                .timestamp(Instant.now())
                .vmId("vm-complex")
                .data(Map.of("status", "processed", "code", 200))
                .build();

        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(complexAck);

        // 调用控制器方法
        controller.onProtocol(complexMessage, mockPrincipal);

        // 验证传递给服务层的消息数据完整性
        ArgumentCaptor<ProtocolMessage> messageCaptor = ArgumentCaptor.forClass(ProtocolMessage.class);
        verify(protocolService).handle(messageCaptor.capture());

        ProtocolMessage capturedMessage = messageCaptor.getValue();
        assertEquals(ProtocolType.ROUND_START, capturedMessage.getType());
        assertEquals("training-msg-001", capturedMessage.getId());
        assertEquals("vm-complex", capturedMessage.getVmId());
        assertEquals(complexData, capturedMessage.getData());

        // 验证发送的ACK数据完整性
        ArgumentCaptor<ProtocolAck> ackCaptor = ArgumentCaptor.forClass(ProtocolAck.class);
        verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), ackCaptor.capture());
        
        ProtocolAck capturedAck = ackCaptor.getValue();
        assertEquals(ProtocolType.ROUND_START, capturedAck.getType());
        assertEquals("vm-complex", capturedAck.getVmId());
        assertNotNull(capturedAck.getData());
    }

    /**
     * 测试控制器构造函数
     */
    @Test
    void testControllerConstruction() {
        // 测试正常构造
        WebSocketProtocolController newController = new WebSocketProtocolController(
                protocolService, messagingTemplate
        );
        assertNotNull(newController);

        // 测试构造函数参数验证（在实际应用中，Spring会处理这些）
        assertDoesNotThrow(() -> {
            new WebSocketProtocolController(null, messagingTemplate);
        });

        assertDoesNotThrow(() -> {
            new WebSocketProtocolController(protocolService, null);
        });
    }

    /**
     * 测试VM ID特殊字符处理
     */
    @Test
    void testOnProtocol_VmIdWithSpecialCharacters() {
        // 准备包含特殊字符的VM ID
        ProtocolMessage messageWithSpecialVmId = ProtocolMessage.builder()
                .type(ProtocolType.CONNECT)
                .id("msg-special")
                .timestamp(Instant.now())
                .vmId("vm-001_test.special-chars")
                .data(new HashMap<>())
                .build();

        when(protocolService.handle(any(ProtocolMessage.class))).thenReturn(sampleAck);

        // 调用控制器方法
        controller.onProtocol(messageWithSpecialVmId, mockPrincipal);

        // 验证VM专属topic路径构建正确
        verify(messagingTemplate).convertAndSend(
                eq("/topic/vm/vm-001_test.special-chars"),
                eq(sampleAck)
        );
    }
}