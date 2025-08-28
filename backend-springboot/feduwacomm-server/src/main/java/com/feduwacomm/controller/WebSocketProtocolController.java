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

    private final WebSocketProtocolService protocolService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketProtocolController(WebSocketProtocolService protocolService, SimpMessagingTemplate messagingTemplate) {
        this.protocolService = protocolService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/protocol")
    public void onProtocol(@Payload ProtocolMessage message, Principal principal) {
        ProtocolAck ack = protocolService.handle(message);
        // 点对点回复给当前用户
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/reply", ack);
        }
        // 同时发送到 vm 专属 topic 便于可视化
        if (message != null && message.getVmId() != null) {
            messagingTemplate.convertAndSend("/topic/vm/" + message.getVmId(), ack);
        }
    }
} 