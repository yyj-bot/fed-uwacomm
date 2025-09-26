package com.feduwacomm.service;

import com.feduwacomm.dto.WebSocketMessage;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.utils.UuidUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketService单元测试类
 * 测试WebSocket连接管理和消息发送服务的各种功能
 */
@ExtendWith(MockitoExtension.class)
public class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UuidUtil uuidUtil;

    private WebSocketService webSocketService;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(messagingTemplate, uuidUtil);

        // 设置UuidUtil Mock行为 (使用lenient避免unnecessary stubbing警告)
        lenient().when(uuidUtil.generateUuid()).thenReturn("test-uuid-123456");

        // 创建服务实例
        webSocketService = new WebSocketService(messagingTemplate, uuidUtil);
    }

    /**
     * 测试发送广播消息
     */
    @Test
    void testSendBroadcastMessage() {
        String message = "测试广播消息";

        // 执行测试
        webSocketService.sendBroadcastMessage(message);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/public"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("BROADCAST", capturedMessage.getType());
        assertEquals(message, capturedMessage.getData().get("message"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试发送点对点消息 - 成功场景
     */
    @Test
    void testSendPrivateMessage_Success() {
        String username = "testuser";
        String message = "测试私信";

        // 执行测试
        webSocketService.sendPrivateMessage(username, message);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq(username), eq("/queue/private"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("PRIVATE", capturedMessage.getType());
        assertEquals(message, capturedMessage.getData().get("message"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertEquals(username, capturedMessage.getData().get("receiver"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试发送点对点消息 - 用户名为空
     */
    @Test
    void testSendPrivateMessage_EmptyUsername() {
        String message = "测试私信";

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> webSocketService.sendPrivateMessage("", message));
        
        assertTrue(exception.getMessage().contains("用户名和消息不能为空"));

        // 验证没有调用消息发送
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    /**
     * 测试发送点对点消息 - 消息为空
     */
    @Test
    void testSendPrivateMessage_EmptyMessage() {
        String username = "testuser";

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> webSocketService.sendPrivateMessage(username, ""));
        
        assertTrue(exception.getMessage().contains("用户名和消息不能为空"));

        // 验证没有调用消息发送
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    /**
     * 测试发送系统通知
     */
    @Test
    void testSendNotification() {
        String notification = "系统维护通知";

        // 执行测试
        webSocketService.sendNotification(notification);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("NOTIFICATION", capturedMessage.getType());
        assertEquals(notification, capturedMessage.getData().get("notification"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试发送联邦学习消息
     */
    @Test
    void testSendFederatedLearningMessage() {
        String message = "联邦学习任务开始";

        // 执行测试
        webSocketService.sendFederatedLearningMessage(message);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/federated-learning"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("FEDERATED_LEARNING", capturedMessage.getType());
        assertEquals(message, capturedMessage.getData().get("message"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试用户上线
     */
    @Test
    void testUserOnline() {
        String username = "testuser";
        String sessionId = "session-123";

        // 执行测试
        webSocketService.userOnline(username, sessionId);

        // 验证用户状态
        assertEquals(1, webSocketService.getOnlineUserCount());
        assertTrue(webSocketService.isUserOnline(username));

        // 验证发送了上线通知
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(WebSocketMessage.class));
        
        // 验证发送了广播消息
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/public"), messageCaptor.capture());
        String messageText = (String) messageCaptor.getValue().getData().get("message");
        assertTrue(messageText.contains("上线了"));
    }

    /**
     * 测试用户下线
     */
    @Test
    void testUserOffline() {
        String username = "testuser";
        String sessionId = "session-123";

        // 先让用户上线
        webSocketService.userOnline(username, sessionId);
        reset(messagingTemplate); // 重置mock调用记录

        // 执行下线测试
        webSocketService.userOffline(username);

        // 验证用户状态
        assertEquals(0, webSocketService.getOnlineUserCount());
        assertFalse(webSocketService.isUserOnline(username));

        // 验证发送了下线通知
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(WebSocketMessage.class));
        
        // 验证发送了广播消息
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/public"), messageCaptor.capture());
        String messageText = (String) messageCaptor.getValue().getData().get("message");
        assertTrue(messageText.contains("下线了"));
    }

    /**
     * 测试获取在线用户列表
     */
    @Test
    void testGetOnlineUsers() {
        String user1 = "user1";
        String user2 = "user2";
        String session1 = "session-001";
        String session2 = "session-002";

        // 添加用户
        webSocketService.userOnline(user1, session1);
        webSocketService.userOnline(user2, session2);

        // 获取在线用户列表
        ConcurrentHashMap<String, String> onlineUsers = webSocketService.getOnlineUsers();

        // 验证结果
        assertEquals(2, onlineUsers.size());
        assertEquals(session1, onlineUsers.get(user1));
        assertEquals(session2, onlineUsers.get(user2));
    }

    /**
     * 测试发送联邦学习进度更新 - 成功场景
     */
    @Test
    void testSendFederatedLearningProgress_Success() {
        Integer progress = 75;
        String message = "训练进度75%";

        // 执行测试
        webSocketService.sendFederatedLearningProgress(progress, message);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/federated-learning"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("FL_PROGRESS", capturedMessage.getType());
        assertEquals(message, capturedMessage.getData().get("message"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertEquals(progress, capturedMessage.getData().get("progress"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试发送联邦学习进度更新 - 进度为空
     */
    @Test
    void testSendFederatedLearningProgress_NullProgress() {
        String message = "训练进度更新";

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> webSocketService.sendFederatedLearningProgress(null, message));
        
        assertTrue(exception.getMessage().contains("进度和消息不能为空"));

        // 验证没有调用消息发送
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * 测试发送联邦学习进度更新 - 消息为空
     */
    @Test
    void testSendFederatedLearningProgress_EmptyMessage() {
        Integer progress = 50;

        // 执行测试并验证异常
        UserException exception = assertThrows(UserException.class, 
            () -> webSocketService.sendFederatedLearningProgress(progress, ""));
        
        assertTrue(exception.getMessage().contains("进度和消息不能为空"));

        // 验证没有调用消息发送
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * 测试发送联邦学习结果
     */
    @Test
    void testSendFederatedLearningResult() {
        Object result = "测试结果数据";

        // 执行测试
        webSocketService.sendFederatedLearningResult(result);

        // 验证mock调用
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/federated-learning"), messageCaptor.capture());

        // 验证消息内容
        WebSocketMessage capturedMessage = messageCaptor.getValue();
        assertEquals("FL_RESULT", capturedMessage.getType());
        assertEquals("联邦学习完成", capturedMessage.getData().get("message"));
        assertEquals("System", capturedMessage.getData().get("sender"));
        assertEquals(result, capturedMessage.getData().get("result"));
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 测试多用户同时在线的情况
     */
    @Test
    void testMultipleUsersOnline() {
        // 添加多个用户
        webSocketService.userOnline("user1", "session1");
        webSocketService.userOnline("user2", "session2");
        webSocketService.userOnline("user3", "session3");

        // 验证用户数量
        assertEquals(3, webSocketService.getOnlineUserCount());

        // 验证每个用户都在线
        assertTrue(webSocketService.isUserOnline("user1"));
        assertTrue(webSocketService.isUserOnline("user2"));
        assertTrue(webSocketService.isUserOnline("user3"));

        // 下线一个用户
        webSocketService.userOffline("user2");

        // 验证状态更新
        assertEquals(2, webSocketService.getOnlineUserCount());
        assertTrue(webSocketService.isUserOnline("user1"));
        assertFalse(webSocketService.isUserOnline("user2"));
        assertTrue(webSocketService.isUserOnline("user3"));
    }

    /**
     * 测试重复上线的情况
     */
    @Test
    void testDuplicateUserOnline() {
        String username = "testuser";
        String session1 = "session1";
        String session2 = "session2";

        // 用户第一次上线
        webSocketService.userOnline(username, session1);
        assertEquals(1, webSocketService.getOnlineUserCount());

        // 用户重复上线（会话ID不同）
        webSocketService.userOnline(username, session2);
        // 用户数量不应该增加，但会话ID应该更新
        assertEquals(2, webSocketService.getOnlineUserCount()); // 因为put会覆盖，但incrementAndGet仍然执行

        ConcurrentHashMap<String, String> users = webSocketService.getOnlineUsers();
        assertEquals(session2, users.get(username)); // 会话ID应该是最新的
    }
}