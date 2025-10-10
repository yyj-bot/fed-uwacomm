// 虚拟机API Mock数据
// 基于 vm-api-reference.md 和 vm-round-models-api-reference.md 文档

import { baseVmList, getVmById } from './shared/vm-base'

export const vmApiMock = {
  // 3.1 虚拟机注册接口 - POST /api/v1/vm/register
  register: {
    success: {
      code: 200,
      message: "虚拟机注册成功",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        name: "水声联邦学习节点-001",
        status: "OFFLINE",
        connectionStatus: "DISCONNECTED",
        createdAt: "2024-01-01T00:00:00.000Z",
        sessionId: "session-123456",
        accessToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhMWIyYzNkNGU1ZjY3ODkwMTIzNDU2Nzg5MDEyMzQ1NiIsInZtSWQiOiJhMWIyYzNkNGU1ZjY3ODkwMTIzNDU2Nzg5MDEyMzQ1NiIsImlhdCI6MTcwNDEwMDgwMCwiZXhwIjoxNzA0MTg3MjAwfQ.example_signature",
        secretId: "s3cr3t_8f14e45fceea167a5a36dedd4bea2543",
        tokenExpireSeconds: 86400,
        apiEndpoints: {
          status: "/api/v1/vm/a1b2c3d4e5f678901234567890123456/status",
          control: "/api/v1/vm/a1b2c3d4e5f678901234567890123456/control"
        }
      }
    },
    error400: {
      code: 400,
      message: "请求参数错误",
      data: {
        errors: [
          {
            field: "name",
            message: "虚拟机名称不能为空"
          },
          {
            field: "cpuCores",
            message: "CPU核心数必须大于0"
          }
        ]
      }
    },
    error409: {
      code: 409,
      message: "虚拟机已存在",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        existingName: "水声联邦学习节点-001"
      }
    },
    error500: {
      code: 500,
      message: "服务器内部错误",
      data: {
        error: "数据库连接失败",
        requestId: "req-123456"
      }
    }
  },

  // 3.2 Token刷新接口 - POST /api/v1/vm/token/refresh
  tokenRefresh: {
    success: {
      code: 200,
      message: "刷新成功",
      data: {
        accessToken: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhMWIyYzNkNGU1ZjY3ODkwMTIzNDU2Nzg5MDEyMzQ1NiIsInZtSWQiOiJhMWIyYzNkNGU1ZjY3ODkwMTIzNDU2Nzg5MDEyMzQ1NiIsImlhdCI6MTcwNDEwMDgwMCwiZXhwIjoxNzA0MTg3MjAwfQ.new_signature",
        tokenExpireSeconds: 86400,
        secretId: "s3cr3t_new_7c222fb2927d828af22f592134e8932480637c0d"
      }
    }
  },

  // 4.1 虚拟机列表查询接口 - GET /api/vm/list
  list: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        total: baseVmList.length,
        page: 1,
        size: 20,
        pages: 1,
        list: baseVmList.map(vm => ({
          vmId: vm.vmId,
          name: vm.name,
          ipAddress: vm.ipAddress,
          port: vm.port,
          status: vm.status,
          osType: vm.osType,
          cpuCores: vm.cpuCores,
          memoryMb: vm.memoryMb,
          diskGb: vm.diskGb,
          connectionStatus: vm.connectionStatus,
          lastHeartbeat: vm.lastHeartbeat,
          createdAt: vm.createdAt,
          updatedAt: vm.updatedAt
        }))
      }
    }
  },

  // 4.2 虚拟机详情查询接口 - GET /api/vm/{vmId}
  // 数据来源：baseVmList（通过vmId动态获取）
  detail: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        ...(baseVmList[0] || {}),
        wsSessionId: "session-123456"
      }
    }
  },

  // 4.3 虚拟机更新接口 - PUT /api/vm/{vmId}
  update: {
    success: {
      code: 200,
      message: "虚拟机更新成功",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        name: "水声联邦学习节点-001-更新",
        updatedAt: "2024-01-01T00:00:00.000Z"
      }
    }
  },

  // 4.4 虚拟机删除接口 - DELETE /api/vm/{vmId}
  delete: {
    success: {
      code: 200,
      message: "虚拟机删除成功",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        deletedAt: "2024-01-01T00:00:00.000Z"
      }
    }
  },

  // 5.1 虚拟机启动接口 - POST /api/vm/{vmId}/start
  start: {
    success: {
      code: 200,
      message: "虚拟机启动命令已发送",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        status: "STARTING",
        commandId: "cmd-123456",
        estimatedTime: 60
      }
    }
  },

  // 5.2 虚拟机停止接口 - POST /api/vm/{vmId}/stop
  stop: {
    success: {
      code: 200,
      message: "虚拟机停止命令已发送",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        status: "STOPPING",
        commandId: "cmd-123457",
        estimatedTime: 30
      }
    }
  },

  // 5.3 虚拟机重启接口 - POST /api/vm/{vmId}/restart
  restart: {
    success: {
      code: 200,
      message: "虚拟机重启命令已发送",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        status: "STARTING",
        commandId: "cmd-123458",
        estimatedTime: 120
      }
    }
  },

  // 6.1 虚拟机状态查询接口 - GET /api/vm/{vmId}/status
  status: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        vmId: "a1b2c3d4e5f678901234567890123456",
        status: "RUNNING",
        connectionStatus: "CONNECTED",
        uptime: 3600,
        resourceUsage: {
          cpu: 25.5,
          memory: 60.2,
          disk: 45.8,
          gpu: 15.3
        },
        network: {
          ipAddress: "192.168.1.100",
          macAddress: "00:11:22:33:44:55",
          port: 22,
          uploadSpeed: 1024,
          downloadSpeed: 2048,
          latency: 50
        },
        processes: {
          total: 150,
          active: 25,
          system: 10,
          user: 15
        },
        lastHeartbeat: "2024-01-01T00:00:00.000Z",
        wsSessionId: "session-123456"
      }
    }
  }
};

// 本地模型（Local Model）API Mock数据
// 基于 vm-round-models-api-reference.md 文档
export const vmRoundModelsApiMock = {
  // 2.1 本地模型结果分页查询 - GET /api/model/vm-round-models
  list: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        total: 100,
        pages: 10,
        current: 1,
        size: 10,
        records: [
          {
            vmRoundModelId: "vmrm-001",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 25,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.88,
              loss: 0.12,
              precision: 0.85,
              recall: 0.82,
              f1Score: 0.835
            },
            createdAt: "2024-01-01T00:00:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-002",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 24,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.86,
              loss: 0.14,
              precision: 0.83,
              recall: 0.80,
              f1Score: 0.815
            },
            createdAt: "2024-01-01T00:10:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-003",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 25,
            vmId: "b2c3d4e5f6789012345678901234567a",
            metrics: {
              accuracy: 0.84,
              loss: 0.16,
              precision: 0.81,
              recall: 0.78,
              f1Score: 0.795
            },
            createdAt: "2024-01-01T00:05:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-004",
            taskId: "b2c3d4e5f6789012345678901234567a",
            roundNumber: 15,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.75,
              loss: 0.25,
              precision: 0.72,
              recall: 0.70,
              f1Score: 0.71
            },
            createdAt: "2024-01-02T00:00:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-005",
            taskId: "c3d4e5f67890123456789012345678ab",
            roundNumber: 30,
            vmId: "c3d4e5f67890123456789012345678ab",
            metrics: {
              accuracy: 0.92,
              loss: 0.08,
              precision: 0.90,
              recall: 0.89,
              f1Score: 0.895
            },
            createdAt: "2024-01-03T00:00:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-006",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 23,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.83,
              loss: 0.17,
              precision: 0.80,
              recall: 0.77,
              f1Score: 0.785
            },
            createdAt: "2024-01-01T00:20:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-007",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 22,
            vmId: "b2c3d4e5f6789012345678901234567a",
            metrics: {
              accuracy: 0.81,
              loss: 0.19,
              precision: 0.78,
              recall: 0.75,
              f1Score: 0.765
            },
            createdAt: "2024-01-01T00:30:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-008",
            taskId: "b2c3d4e5f6789012345678901234567a",
            roundNumber: 18,
            vmId: "c3d4e5f67890123456789012345678ab",
            metrics: {
              accuracy: 0.79,
              loss: 0.21,
              precision: 0.76,
              recall: 0.73,
              f1Score: 0.745
            },
            createdAt: "2024-01-02T00:15:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-009",
            taskId: "c3d4e5f67890123456789012345678ab",
            roundNumber: 28,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.87,
              loss: 0.13,
              precision: 0.84,
              recall: 0.81,
              f1Score: 0.825
            },
            createdAt: "2024-01-03T00:10:00.000Z"
          },
          {
            vmRoundModelId: "vmrm-010",
            taskId: "a1b2c3d4e5f678901234567890123456",
            roundNumber: 21,
            vmId: "a1b2c3d4e5f678901234567890123456",
            metrics: {
              accuracy: 0.80,
              loss: 0.20,
              precision: 0.77,
              recall: 0.74,
              f1Score: 0.755
            },
            createdAt: "2024-01-01T00:40:00.000Z"
          }
        ]
      }
    }
  },

  // 2.2 本地模型结果详情查询 - GET /api/model/vm-round-models/{vmRoundModelId}
  detail: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        vmRoundModelId: "vmrm-001",
        taskId: "a1b2c3d4e5f678901234567890123456",
        roundNumber: 25,
        vmId: "a1b2c3d4e5f678901234567890123456",
        modelJson: {
          architecture: "CNN",
          layers: [
            {
              type: "Conv2D",
              filters: 32,
              kernelSize: [3, 3],
              activation: "relu",
              inputShape: [28, 28, 1]
            },
            {
              type: "MaxPooling2D",
              poolSize: [2, 2]
            },
            {
              type: "Conv2D",
              filters: 64,
              kernelSize: [3, 3],
              activation: "relu"
            },
            {
              type: "MaxPooling2D",
              poolSize: [2, 2]
            },
            {
              type: "Flatten"
            },
            {
              type: "Dense",
              units: 128,
              activation: "relu"
            },
            {
              type: "Dropout",
              rate: 0.5
            },
            {
              type: "Dense",
              units: 10,
              activation: "softmax"
            }
          ],
          optimizer: {
            type: "Adam",
            learningRate: 0.001,
            beta1: 0.9,
            beta2: 0.999
          },
          loss: "categorical_crossentropy",
          metrics: ["accuracy"],
          weights: {
            totalParameters: 1235467,
            trainableParameters: 1235467,
            nonTrainableParameters: 0
          }
        },
        metrics: {
          accuracy: 0.88,
          loss: 0.12,
          precision: 0.85,
          recall: 0.82,
          f1Score: 0.835,
          trainingTime: 45.6,
          epochs: 5,
          batchSize: 32,
          learningRate: 0.001,
          validationAccuracy: 0.86,
          validationLoss: 0.14
        },
        createdAt: "2024-01-01T00:00:00.000Z"
      }
    }
  },

  // 2.3 本地模型训练指标趋势 - GET /api/model/vm-round-models/metrics/trend
  metricsTrend: {
    success: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        vmId: "a1b2c3d4e5f678901234567890123456",
        metric: "accuracy",
        trend: [
          { roundNumber: 1, value: 0.65 },
          { roundNumber: 2, value: 0.68 },
          { roundNumber: 3, value: 0.71 },
          { roundNumber: 4, value: 0.73 },
          { roundNumber: 5, value: 0.75 },
          { roundNumber: 6, value: 0.76 },
          { roundNumber: 7, value: 0.78 },
          { roundNumber: 8, value: 0.79 },
          { roundNumber: 9, value: 0.80 },
          { roundNumber: 10, value: 0.81 },
          { roundNumber: 11, value: 0.82 },
          { roundNumber: 12, value: 0.83 },
          { roundNumber: 13, value: 0.84 },
          { roundNumber: 14, value: 0.85 },
          { roundNumber: 15, value: 0.85 },
          { roundNumber: 16, value: 0.86 },
          { roundNumber: 17, value: 0.86 },
          { roundNumber: 18, value: 0.87 },
          { roundNumber: 19, value: 0.87 },
          { roundNumber: 20, value: 0.88 },
          { roundNumber: 21, value: 0.80 },
          { roundNumber: 22, value: 0.81 },
          { roundNumber: 23, value: 0.83 },
          { roundNumber: 24, value: 0.86 },
          { roundNumber: 25, value: 0.88 }
        ]
      }
    },
    // 损失趋势示例
    lossTrend: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        vmId: "a1b2c3d4e5f678901234567890123456",
        metric: "loss",
        trend: [
          { roundNumber: 1, value: 0.45 },
          { roundNumber: 2, value: 0.42 },
          { roundNumber: 3, value: 0.39 },
          { roundNumber: 4, value: 0.37 },
          { roundNumber: 5, value: 0.35 },
          { roundNumber: 6, value: 0.34 },
          { roundNumber: 7, value: 0.32 },
          { roundNumber: 8, value: 0.31 },
          { roundNumber: 9, value: 0.30 },
          { roundNumber: 10, value: 0.29 },
          { roundNumber: 11, value: 0.28 },
          { roundNumber: 12, value: 0.27 },
          { roundNumber: 13, value: 0.26 },
          { roundNumber: 14, value: 0.25 },
          { roundNumber: 15, value: 0.25 },
          { roundNumber: 16, value: 0.24 },
          { roundNumber: 17, value: 0.24 },
          { roundNumber: 18, value: 0.23 },
          { roundNumber: 19, value: 0.23 },
          { roundNumber: 20, value: 0.22 },
          { roundNumber: 21, value: 0.20 },
          { roundNumber: 22, value: 0.19 },
          { roundNumber: 23, value: 0.17 },
          { roundNumber: 24, value: 0.14 },
          { roundNumber: 25, value: 0.12 }
        ]
      }
    }
  },

  // 2.4 本地模型最佳/离群查询 - GET /api/model/vm-round-models/metrics/best
  metricsBest: {
    // 最佳准确率
    bestAccuracy: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        metric: "accuracy",
        type: "best",
        result: {
          vmRoundModelId: "vmrm-005",
          roundNumber: 30,
          vmId: "c3d4e5f67890123456789012345678ab",
          value: 0.92
        }
      }
    },
    // 最低损失
    bestLoss: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        metric: "loss",
        type: "best",
        result: {
          vmRoundModelId: "vmrm-005",
          roundNumber: 30,
          vmId: "c3d4e5f67890123456789012345678ab",
          value: 0.08
        }
      }
    },
    // 离群准确率（异常低）
    outlierAccuracy: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        metric: "accuracy",
        type: "outlier",
        result: {
          vmRoundModelId: "vmrm-004",
          roundNumber: 15,
          vmId: "a1b2c3d4e5f678901234567890123456",
          value: 0.75
        }
      }
    },
    // 离群损失（异常高）
    outlierLoss: {
      code: 200,
      message: "查询成功",
      data: {
        taskId: "a1b2c3d4e5f678901234567890123456",
        metric: "loss",
        type: "outlier",
        result: {
          vmRoundModelId: "vmrm-004",
          roundNumber: 15,
          vmId: "a1b2c3d4e5f678901234567890123456",
          value: 0.25
        }
      }
    }
  }
};

// 多虚拟机对比数据示例
export const multiVmComparisonMock = {
  // 多虚拟机同轮次对比
  roundComparison: {
    code: 200,
    message: "查询成功",
    data: {
      taskId: "a1b2c3d4e5f678901234567890123456",
      roundNumber: 25,
      vmResults: [
        {
          vmId: "a1b2c3d4e5f678901234567890123456",
          vmName: "水声联邦学习节点-001",
          metrics: {
            accuracy: 0.88,
            loss: 0.12,
            precision: 0.85,
            recall: 0.82,
            f1Score: 0.835
          }
        },
        {
          vmId: "b2c3d4e5f6789012345678901234567a",
          vmName: "水声联邦学习节点-002",
          metrics: {
            accuracy: 0.84,
            loss: 0.16,
            precision: 0.81,
            recall: 0.78,
            f1Score: 0.795
          }
        },
        {
          vmId: "c3d4e5f67890123456789012345678ab",
          vmName: "水声联邦学习节点-003",
          metrics: {
            accuracy: 0.86,
            loss: 0.14,
            precision: 0.83,
            recall: 0.80,
            f1Score: 0.815
          }
        }
      ]
    }
  },

  // 多虚拟机趋势对比
  trendComparison: {
    code: 200,
    message: "查询成功",
    data: {
      taskId: "a1b2c3d4e5f678901234567890123456",
      metric: "accuracy",
      vmTrends: [
        {
          vmId: "a1b2c3d4e5f678901234567890123456",
          vmName: "水声联邦学习节点-001",
          trend: [
            { roundNumber: 20, value: 0.82 },
            { roundNumber: 21, value: 0.80 },
            { roundNumber: 22, value: 0.81 },
            { roundNumber: 23, value: 0.83 },
            { roundNumber: 24, value: 0.86 },
            { roundNumber: 25, value: 0.88 }
          ]
        },
        {
          vmId: "b2c3d4e5f6789012345678901234567a",
          vmName: "水声联邦学习节点-002",
          trend: [
            { roundNumber: 20, value: 0.78 },
            { roundNumber: 21, value: 0.79 },
            { roundNumber: 22, value: 0.81 },
            { roundNumber: 23, value: 0.82 },
            { roundNumber: 24, value: 0.83 },
            { roundNumber: 25, value: 0.84 }
          ]
        },
        {
          vmId: "c3d4e5f67890123456789012345678ab",
          vmName: "水声联邦学习节点-003",
          trend: [
            { roundNumber: 20, value: 0.80 },
            { roundNumber: 21, value: 0.81 },
            { roundNumber: 22, value: 0.82 },
            { roundNumber: 23, value: 0.84 },
            { roundNumber: 24, value: 0.85 },
            { roundNumber: 25, value: 0.86 }
          ]
        }
      ]
    }
  }
};

// 请求参数示例
export const vmApiRequestExamples = {
  // 虚拟机注册请求参数
  register: {
    name: "水声联邦学习节点-001",
    ipAddress: "192.168.1.100",
    port: 22,
    osType: "Ubuntu 20.04",
    cpuCores: 4,
    memoryMb: 8192,
    diskGb: 100,
    systemInfo: { os: "Ubuntu 20.04 LTS" },
    capabilities: { supportedAlgorithms: ["FEDAVG"] }
  },

  // Token刷新请求参数
  tokenRefresh: {
    vmId: "a1b2c3d4e5f678901234567890123456",
    secretId: "s3cr3t_8f14e45fceea167a5a36dedd4bea2543"
  },

  // 虚拟机更新请求参数
  update: {
    name: "水声联邦学习节点-001-更新",
    ipAddress: "192.168.1.101",
    port: 2222,
    osType: "Ubuntu 20.04",
    cpuCores: 8,
    memoryMb: 16384,
    diskGb: 200,
    systemInfo: {
      os: "Ubuntu 20.04 LTS",
      kernel: "5.4.0-42-generic",
      python: "3.8.10",
      gpu: "NVIDIA Tesla V100",
      cuda: "11.0",
      cudnn: "8.0.5"
    },
    capabilities: {
      supportedAlgorithms: ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
      maxBatchSize: 256,
      maxMemoryUsage: 12288,
      gpuMemory: 16384,
      networkSpeed: 1000
    },
    networkConfig: {
      uploadSpeed: 200,
      downloadSpeed: 400,
      latency: 30,
      bandwidth: 1000
    },
    metadata: {
      description: "水声联邦学习专用虚拟机节点-更新版",
      location: "实验室A-机架01",
      owner: "张三",
      department: "水声工程学院",
      tags: ["水声", "联邦学习", "GPU节点", "高性能"]
    }
  },

  // 虚拟机启动请求参数
  start: {
    timeout: 300,
    config: {
      memory: "8GB",
      cpu: "8cores",
      disk: "100GB",
      network: "bridge"
    },
    environment: {
      variables: {
        PYTHONPATH: "/app",
        CUDA_VISIBLE_DEVICES: "0"
      }
    }
  },

  // 虚拟机停止请求参数
  stop: {
    force: false,
    timeout: 60,
    saveState: true
  },

  // 虚拟机重启请求参数
  restart: {
    timeout: 300,
    graceful: true,
    config: {
      memory: "8GB",
      cpu: "8cores"
    }
  }
};

// 查询参数示例
export const vmRoundModelsApiQueryExamples = {
  // 分页查询参数
  list: {
    taskId: "a1b2c3d4e5f678901234567890123456",
    roundNumber: 25,
    vmId: "a1b2c3d4e5f678901234567890123456",
    page: 1,
    size: 10
  },

  // 指标趋势查询参数
  metricsTrend: {
    taskId: "a1b2c3d4e5f678901234567890123456",
    vmId: "a1b2c3d4e5f678901234567890123456",
    metric: "accuracy" // 可选值: accuracy, loss, precision, recall, f1Score
  },

  // 最佳/离群查询参数
  metricsBest: {
    taskId: "a1b2c3d4e5f678901234567890123456",
    metric: "accuracy", // 可选值: accuracy, loss, precision, recall, f1Score
    type: "best" // 可选值: best, outlier
  }
};
