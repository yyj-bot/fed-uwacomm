package com.feduwacomm.dto;

import java.time.LocalDateTime;

/**
 * WebSocket消息传输对象
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public class WebSocketMessage {

    private String type; // 消息类型
    private String content; // 消息内容
    private String sender; // 发送者
    private String receiver; // 接收者（可选，用于点对点消息）
    private LocalDateTime timestamp; // 时间戳
    private Object data; // 附加数据

    // 构造函数
    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(String type, String content, String sender) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(String type, String content, String sender, String receiver) {
        this.type = type;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = LocalDateTime.now();
    }

    // Getter和Setter方法
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "WebSocketMessage{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}