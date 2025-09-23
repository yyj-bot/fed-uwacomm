# SpringBoot WebSocket使用指南

本文档说明如何在FedUWAComm项目中使用WebSocket功能。

## 1. 项目结构

```
backend-springboot/
├── feduwacomm-server/src/main/
│   ├── java/com/feduwacomm/
│   │   ├── config/
│   │   │   ├── WebSocketConfig.java           # WebSocket配置
│   │   │   └── WebSocketEventListener.java    # 事件监听器
│   │   ├── controller/
│   │   │   ├── WebSocketController.java       # WebSocket消息处理
│   │   │   └── WebSocketTestController.java   # 测试接口
│   │   └── service/
│   │       └── WebSocketService.java          # WebSocket服务
│   └── resources/
│       ├── static/
│       │   └── websocket-test.html            # 测试页面
│       └── application.yml                    # 配置文件
└── feduwacomm-pojo/src/main/java/com/feduwacomm/
    └── dto/
        └── WebSocketMessage.java              # 消息传输对象
```

## 2. WebSocket端点

### 2.1 连接端点
- **SockJS端点**: `/ws` - 支持SockJS的WebSocket连接
- **原生WebSocket端点**: `/ws-native` - 原生WebSocket连接

### 2.2 消息目标

#### 客户端发送消息目标（前缀：/app）
- `/app/chat` - 发送聊天消息
- `/app/join` - 用户加入聊天室
- `/app/leave` - 用户离开聊天室
- `/app/private-message` - 发送私信
- `/app/notification` - 发送系统通知
- `/app/federated-learning` - 发送联邦学习消息

#### 服务器广播消息目标（前缀：/topic）
- `/topic/public` - 公共聊天消息
- `/topic/notifications` - 系统通知
- `/topic/federated-learning` - 联邦学习消息
- `/topic/user-count` - 在线用户统计

#### 点对点消息目标（前缀：/user）
- `/user/{username}/queue/private` - 私信消息

## 3. 消息格式

### 3.1 WebSocketMessage结构
```json
{
    "type": "CHAT|JOIN|LEAVE|PRIVATE|NOTIFICATION|FEDERATED_LEARNING|SYSTEM",
    "content": "消息内容",
    "sender": "发送者用户名",
    "receiver": "接收者用户名（可选）",
    "timestamp": "2024-01-01T12:00:00",
    "data": "附加数据（可选）"
}
```

### 3.2 消息类型说明
- **CHAT**: 普通聊天消息
- **JOIN**: 用户加入聊天室
- **LEAVE**: 用户离开聊天室
- **PRIVATE**: 私信消息
- **NOTIFICATION**: 系统通知
- **FEDERATED_LEARNING**: 联邦学习相关消息
- **SYSTEM**: 系统消息

## 4. 客户端连接示例

### 4.1 JavaScript客户端
```javascript
// 连接WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    
    // 订阅公共消息
    stompClient.subscribe('/topic/public', function (message) {
        console.log('收到公共消息:', JSON.parse(message.body));
    });
    
    // 订阅私信
    stompClient.subscribe('/user/queue/private', function (message) {
        console.log('收到私信:', JSON.parse(message.body));
    });
    
    // 订阅系统通知
    stompClient.subscribe('/topic/notifications', function (message) {
        console.log('收到系统通知:', JSON.parse(message.body));
    });
    
    // 订阅联邦学习消息
    stompClient.subscribe('/topic/federated-learning', function (message) {
        console.log('收到联邦学习消息:', JSON.parse(message.body));
    });
});
```

### 4.2 发送消息示例
```javascript
// 发送聊天消息
stompClient.send("/app/chat", {}, JSON.stringify({
    type: 'CHAT',
    content: 'Hello World!',
    sender: 'username'
}));

// 发送私信
stompClient.send("/app/private-message", {}, JSON.stringify({
    type: 'PRIVATE',
    content: '私信内容',
    sender: 'sender_username',
    receiver: 'receiver_username'
}));

// 发送系统通知
stompClient.send("/app/notification", {}, JSON.stringify({
    type: 'NOTIFICATION',
    content: '系统通知内容',
    sender: 'username'
}));
```

## 5. RESTful API测试接口

### 5.1 发送广播消息
```bash
POST /api/websocket/broadcast
Content-Type: application/json

{
    "message": "广播消息内容"
}
```

### 5.2 发送私信
```bash
POST /api/websocket/private
Content-Type: application/json

{
    "username": "target_user",
    "message": "私信内容"
}
```

### 5.3 发送系统通知
```bash
POST /api/websocket/notification
Content-Type: application/json

{
    "notification": "系统通知内容"
}
```

### 5.4 发送联邦学习消息
```bash
POST /api/websocket/federated-learning
Content-Type: application/json

{
    "message": "联邦学习消息内容"
}
```

### 5.5 获取在线用户统计
```bash
GET /api/websocket/stats
```

### 5.6 检查用户在线状态
```bash
GET /api/websocket/online/{username}
```

### 5.7 发送联邦学习进度
```bash
POST /api/websocket/fl-progress
Content-Type: application/json

{
    "progress": 50,
    "message": "训练进度50%"
}
```

### 5.8 发送联邦学习结果
```bash
POST /api/websocket/fl-result
Content-Type: application/json

{
    "result": {
        "accuracy": 0.95,
        "loss": 0.05,
        "epochs": 100
    }
}
```

## 6. 测试页面

访问 `http://localhost:8080/api/websocket-test.html` 可以使用Web界面测试WebSocket功能。

### 6.1 测试页面功能
- 连接/断开WebSocket
- 发送聊天消息
- 发送私信
- 发送系统通知
- 发送联邦学习消息
- 实时查看各种消息
- 显示在线用户统计

## 7. 联邦学习集成

### 7.1 在Python模块中集成WebSocket
```python
import websocket
import json
import threading

class WebSocketClient:
    def __init__(self, url="ws://localhost:8080/ws-native"):
        self.url = url
        self.ws = None
        
    def connect(self):
        self.ws = websocket.WebSocketApp(
            self.url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close
        )
        
        # 在后台线程中运行WebSocket
        wst = threading.Thread(target=self.ws.run_forever)
        wst.daemon = True
        wst.start()
    
    def on_open(self, ws):
        print("WebSocket连接已建立")
        
    def on_message(self, ws, message):
        data = json.loads(message)
        print(f"收到消息: {data}")
        
    def on_error(self, ws, error):
        print(f"WebSocket错误: {error}")
        
    def on_close(self, ws, close_status_code, close_msg):
        print("WebSocket连接已关闭")
    
    def send_federated_learning_message(self, message):
        if self.ws:
            data = {
                "type": "FEDERATED_LEARNING",
                "content": message,
                "sender": "python_client"
            }
            self.ws.send(json.dumps(data))
```

### 7.2 在联邦学习过程中发送进度
```python
def send_progress(progress, message):
    # 通过RESTful API发送进度
    import requests
    
    url = "http://localhost:8080/api/websocket/fl-progress"
    data = {
        "progress": progress,
        "message": message
    }
    
    response = requests.post(url, json=data)
    return response.json()
```

## 8. 安全配置

### 8.1 跨域配置
WebSocket已配置允许跨域访问：
```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("*")
    .withSockJS();
```

### 8.2 认证集成（可选）
可以在WebSocket连接时添加认证：
```java
@EventListener
public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    // 获取用户认证信息
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    String username = sha.getUser().getName();
    
    // 验证用户权限
    if (isUserAuthorized(username)) {
        // 允许连接
    } else {
        // 拒绝连接
    }
}
```

## 9. 性能优化

### 9.1 消息大小限制
```java
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // 配置消息转换器
        return false;
    }
}
```

### 9.2 连接池配置
```yaml
spring:
  websocket:
    max-text-message-size: 8192
    max-binary-message-size: 8192
```

## 10. 监控和日志

### 10.1 连接监控
- 在线用户数量统计
- 连接状态监控
- 消息发送统计

### 10.2 日志记录
```java
@Slf4j
public class WebSocketController {
    @MessageMapping("/chat")
    public WebSocketMessage handleChatMessage(@Payload WebSocketMessage chatMessage) {
        log.info("收到聊天消息: {}", chatMessage);
        // 处理消息
    }
}
```

## 11. 故障排除

### 11.1 常见问题
1. **连接失败**: 检查WebSocket端点是否正确
2. **消息丢失**: 检查消息格式是否正确
3. **跨域问题**: 确认CORS配置
4. **认证失败**: 检查用户权限配置

### 11.2 调试方法
1. 使用浏览器开发者工具查看WebSocket连接
2. 查看服务器日志
3. 使用测试页面进行功能验证
4. 检查网络连接状态

## 12. 扩展功能

### 12.1 消息持久化
可以添加消息存储功能，将重要消息保存到数据库。

### 12.2 消息队列集成
可以集成RabbitMQ或Kafka来处理大量消息。

### 12.3 集群支持
可以配置Redis来支持WebSocket集群部署。

### 12.4 消息加密
可以添加消息加密功能来保护敏感数据。 