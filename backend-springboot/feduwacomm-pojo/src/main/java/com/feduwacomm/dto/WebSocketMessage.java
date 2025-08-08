package com.feduwacomm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket消息传输对象
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private String type; // 消息类型
    private String content; // 消息内容
    private String sender; // 发送者
    private String receiver; // 接收者（可选，用于点对点消息）
    private LocalDateTime timestamp; // 时间戳
    private Object data; // 附加数据
}