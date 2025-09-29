# 水声联邦学习系统 WebSocket 消息格式定义

## 1. 概述

本文档定义了水声联邦学习系统WebSocket通信中所有消息的标准格式和字段约束。具体的协议消息定义请参考《WebSocket协议文档-中心化-简化.md》。

## 2. 消息格式

### 2.1 标准消息格式
所有WebSocket消息都采用JSON格式，包含以下字段：

```json
{
  "type": "MESSAGE_TYPE",
  "id": "unique_message_id",
  "timestamp": "2024-01-01T00:00:00.000Z",
  "vmId": "a1b2c3d4e5f678901234567890123456",
  "data": {},
  "signature": "base64_encoded_signature"
}
```

### 2.2 字段说明
- `type`: 消息类型（必需）
- `id`: 消息唯一标识（必需）
- `timestamp`: 消息时间戳（必需）
- `vmId`: 虚拟机ID，32位UUID格式（必需）
- `data`: 消息数据（必需）
- `signature`: 数字签名（必需，用于安全验证）

### 2.3 消息ID生成规则

#### 2.3.1 格式定义
消息ID采用统一格式：`{prefix}-{timestamp}-{random}`

#### 2.3.2 组成部分说明
- **prefix (前缀)**：消息来源标识
  - `client`：虚拟机端消息，固定6字符
  - `server`：后端消息，固定6字符
  - `cmd`：命令消息，固定3字符
- **timestamp (时间戳)**：Unix毫秒时间戳，固定13位数字
  - 格式：1704067200000（表示2024-01-01 00:00:00.000 GMT）
  - 范围：2000-01-01 至 2286-11-20
- **random (随机数)**：6位随机数字，范围000000-999999
  - 不足位数使用前导零补齐
  - 同一毫秒内应确保随机数唯一性

#### 2.3.3 完整示例
- 客户端消息：`client-1704067200000-123456` (总长度26字符)
- 服务器消息：`server-1704067200000-654321` (总长度26字符)
- 命令消息：`cmd-1704067200000-789012` (总长度23字符)

#### 2.3.4 生成算法
```pseudocode
function generateMessageId(prefix) {
    timestamp = getCurrentMilliseconds() // 13位Unix毫秒时间戳
    random = generateRandomNumber(0, 999999) // 6位随机数
    randomPadded = padLeft(random, 6, '0') // 左补零至6位
    return prefix + "-" + timestamp + "-" + randomPadded
}
```

#### 2.3.5 验证规则
- **格式正则表达式**：`^(client|server)-\d{13}-\d{6}$|^cmd-\d{13}-\d{6}$`
- **时间戳有效性**：时间戳应为合理的Unix毫秒值
- **随机数格式**：必须为6位数字，不足位数需补零
- **前缀有效性**：仅允许client、server、cmd三种前缀

## 3. 消息类型定义

本系统包含34个标准协议消息类型，按功能分类如下：

### 3.1 连接管理消息 (4个)
- `CONNECT` / `CONNECT_ACK` - 建立连接和确认
- `HEARTBEAT` / `HEARTBEAT_ACK` - 心跳维持和响应

### 3.2 任务管理消息 (10个)
- `FEDERATED_TASK_START` / `FEDERATED_TASK_START_ACK` - 任务启动
- `FEDERATED_TASK_STOP` / `FEDERATED_TASK_STOP_ACK` - 任务停止
- `FEDERATED_TASK_RESUME` / `FEDERATED_TASK_RESUME_ACK` - 任务恢复
- `FEDERATED_TASK_DELETE` / `FEDERATED_TASK_DELETE_ACK` - 任务删除
- `FEDERATED_TASK_STATUS_QUERY` / `FEDERATED_TASK_STATUS_RESPONSE` - 任务状态查询

### 3.3 轮次管理消息 (9个)
- `ROUND_START` / `ROUND_START_ACK` - 轮次开始
- `ROUND_ABORT` - 轮次中止
- `GRADIENT_UPLOAD` / `GRADIENT_UPLOAD_ACK` - 梯度上传
- `GLOBAL_MODEL_BROADCAST` / `GLOBAL_MODEL_BROADCAST_ACK` - 全局模型广播
- `ROUND_COMPLETE` / `ROUND_COMPLETE_ACK` - 轮次完成

### 3.4 状态监控消息 (3个)
- `VM_STATUS_QUERY` / `VM_STATUS_RESPONSE` - 虚拟机状态查询
- `ERROR` - 错误报告

### 3.5 虚拟机控制消息 (4个)
- `VM_START` / `VM_START_ACK` - 虚拟机启动
- `VM_STOP` / `VM_STOP_ACK` - 虚拟机停止

### 3.6 数据集管理消息 (4个)
- `DATASET_CREATE` - 创建数据集
- `DATASET_APPEND_ROWS` - 追加数据行
- `DATASET_COMPLETE` - 完成数据集上传
- `DATASET_STATUS_QUERY` / `DATASET_STATUS_RESPONSE` - 数据集状态查询
- `DATASET_DELETE` - 删除数据集

## 4. 特殊字段约束

### 4.1 任务ID字段 (taskId)
- **格式**: `fedtask-{6位数字随机ID}`
- **示例**: `fedtask-123456`
- **用途**: 多任务并发场景下的精确任务控制

### 4.2 虚拟机ID字段 (vmId)
- **格式**: 32位UUID格式的十六进制字符串
- **示例**: `a1b2c3d4e5f678901234567890123456`
- **特殊值**: `broadcast` - 用于广播消息

### 4.3 时间戳字段 (timestamp)
- **格式**: ISO 8601标准时间格式
- **示例**: `2024-01-01T00:00:00.000Z`
- **时区**: 统一使用UTC时间

### 4.4 数字签名字段 (signature)
- **格式**: Base64编码的数字签名
- **用途**: 消息完整性和身份验证
- **算法**: 根据系统安全策略确定

---

**说明**: 具体的协议消息格式和字段定义请参考《WebSocket协议文档-中心化-简化.md》文档。

---

**文档版本**: 1.0
**最后更新**: 2024-01-01
**维护者**: FedUWAComm开发团队