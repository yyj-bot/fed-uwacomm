# WebSocket 协议消息类型重构 - v1.4 修改说明

## 概述

本文档描述了 v1.4 版本中对 WebSocket 协议消息类型的重构，主要解决了以下问题：
1. 消息类型定义分散（枚举 vs 字符串）
2. 协议消息与通知消息混合
3. 类型安全性不足
4. 维护困难

## v1.4 主要变更

### 协议文档更新
1. **协议版本升级**: 从 v1.3 升级到 v1.4，完善ACK响应机制和任务管理消息，补充服务端通知消息
2. **实现职责标识**: 为所有消息类型添加了 🔵(虚拟机端实现) 和 🟢(后端实现) 标识
3. **新增协议消息**: 添加了完整的ACK响应机制和任务管理相关消息类型
4. **通知消息规范化**: 统一采用 `_NOTIFICATION` 后缀的命名约定
5. **详细实现说明**: 为每个消息类型添加了具体的消息作用说明和虚拟机端/后端的实现要求

### 通知消息命名规范化
所有服务端主动推送的通知消息都采用 `_NOTIFICATION` 后缀，前缀与对应的协议消息保持一致，表明该消息处理已完成：

- `DATASET_CREATED` → `DATASET_CREATE_NOTIFICATION`
- `DATASET_ROWS_APPENDED` → `DATASET_APPEND_ROWS_NOTIFICATION`
- `DATASET_COMPLETED` → `DATASET_COMPLETE_NOTIFICATION`
- `TRAINING_STARTED` → `TRAINING_START_NOTIFICATION`
- `TRAINING_START_FAILED` → `TRAINING_START_FAILURE_NOTIFICATION`
- `TRAINING_STOP_COMMAND` → `TRAINING_STOP_COMMAND_NOTIFICATION`
- `TRAINING_PROGRESS_QUERY` → `TRAINING_PROGRESS_QUERY_NOTIFICATION`
- `TRAINING_PROGRESS_UPDATE` → `TRAINING_PROGRESS_UPDATE_NOTIFICATION`
- `MODEL_DOWNLOAD_RESPONSE` → `MODEL_DOWNLOAD_NOTIFICATION`
- `STATUS_UPDATED` → `STATUS_UPDATE_NOTIFICATION`

### 协议实现职责明确化

在 v1.4 版本中，我们为每个协议消息明确了实现职责，并提供了详细的实现指导：

#### 职责分工原则

**🔵 虚拟机端职责**:
- 主动发送状态信息和进度报告
- 响应后端的控制指令和查询请求
- 执行本地机器学习训练任务
- 管理本地数据集和模型文件
- 维护与后端的心跳连接

**🟢 后端职责**:
- 发送任务控制和管理指令
- 处理虚拟机状态信息并进行调度
- 执行联邦学习算法和模型聚合
- 管理全局模型和任务状态
- 发送业务层通知消息

#### 消息实现说明格式

每个消息类型现在都包含以下信息：
- **消息作用**: 该消息的具体用途和业务意义
- **虚拟机端实现**: 虚拟机如何发送或处理该消息
- **后端实现**: 后端如何发送或处理该消息

#### 关键实现要点

1. **连接管理**: 虚拟机负责发起连接并上报能力，后端负责验证和分配资源
2. **心跳机制**: 虚拟机主动发送心跳，后端监控连接状态
3. **训练控制**: 后端发送训练指令，虚拟机执行并反馈结果
4. **模型同步**: 虚拟机上传本地模型，后端聚合后下发全局模型
5. **状态监控**: 虚拟机上报状态，后端查询并收集信息
6. **数据管理**: 虚拟机创建和管理数据集，后端确认和存储

## 重构方案

### 1. 统一消息类型枚举

将原本分散在 `ProtocolType` 枚举和服务端字符串常量中的消息类型，统一到一个枚举中：

```java
public enum MessageType {
    // ========== 连接管理消息 ==========
    CONNECT,                    // 客户端连接请求 🔵
    CONNECT_ACK,               // 服务器连接确认 🟢

    // ========== 心跳消息 ==========
    HEARTBEAT,                 // 客户端心跳 🔵
    HEARTBEAT_ACK,             // 服务器心跳响应 🟢

    // ========== 虚拟机控制消息 ==========
    VM_START,                  // 启动虚拟机命令 🟢
    VM_STOP,                   // 停止虚拟机命令 🟢

    // ========== 训练控制消息（协议层） ==========
    TRAINING_START,            // 开始训练命令 🟢
    TRAINING_START_ACK,        // 训练开始确认 🟢
    TRAINING_START_RESPONSE,   // 训练开始响应 🔵
    TRAINING_START_RESPONSE_ACK, // 训练开始响应确认 🟢
    TRAINING_STOP,             // 停止训练命令 🟢
    TRAINING_STOP_ACK,         // 训练停止确认 🟢
    TRAINING_PROGRESS,         // 训练进度查询 🟢
    TRAINING_PROGRESS_ACK,     // 训练进度查询确认 🟢
    TRAINING_PROGRESS_RESPONSE, // 训练进度响应 🔵
    TRAINING_PROGRESS_RESPONSE_ACK, // 训练进度响应确认 🟢

    // ========== 训练控制消息（业务层通知） ==========
    TRAINING_START_COMMAND_NOTIFICATION,    // 训练开始指令通知 🟢
    TRAINING_START_NOTIFICATION,             // 训练开始成功通知 🟢
    TRAINING_START_FAILURE_NOTIFICATION,     // 训练开始失败通知 🟢
    TRAINING_STOP_COMMAND_NOTIFICATION,      // 训练停止指令通知 🟢
    TRAINING_PROGRESS_QUERY_NOTIFICATION,    // 训练进度查询指令通知 🟢
    TRAINING_PROGRESS_UPDATE_NOTIFICATION,   // 训练进度更新通知 🟢

    // ========== 模型传输消息 ==========
    MODEL_UPLOAD,              // 本地模型上传 🔵
    MODEL_DOWNLOAD,            // 全局模型下载请求 🔵
    MODEL_DOWNLOAD_NOTIFICATION,   // 模型下载通知 🟢
    GLOBAL_MODEL_UPDATE,            // 全局模型更新通知 🟢
    MODEL_UPDATE_ACK,          // 模型更新确认 🔵

    // ========== 任务管理消息 ==========
    TASK_START,                // 通用任务启动 🟢
    TASK_START_ACK,            // 通用任务启动确认 🟢
    FEDERATED_TASK_START,      // 联邦学习任务启动 🟢
    FEDERATED_TASK_START_ACK,  // 联邦学习任务启动确认 🔵

    // ========== 状态查询消息 ==========
    STATUS_QUERY,              // 状态查询请求 🟢
    STATUS_RESPONSE,           // 状态查询响应 🔵
    STATUS_UPDATE_NOTIFICATION, // 状态更新通知 🟢
    BATCH_STATUS_QUERY,        // 批量状态查询 🟢
    BATCH_STATUS_RESPONSE,     // 批量状态响应 🔵

    // ========== 数据集同步消息（协议层） ==========
    DATASET_CREATE,            // 创建数据集 🔵
    DATASET_CREATE_ACK,        // 创建数据集确认 🟢
    DATASET_APPEND_ROWS,       // 追加数据行 🔵
    DATASET_APPEND_ROWS_ACK,   // 追加数据行确认 🟢
    DATASET_COMPLETE,          // 完成数据集上传 🔵
    DATASET_COMPLETE_ACK,      // 完成数据集确认 🟢
    DATASET_STATUS_QUERY,      // 数据集状态查询 🟢
    DATASET_STATUS_RESPONSE,   // 数据集状态响应 🟢
    DATASET_DELETE,            // 删除数据集 🔵
    DATASET_DELETE_ACK,        // 删除数据集确认 🟢

    // ========== 数据集同步消息（业务层通知） ==========
    DATASET_CREATE_NOTIFICATION,           // 数据集创建通知 🟢
    DATASET_APPEND_ROWS_NOTIFICATION,      // 数据行添加通知 🟢
    DATASET_COMPLETE_NOTIFICATION,         // 数据集完成通知 🟢

    // ========== 错误处理消息 ==========
    ERROR,                     // 通用错误 🔵🟢
    CONNECTION_ERROR,          // 连接错误 🔵🟢
    MESSAGE_ERROR,             // 消息错误 🔵🟢
    STATUS_QUERY_ERROR         // 状态查询错误 🔵🟢
}
```

### 2. 消息分类说明

**图例**:
- 🔵 **虚拟机端实现**: 消息由虚拟机端发起或处理
- 🟢 **后端实现**: 消息由后端服务器发起或处理

**消息类型分类**:

#### 协议消息 (Protocol Messages)
- **特点**: 双向交互，遵循请求-响应模式
- **用途**: 正式的协议层通信
- **示例**: `TRAINING_START` → `TRAINING_START_ACK` → `TRAINING_START_RESPONSE` → `TRAINING_START_RESPONSE_ACK`

#### 通知消息 (Notification Messages)
- **特点**: 单向推送，无需响应确认
- **用途**: 实时状态更新和事件通知
- **命名约定**: 使用 `_NOTIFICATION` 后缀，前缀与协议消息保持一致
- **示例**: `TRAINING_START_NOTIFICATION`, `DATASET_CREATE_NOTIFICATION`, `STATUS_UPDATE_NOTIFICATION`

#### 指令消息 (Command Messages)
- **特点**: 服务端向客户端发送的业务指令
- **用途**: 具体的操作指令传递
- **命名约定**: 指令类通知消息也使用 `_COMMAND_NOTIFICATION` 后缀
- **示例**: `TRAINING_START_COMMAND_NOTIFICATION`, `TRAINING_STOP_COMMAND_NOTIFICATION`

### 3. 代码重构建议

#### 3.1 更新 ProtocolType 枚举

```java
// 原有的 ProtocolType.java 应该重命名为 MessageType.java
public enum MessageType {
    // 按上述分类添加所有消息类型
    // ...
}
```

#### 3.2 更新 WebSocketProtocolService

```java
// 替换所有硬编码字符串
// 原代码:
sendToVmTopic(vmId, mapOf("type", "DATASET_CREATED", ...));

// 重构后:
sendToVmTopic(vmId, buildNotificationMessage(MessageType.DATASET_CREATE_NOTIFICATION, ...));
```

#### 3.3 添加消息构建工具类

```java
public class MessageBuilder {
    public static Map<String, Object> buildProtocolMessage(MessageType type, String vmId, Map<String, Object> data) {
        // 构建标准协议消息
    }

    public static Map<String, Object> buildNotificationMessage(MessageType type, String vmId, Map<String, Object> data) {
        // 构建通知消息
    }

    public static Map<String, Object> buildCommandMessage(MessageType type, String vmId, Map<String, Object> data) {
        // 构建指令消息
    }
}
```

## 4. 迁移计划

### 阶段1: 扩展枚举
1. 在 `ProtocolType` 中添加所有缺失的消息类型
2. 标记原有字符串常量为 `@Deprecated`
3. 保持向后兼容

### 阶段2: 重构服务代码
1. 更新 `WebSocketProtocolService` 使用枚举
2. 添加消息构建工具类
3. 更新所有消息发送点

### 阶段3: 清理和优化
1. 移除废弃的字符串常量
2. 重命名 `ProtocolType` 为 `MessageType`
3. 完善单元测试覆盖

## 5. 受影响的文件

- `feduwacomm-pojo/src/main/java/com/feduwacomm/dto/ProtocolType.java`
- `feduwacomm-server/src/main/java/com/feduwacomm/service/WebSocketProtocolService.java`
- `python-vm/src/feduwacomm/ml/api/websocket_client.py`
- 所有测试文件

## 6. 向后兼容性

重构过程中需要确保：
1. 现有客户端代码继续工作
2. 协议文档同步更新
3. 测试用例全面覆盖新的消息类型

## 7. 消息ID格式标准化 (v1.4.1)

### 变更背景

在 v1.4 版本中发现协议文档对消息ID格式的定义过于模糊，缺乏具体的位数规范和验证标准，导致实现不一致的问题。为确保系统的一致性和可维护性，对消息ID格式进行标准化定义。

### 标准化规范

#### 原有模糊定义 (已废弃)
```markdown
❌ 缺乏具体规范
- 客户端消息：client-{timestamp}-{random}
- 服务器消息：server-{timestamp}-{random}
- 命令消息：cmd-{timestamp}-{random}

❌ 存在的问题
- timestamp 位数不明确
- random 位数和格式不统一
- 缺少验证规则和示例
```

#### 新的标准化定义 (v1.4.1)
```markdown
✅ 精确的格式规范
格式：{prefix}-{timestamp}-{random}

组成部分：
- prefix: client(6字符)/server(6字符)/cmd(3字符)
- timestamp: 13位Unix毫秒时间戳
- random: 6位随机数字(000000-999999，左补零)

完整示例：
- client-1704067200000-123456 (26字符)
- server-1704067200000-654321 (26字符)
- cmd-1704067200000-789012 (23字符)

验证规则：
- 正则表达式：^(client|server)-\d{13}-\d{6}$|^cmd-\d{13}-\d{6}$
- 时间戳有效性检查
- 随机数格式验证
```

### 实现影响

#### 影响范围
- **仅限 backend-springboot 模块**：按照新标准实现消息ID生成
- **协议文档**：更新为精确的格式定义和验证规则
- **不涉及其他模块**：python-vm 和 frontend 模块保持现有实现

#### 代码变更
```java
// 更新前 - MessageBuilder.java (无ID生成功能)
public static Map<String, Object> createMessage(ProtocolType type) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", type.name());
    return message; // ❌ 缺少id字段
}

// 更新后 - MessageBuilder.java (新增标准化ID生成)
public static Map<String, Object> createMessage(ProtocolType type) {
    Map<String, Object> message = new HashMap<>();
    message.put("type", type.name());
    message.put("id", generateServerId()); // ✅ 符合标准格式
    return message;
}

// 新增ID生成方法
public static String generateServerId() {
    return generateMessageId("server");
}

private static String generateMessageId(String prefix) {
    long timestamp = System.currentTimeMillis();
    int random = new SecureRandom().nextInt(1000000);
    return String.format("%s-%d-%06d", prefix, timestamp, random);
}
```

### 迁移指南

#### 实施步骤
1. **协议文档更新**：已完成精确格式定义
2. **backend实现更新**：
   - MessageBuilder.java: 添加标准化ID生成方法
   - MockVirtualMachine.java: 更新测试代码ID格式
3. **验证测试**：确保生成的ID符合新规范

#### 验证方法
```java
// 验证生成的消息ID格式
public static boolean isValidMessageId(String messageId) {
    return messageId.matches("^(client|server)-\\d{13}-\\d{6}$|^cmd-\\d{13}-\\d{6}$");
}
```

### 向后兼容性

**不提供向后兼容性**：直接采用新的标准化格式，确保系统一致性。

### 相关文档
- [WebSocket 协议文档 v1.4](../WebSocket协议文档-中心化实现.md) - 已更新消息ID生成规则
- [废弃接口说明 v1.4](../removed/removed-interfaces-v1.4.md)

## 8. WebSocket端点统一优化 (v1.4.2)

### 变更背景

在v1.4版本的实际测试中发现，当前WebSocket配置存在两个端点(`/ws`和`/ws-native`)导致的协议不匹配问题：

- **协议冲突**：`/ws`端点配置了SockJS支持，但`StandardWebSocketClient`只支持原生WebSocket协议
- **连接失败**：尝试用原生WebSocket客户端连接SockJS端点时出现HTTP 400错误
- **架构复杂**：维护两套端点增加了配置复杂度和测试负担

### 统一方案

为了解决协议不匹配问题并简化架构，决定采用**单一原生WebSocket端点**策略：

#### 端点配置变更

```java
// 变更前 - WebSocketConfig.java (双端点配置)
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // SockJS端点 - 与StandardWebSocketClient不兼容
    registry.addEndpoint("/ws")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*")
            .withSockJS(); // ❌ SockJS导致协议不匹配

    // 原生WebSocket端点 - 功能重复
    registry.addEndpoint("/ws-native")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*"); // ❌ 冗余配置
}

// 变更后 - WebSocketConfig.java (统一端点配置)
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 统一使用原生WebSocket端点，移除SockJS支持
    registry.addEndpoint("/ws")
            .addInterceptors(handshakeAuthInterceptor)
            .setAllowedOriginPatterns("*"); // ✅ 简化配置，避免协议冲突

    // 移除 /ws-native 端点，统一使用 /ws
}
```

#### 客户端连接更新

```java
// 变更前 - CompleteFederatedLearningFlowTest.java
String websocketUrl = "ws://localhost:" + port + "/ws-native"; // ❌ 使用独立的原生端点

// 变更后 - CompleteFederatedLearningFlowTest.java
String websocketUrl = "ws://localhost:" + port + "/ws"; // ✅ 使用统一端点，去除SockJS
```

### 技术优势

#### 性能优化
- **减少开销**：移除SockJS的额外协议层，降低连接建立时间
- **内存优化**：单一端点减少资源占用
- **传输效率**：原生WebSocket协议在大数据传输(如1GB模型参数)时性能更优

#### 架构简化
- **配置统一**：所有客户端使用相同的连接端点
- **维护简化**：减少配置项和测试场景
- **兼容性**：2024年所有现代环境都原生支持WebSocket

#### 联邦学习优化
- **大文件传输**：原生WebSocket更适合MODEL_UPLOAD等大消息传输
- **连接稳定性**：避免SockJS fallback机制在稳定网络环境中的不必要复杂性
- **Java客户端友好**：`StandardWebSocketClient`原生支持，无需额外依赖

### 实施影响

#### 配置文件变更
- **WebSocketConfig.java**：移除SockJS配置，统一端点路径
- **WebSocketProperties.java**：保持现有配置，仅使用`endpoint`属性

#### 测试代码更新
- **CompleteFederatedLearningFlowTest.java**：更新WebSocket连接URL
- **MockVirtualMachine.java**：确保客户端配置与服务器匹配

#### 文档更新
- **协议文档**：更新连接示例为统一的`/ws`端点
- **集成指南**：简化客户端连接说明

### 向后兼容性

**不提供向后兼容性**：直接采用统一端点配置，确保架构清晰和性能最优。

### 迁移检查清单

**服务器端**：
- [ ] 更新WebSocketConfig.java移除SockJS配置
- [ ] 移除/ws-native端点注册
- [ ] 验证/ws端点配置正确

**客户端测试**：
- [ ] 更新CompleteFederatedLearningFlowTest.java使用/ws端点
- [ ] 验证MockVirtualMachine.java配置匹配
- [ ] 测试WebSocket连接建立成功

**功能验证**：
- [ ] 验证CONNECT协议消息正常工作
- [ ] 测试MODEL_UPLOAD大消息传输稳定
- [ ] 确认心跳和状态查询功能正常

### 预期效果

- **解决HTTP 400连接错误**：消除StandardWebSocketClient与SockJS的协议冲突
- **提升大数据传输稳定性**：原生WebSocket更适合联邦学习的大模型参数传输
- **简化开发和维护**：统一的端点配置减少复杂性
- **保持现有性能配置**：维持1GB传输限制和心跳机制设置

## 9. 文档更新

重构完成后需要更新：
- WebSocket 协议文档
- API 参考文档
- 客户端集成指南
- 错误处理指南