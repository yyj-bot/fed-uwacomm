package com.feduwacomm.controller;

import com.feduwacomm.dto.ProtocolAck;
import com.feduwacomm.dto.ProtocolMessage;
import com.feduwacomm.service.WebSocketProtocolService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketProtocolController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketProtocolController.class);

    private final WebSocketProtocolService protocolService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketProtocolController(WebSocketProtocolService protocolService, SimpMessagingTemplate messagingTemplate) {
        this.protocolService = protocolService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/protocol")
    public void onProtocol(@Payload ProtocolMessage message, Principal principal) {
        try {
            // 添加调试日志
            logger.info("收到WebSocket消息 - Type: {}, VmId: {}, Principal: {}",
                       message != null ? message.getType() : null,
                       message != null ? message.getVmId() : null,
                       principal != null ? principal.getName() : null);

            ProtocolAck ack = protocolService.handle(message);

            logger.debug("消息处理完成，发送ACK - AckType: {}", ack != null ? ack.getType() : null);

            // 点对点回复给当前用户
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/reply", ack);
            }
            // 同时发送到 vm 专属 topic 便于可视化
            if (message != null && message.getVmId() != null) {
                messagingTemplate.convertAndSend("/topic/vm/" + message.getVmId(), ack);
            }
        } catch (Exception e) {
            logger.error("WebSocket消息处理异常 - Message: {}, Principal: {}",
                        message != null ? message.getType() : null,
                        principal != null ? principal.getName() : null, e);
        }
    }
} 