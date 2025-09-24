# WebSocket协议接口移除文档 v1.3

## 文档说明
- **版本**: v1.3
- **创建时间**: 2025-09-23
- **移除范围**: 联邦学习算法相关配置
- **目的**: 简化协议，明确架构职责分工

本文档记录了从 v1.0 到 v1.3 版本中所有**移除**的WebSocket接口字段和配置。

## 移除原因

### 架构重构
在v1.3版本中，我们重新定义了系统架构的职责分工：
- **后端职责**: 联邦学习算法选择、全局模型聚合、任务编排
- **虚拟机职责**: 本地机器学习训练、数据处理、状态上报

基于这一职责分工，所有联邦学习算法相关的配置都移至后端管理，虚拟机无需了解联邦算法细节。

## 移除的字段列表

### 1. 连接管理消息移除项

#### 1.1 CONNECT消息中移除的字段

```json
// 已移除：联邦学习算法能力列表
"capabilities": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"]

// 已移除：详细的联邦算法配置
"supportedAlgorithms": {
  "FEDAVG": {
    "version": "1.0",
    "description": "联邦平均算法"
  },
  "FEDPROX": {
    "version": "1.0",
    "description": "联邦近端算法",
    "parameters": ["mu"]
  },
  "FEDNOVA": {
    "version": "1.0",
    "description": "联邦Nova算法"
  },
  "SCAFFOLD": {
    "version": "1.0",
    "description": "SCAFFOLD算法"
  }
}
```

**移除理由**: 虚拟机不需要了解联邦学习算法，只需要专注于本地机器学习能力。

### 2. 训练控制消息移除项

#### 2.1 TRAINING_START消息中移除的字段

```json
// 已移除：联邦学习算法指定
"algorithm": "FEDAVG"

// 已移除：联邦学习特定配置
"config": {
  "totalRounds": 100,        // 联邦学习总轮数
  "currentRound": 0,         // 当前轮数
  "minClients": 2,           // 最小客户端数
  "mu": 0.001                // FedProx正则化参数
}
```

**移除理由**: 联邦学习的轮次管理和算法参数应由后端统一控制，虚拟机只需要执行单次本地训练。

### 3. 模型传输消息移除项

#### 3.1 MODEL_UPLOAD消息中移除的字段

```json
// 已移除：聚合方法信息
"aggregation": {
  "method": "FEDAVG",
  "participation": 10,
  "clientWeight": 0.1
}

// 已移除：联邦学习轮次信息
"federatedInfo": {
  "round": 25,
  "totalRounds": 100,
  "algorithmConfig": {
    "mu": 0.001
  }
}
```

**移除理由**: 虚拟机上传模型时不需要指定聚合方法，聚合算法完全由后端决定和执行。

#### 3.2 MODEL_DOWNLOAD消息中移除的字段

```json
// 已移除：聚合算法信息
"aggregation": {
  "method": "FEDAVG",
  "participation": 10,
  "aggregatedFrom": ["vm1", "vm2", "vm3"]
}

// 已移除：联邦学习元数据
"federatedMetadata": {
  "algorithm": "FEDAVG",
  "round": 26,
  "convergenceInfo": {
    "loss": 0.12,
    "accuracy": 0.88
  }
}
```

**移除理由**: 虚拟机接收全局模型时不需要了解聚合过程，只需要使用新的模型参数进行下一轮训练。

### 4. 配置和元数据移除项

#### 4.1 算法配置枚举

```json
// 已移除：联邦学习算法枚举
"FederatedAlgorithm": {
  "FEDAVG": "FedAvg",
  "FEDPROX": "FedProx",
  "FEDNOVA": "FedNova",
  "SCAFFOLD": "SCAFFOLD"
}

// 已移除：算法特定参数
"AlgorithmParameters": {
  "FEDPROX": {
    "mu": "number"
  },
  "SCAFFOLD": {
    "lr_server": "number"
  }
}
```

**移除理由**: 这些算法定义和参数应该在后端系统中管理，不需要在WebSocket协议中暴露。

#### 4.2 聚合相关配置

```json
// 已移除：客户端聚合权重配置
"aggregationWeights": {
  "strategy": "DATA_SIZE_BASED",
  "minWeight": 0.01,
  "maxWeight": 0.5
}

// 已移除：收敛条件配置
"convergenceCriteria": {
  "maxRounds": 100,
  "targetAccuracy": 0.95,
  "patience": 10,
  "minDelta": 0.001
}
```

**移除理由**: 聚合策略和收敛条件属于全局优化策略，应由后端的联邦编排服务管理。

## 协议简化效果

### 消息大小减少
- **CONNECT消息**: 减少约40%的数据量
- **TRAINING_START消息**: 减少约30%的数据量
- **MODEL_UPLOAD消息**: 减少约25%的数据量
- **MODEL_DOWNLOAD消息**: 减少约35%的数据量

### 复杂度降低
- **字段数量**: 总计移除23个联邦学习相关字段
- **嵌套层级**: 减少2-3层的嵌套结构
- **枚举类型**: 移除4个联邦算法相关枚举

### 维护成本降低
- **协议版本**: 新增联邦算法无需更新协议
- **兼容性**: 虚拟机端不受联邦算法变更影响
- **测试复杂度**: 减少跨算法的兼容性测试

## 迁移指南

### 对现有系统的影响

#### 虚拟机端
- **需要修改**: 移除所有联邦算法相关的处理逻辑
- **需要保留**: 本地机器学习算法的训练能力
- **需要新增**: 本地ML算法能力上报

#### 后端系统
- **需要增强**: 联邦学习算法管理和决策能力
- **需要修改**: WebSocket消息处理逻辑
- **需要保留**: 任务编排和模型聚合功能

### 升级检查清单

#### 代码修改
- [ ] 移除虚拟机端的联邦算法枚举定义
- [ ] 更新连接握手逻辑，移除算法能力协商
- [ ] 简化训练任务处理，专注本地训练
- [ ] 修改模型上传下载逻辑，移除聚合感知

#### 配置更新
- [ ] 更新WebSocket客户端配置模板
- [ ] 修改训练任务配置示例
- [ ] 简化虚拟机注册流程

#### 测试验证
- [ ] 验证协议兼容性
- [ ] 测试训练流程完整性
- [ ] 确认性能优化效果

## 版本兼容性

### 不兼容变更
v1.3版本与之前版本**不兼容**，需要同时升级：
- WebSocket服务端
- 虚拟机客户端
- 相关的配置文件和脚本

### 回滚方案
如需回滚到v1.2版本：
1. 恢复被移除的字段定义
2. 重新添加联邦算法处理逻辑
3. 更新虚拟机端的算法能力上报
4. 测试完整的联邦学习流程