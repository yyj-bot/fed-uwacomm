# FedUWAComm 联邦学习完整流程测试文档（使用管理员账号）

## 测试环境配置

### 前置条件
- 后端服务运行在 `http://localhost:8080`
- WebSocket 端点: `ws://localhost:8080/ws`
- 数据库已初始化
- 管理员账号已存在：`admin` / `ab123456`
- 准备 5 个虚拟机实例进行测试

### 测试数据准备
```json
{
  "adminUser": {
    "username": "admin",
    "password": "ab123456"
  },
  "virtualMachines": [
    {"name": "VM-Node-1", "ipAddress": "192.168.1.101", "port": 8081},
    {"name": "VM-Node-2", "ipAddress": "192.168.1.102", "port": 8082},
    {"name": "VM-Node-3", "ipAddress": "192.168.1.103", "port": 8083},
    {"name": "VM-Node-4", "ipAddress": "192.168.1.104", "port": 8084},
    {"name": "VM-Node-5", "ipAddress": "192.168.1.105", "port": 8085}
  ]
}
```

**重要说明: 虚拟机ID动态生成**
- 虚拟机ID (vmId) 由后端系统在注册时自动生成，格式为 `vm-{uuid}`
- 测试过程中需要保存注册时返回的vmId用于后续API调用
- 所有涉及vmId的API示例使用变量占位符 `{vm1Id}`, `{vm2Id}` 等表示
- 单元测试实现中会正确处理动态vmId的获取和使用

## 完整流程测试用例

### 阶段1: 管理员登录

#### 1.1 管理员登录
```http
POST /api/user/login
Content-Type: application/json

{
  "loginIdentifier": "admin",
  "password": "ab123456",
  "rememberMe": true
}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user": {
      "userId": "admin-uuid-123",
      "username": "admin",
      "userType": "ADMIN"
    },
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "expiresIn": 3600
  }
}
```

**测试验证点:**
- 响应状态码为200
- 返回有效的JWT Token
- 用户类型为ADMIN

### 阶段2: 虚拟机注册流程

#### 2.1 虚拟机自注册 (并行执行5次)

**VM-001 注册:**
```http
POST /api/v1/vm/register
Content-Type: application/json

{
  "name": "VM-Node-1",
  "ipAddress": "192.168.1.101",
  "port": 8081,
  "capabilities": {
    "cpuCores": 8,
    "memoryMb": 16384,
    "gpuCount": 1,
    "algorithms": ["FEDAVG", "FEDPROX"]
  },
  "specifications": {
    "os": "Ubuntu 20.04",
    "pythonVersion": "3.8.10",
    "frameworks": ["pytorch", "tensorflow"]
  }
}
```

**VM-002 注册:**
```http
POST /api/v1/vm/register
Content-Type: application/json

{
  "name": "VM-Node-2",
  "ipAddress": "192.168.1.102",
  "port": 8082,
  "capabilities": {
    "cpuCores": 6,
    "memoryMb": 12288,
    "gpuCount": 0,
    "algorithms": ["FEDAVG"]
  },
  "specifications": {
    "os": "Ubuntu 20.04",
    "pythonVersion": "3.8.10",
    "frameworks": ["pytorch"]
  }
}
```

**VM-003 注册:**
```http
POST /api/v1/vm/register
Content-Type: application/json

{
  "name": "VM-Node-3",
  "ipAddress": "192.168.1.103",
  "port": 8083,
  "capabilities": {
    "cpuCores": 4,
    "memoryMb": 8192,
    "gpuCount": 1,
    "algorithms": ["FEDAVG", "FEDPROX"]
  },
  "specifications": {
    "os": "Ubuntu 20.04",
    "pythonVersion": "3.8.10",
    "frameworks": ["pytorch", "tensorflow"]
  }
}
```

**VM-004 注册:**
```http
POST /api/v1/vm/register
Content-Type: application/json

{
  "name": "VM-Node-4",
  "ipAddress": "192.168.1.104",
  "port": 8084,
  "capabilities": {
    "cpuCores": 12,
    "memoryMb": 24576,
    "gpuCount": 2,
    "algorithms": ["FEDAVG", "FEDPROX", "FEDOPT"]
  },
  "specifications": {
    "os": "Ubuntu 20.04",
    "pythonVersion": "3.8.10",
    "frameworks": ["pytorch", "tensorflow"]
  }
}
```

**VM-005 注册:**
```http
POST /api/v1/vm/register
Content-Type: application/json

{
  "name": "VM-Node-5",
  "ipAddress": "192.168.1.105",
  "port": 8085,
  "capabilities": {
    "cpuCores": 16,
    "memoryMb": 32768,
    "gpuCount": 4,
    "algorithms": ["FEDAVG", "FEDPROX", "FEDOPT"]
  },
  "specifications": {
    "os": "Ubuntu 20.04",
    "pythonVersion": "3.8.10",
    "frameworks": ["pytorch", "tensorflow"]
  }
}
```

**预期响应 (每个VM):**
```json
{
  "code": 200,
  "message": "虚拟机注册成功",
  "data": {
    "vmId": "vm-auto-generated-uuid-001",
    "sessionId": "session-uuid-123",
    "secretId": "secret-key-456",
    "websocketUrl": "ws://localhost:8080/ws",
    "heartbeatInterval": 30,
    "status": "REGISTERED"
  }
}
```

**注意:** vmId由后端系统自动生成，格式为UUID或其他唯一标识符。测试中需要保存返回的vmId用于后续操作。

### 阶段3: WebSocket连接建立 (虚拟机端)

#### 3.1 建立WebSocket连接 (所有5个VM)
```javascript
// VM-001 WebSocket连接
const ws1 = new WebSocket('ws://localhost:8080/ws');

ws1.onopen = function() {
  const connectMessage = {
    "type": "CONNECT",
    "id": "msg-" + Date.now(),
    "timestamp": new Date().toISOString(),
    "vmId": "{vm1Id}",
    "data": {
      "sessionId": "session-uuid-123",
      "capabilities": {
        "cpuCores": 8,
        "memoryMb": 16384,
        "gpuCount": 1
      }
    }
  };

  ws1.send(JSON.stringify(connectMessage));
};

// VM-002 WebSocket连接
const ws2 = new WebSocket('ws://localhost:8080/ws');

ws2.onopen = function() {
  const connectMessage = {
    "type": "CONNECT",
    "id": "msg-" + Date.now(),
    "timestamp": new Date().toISOString(),
    "vmId": "{vm2Id}",
    "data": {
      "sessionId": "session-uuid-456",
      "capabilities": {
        "cpuCores": 6,
        "memoryMb": 12288,
        "gpuCount": 0
      }
    }
  };

  ws2.send(JSON.stringify(connectMessage));
};

// VM-003 WebSocket连接
const ws3 = new WebSocket('ws://localhost:8080/ws');

ws3.onopen = function() {
  const connectMessage = {
    "type": "CONNECT",
    "id": "msg-" + Date.now(),
    "timestamp": new Date().toISOString(),
    "vmId": "{vm3Id}",
    "data": {
      "sessionId": "session-uuid-789",
      "capabilities": {
        "cpuCores": 4,
        "memoryMb": 8192,
        "gpuCount": 1
      }
    }
  };

  ws3.send(JSON.stringify(connectMessage));
};

// VM-004 WebSocket连接
const ws4 = new WebSocket('ws://localhost:8080/ws');

ws4.onopen = function() {
  const connectMessage = {
    "type": "CONNECT",
    "id": "msg-" + Date.now(),
    "timestamp": new Date().toISOString(),
    "vmId": "{vm4Id}",
    "data": {
      "sessionId": "session-uuid-abc",
      "capabilities": {
        "cpuCores": 12,
        "memoryMb": 24576,
        "gpuCount": 2
      }
    }
  };

  ws4.send(JSON.stringify(connectMessage));
};

// VM-005 WebSocket连接
const ws5 = new WebSocket('ws://localhost:8080/ws');

ws5.onopen = function() {
  const connectMessage = {
    "type": "CONNECT",
    "id": "msg-" + Date.now(),
    "timestamp": new Date().toISOString(),
    "vmId": "{vm5Id}",
    "data": {
      "sessionId": "session-uuid-def",
      "capabilities": {
        "cpuCores": 16,
        "memoryMb": 32768,
        "gpuCount": 4
      }
    }
  };

  ws5.send(JSON.stringify(connectMessage));
};
```

**预期WebSocket响应 (每个VM):**
```json
{
  "type": "CONNECT_ACK",
  "id": "server-1234567890-abc123",
  "timestamp": "2024-01-01T10:00:00Z",
  "vmId": "{vm1Id}",
  "data": {
    "sessionId": "session-uuid-123",
    "serverTime": "2024-01-01T10:00:00Z",
    "heartbeatInterval": 30,
    "maxMessageSize": 10485760,
    "supportedFeatures": ["ENCRYPTION", "COMPRESSION", "BATCH_OPERATIONS"]
  }
}
```

#### 3.2 心跳维持 (所有VM定期发送)
```javascript
// 为每个VM设置心跳
function setupHeartbeat(ws, vmId) {
  setInterval(() => {
    const heartbeatMessage = {
      "type": "HEARTBEAT",
      "id": "heartbeat-" + Date.now(),
      "timestamp": new Date().toISOString(),
      "vmId": vmId,
      "data": {
        "status": "ACTIVE",
        "cpuUsage": Math.random() * 50 + 20,
        "memoryUsage": Math.random() * 40 + 40
      }
    };

    ws.send(JSON.stringify(heartbeatMessage));
  }, 30000);
}

// 为所有VM设置心跳
setupHeartbeat(ws1, "vm-001");
setupHeartbeat(ws2, "vm-002");
setupHeartbeat(ws3, "vm-003");
setupHeartbeat(ws4, "vm-004");
setupHeartbeat(ws5, "vm-005");
```

### 阶段4: 训练数据管理

#### 4.1 上传训练数据集1 (声学特征数据)
```http
POST /api/training-data/upload
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="dataType"

ACOUSTIC_FEATURES
--boundary
Content-Disposition: form-data; name="title"

Underwater Acoustic Dataset - Features
--boundary
Content-Disposition: form-data; name="description"

声学特征数据集，包含频域和时域特征
--boundary
Content-Disposition: form-data; name="file"; filename="acoustic_features.csv"
Content-Type: text/csv

frequency,amplitude,phase,snr,distance,depth
100.5,0.8,1.2,15.5,100,50
125.3,0.7,1.5,14.2,120,55
...
--boundary--
```

#### 4.2 上传训练数据集2 (环境参数数据)
```http
POST /api/training-data/upload
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="dataType"

ENVIRONMENT_PARAMS
--boundary
Content-Disposition: form-data; name="title"

Underwater Environment Parameters
--boundary
Content-Disposition: form-data; name="description"

水下环境参数数据集
--boundary
Content-Disposition: form-data; name="file"; filename="env_params.csv"
Content-Type: text/csv

temperature,salinity,pressure,current_speed,bottom_type
15.2,35.1,10.5,0.3,sand
16.1,34.8,11.2,0.5,rock
...
--boundary--
```

**预期响应 (每个数据集):**
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "datasetId": "dataset-features-uuid",
    "title": "Underwater Acoustic Dataset - Features",
    "dataType": "ACOUSTIC_FEATURES",
    "size": 2048576,
    "rowCount": 5000,
    "status": "READY"
  }
}
```

#### 4.3 数据集验证
```http
POST /api/training-data/dataset-features-uuid/validate
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "validationRules": {
    "checkMissingValues": true,
    "checkDataTypes": true,
    "checkRanges": true,
    "checkOutliers": true
  },
  "sampleSize": 1000
}
```

### 阶段5: 联邦学习任务配置

#### 5.1 获取可用虚拟机
```http
GET /api/federated/config/available-vms?algorithm=FEDAVG&minCpuCores=4&minMemoryMb=8192&status=CONNECTED
Authorization: Bearer {accessToken}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 5,
    "availableVms": [
      {
        "vmId": "{vm1Id}",
        "name": "VM-Node-1",
        "status": "CONNECTED",
        "capabilities": {
          "cpuCores": 8,
          "memoryMb": 16384,
          "gpuCount": 1,
          "algorithms": ["FEDAVG", "FEDPROX"]
        },
        "currentLoad": 25.5,
        "lastHeartbeat": "2024-01-01T10:05:00Z"
      },
      {
        "vmId": "{vm2Id}",
        "name": "VM-Node-2",
        "status": "CONNECTED",
        "capabilities": {
          "cpuCores": 6,
          "memoryMb": 12288,
          "gpuCount": 0,
          "algorithms": ["FEDAVG"]
        },
        "currentLoad": 30.2,
        "lastHeartbeat": "2024-01-01T10:05:15Z"
      },
      {
        "vmId": "{vm3Id}",
        "name": "VM-Node-3",
        "status": "CONNECTED",
        "capabilities": {
          "cpuCores": 4,
          "memoryMb": 8192,
          "gpuCount": 1,
          "algorithms": ["FEDAVG", "FEDPROX"]
        },
        "currentLoad": 20.8,
        "lastHeartbeat": "2024-01-01T10:05:30Z"
      },
      {
        "vmId": "{vm4Id}",
        "name": "VM-Node-4",
        "status": "CONNECTED",
        "capabilities": {
          "cpuCores": 12,
          "memoryMb": 24576,
          "gpuCount": 2,
          "algorithms": ["FEDAVG", "FEDPROX", "FEDOPT"]
        },
        "currentLoad": 15.3,
        "lastHeartbeat": "2024-01-01T10:05:45Z"
      },
      {
        "vmId": "{vm5Id}",
        "name": "VM-Node-5",
        "status": "CONNECTED",
        "capabilities": {
          "cpuCores": 16,
          "memoryMb": 32768,
          "gpuCount": 4,
          "algorithms": ["FEDAVG", "FEDPROX", "FEDOPT"]
        },
        "currentLoad": 18.7,
        "lastHeartbeat": "2024-01-01T10:06:00Z"
      }
    ],
    "recommendedSelection": ["{vm1Id}", "{vm2Id}", "{vm3Id}", "{vm4Id}", "{vm5Id}"]
  }
}
```

#### 5.2 获取可用数据集
```http
GET /api/federated/config/available-datasets?status=READY
Authorization: Bearer {accessToken}
```

#### 5.3 数据分配预览 (5台VM)
```http
POST /api/federated/tasks/preview-distribution
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "datasetId": "dataset-features-uuid",
  "distributionStrategy": "UNIFORM",
  "participants": [
    {"vmId": "{vm1Id}", "requestedRatio": 0.20},
    {"vmId": "{vm2Id}", "requestedRatio": 0.15},
    {"vmId": "{vm3Id}", "requestedRatio": 0.15},
    {"vmId": "{vm4Id}", "requestedRatio": 0.25},
    {"vmId": "{vm5Id}", "requestedRatio": 0.25}
  ]
}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "预览生成成功",
  "data": {
    "distributionResult": {
      "strategy": "UNIFORM",
      "totalRows": 5000,
      "participants": [
        {
          "vmId": "{vm1Id}",
          "assignedRatio": 0.20,
          "assignedRows": 1000,
          "estimatedSize": 409600
        },
        {
          "vmId": "{vm2Id}",
          "assignedRatio": 0.15,
          "assignedRows": 750,
          "estimatedSize": 307200
        },
        {
          "vmId": "{vm3Id}",
          "assignedRatio": 0.15,
          "assignedRows": 750,
          "estimatedSize": 307200
        },
        {
          "vmId": "{vm4Id}",
          "assignedRatio": 0.25,
          "assignedRows": 1250,
          "estimatedSize": 512000
        },
        {
          "vmId": "{vm5Id}",
          "assignedRatio": 0.25,
          "assignedRows": 1250,
          "estimatedSize": 512000
        }
      ]
    },
    "qualityMetrics": {
      "dataBalance": 0.92,
      "featureDistribution": "UNIFORM",
      "labelDistribution": "BALANCED"
    }
  }
}
```

#### 5.4 参与者验证
```http
POST /api/federated/tasks/validate-participants
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "algorithm": "FEDAVG",
  "taskType": "SUPERVISED_LEARNING",
  "participants": [
    {"vmId": "{vm1Id}", "role": "PARTICIPANT"},
    {"vmId": "{vm2Id}", "role": "PARTICIPANT"},
    {"vmId": "{vm3Id}", "role": "PARTICIPANT"},
    {"vmId": "{vm4Id}", "role": "PARTICIPANT"},
    {"vmId": "{vm5Id}", "role": "PARTICIPANT"}
  ]
}
```

#### 5.5 创建联邦学习任务 (5台VM参与)
```http
POST /api/federated/tasks
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "taskName": "水下声学通信优化联邦学习 - 5VM测试",
  "description": "使用5台虚拟机进行联邦学习优化水下声学通信参数",
  "algorithm": "FEDAVG",
  "taskType": "SUPERVISED_LEARNING",
  "participantConfig": {
    "participants": [
      {
        "vmId": "{vm1Id}",
        "role": "PARTICIPANT",
        "datasetId": "dataset-features-uuid",
        "dataRatio": 0.20,
        "priority": "HIGH"
      },
      {
        "vmId": "{vm2Id}",
        "role": "PARTICIPANT",
        "datasetId": "dataset-features-uuid",
        "dataRatio": 0.15,
        "priority": "MEDIUM"
      },
      {
        "vmId": "{vm3Id}",
        "role": "PARTICIPANT",
        "datasetId": "dataset-features-uuid",
        "dataRatio": 0.15,
        "priority": "MEDIUM"
      },
      {
        "vmId": "{vm4Id}",
        "role": "PARTICIPANT",
        "datasetId": "dataset-features-uuid",
        "dataRatio": 0.25,
        "priority": "HIGH"
      },
      {
        "vmId": "{vm5Id}",
        "role": "PARTICIPANT",
        "datasetId": "dataset-features-uuid",
        "dataRatio": 0.25,
        "priority": "HIGH"
      }
    ],
    "minParticipants": 3,
    "maxParticipants": 5,
    "aggregationStrategy": "WEIGHTED_AVERAGE"
  },
  "algorithmConfig": {
    "totalRounds": 12,
    "localEpochs": 4,
    "learningRate": 0.01,
    "batchSize": 32,
    "optimizerType": "ADAM",
    "lossFunction": "CROSS_ENTROPY",
    "modelConfig": {
      "framework": "pytorch",
      "architecture": "MLP",
      "inputSize": 784,
      "hiddenLayers": [256, 128, 64],
      "outputSize": 10,
      "activationFunction": "RELU",
      "dropoutRate": 0.2
    },
    "convergenceConfig": {
      "targetAccuracy": 0.96,
      "patienceRounds": 3,
      "minImprovement": 0.001
    }
  },
  "resourceConfig": {
    "maxTrainingTime": 7200,
    "memoryLimit": 8192,
    "cpuLimit": 4
  }
}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "taskName": "水下声学通信优化联邦学习 - 5VM测试",
    "status": "CREATED",
    "participantCount": 5,
    "estimatedDuration": "2.5小时",
    "nextStep": "START",
    "createdAt": "2024-01-01T10:10:00Z",
    "configurationSummary": {
      "algorithm": "FEDAVG",
      "totalRounds": 12,
      "participatingVMs": ["{vm1Id}", "{vm2Id}", "{vm3Id}", "{vm4Id}", "{vm5Id}"],
      "datasetCount": 1
    }
  }
}
```

### 阶段6: 任务启动和执行

#### 6.1 启动联邦学习任务
```http
POST /api/federated/tasks/task-acoustic-5vm-uuid-456/start
Authorization: Bearer {accessToken}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "任务启动成功",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "RUNNING",
    "currentRound": 0,
    "totalRounds": 12,
    "startTime": "2024-01-01T10:15:00Z",
    "participants": [
      {"vmId": "{vm1Id}", "status": "INITIALIZING"},
      {"vmId": "{vm2Id}", "status": "INITIALIZING"},
      {"vmId": "{vm3Id}", "status": "INITIALIZING"},
      {"vmId": "{vm4Id}", "status": "INITIALIZING"},
      {"vmId": "{vm5Id}", "status": "INITIALIZING"}
    ]
  }
}
```

#### 6.2 WebSocket训练流程执行

**6.2.1 服务端发送训练开始指令**
所有5个VM WebSocket接收消息（以vm1为例）:
```json
{
  "type": "TRAINING_START_COMMAND",
  "vmId": "{vm1Id}",
  "taskId": "task-acoustic-5vm-uuid-456",
  "algorithm": "FEDAVG",
  "config": {
    "totalRounds": 12,
    "localEpochs": 4,
    "learningRate": 0.01,
    "batchSize": 32,
    "modelConfig": {
      "framework": "pytorch",
      "architecture": "MLP",
      "inputSize": 784,
      "hiddenLayers": [256, 128, 64],
      "outputSize": 10
    }
  },
  "data": {
    "assignedDataRows": 1000,
    "dataStartIndex": 0,
    "dataEndIndex": 999
  },
  "message": "请开始训练任务"
}
```

**6.2.2 所有VM确认训练开始**
```javascript
// VM-001 响应
const trainingStartResponse1 = {
  "type": "TRAINING_START_RESPONSE",
  "id": "response-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm1Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "localDatasetSize": 1000,
    "modelInitialized": true,
    "estimatedRoundTime": 280
  }
};

// VM-002 响应
const trainingStartResponse2 = {
  "type": "TRAINING_START_RESPONSE",
  "id": "response-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm2Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "localDatasetSize": 750,
    "modelInitialized": true,
    "estimatedRoundTime": 320
  }
};

// VM-003 响应
const trainingStartResponse3 = {
  "type": "TRAINING_START_RESPONSE",
  "id": "response-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm3Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "localDatasetSize": 750,
    "modelInitialized": true,
    "estimatedRoundTime": 350
  }
};

// VM-004 响应
const trainingStartResponse4 = {
  "type": "TRAINING_START_RESPONSE",
  "id": "response-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm4Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "localDatasetSize": 1250,
    "modelInitialized": true,
    "estimatedRoundTime": 200
  }
};

// VM-005 响应
const trainingStartResponse5 = {
  "type": "TRAINING_START_RESPONSE",
  "id": "response-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm5Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "status": "SUCCESS",
    "message": "训练已成功开始",
    "localDatasetSize": 1250,
    "modelInitialized": true,
    "estimatedRoundTime": 180
  }
};
```

#### 6.3 联邦学习轮次执行 (12轮训练)

**6.3.1 第1轮训练 - 所有5个VM上传模型**
```javascript
// VM-001 上传模型
const modelUploadMessage1 = {
  "type": "MODEL_UPLOAD",
  "id": "model-upload-r1-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm1Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "round": 1,
    "parameters": {
      "weights": {
        "layer1": [/* 256x784 权重矩阵 */],
        "layer2": [/* 128x256 权重矩阵 */],
        "layer3": [/* 64x128 权重矩阵 */],
        "output": [/* 10x64 权重矩阵 */]
      },
      "biases": {
        "layer1": [/* 256个偏置 */],
        "layer2": [/* 128个偏置 */],
        "layer3": [/* 64个偏置 */],
        "output": [/* 10个偏置 */]
      },
      "metadata": {
        "parameterCount": 235146,
        "modelSize": 941584,
        "checksum": "sha256:vm001_r1_abc123..."
      }
    },
    "metrics": {
      "accuracy": 0.73,
      "loss": 0.84,
      "trainingTime": 275,
      "dataPoints": 1000,
      "epochs": 4,
      "convergenceRate": 0.05,
      "memoryUsage": 4096
    },
    "deviceInfo": {
      "gpuUsed": true,
      "cpuCores": 8,
      "memoryMb": 16384
    }
  }
};

// VM-002 上传模型
const modelUploadMessage2 = {
  "type": "MODEL_UPLOAD",
  "id": "model-upload-r1-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm2Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "round": 1,
    "parameters": {
      /* 不同的权重和偏置值 */
    },
    "metrics": {
      "accuracy": 0.69,
      "loss": 0.91,
      "trainingTime": 315,
      "dataPoints": 750,
      "epochs": 4,
      "convergenceRate": 0.04,
      "memoryUsage": 3072
    },
    "deviceInfo": {
      "gpuUsed": false,
      "cpuCores": 6,
      "memoryMb": 12288
    }
  }
};

// VM-003 上传模型
const modelUploadMessage3 = {
  "type": "MODEL_UPLOAD",
  "id": "model-upload-r1-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm3Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "round": 1,
    "parameters": {
      /* 第三组权重和偏置值 */
    },
    "metrics": {
      "accuracy": 0.71,
      "loss": 0.87,
      "trainingTime": 345,
      "dataPoints": 750,
      "epochs": 4,
      "convergenceRate": 0.045,
      "memoryUsage": 2048
    },
    "deviceInfo": {
      "gpuUsed": true,
      "cpuCores": 4,
      "memoryMb": 8192
    }
  }
};

// VM-004 上传模型
const modelUploadMessage4 = {
  "type": "MODEL_UPLOAD",
  "id": "model-upload-r1-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm4Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "round": 1,
    "parameters": {
      /* 第四组权重和偏置值 */
    },
    "metrics": {
      "accuracy": 0.76,
      "loss": 0.79,
      "trainingTime": 195,
      "dataPoints": 1250,
      "epochs": 4,
      "convergenceRate": 0.055,
      "memoryUsage": 6144
    },
    "deviceInfo": {
      "gpuUsed": true,
      "cpuCores": 12,
      "memoryMb": 24576
    }
  }
};

// VM-005 上传模型
const modelUploadMessage5 = {
  "type": "MODEL_UPLOAD",
  "id": "model-upload-r1-" + Date.now(),
  "timestamp": new Date().toISOString(),
  "vmId": "{vm5Id}",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "round": 1,
    "parameters": {
      /* 第五组权重和偏置值 */
    },
    "metrics": {
      "accuracy": 0.78,
      "loss": 0.75,
      "trainingTime": 175,
      "dataPoints": 1250,
      "epochs": 4,
      "convergenceRate": 0.06,
      "memoryUsage": 8192
    },
    "deviceInfo": {
      "gpuUsed": true,
      "cpuCores": 16,
      "memoryMb": 32768
    }
  }
};
```

**6.3.2 服务端模型聚合完成，推送全局模型更新**
所有VM接收消息:
```json
{
  "type": "GLOBAL_MODEL_UPDATE",
  "vmId": "{vm1Id}",
  "taskId": "task-acoustic-5vm-uuid-456",
  "round": 1,
  "data": {
    "aggregatedMetrics": {
      "averageAccuracy": 0.734,
      "averageLoss": 0.832,
      "participantCount": 5,
      "convergenceScore": 0.87,
      "weightedAccuracy": 0.742
    },
    "nextRoundConfig": {
      "round": 2,
      "adjustedLearningRate": 0.0095,
      "targetImprovement": 0.04
    }
  },
  "message": "第1轮全局模型已更新，请下载最新版本开始第2轮训练"
}
```

**6.3.3 重复执行剩余11轮训练**
继续执行第2轮到第12轮的相同流程，每轮模型精度应该逐步提升。

### 阶段7: 任务监控和状态查询

#### 7.1 查询任务详细状态
```http
GET /api/federated/tasks/task-acoustic-5vm-uuid-456
Authorization: Bearer {accessToken}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "taskName": "水下声学通信优化联邦学习 - 5VM测试",
    "status": "RUNNING",
    "progress": {
      "currentRound": 6,
      "totalRounds": 12,
      "completionPercentage": 50.0,
      "estimatedRemaining": "62分钟"
    },
    "participants": [
      {
        "vmId": "{vm1Id}",
        "status": "TRAINING",
        "currentAccuracy": 0.91,
        "currentLoss": 0.28,
        "lastUpdate": "2024-01-01T11:20:00Z",
        "roundsCompleted": 6,
        "averageRoundTime": 275
      },
      {
        "vmId": "{vm2Id}",
        "status": "TRAINING",
        "currentAccuracy": 0.88,
        "currentLoss": 0.34,
        "lastUpdate": "2024-01-01T11:20:10Z",
        "roundsCompleted": 6,
        "averageRoundTime": 315
      },
      {
        "vmId": "{vm3Id}",
        "status": "TRAINING",
        "currentAccuracy": 0.89,
        "currentLoss": 0.32,
        "lastUpdate": "2024-01-01T11:20:20Z",
        "roundsCompleted": 6,
        "averageRoundTime": 340
      },
      {
        "vmId": "{vm4Id}",
        "status": "TRAINING",
        "currentAccuracy": 0.93,
        "currentLoss": 0.25,
        "lastUpdate": "2024-01-01T11:20:30Z",
        "roundsCompleted": 6,
        "averageRoundTime": 195
      },
      {
        "vmId": "{vm5Id}",
        "status": "AGGREGATING",
        "currentAccuracy": 0.94,
        "currentLoss": 0.23,
        "lastUpdate": "2024-01-01T11:20:40Z",
        "roundsCompleted": 6,
        "averageRoundTime": 175
      }
    ],
    "metrics": {
      "globalAccuracy": 0.910,
      "globalLoss": 0.284,
      "convergenceRate": 0.028,
      "convergenceTrend": "IMPROVING",
      "dataDistribution": "BALANCED"
    },
    "resourceUsage": {
      "totalCpuHours": 18.2,
      "totalMemoryMb": 92160,
      "totalGpuHours": 12.5,
      "networkTraffic": "4.8GB"
    }
  }
}
```

#### 7.2 查询资源使用情况
```http
GET /api/federated/tasks/task-acoustic-5vm-uuid-456/resource-usage
Authorization: Bearer {accessToken}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "participantMetrics": [
      {
        "vmId": "{vm1Id}",
        "cpuUsage": {
          "current": 72.5,
          "average": 68.2,
          "peak": 89.3
        },
        "memoryUsage": {
          "current": 12288,
          "average": 10240,
          "peak": 14336
        },
        "gpuUsage": {
          "current": 82.1,
          "average": 75.8,
          "peak": 95.2
        },
        "networkIO": {
          "uploaded": "780MB",
          "downloaded": "620MB"
        }
      },
      {
        "vmId": "{vm2Id}",
        "cpuUsage": {
          "current": 65.8,
          "average": 62.1,
          "peak": 78.9
        },
        "memoryUsage": {
          "current": 9216,
          "average": 8192,
          "peak": 10240
        },
        "gpuUsage": {
          "current": 0,
          "average": 0,
          "peak": 0
        },
        "networkIO": {
          "uploaded": "580MB",
          "downloaded": "465MB"
        }
      },
      {
        "vmId": "{vm3Id}",
        "cpuUsage": {
          "current": 78.3,
          "average": 73.5,
          "peak": 92.1
        },
        "memoryUsage": {
          "current": 6144,
          "average": 5120,
          "peak": 7168
        },
        "gpuUsage": {
          "current": 79.6,
          "average": 72.4,
          "peak": 91.8
        },
        "networkIO": {
          "uploaded": "580MB",
          "downloaded": "465MB"
        }
      },
      {
        "vmId": "{vm4Id}",
        "cpuUsage": {
          "current": 58.2,
          "average": 55.1,
          "peak": 71.3
        },
        "memoryUsage": {
          "current": 18432,
          "average": 16384,
          "peak": 20480
        },
        "gpuUsage": {
          "current": 85.3,
          "average": 79.7,
          "peak": 96.4
        },
        "networkIO": {
          "uploaded": "980MB",
          "downloaded": "775MB"
        }
      },
      {
        "vmId": "{vm5Id}",
        "cpuUsage": {
          "current": 52.7,
          "average": 48.9,
          "peak": 65.2
        },
        "memoryUsage": {
          "current": 24576,
          "average": 20480,
          "peak": 28672
        },
        "gpuUsage": {
          "current": 88.9,
          "average": 82.1,
          "peak": 98.7
        },
        "networkIO": {
          "uploaded": "980MB",
          "downloaded": "775MB"
        }
      }
    ],
    "aggregateMetrics": {
      "totalCpuUsage": 65.5,
      "totalMemoryUsage": 70656,
      "totalNetworkTraffic": "7.6GB",
      "efficiency": 0.89
    }
  }
}
```

### 阶段8: 任务完成和结果获取

#### 8.1 查询最终任务结果
```http
GET /api/federated/tasks/task-acoustic-5vm-uuid-456/results
Authorization: Bearer {accessToken}
```

**预期响应:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "taskId": "task-acoustic-5vm-uuid-456",
    "taskName": "水下声学通信优化联邦学习 - 5VM测试",
    "status": "COMPLETED",
    "completionTime": "2024-01-01T12:45:00Z",
    "totalDuration": "2小时30分钟",
    "finalMetrics": {
      "finalAccuracy": 0.957,
      "finalLoss": 0.124,
      "convergenceRound": 10,
      "targetAccuracyReached": true,
      "improvementOverInitial": 0.223,
      "convergenceStability": 0.96
    },
    "participantResults": [
      {
        "vmId": "{vm1Id}",
        "role": "PARTICIPANT",
        "contribution": 0.20,
        "finalLocalAccuracy": 0.959,
        "averageRoundTime": 275,
        "totalTrainingTime": "55分钟",
        "dataContribution": 1000,
        "modelUploads": 12,
        "aggregationParticipation": 12
      },
      {
        "vmId": "{vm2Id}",
        "role": "PARTICIPANT",
        "contribution": 0.15,
        "finalLocalAccuracy": 0.951,
        "averageRoundTime": 315,
        "totalTrainingTime": "63分钟",
        "dataContribution": 750,
        "modelUploads": 12,
        "aggregationParticipation": 12
      },
      {
        "vmId": "{vm3Id}",
        "role": "PARTICIPANT",
        "contribution": 0.15,
        "finalLocalAccuracy": 0.954,
        "averageRoundTime": 340,
        "totalTrainingTime": "68分钟",
        "dataContribution": 750,
        "modelUploads": 12,
        "aggregationParticipation": 12
      },
      {
        "vmId": "{vm4Id}",
        "role": "PARTICIPANT",
        "contribution": 0.25,
        "finalLocalAccuracy": 0.962,
        "averageRoundTime": 195,
        "totalTrainingTime": "39分钟",
        "dataContribution": 1250,
        "modelUploads": 12,
        "aggregationParticipation": 12
      },
      {
        "vmId": "{vm5Id}",
        "role": "PARTICIPANT",
        "contribution": 0.25,
        "finalLocalAccuracy": 0.964,
        "averageRoundTime": 175,
        "totalTrainingTime": "35分钟",
        "dataContribution": 1250,
        "modelUploads": 12,
        "aggregationParticipation": 12,
        "aggregationOperations": 12
      }
    ],
    "globalModel": {
      "modelId": "final-model-5vm-acoustic-uuid",
      "version": "1.0",
      "accuracy": 0.957,
      "loss": 0.124,
      "parameterCount": 235146,
      "modelSize": 941584,
      "framework": "pytorch",
      "downloadUrl": "/api/model/download/final-model-5vm-acoustic-uuid",
      "checksumSHA256": "e7a9c5d8f2b6..."
    },
    "convergenceAnalysis": {
      "convergencePattern": "STEADY_IMPROVEMENT",
      "plateauRounds": 2,
      "optimalStoppingRound": 10,
      "overfittingDetected": false
    },
    "resourceSummary": {
      "totalCpuHours": 36.5,
      "totalMemoryUsage": "70.6GB-hours",
      "totalGpuHours": 24.8,
      "totalNetworkTraffic": "12.4GB",
      "energyEfficiency": 0.91,
      "costEstimate": "$28.70"
    },
    "qualityMetrics": {
      "dataUtilization": 0.99,
      "modelConsistency": 0.96,
      "participantReliability": 0.98,
      "communicationEfficiency": 0.93
    }
  }
}
```

#### 8.2 下载最终模型
```http
GET /api/model/download/final-model-5vm-acoustic-uuid?format=pytorch&compressed=true
Authorization: Bearer {accessToken}
```

### 阶段9: 清理和断开连接

#### 9.1 VM断开WebSocket连接
```javascript
// 优雅关闭WebSocket连接
ws1.close(1000, "Task completed successfully");
ws2.close(1000, "Task completed successfully");
ws3.close(1000, "Task completed successfully");
ws4.close(1000, "Task completed successfully");
ws5.close(1000, "Task completed successfully");
```

## 完整的单元测试实现

### 测试类结构
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CompleteFederatedLearningFlowTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private String baseUrl;
    private String adminAccessToken;
    private String taskId;

    // 测试数据 - 5台虚拟机（vmId由后端注册时自动生成）
    private final List<VmTestData> virtualMachines = Arrays.asList(
        new VmTestData(null, "VM-Node-1", "192.168.1.101", 8081, 8, 16384, 1),
        new VmTestData(null, "VM-Node-2", "192.168.1.102", 8082, 6, 12288, 0),
        new VmTestData(null, "VM-Node-3", "192.168.1.103", 8083, 4, 8192, 1),
        new VmTestData(null, "VM-Node-4", "192.168.1.104", 8084, 12, 24576, 2),
        new VmTestData(null, "VM-Node-5", "192.168.1.105", 8085, 16, 32768, 4)
    );

    // 存储注册后的虚拟机ID
    private final List<String> registeredVmIds = new ArrayList<>();

    // WebSocket客户端
    private final List<MockVirtualMachine> mockVMs = new ArrayList<>();

    @BeforeAll
    void setupTest() {
        baseUrl = "http://localhost:" + port;
        // 初始化Mock虚拟机
        for (VmTestData vmData : virtualMachines) {
            mockVMs.add(new MockVirtualMachine(vmData));
        }
    }

    @Test
    @Order(1)
    void test01_AdminLogin() {
        // 管理员登录测试
        LoginRequest request = new LoginRequest("admin", "ab123456", true);

        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.postForEntity(
            baseUrl + "/api/user/login",
            request,
            new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getUser().getUserType()).isEqualTo("ADMIN");

        adminAccessToken = response.getBody().getData().getAccessToken();
        assertThat(adminAccessToken).isNotNull();

        System.out.println("✅ 管理员登录成功");
    }

    @Test
    @Order(2)
    void test02_VirtualMachinesRegistration() {
        // 并行注册所有5台虚拟机
        List<CompletableFuture<Void>> registrationFutures = mockVMs.stream()
            .map(vm -> CompletableFuture.runAsync(() -> {
                try {
                    String vmId = vm.register(baseUrl); // 注册并获取后端生成的vmId
                    registeredVmIds.add(vmId); // 存储注册后的vmId
                    assertThat(vm.isRegistered()).isTrue();
                    assertThat(vmId).isNotNull().matches("vm-[a-f0-9\\-]{36}"); // 验证UUID格式
                    System.out.println("✅ " + vm.getName() + " 注册成功，vmId: " + vmId);
                } catch (Exception e) {
                    fail("VM registration failed: " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        // 等待所有注册完成
        CompletableFuture.allOf(registrationFutures.toArray(new CompletableFuture[0]))
            .join();

        // 验证所有VM都已注册并有正确的vmId
        mockVMs.forEach(vm -> assertThat(vm.isRegistered()).isTrue());
        assertThat(registeredVmIds).hasSize(5); // 确保有5个不同的vmId
        System.out.println("✅ 所有5台虚拟机注册完成，生成的vmIds: " + registeredVmIds);
    }

    @Test
    @Order(3)
    void test03_WebSocketConnections() {
        // 建立WebSocket连接
        String websocketUrl = "ws://localhost:" + port + "/ws";

        List<CompletableFuture<Void>> connectionFutures = mockVMs.stream()
            .map(vm -> CompletableFuture.runAsync(() -> {
                try {
                    vm.connectWebSocket(websocketUrl);
                    Thread.sleep(2000); // 等待连接稳定
                    assertThat(vm.isConnected()).isTrue();
                    System.out.println("✅ " + vm.getVmId() + " WebSocket连接成功");
                } catch (Exception e) {
                    fail("WebSocket connection failed: " + e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(connectionFutures.toArray(new CompletableFuture[0]))
            .join();

        // 启动心跳
        mockVMs.forEach(MockVirtualMachine::startHeartbeat);

        // 等待心跳稳定
        Thread.sleep(5000);
        System.out.println("✅ 所有VM WebSocket连接建立并开始心跳");
    }

    @Test
    @Order(4)
    void test04_TrainingDataUpload() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 创建测试数据文件
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("dataType", "ACOUSTIC_FEATURES");
        body.add("title", "5VM Test Acoustic Dataset");
        body.add("description", "5台虚拟机测试用声学特征数据集");

        // 创建模拟CSV文件 - 增大数据集以适应5台VM
        String csvContent = generateTestCsvData(8000);
        ByteArrayResource fileResource = new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test_acoustic_data_5vm.csv";
            }
        };
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<ApiResponse<TrainingDataUploadResponse>> response =
            restTemplate.postForEntity(
                baseUrl + "/api/training-data/upload",
                requestEntity,
                new ParameterizedTypeReference<ApiResponse<TrainingDataUploadResponse>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getRowCount()).isEqualTo(8000);

        String datasetId = response.getBody().getData().getDatasetId();
        assertThat(datasetId).isNotNull();

        // 保存数据集ID供后续使用
        this.datasetId = datasetId;
        System.out.println("✅ 训练数据上传成功，数据集ID: " + datasetId);
    }

    @Test
    @Order(5)
    void test05_QueryAvailableResources() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询可用虚拟机
        ResponseEntity<ApiResponse<AvailableVmsResponse>> vmResponse =
            restTemplate.exchange(
                baseUrl + "/api/federated/config/available-vms?algorithm=FEDAVG&minCpuCores=4",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<AvailableVmsResponse>>() {}
            );

        assertThat(vmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(vmResponse.getBody().getData().getTotal()).isEqualTo(5);

        // 查询可用数据集
        ResponseEntity<ApiResponse<AvailableDatasetsResponse>> datasetResponse =
            restTemplate.exchange(
                baseUrl + "/api/federated/config/available-datasets?status=READY",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<AvailableDatasetsResponse>>() {}
            );

        assertThat(datasetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(datasetResponse.getBody().getData().getTotal()).isGreaterThanOrEqualTo(1);

        System.out.println("✅ 资源查询成功 - 5台VM可用，数据集就绪");
    }

    @Test
    @Order(6)
    void test06_CreateFederatedTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 使用注册时生成的vmId构建5台VM的任务创建请求
        TaskCreateRequest request = TaskCreateRequest.builder()
            .taskName("5VM完整流程测试任务")
            .description("使用5台虚拟机的完整联邦学习流程测试")
            .algorithm("FEDAVG")
            .taskType("SUPERVISED_LEARNING")
            .participantConfig(ParticipantConfig.builder()
                .participants(Arrays.asList(
                    ParticipantRequest.builder()
                        .vmId(registeredVmIds.get(0)) // 使用动态生成的vmId
                        .role("PARTICIPANT")
                        .datasetId(datasetId)
                        .dataRatio(0.20)
                        .build(),
                    ParticipantRequest.builder()
                        .vmId(registeredVmIds.get(1))
                        .role("PARTICIPANT")
                        .datasetId(datasetId)
                        .dataRatio(0.15)
                        .build(),
                    ParticipantRequest.builder()
                        .vmId(registeredVmIds.get(2))
                        .role("PARTICIPANT")
                        .datasetId(datasetId)
                        .dataRatio(0.15)
                        .build(),
                    ParticipantRequest.builder()
                        .vmId(registeredVmIds.get(3))
                        .role("PARTICIPANT")
                        .datasetId(datasetId)
                        .dataRatio(0.25)
                        .build(),
                    ParticipantRequest.builder()
                        .vmId(registeredVmIds.get(4))
                        .role("AGGREGATOR")
                        .datasetId(datasetId)
                        .dataRatio(0.25)
                        .build()
                ))
                .build())
            .algorithmConfig(AlgorithmConfig.builder()
                .totalRounds(8)  // 适当减少轮次以适应单体服务器
                .localEpochs(3)
                .learningRate(0.01)
                .batchSize(32)
                .build())
            .build();

        HttpEntity<TaskCreateRequest> requestEntity = new HttpEntity<>(request, headers);

        ResponseEntity<ApiResponse<TaskOperationResponse>> response =
            restTemplate.postForEntity(
                baseUrl + "/api/federated/tasks",
                requestEntity,
                new ParameterizedTypeReference<ApiResponse<TaskOperationResponse>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("CREATED");

        taskId = response.getBody().getData().getTaskId();
        assertThat(taskId).isNotNull();

        System.out.println("✅ 5VM联邦学习任务创建成功，任务ID: " + taskId);
    }

    @Test
    @Order(7)
    void test07_StartFederatedTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<ApiResponse<TaskOperationResponse>> response =
            restTemplate.postForEntity(
                baseUrl + "/api/federated/tasks/" + taskId + "/start",
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<TaskOperationResponse>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("RUNNING");

        // 等待训练开始指令到达VM
        Thread.sleep(3000);
        System.out.println("✅ 联邦学习任务启动成功");
    }

    @Test
    @Order(8)
    void test08_ExecuteFederatedLearning() throws InterruptedException {
        // 模拟完整的联邦学习执行过程 - 8轮训练
        int totalRounds = 8;

        for (int round = 1; round <= totalRounds; round++) {
            System.out.println("🔄 执行第" + round + "轮训练...");

            // 等待训练指令
            Thread.sleep(2000);

            // 所有5台VM并行执行本地训练并上传模型
            List<CompletableFuture<Void>> trainingFutures = mockVMs.stream()
                .map(vm -> CompletableFuture.runAsync(() -> {
                    try {
                        vm.simulateTrainingRound(taskId, round);
                        System.out.println("  ✅ " + vm.getVmId() + " 第" + round + "轮训练完成");
                    } catch (Exception e) {
                        fail("Training round " + round + " failed for " + vm.getVmId());
                    }
                }))
                .collect(Collectors.toList());

            // 等待本轮训练完成
            CompletableFuture.allOf(trainingFutures.toArray(new CompletableFuture[0]))
                .join();

            // 等待模型聚合
            Thread.sleep(3000);

            // 验证任务状态
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminAccessToken);

            ResponseEntity<ApiResponse<TaskDetailResponse>> statusResponse =
                restTemplate.exchange(
                    baseUrl + "/api/federated/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<TaskDetailResponse>>() {}
                );

            assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            TaskDetailResponse taskDetail = statusResponse.getBody().getData();
            assertThat(taskDetail.getProgress().getCurrentRound())
                .isGreaterThanOrEqualTo(round);

            System.out.println("✅ 第" + round + "轮训练完成，当前精度: " +
                String.format("%.3f", taskDetail.getMetrics().getGlobalAccuracy()));
        }

        // 等待任务完全结束
        Thread.sleep(5000);
        System.out.println("🎉 所有8轮联邦学习训练完成");
    }

    @Test
    @Order(9)
    void test09_VerifyTaskCompletion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询最终任务状态
        ResponseEntity<ApiResponse<TaskDetailResponse>> response =
            restTemplate.exchange(
                baseUrl + "/api/federated/tasks/" + taskId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<TaskDetailResponse>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskDetailResponse taskDetail = response.getBody().getData();

        assertThat(taskDetail.getStatus()).isIn("COMPLETED", "CONVERGED");
        assertThat(taskDetail.getProgress().getCurrentRound()).isEqualTo(8);
        assertThat(taskDetail.getMetrics().getGlobalAccuracy()).isGreaterThan(0.0);

        System.out.println("✅ 任务完成验证成功 - 状态: " + taskDetail.getStatus() +
            ", 最终精度: " + String.format("%.3f", taskDetail.getMetrics().getGlobalAccuracy()));
    }

    @Test
    @Order(10)
    void test10_RetrieveResults() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        // 查询任务结果
        ResponseEntity<ApiResponse<TaskResultResponse>> response =
            restTemplate.exchange(
                baseUrl + "/api/federated/tasks/" + taskId + "/results",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<TaskResultResponse>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResultResponse result = response.getBody().getData();

        assertThat(result.getTaskId()).isEqualTo(taskId);
        assertThat(result.getStatus()).isIn("COMPLETED", "CONVERGED");
        assertThat(result.getFinalMetrics().getFinalAccuracy()).isGreaterThan(0.0);
        assertThat(result.getParticipantResults()).hasSize(5);
        assertThat(result.getGlobalModel()).isNotNull();
        assertThat(result.getGlobalModel().getModelId()).isNotNull();

        System.out.println("✅ 结果获取成功 - 5个参与者结果完整，全局模型可用");
        System.out.println("   最终精度: " + String.format("%.3f", result.getFinalMetrics().getFinalAccuracy()));
        System.out.println("   训练时长: " + result.getTotalDuration());
    }

    @Test
    @Order(11)
    void test11_QueryModelVersions() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminAccessToken);

        ResponseEntity<ApiResponse<PageResponse<ModelVersionResponse>>> response =
            restTemplate.exchange(
                baseUrl + "/api/model/versions?taskId=" + taskId + "&page=1&size=50",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ModelVersionResponse>>>() {}
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ModelVersionResponse> models = response.getBody().getData();

        // 应该有至少40个模型版本（5个VM * 8轮）
        assertThat(models.getTotal()).isGreaterThanOrEqualTo(40);
        assertThat(models.getItems()).isNotEmpty();

        System.out.println("✅ 模型版本查询成功 - 共" + models.getTotal() + "个模型版本");
    }

    @Test
    @Order(12)
    void test12_CleanupConnections() {
        // 优雅关闭所有WebSocket连接
        mockVMs.forEach(vm -> {
            try {
                vm.disconnect();
                System.out.println("✅ " + vm.getVmId() + " 断开连接");
            } catch (Exception e) {
                // 忽略断开连接时的异常
            }
        });

        // 等待连接清理
        Thread.sleep(2000);

        // 验证连接已断开
        mockVMs.forEach(vm -> assertThat(vm.isConnected()).isFalse());

        System.out.println("🎯 完整的5VM联邦学习流程测试成功完成！");
    }

    private String datasetId;

    // 辅助方法
    private String generateTestCsvData(int rows) {
        StringBuilder csv = new StringBuilder();
        csv.append("frequency,amplitude,phase,snr,distance,depth\n");

        Random random = new Random(42); // 固定种子确保可重复性
        for (int i = 0; i < rows; i++) {
            csv.append(String.format("%.1f,%.2f,%.2f,%.1f,%d,%d\n",
                100 + random.nextFloat() * 900,
                random.nextFloat(),
                random.nextFloat() * 6.28,
                10 + random.nextFloat() * 20,
                50 + random.nextInt(500),
                20 + random.nextInt(100)
            ));
        }

        return csv.toString();
    }
}
```

### VmTestData 数据类
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VmTestData {
    private String vmId;
    private String name;
    private String ipAddress;
    private int port;
    private int cpuCores;
    private int memoryMb;
    private int gpuCount;
}
```

### Mock虚拟机实现 (针对5台VM优化)
```java
@Component
public class MockVirtualMachine {
    private final VmTestData vmData;
    private WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean registered = false;
    private boolean connected = false;
    private String sessionId;
    private ScheduledExecutorService heartbeatExecutor;

    public MockVirtualMachine(VmTestData vmData) {
        this.vmData = vmData;
    }

    public String register(String baseUrl) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        VmRegisterRequest request = VmRegisterRequest.builder()
            // 注意：不传vmId，让后端自动生成
            .name(vmData.getName())
            .ipAddress(vmData.getIpAddress())
            .port(vmData.getPort())
            .capabilities(generateCapabilities())
            .specifications(generateSpecifications())
            .build();

        ResponseEntity<ApiResponse<VmRegisterResponse>> response =
            restTemplate.postForEntity(
                baseUrl + "/api/v1/vm/register",
                request,
                new ParameterizedTypeReference<ApiResponse<VmRegisterResponse>>() {}
            );

        if (response.getStatusCode() == HttpStatus.OK) {
            VmRegisterResponse responseData = response.getBody().getData();
            sessionId = responseData.getSessionId();
            String generatedVmId = responseData.getVmId(); // 获取后端生成的vmId
            vmData.setVmId(generatedVmId); // 更新本地vmId
            registered = true;
            return generatedVmId; // 返回生成的vmId
        } else {
            throw new RuntimeException("VM registration failed for " + vmData.getName());
        }
    }

    public void connectWebSocket(String websocketUrl) throws Exception {
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
            new StandardWebSocketClient(),
            new WebSocketHandler(),
            websocketUrl
        );

        manager.start();

        // 等待连接建立
        Thread.sleep(1000);

        // 发送连接消息
        ProtocolMessage connectMessage = ProtocolMessage.builder()
            .type(ProtocolType.CONNECT)
            .id("connect-" + System.currentTimeMillis())
            .timestamp(Instant.now())
            .vmId(vmData.getVmId())
            .data(Map.of(
                "sessionId", sessionId,
                "capabilities", generateCapabilities()
            ))
            .build();

        sendMessage(connectMessage);
        connected = true;
    }

    public void simulateTrainingRound(String taskId, int round) throws Exception {
        // 根据VM性能调整训练时间
        int baseTrainingTime = getBaseTrainingTime();
        Thread.sleep(baseTrainingTime + (int)(Math.random() * 500));

        // 生成基于VM能力的训练结果
        TrainingMetrics metrics = generateTrainingMetrics(round);

        ProtocolMessage modelUpload = ProtocolMessage.builder()
            .type(ProtocolType.MODEL_UPLOAD)
            .id("upload-r" + round + "-" + System.currentTimeMillis())
            .timestamp(Instant.now())
            .vmId(vmData.getVmId())
            .data(Map.of(
                "taskId", taskId,
                "round", round,
                "parameters", generateMockModelParameters(),
                "metrics", Map.of(
                    "accuracy", metrics.getAccuracy(),
                    "loss", metrics.getLoss(),
                    "trainingTime", metrics.getTrainingTime(),
                    "dataPoints", getDataPointsForVM(),
                    "epochs", 3
                ),
                "deviceInfo", Map.of(
                    "gpuUsed", vmData.getGpuCount() > 0,
                    "cpuCores", vmData.getCpuCores(),
                    "memoryMb", vmData.getMemoryMb()
                )
            ))
            .build();

        sendMessage(modelUpload);
    }

    private int getBaseTrainingTime() {
        // 根据VM性能计算基础训练时间
        double cpuFactor = 1.0 / (vmData.getCpuCores() / 4.0);
        double gpuFactor = vmData.getGpuCount() > 0 ? 0.6 : 1.0;
        return (int)(2000 * cpuFactor * gpuFactor);
    }

    private TrainingMetrics generateTrainingMetrics(int round) {
        // 根据VM性能和轮次生成训练指标
        double vmPerformanceFactor = (vmData.getCpuCores() + vmData.getGpuCount() * 4) / 20.0;
        double baseAccuracy = 0.6 + (round * 0.03) + (vmPerformanceFactor * 0.05) + (Math.random() * 0.03);
        double baseLoss = 1.2 - (round * 0.06) - (vmPerformanceFactor * 0.02) - (Math.random() * 0.05);

        return new TrainingMetrics(
            Math.min(baseAccuracy, 0.98),
            Math.max(baseLoss, 0.05),
            getBaseTrainingTime()
        );
    }

    private int getDataPointsForVM() {
        // 基于VM的名称来确定数据分配比例，而不是vmId
        // 因为vmId是动态生成的，但名称是固定的
        switch (vmData.getName()) {
            case "VM-Node-1": return 1600;  // 20% of 8000
            case "VM-Node-2": return 1200;  // 15% of 8000
            case "VM-Node-3": return 1200;  // 15% of 8000
            case "VM-Node-4": return 2000;  // 25% of 8000
            case "VM-Node-5": return 2000;  // 25% of 8000
            default: return 1000;
        }
    }

    // 其他方法保持不变...

    @Data
    @AllArgsConstructor
    private static class TrainingMetrics {
        private double accuracy;
        private double loss;
        private int trainingTime;
    }
}
```

## 测试执行指南

### 运行前检查
1. 确保后端服务正常运行
2. 确保数据库连接正常
3. 确保WebSocket端点可访问
4. 管理员账号已创建

### 执行测试
```bash
# 运行完整流程测试
mvn test -Dtest=CompleteFederatedLearningFlowTest

# 或者运行特定测试方法
mvn test -Dtest=CompleteFederatedLearningFlowTest#test08_ExecuteFederatedLearning
```

### 预期结果
- 所有5台虚拟机成功注册
- WebSocket连接稳定
- 8轮联邦学习训练完成
- 模型精度逐步提升
- 最终模型可下载
- 资源使用合理

这个测试文档专门针对5台虚拟机的单体服务器环境进行了优化，去除了压力测试内容，确保测试的稳定性和可执行性。