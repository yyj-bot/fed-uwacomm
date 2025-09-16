# 虚拟机部署管理 API 参考文档

## 1. 概述

虚拟机部署管理API提供自动化的虚拟机部署、配置、管理和销毁功能。支持多种虚拟化平台，实现联邦学习环境的快速部署和弹性扩缩容。

### 1.1 基础信息
- **模块**: 虚拟机部署管理 (VM Deployment Management)
- **基础URL**: `http://localhost:8080/api/vm/deploy`
- **认证方式**: JWT Token
- **数据格式**: JSON

### 1.2 功能概述
- 自动化虚拟机部署
- 多平台支持(Docker, VMware, KVM等)
- 部署模板管理
- 实时部署状态监控
- 批量部署操作
- 资源配额管理

---

## 2. API 接口列表

### 2.1 创建部署任务
创建虚拟机自动化部署任务

**接口信息**
- **URL**: `POST /api/vm/deploy`
- **描述**: 创建虚拟机部署任务
- **认证**: 需要JWT Token (ADMIN权限)

**请求参数**
```json
{
  "deploymentName": "federated-learning-cluster",
  "deploymentType": "BATCH",
  "platform": {
    "type": "DOCKER",
    "endpoint": "unix:///var/run/docker.sock",
    "credentials": {
      "registryUrl": "registry.example.com",
      "username": "admin",
      "password": "password"
    }
  },
  "vmTemplate": {
    "templateId": "federated-vm-template-v1.2",
    "baseImage": "feduwacomm/vm-runtime:latest",
    "resources": {
      "cpu": 2,
      "memory": "4GB",
      "storage": "20GB",
      "gpu": false
    },
    "environment": {
      "FEDERATED_SERVER_URL": "https://federated-server.example.com",
      "PYTHON_VERSION": "3.9",
      "CUDA_ENABLED": "false"
    },
    "volumes": [
      {
        "hostPath": "/data/federated",
        "containerPath": "/app/data",
        "mode": "rw"
      }
    ],
    "ports": [
      {
        "hostPort": 8080,
        "containerPort": 8080,
        "protocol": "tcp"
      }
    ]
  },
  "deploymentConfig": {
    "vmCount": 5,
    "namingPattern": "vm-fed-{index:03d}",
    "networkConfig": {
      "networkName": "federated-network",
      "subnetCidr": "192.168.100.0/24",
      "enableInterVmCommunication": true
    },
    "securityConfig": {
      "enableSsh": true,
      "sshPublicKey": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC...",
      "firewallRules": [
        {
          "port": 22,
          "protocol": "tcp",
          "source": "0.0.0.0/0",
          "action": "allow"
        },
        {
          "port": 8080,
          "protocol": "tcp",
          "source": "192.168.100.0/24",
          "action": "allow"
        }
      ]
    },
    "resourceConstraints": {
      "maxCpuPercent": 80,
      "maxMemoryPercent": 90,
      "maxStorageGB": 100,
      "maxNetworkMbps": 1000
    }
  },
  "schedulingOptions": {
    "priority": "HIGH",
    "deploymentStrategy": "PARALLEL",
    "maxConcurrentDeployments": 3,
    "deploymentTimeout": 1800,
    "healthCheckEnabled": true,
    "healthCheckTimeout": 300
  },
  "notificationConfig": {
    "notifyOnCompletion": true,
    "notifyOnError": true,
    "webhookUrl": "https://example.com/deployment-webhook",
    "emailNotifications": ["admin@example.com"]
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "部署任务创建成功",
  "data": {
    "deploymentId": "deploy_001",
    "deploymentName": "federated-learning-cluster",
    "status": "CREATED",
    "createdAt": "2024-09-10T10:00:00Z",
    "estimatedCompletion": "2024-09-10T10:30:00Z",
    "platform": "DOCKER",
    "vmCount": 5,
    "deploymentPlan": {
      "totalSteps": 15,
      "estimatedDuration": "00:30:00",
      "resourceRequirements": {
        "totalCpu": 10,
        "totalMemory": "20GB",
        "totalStorage": "100GB",
        "networkPorts": [
          "8080-8084",
          "22"
        ]
      },
      "vmSpecs": [
        {
          "vmName": "vm-fed-001",
          "ipAddress": "192.168.100.10",
          "resources": {
            "cpu": 2,
            "memory": "4GB",
            "storage": "20GB"
          }
        },
        {
          "vmName": "vm-fed-002",
          "ipAddress": "192.168.100.11",
          "resources": {
            "cpu": 2,
            "memory": "4GB",
            "storage": "20GB"
          }
        }
      ]
    }
  }
}
```

### 2.2 启动部署任务
启动已创建的虚拟机部署任务

**接口信息**
- **URL**: `POST /api/vm/deploy/{deploymentId}/start`
- **描述**: 启动虚拟机部署
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `deploymentId` (string, required): 部署任务ID

**请求参数**
```json
{
  "preDeploymentChecks": true,
  "skipExistingVms": false,
  "forceRedeploy": false,
  "dryRun": false
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "部署任务已启动",
  "data": {
    "deploymentId": "deploy_001",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:01:00Z",
    "preDeploymentChecks": {
      "platformConnectivity": "PASSED",
      "resourceAvailability": "PASSED",
      "templateValidity": "PASSED",
      "networkConfiguration": "PASSED"
    },
    "progress": {
      "totalVms": 5,
      "deploying": 3,
      "deployed": 0,
      "failed": 0,
      "percentage": 15.0
    }
  }
}
```

### 2.3 查询部署状态
查询虚拟机部署任务的详细状态

**接口信息**
- **URL**: `GET /api/vm/deploy/{deploymentId}/status`
- **描述**: 查询部署状态
- **认证**: 需要JWT Token

**路径参数**
- `deploymentId` (string, required): 部署任务ID

**查询参数**
- `includeVmDetails` (boolean, optional): 是否包含虚拟机详情，默认true
- `includeLogs` (boolean, optional): 是否包含部署日志，默认false
- `refresh` (boolean, optional): 是否刷新状态，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "deploymentId": "deploy_001",
    "deploymentName": "federated-learning-cluster",
    "status": "IN_PROGRESS",
    "createdAt": "2024-09-10T10:00:00Z",
    "startedAt": "2024-09-10T10:01:00Z",
    "lastUpdated": "2024-09-10T10:15:30Z",
    "platform": "DOCKER",
    "progress": {
      "totalVms": 5,
      "deploying": 1,
      "deployed": 3,
      "failed": 1,
      "percentage": 75.0
    },
    "resourceUsage": {
      "allocatedCpu": 6,
      "allocatedMemory": "12GB",
      "allocatedStorage": "60GB",
      "usedPorts": ["8080", "8081", "8082"]
    },
    "vmDetails": [
      {
        "vmName": "vm-fed-001",
        "vmId": "container_abc123",
        "status": "DEPLOYED",
        "ipAddress": "192.168.100.10",
        "deployedAt": "2024-09-10T10:05:15Z",
        "healthStatus": "HEALTHY",
        "resources": {
          "cpu": 2,
          "memory": "4GB",
          "storage": "20GB"
        },
        "services": {
          "ssh": {
            "status": "RUNNING",
            "port": 22,
            "accessible": true
          },
          "federated-client": {
            "status": "RUNNING",
            "port": 8080,
            "version": "1.0.0"
          }
        }
      },
      {
        "vmName": "vm-fed-002",
        "vmId": "container_def456",
        "status": "DEPLOYED",
        "ipAddress": "192.168.100.11",
        "deployedAt": "2024-09-10T10:07:30Z",
        "healthStatus": "HEALTHY",
        "resources": {
          "cpu": 2,
          "memory": "4GB",
          "storage": "20GB"
        }
      },
      {
        "vmName": "vm-fed-003",
        "vmId": "container_ghi789",
        "status": "DEPLOYED",
        "ipAddress": "192.168.100.12",
        "deployedAt": "2024-09-10T10:09:45Z",
        "healthStatus": "STARTING",
        "resources": {
          "cpu": 2,
          "memory": "4GB",
          "storage": "20GB"
        }
      },
      {
        "vmName": "vm-fed-004",
        "status": "DEPLOYING",
        "progress": 65.0,
        "currentStep": "配置网络",
        "estimatedCompletion": "2024-09-10T10:18:00Z"
      },
      {
        "vmName": "vm-fed-005",
        "status": "FAILED",
        "errorMessage": "镜像拉取失败: registry.example.com/feduwacomm/vm-runtime:latest not found",
        "errorCode": "IMAGE_PULL_ERROR",
        "failedAt": "2024-09-10T10:12:30Z",
        "retryCount": 2,
        "maxRetries": 3
      }
    ],
    "networkInfo": {
      "networkName": "federated-network",
      "networkId": "net_federated_001",
      "subnetCidr": "192.168.100.0/24",
      "gateway": "192.168.100.1",
      "dnsServers": ["8.8.8.8", "8.8.4.4"]
    }
  }
}
```

### 2.4 配置已部署虚拟机
对已部署的虚拟机进行配置更新

**接口信息**
- **URL**: `POST /api/vm/deploy/{deploymentId}/configure`
- **描述**: 配置已部署的虚拟机
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `deploymentId` (string, required): 部署任务ID

**请求参数**
```json
{
  "targetVms": ["vm-fed-001", "vm-fed-002"],
  "configurations": {
    "environment": {
      "FEDERATED_SERVER_URL": "https://new-server.example.com",
      "LOG_LEVEL": "DEBUG"
    },
    "services": {
      "federatedClient": {
        "restart": true,
        "configUpdate": {
          "serverEndpoint": "https://new-server.example.com/api",
          "heartbeatInterval": 30
        }
      }
    },
    "resources": {
      "memory": "6GB"
    },
    "networking": {
      "additionalPorts": [9090],
      "firewallUpdates": [
        {
          "port": 9090,
          "protocol": "tcp",
          "source": "0.0.0.0/0",
          "action": "allow"
        }
      ]
    }
  },
  "configurationOptions": {
    "applyMode": "ROLLING",
    "maxConcurrent": 2,
    "rollbackOnFailure": true,
    "validateAfterApply": true
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "配置更新已启动",
  "data": {
    "configurationId": "config_001",
    "deploymentId": "deploy_001",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:20:00Z",
    "targetVms": ["vm-fed-001", "vm-fed-002"],
    "progress": {
      "totalVms": 2,
      "configuring": 1,
      "completed": 0,
      "failed": 0,
      "percentage": 25.0
    },
    "configurationDetails": [
      {
        "vmName": "vm-fed-001",
        "status": "IN_PROGRESS",
        "currentStep": "更新环境变量",
        "progress": 50.0
      },
      {
        "vmName": "vm-fed-002",
        "status": "PENDING",
        "estimatedStart": "2024-09-10T10:22:00Z"
      }
    ]
  }
}
```

### 2.5 扩容部署
为现有部署添加更多虚拟机

**接口信息**
- **URL**: `POST /api/vm/deploy/{deploymentId}/scale`
- **描述**: 扩容虚拟机部署
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `deploymentId` (string, required): 部署任务ID

**请求参数**
```json
{
  "scaleOperation": "SCALE_OUT",
  "additionalVmCount": 3,
  "vmTemplate": {
    "inheritFromDeployment": true,
    "overrides": {
      "resources": {
        "memory": "6GB"
      }
    }
  },
  "namingPattern": "vm-fed-scale-{index:03d}",
  "networkConfig": {
    "useExistingNetwork": true,
    "ipRangeStart": "192.168.100.20"
  },
  "scalingOptions": {
    "deploymentStrategy": "PARALLEL",
    "healthCheckEnabled": true,
    "autoRegister": true
  }
}
```

**响应示例**
```json
{
  "code": 200,
  "message": "扩容操作已启动",
  "data": {
    "scaleOperationId": "scale_001",
    "deploymentId": "deploy_001",
    "status": "IN_PROGRESS",
    "scaleOperation": "SCALE_OUT",
    "startedAt": "2024-09-10T10:25:00Z",
    "additionalVms": {
      "count": 3,
      "names": ["vm-fed-scale-001", "vm-fed-scale-002", "vm-fed-scale-003"],
      "ipAddresses": ["192.168.100.20", "192.168.100.21", "192.168.100.22"]
    },
    "progress": {
      "totalNewVms": 3,
      "deploying": 3,
      "deployed": 0,
      "failed": 0,
      "percentage": 10.0
    }
  }
}
```

### 2.6 销毁部署
销毁整个虚拟机部署或指定虚拟机

**接口信息**
- **URL**: `DELETE /api/vm/deploy/{deploymentId}`
- **描述**: 销毁虚拟机部署
- **认证**: 需要JWT Token (ADMIN权限)

**路径参数**
- `deploymentId` (string, required): 部署任务ID

**查询参数**
- `vmNames` (string[], optional): 指定要销毁的虚拟机名称
- `force` (boolean, optional): 强制销毁，默认false
- `preserveData` (boolean, optional): 保留数据卷，默认false
- `cleanup` (boolean, optional): 清理网络和资源，默认true

**响应示例**
```json
{
  "code": 200,
  "message": "销毁操作已启动",
  "data": {
    "destructionId": "destroy_001",
    "deploymentId": "deploy_001",
    "status": "IN_PROGRESS",
    "startedAt": "2024-09-10T10:30:00Z",
    "targetVms": ["vm-fed-001", "vm-fed-002", "vm-fed-003"],
    "destructionPlan": {
      "steps": [
        "停止虚拟机服务",
        "断开网络连接",
        "销毁虚拟机实例",
        "清理存储卷",
        "释放网络资源"
      ],
      "estimatedDuration": "00:10:00"
    },
    "progress": {
      "totalVms": 3,
      "destroying": 3,
      "destroyed": 0,
      "failed": 0,
      "percentage": 15.0
    }
  }
}
```

### 2.7 获取部署列表
获取用户的虚拟机部署历史

**接口信息**
- **URL**: `GET /api/vm/deploy`
- **描述**: 获取部署列表
- **认证**: 需要JWT Token

**查询参数**
- `status` (string, optional): 部署状态过滤
- `platform` (string, optional): 平台类型过滤
- `page` (integer, optional): 页码，默认1
- `size` (integer, optional): 每页大小，默认20
- `sortBy` (string, optional): 排序字段，默认createdAt
- `sortOrder` (string, optional): 排序方向，默认desc

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 15,
    "page": 1,
    "size": 20,
    "items": [
      {
        "deploymentId": "deploy_001",
        "deploymentName": "federated-learning-cluster",
        "status": "DEPLOYED",
        "platform": "DOCKER",
        "vmCount": 5,
        "createdAt": "2024-09-10T10:00:00Z",
        "completedAt": "2024-09-10T10:25:30Z",
        "duration": "00:25:30",
        "healthyVms": 5,
        "failedVms": 0
      },
      {
        "deploymentId": "deploy_002",
        "deploymentName": "test-cluster",
        "status": "FAILED",
        "platform": "DOCKER",
        "vmCount": 3,
        "createdAt": "2024-09-09T15:30:00Z",
        "failedAt": "2024-09-09T15:45:15Z",
        "duration": "00:15:15",
        "errorMessage": "资源不足"
      }
    ]
  }
}
```

### 2.8 获取部署模板
获取可用的虚拟机部署模板

**接口信息**
- **URL**: `GET /api/vm/deploy/templates`
- **描述**: 获取部署模板列表
- **认证**: 需要JWT Token

**查询参数**
- `platform` (string, optional): 平台类型过滤
- `category` (string, optional): 模板类别过滤
- `includeDetails` (boolean, optional): 是否包含模板详情，默认false

**响应示例**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "templates": [
      {
        "templateId": "federated-vm-template-v1.2",
        "templateName": "联邦学习VM模板 v1.2",
        "platform": "DOCKER",
        "category": "FEDERATED_LEARNING",
        "version": "1.2.0",
        "description": "标准的联邦学习虚拟机模板，包含Python运行环境和联邦学习客户端",
        "baseImage": "feduwacomm/vm-runtime:latest",
        "defaultResources": {
          "cpu": 2,
          "memory": "4GB",
          "storage": "20GB"
        },
        "supportedFeatures": [
          "FEDERATED_TRAINING",
          "DATA_PROCESSING",
          "MODEL_VALIDATION"
        ],
        "createdAt": "2024-09-01T10:00:00Z",
        "updatedAt": "2024-09-08T15:30:00Z"
      },
      {
        "templateId": "high-performance-vm-v1.0",
        "templateName": "高性能计算VM模板",
        "platform": "DOCKER",
        "category": "HIGH_PERFORMANCE",
        "version": "1.0.0",
        "description": "高性能计算虚拟机模板，支持GPU加速和大内存处理",
        "baseImage": "feduwacomm/hpc-runtime:gpu-latest",
        "defaultResources": {
          "cpu": 8,
          "memory": "16GB",
          "storage": "50GB",
          "gpu": true
        }
      }
    ]
  }
}
```

---

## 3. 数据模型定义

### 3.1 VmDeployment
```json
{
  "deploymentId": "string",         // 部署任务唯一标识
  "deploymentName": "string",       // 部署名称
  "status": "string",               // 部署状态
  "platform": "string",            // 部署平台
  "vmCount": "number",              // 虚拟机数量
  "createdAt": "string",            // 创建时间
  "startedAt": "string",            // 开始时间
  "completedAt": "string",          // 完成时间
  "progress": "DeploymentProgress", // 部署进度
  "vmDetails": ["VmInstance"]       // 虚拟机详情
}
```

### 3.2 VmInstance
```json
{
  "vmName": "string",               // 虚拟机名称
  "vmId": "string",                 // 虚拟机实例ID
  "status": "string",               // 虚拟机状态
  "ipAddress": "string",            // IP地址
  "resources": "ResourceSpec",      // 资源规格
  "services": "object",             // 运行的服务
  "healthStatus": "string"          // 健康状态
}
```

### 3.3 部署状态枚举
- `CREATED`: 已创建
- `IN_PROGRESS`: 部署中
- `DEPLOYED`: 已部署
- `CONFIGURING`: 配置中
- `SCALING`: 扩缩容中
- `FAILED`: 部署失败
- `DESTROYING`: 销毁中
- `DESTROYED`: 已销毁

### 3.4 平台类型枚举
- `DOCKER`: Docker容器
- `VMWARE`: VMware虚拟机
- `KVM`: KVM虚拟机
- `KUBERNETES`: Kubernetes Pod
- `AWS_EC2`: AWS EC2实例
- `AZURE_VM`: Azure虚拟机

---

## 4. 错误码定义

### 4.1 部署相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| DEPLOYMENT_NOT_FOUND | 404 | 部署任务不存在 |
| DEPLOYMENT_CREATION_FAILED | 500 | 部署创建失败 |
| DEPLOYMENT_START_FAILED | 500 | 部署启动失败 |
| DEPLOYMENT_TIMEOUT | 408 | 部署超时 |
| DEPLOYMENT_RESOURCE_INSUFFICIENT | 503 | 资源不足 |
| DEPLOYMENT_PLATFORM_ERROR | 502 | 平台连接错误 |
| DEPLOYMENT_TEMPLATE_INVALID | 422 | 模板配置无效 |
| DEPLOYMENT_NETWORK_ERROR | 502 | 网络配置错误 |

### 4.2 虚拟机相关错误码
| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| VM_CREATION_FAILED | 500 | 虚拟机创建失败 |
| VM_START_FAILED | 500 | 虚拟机启动失败 |
| VM_CONFIGURATION_FAILED | 500 | 虚拟机配置失败 |
| VM_HEALTH_CHECK_FAILED | 503 | 健康检查失败 |
| VM_IMAGE_PULL_FAILED | 502 | 镜像拉取失败 |

---

## 5. 使用示例

### 5.1 完整的部署流程

```javascript
// 1. 创建部署任务
const createDeployment = async (config) => {
  const response = await fetch('/api/vm/deploy', {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      deploymentName: 'federated-learning-cluster',
      deploymentType: 'BATCH',
      platform: {
        type: 'DOCKER',
        endpoint: 'unix:///var/run/docker.sock'
      },
      vmTemplate: {
        templateId: 'federated-vm-template-v1.2',
        resources: {
          cpu: 2,
          memory: '4GB',
          storage: '20GB'
        }
      },
      deploymentConfig: {
        vmCount: 5,
        namingPattern: 'vm-fed-{index:03d}'
      }
    })
  });
  
  return await response.json();
};

// 2. 启动部署
const startDeployment = async (deploymentId) => {
  const response = await fetch(`/api/vm/deploy/${deploymentId}/start`, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      preDeploymentChecks: true,
      skipExistingVms: false
    })
  });
  
  return await response.json();
};

// 3. 监控部署进度
const monitorDeployment = async (deploymentId) => {
  const response = await fetch(`/api/vm/deploy/${deploymentId}/status?includeVmDetails=true`, {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + token
    }
  });
  
  return await response.json();
};
```

### 5.2 实时监控部署状态

```javascript
const trackDeploymentProgress = (deploymentId, onUpdate, onComplete, onError) => {
  const checkProgress = async () => {
    try {
      const status = await monitorDeployment(deploymentId);
      onUpdate(status.data);
      
      if (status.data.status === 'DEPLOYED') {
        onComplete(status.data);
        return;
      } else if (['FAILED', 'DESTROYED'].includes(status.data.status)) {
        onError(status.data);
        return;
      }
      
      // 继续监控
      setTimeout(checkProgress, 10000); // 每10秒检查一次
    } catch (error) {
      onError(error);
    }
  };
  
  checkProgress();
};

// 使用示例
trackDeploymentProgress('deploy_001',
  (status) => {
    console.log(`部署进度: ${status.progress.percentage}%`);
    console.log(`已部署: ${status.progress.deployed}/${status.progress.totalVms}`);
  },
  (finalStatus) => {
    console.log('部署完成!');
    console.log(`健康虚拟机: ${finalStatus.healthyVms}/${finalStatus.vmCount}`);
  },
  (error) => {
    console.error('部署失败:', error);
  }
);
```

### 5.3 批量配置虚拟机

```javascript
const configureVms = async (deploymentId, vmNames, configurations) => {
  const response = await fetch(`/api/vm/deploy/${deploymentId}/configure`, {
    method: 'POST',
    headers: {
      'Authorization': 'Bearer ' + token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      targetVms: vmNames,
      configurations: configurations,
      configurationOptions: {
        applyMode: 'ROLLING',
        maxConcurrent: 2,
        rollbackOnFailure: true
      }
    })
  });
  
  return await response.json();
};

// 使用示例
const updateServerConfig = async () => {
  const result = await configureVms('deploy_001', 
    ['vm-fed-001', 'vm-fed-002', 'vm-fed-003'],
    {
      environment: {
        FEDERATED_SERVER_URL: 'https://new-server.example.com',
        LOG_LEVEL: 'DEBUG'
      },
      services: {
        federatedClient: {
          restart: true,
          configUpdate: {
            serverEndpoint: 'https://new-server.example.com/api'
          }
        }
      }
    }
  );
  
  console.log('配置更新已启动:', result.data.configurationId);
};
```

---

## 6. 最佳实践

### 6.1 部署规划
- 根据联邦学习任务需求选择合适的虚拟机规格
- 合理规划网络拓扑和IP地址分配
- 设置适当的资源约束和配额限制
- 考虑故障域分离和高可用性部署

### 6.2 性能优化
- 使用本地镜像缓存加速部署
- 并行部署多个虚拟机以减少总体时间
- 预先拉取镜像到部署节点
- 使用SSD存储提高I/O性能

### 6.3 安全考虑
- 使用强密码和SSH密钥认证
- 配置防火墙规则限制网络访问
- 定期更新镜像和安全补丁
- 实施网络隔离和访问控制

### 6.4 监控和维护
- 实施全面的健康检查和监控
- 设置自动化的日志收集和分析
- 建立故障自动恢复机制
- 定期进行备份和灾难恢复演练