# WebSocket 协议废弃接口 - v1.4 移除说明

## 概述

本文档描述了 v1.4 版本中废弃和移除的 WebSocket 接口和实现方式。这些变更主要是为了统一消息类型管理，提高类型安全性和代码维护性，同时明确区分虚拟机端和后端的协议实现职责。

## 主要变更内容

### 协议实现职责明确化
- 为所有消息类型添加了实现职责标识（🔵 虚拟机端实现，🟢 后端实现）
- 每个消息类型现在包含详细的实现说明：
  - **消息作用**: 该消息的具体用途和业务意义
  - **虚拟机端实现**: 虚拟机如何发送或处理该消息
  - **后端实现**: 后端如何发送或处理该消息

### 通知消息命名规范化
- 统一采用 `_NOTIFICATION` 后缀的命名约定
- 前缀与对应的协议消息保持一致
- 明确区分协议层消息和业务层通知消息

## 1. 废弃的实现方式

### 1.1 硬编码字符串消息类型

**移除原因**: 类型不安全，容易出现拼写错误，难以维护

#### 原有实现方式 (已废弃)

```java
// WebSocketProtocolService.java 中的硬编码字符串
sendToVmTopic(vmId, mapOf(
    "type", "DATASET_CREATED",      // ❌ 硬编码字符串
    "vmId", vmId,
    "datasetId", datasetId,
    // ...
));

sendToVmTopic(vmId, mapOf(
    "type", "TRAINING_STARTED",     // ❌ 硬编码字符串
    "vmId", vmId,
    "taskId", taskId,
    // ...
));
```

#### 新的实现方式

```java
// 使用统一的枚举类型，采用_NOTIFICATION后缀
sendToVmTopic(vmId, buildNotificationMessage(
    MessageType.DATASET_CREATE_NOTIFICATION,   // ✅ 类型安全的枚举，新命名约定
    vmId,
    datasetData
));

sendToVmTopic(vmId, buildNotificationMessage(
    MessageType.TRAINING_START_NOTIFICATION,  // ✅ 类型安全的枚举，新命名约定
    vmId,
    trainingData
));
```

### 1.2 废弃的字符串常量列表

以下硬编码字符串消息类型在 v1.4 中被移除：

```java
// 数据集相关通知 (已移除，采用新的_NOTIFICATION命名约定)
"DATASET_CREATED"       → MessageType.DATASET_CREATE_NOTIFICATION
"DATASET_ROWS_APPENDED" → MessageType.DATASET_APPEND_ROWS_NOTIFICATION
"DATASET_COMPLETED"     → MessageType.DATASET_COMPLETE_NOTIFICATION

// 训练相关通知 (已移除，采用新的_NOTIFICATION命名约定)
"TRAINING_START_COMMAND"  → MessageType.TRAINING_START_COMMAND_NOTIFICATION
"TRAINING_STARTED"        → MessageType.TRAINING_START_NOTIFICATION
"TRAINING_START_FAILED"   → MessageType.TRAINING_START_FAILURE_NOTIFICATION
"TRAINING_STOP_COMMAND"   → MessageType.TRAINING_STOP_COMMAND_NOTIFICATION
"TRAINING_PROGRESS_QUERY" → MessageType.TRAINING_PROGRESS_QUERY_NOTIFICATION
"TRAINING_PROGRESS_UPDATE" → MessageType.TRAINING_PROGRESS_UPDATE_NOTIFICATION

// 模型相关通知 (已移除，采用新的_NOTIFICATION命名约定)
"MODEL_DOWNLOAD_RESPONSE" → MessageType.MODEL_DOWNLOAD_NOTIFICATION
"GLOBAL_MODEL_UPDATE"     → MessageType.GLOBAL_MODEL_UPDATE

// 状态相关通知 (已移除，采用新的_NOTIFICATION命名约定)
"STATUS_UPDATED"          → MessageType.STATUS_UPDATE_NOTIFICATION
"STATUS_QUERY"            → MessageType.STATUS_QUERY
```

### 1.3 移除的模糊实现职责

#### 原有问题 (已解决)

在 v1.4 之前，WebSocket 协议文档存在以下问题：

```markdown
❌ 缺乏实现职责说明
#### 连接消息 (CONNECT)
{
  "type": "CONNECT",
  "vmId": "a1b2c3d4e5f678901234567890123456"
}

❌ 不明确谁应该发送或处理此消息
❌ 不清楚具体的实现要求
❌ 虚拟机和后端开发者需要猜测实现方式
```

#### 新的职责明确化 (v1.4+)

```markdown
✅ 明确的实现职责说明
#### 连接请求 (CONNECT) 🔵

**消息作用**: 虚拟机向后端发起 WebSocket 连接请求，建立通信通道并进行身份验证。

**虚拟机端实现**:
- 在启动时主动发送连接请求
- 提供虚拟机身份标识和能力信息
- 等待服务器的连接确认响应

**后端实现**:
- 接收虚拟机的连接请求
- 验证虚拟机身份和权限
- 发送连接确认响应并建立会话
```

#### 移除的模糊描述模式

```markdown
❌ 已移除 - 模糊的消息描述
"此消息用于训练控制"
"虚拟机发送状态信息"
"服务器处理模型数据"

✅ 新方式 - 具体的实现指导
**消息作用**: [具体的业务用途和意义]
**虚拟机端实现**: [详细的发送和处理逻辑]
**后端实现**: [详细的发送和处理逻辑]
```

## 2. 移除的代码模式

### 2.1 直接字符串拼接

```java
// ❌ 已移除的模式
Map<String, Object> message = new HashMap<>();
message.put("type", "TRAINING_" + action.toUpperCase());
message.put("vmId", vmId);
// ...
```

### 2.2 字符串常量类

```java
// ❌ 已移除 - 如果存在类似的常量类
public class MessageConstants {
    public static final String DATASET_CREATED = "DATASET_CREATED";
    public static final String TRAINING_STARTED = "TRAINING_STARTED";
    // ...
}
```

## 3. 不再支持的特性

### 3.1 动态消息类型构建

```java
// ❌ 不再支持动态构建消息类型
String messageType = "TRAINING_" + status + "_" + action;
sendMessage(messageType, data);
```

### 3.2 字符串消息类型验证

```java
// ❌ 不再需要字符串验证
private boolean isValidMessageType(String type) {
    return Arrays.asList("DATASET_CREATED", "TRAINING_STARTED", /*...*/).contains(type);
}
```

## 4. 迁移指南

### 4.1 客户端代码迁移

#### Python 客户端

```python
# 原有方式 (废弃)
def handle_message(self, message):
    if message.get("type") == "DATASET_CREATED":  # ❌ 字符串比较
        self.handle_dataset_created(message)

# 新方式 (使用新的_NOTIFICATION命名约定)
def handle_message(self, message):
    msg_type = MessageType(message.get("type"))   # ✅ 枚举转换
    if msg_type == MessageType.DATASET_CREATE_NOTIFICATION:
        self.handle_dataset_created(message)
```

#### Java 客户端

```java
// 原有方式 (废弃)
@MessageMapping("/websocket/message")
public void handleMessage(@Payload Map<String, Object> message) {
    String type = (String) message.get("type");
    if ("DATASET_CREATED".equals(type)) {         // ❌ 字符串比较
        handleDatasetCreated(message);
    }
}

// 新方式 (使用新的_NOTIFICATION命名约定)
@MessageMapping("/websocket/message")
public void handleMessage(@Payload ProtocolMessage message) {
    if (message.getType() == MessageType.DATASET_CREATE_NOTIFICATION) {  // ✅ 枚举比较
        handleDatasetCreated(message);
    }
}
```

### 4.2 服务端代码迁移

```java
// 原有方式 (废弃)
public void notifyDatasetCreated(String vmId, String datasetId) {
    Map<String, Object> notification = new HashMap<>();
    notification.put("type", "DATASET_CREATED");    // ❌ 硬编码
    notification.put("vmId", vmId);
    notification.put("datasetId", datasetId);
    sendToVmTopic(vmId, notification);
}

// 新方式 (使用新的_NOTIFICATION命名约定)
public void notifyDatasetCreated(String vmId, String datasetId) {
    Map<String, Object> data = Map.of("datasetId", datasetId);
    ProtocolMessage notification = MessageBuilder.buildNotification(
        MessageType.DATASET_CREATE_NOTIFICATION,  // ✅ 类型安全，新命名约定
        vmId,
        data
    );
    sendToVmTopic(vmId, notification);
}
```

## 5. 兼容性说明

### 5.1 向后兼容期

- **v1.4.0 - v1.4.2**: 同时支持新旧方式，旧方式会产生 deprecation 警告
- **v1.5.0+**: 完全移除字符串消息类型支持

### 5.2 迁移检查清单

客户端和服务端代码需要检查以下项目：

**代码重构相关**：
- [ ] 移除所有硬编码的消息类型字符串
- [ ] 更新消息处理逻辑使用枚举类型
- [ ] 更新单元测试使用新的消息类型
- [ ] 验证消息序列化/反序列化正常工作
- [ ] 更新错误处理逻辑
- [ ] 更新日志和调试信息

**实现职责相关**：
- [ ] 检查每个消息类型的实现职责标识（🔵虚拟机端 vs 🟢后端）
- [ ] 确认消息发送和处理逻辑符合职责分工
- [ ] 更新消息处理器以匹配新的实现要求
- [ ] 验证双向通信流程的完整性
- [ ] 检查心跳、状态查询等机制的实现职责
- [ ] 确认联邦学习协调逻辑符合后端职责

## 6. 常见问题

### 6.1 现有客户端是否需要立即更新？

需要。在 v1.4 兼容期内，服务端会同时发送新格式的消息。拒绝向后兼容

### 6.2 如何处理自定义消息类型？

如果有自定义的消息类型，需要：
1. 将其添加到 `MessageType` 枚举中
2. 更新协议文档
3. 通知所有客户端开发者

### 6.3 性能影响

枚举类型比字符串比较有更好的性能，且编译时类型检查可以避免运行时错误。

### 6.4 如何理解实现职责标识？

- **🔵 虚拟机端实现**: 表示此消息应由虚拟机端主动发送或主要处理
- **🟢 后端实现**: 表示此消息应由后端服务器发送或主要处理
- 所有消息都是双向的，两端都需要有处理逻辑，但职责标识表明主要的发起方

### 6.5 实现职责与原有逻辑不符怎么办？

如果发现现有实现与 v1.4 的职责分工不一致：
1. 优先按照 v1.4 的职责分工进行调整
2. 如确实需要调整职责分工，请联系协议维护团队
3. 确保修改后的职责分工在文档中得到更新

### 6.6 职责分工的设计原则是什么？

- **虚拟机端**：主动上报状态、响应指令、执行本地任务
- **后端**：任务调度、模型聚合、状态监控、指令下发
- **对等通信**：连接建立、心跳维护、错误处理等双方都有职责

## 7. 废弃的WebSocket端点配置 (v1.4.2)

### 7.1 移除SockJS支持

**移除原因**: SockJS与StandardWebSocketClient协议不兼容，导致连接失败，且在现代环境中不必要

#### 原有配置 (已废弃)

```java
// WebSocketConfig.java 中的SockJS配置
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*")
            .withSockJS(); // ❌ 已移除 - 与StandardWebSocketClient不兼容
}
```

#### 新的配置方式

```java
// 统一的原生WebSocket端点配置
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*"); // ✅ 原生WebSocket，兼容所有客户端
}
```

### 7.2 移除冗余的`/ws-native`端点

**移除原因**: 功能重复，增加配置复杂度，统一使用`/ws`端点

#### 原有双端点配置 (已废弃)

```java
// 冗余的双端点配置
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // SockJS端点
    registry.addEndpoint("/ws").withSockJS();

    // 原生WebSocket端点 - ❌ 功能重复，已移除
    registry.addEndpoint("/ws-native");
}
```

#### 简化后的单端点配置

```java
// 统一的单端点配置
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 所有客户端统一使用 /ws 端点
    registry.addEndpoint("/ws")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*");
}
```

### 7.3 移除的配置属性

#### WebSocketProperties.java 中的冗余属性

```java
// ❌ 不再使用的属性
private String nativeEndpoint = "/ws-native"; // 已移除，统一使用endpoint

// ✅ 保留的属性
private String endpoint = "/ws"; // 统一端点路径
```

### 7.4 客户端连接URL更新

#### 测试代码更新

```java
// 原有方式 (已废弃)
String websocketUrl = "ws://localhost:" + port + "/ws-native"; // ❌ 独立端点已移除

// 新方式
String websocketUrl = "ws://localhost:" + port + "/ws"; // ✅ 统一端点
```

### 7.5 移除的fallback机制

#### SockJS Fallback配置 (已移除)

```java
// ❌ 不再需要的SockJS fallback配置
.withSockJS()
    .setStreamBytesLimit(512 * 1024)     // 流字节限制
    .setSessionCookieNeeded(false)       // 会话cookie
    .setHeartbeatTime(60000)            // 心跳时间
    .setDisconnectDelay(30000)          // 断开延迟
    .setClientLibraryUrl("...") ;       // 客户端库URL
```

**移除理由**:
- 现代浏览器和Java客户端都原生支持WebSocket
- SockJS fallback增加不必要的复杂性
- 原生WebSocket在大数据传输时性能更优

### 7.6 迁移影响

#### 对现有代码的影响

**Java客户端**:
- 所有测试代码需要更新WebSocket URL为`/ws`
- 移除对`/ws-native`的引用
- StandardWebSocketClient配置保持不变

**配置文件**:
- WebSocketConfig.java移除SockJS相关配置
- 不再需要维护两套端点配置
- WebSocketProperties.java简化属性

**协议兼容性**:
- 原生WebSocket协议完全兼容STOMP
- 消息格式和传输机制保持不变
- 认证和拦截器逻辑不受影响

## 8. 相关文档

- [WebSocket 协议文档 v1.4](../WebSocket协议文档-中心化实现.md)
- [修改接口说明 v1.4](../modified/modified-interfaces-v1.4.md)
- [客户端迁移指南](./client-migration-guide-v1.4.md) (待创建)

## 9. 联系支持

如果在迁移过程中遇到问题，请：
1. 查看协议文档和示例代码
2. 检查单元测试用例
3. 联系开发团队获取支持