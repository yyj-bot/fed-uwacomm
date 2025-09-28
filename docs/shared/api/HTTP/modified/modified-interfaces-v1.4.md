# API接口修改文档 v1.4

## 修改概述
**版本:** v1.4
**日期:** 2025-09-26
**修改类型:** 新增聚合引擎监控接口 + 架构简化

## 主要变更

### 1. 新增聚合引擎监控接口
- **新增接口:** `GET /api/federated/engine/status` - 聚合引擎状态查询
- **新增接口:** `GET /api/federated/strategies/available` - 可用策略查询
- **变更原因:** 完善UniversalAggregationEngine的REST接口暴露，提高系统可观测性
- **影响范围:** 前端监控界面、任务配置界面

### 2. 联邦学习架构简化
- **变更原因:** 明确后端负责聚合，虚拟机只负责训练的架构设计
- **影响范围:** 所有涉及参与者角色配置的API接口

### 3. 移除AGGREGATOR角色
- **实体类更新:** TaskParticipant.role 注释更新，只支持 PARTICIPANT
- **DTO类更新:** 所有DTO中role字段只支持 PARTICIPANT
- **服务层更新:** 角色配置API只返回 PARTICIPANT 选项
- **文档更新:** 所有API示例移除AGGREGATOR角色引用

## 具体接口变更

### 🆕 新增接口详情

#### 聚合引擎状态查询接口
**接口:** `GET /api/federated/engine/status`

**功能描述:**
- 查询UniversalAggregationEngine的运行状态
- 监控当前聚合任务和系统性能指标
- 获取支持的聚合算法列表

**响应示例:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "engineStatus": "RUNNING",
    "currentTasks": [
      {
        "taskId": "c3d4e5f6789012345678901234567890",
        "status": "AGGREGATING",
        "currentRound": 5,
        "algorithm": "FEDERATED_AVERAGING",
        "participantCount": 3
      }
    ],
    "systemMetrics": {
      "cpuUsage": 45.2,
      "memoryUsage": 68.5,
      "diskUsage": 23.8
    },
    "aggregationMetrics": {
      "totalAggregations": 125,
      "successRate": 0.98,
      "averageAggregationTime": 2.3
    },
    "supportedAlgorithms": [
      "FEDERATED_AVERAGING",
      "FEDERATED_PROXIMAL",
      "FEDERATED_NOVA",
      "FEDERATED_SCAFFOLD"
    ]
  }
}
```

#### 可用聚合策略查询接口
**接口:** `GET /api/federated/strategies/available`

**功能描述:**
- 查询系统支持的所有聚合策略
- 获取每种策略的详细参数配置
- 提供策略分类和推荐信息

**响应示例:**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total": 4,
    "strategies": [
      {
        "algorithm": "FEDERATED_AVERAGING",
        "name": "联邦平均算法",
        "description": "经典的FedAvg算法，适用于大多数联邦学习场景",
        "category": "AVERAGING",
        "supportedModelTypes": ["RANDOM_FOREST", "NEURAL_NETWORK"],
        "parameters": [
          {
            "name": "learningRate",
            "type": "DOUBLE",
            "description": "学习率",
            "defaultValue": 0.01,
            "range": { "min": 0.0001, "max": 1.0 }
          }
        ],
        "requirements": {
          "minParticipants": 2,
          "maxParticipants": 100,
          "recommendedParticipants": 5
        }
      }
    ]
  }
}
```

### 🔄 修改接口详情

#### 联邦任务创建接口
**接口:** `POST /api/federated-task/create`

#### 变更前:
```json
{
  "participantConfig": {
    "participants": [
      {"vmId": "vm1", "role": "PARTICIPANT"},
      {"vmId": "vm2", "role": "AGGREGATOR"}
    ]
  }
}
```

#### 变更后:
```json
{
  "participantConfig": {
    "participants": [
      {"vmId": "vm1", "role": "PARTICIPANT"},
      {"vmId": "vm2", "role": "PARTICIPANT"}
    ]
  }
}
```

### 角色配置查询接口
**接口:** `GET /api/federated-task/config/roles`

#### 变更前:
```json
{
  "data": {
    "roles": [
      {"role": "PARTICIPANT", "name": "参与者"},
      {"role": "AGGREGATOR", "name": "聚合器"}
    ]
  }
}
```

#### 变更后:
```json
{
  "data": {
    "roles": [
      {
        "role": "PARTICIPANT",
        "name": "参与者",
        "description": "参与联邦学习训练的客户端节点，所有虚拟机均为参与者角色，聚合由后端服务统一处理"
      }
    ]
  }
}
```

## 兼容性说明

### 向后兼容
- 现有PARTICIPANT角色配置保持不变
- 已有的联邦学习算法实现无需修改
- WebSocket通信协议不受影响

### 不兼容变更
- 不再接受role为"AGGREGATOR"的参与者配置
- 前端角色选择界面需要更新
- 测试用例中的AGGREGATOR角色需要改为PARTICIPANT

## 迁移指南

### 1. 后端代码迁移
- 检查所有创建任务的代码，确保role字段设为"PARTICIPANT"
- 更新验证逻辑，拒绝"AGGREGATOR"角色

### 2. 前端代码迁移
- 移除角色选择界面中的"聚合器"选项
- 更新类型定义，role字段类型改为只接受"PARTICIPANT"

### 3. 测试用例迁移
- 将所有测试中的AGGREGATOR角色改为PARTICIPANT
- 更新预期结果，确保测试通过

## 影响评估

### 新增接口影响
- ✅ **可观测性提升**: 新增的引擎状态接口提供实时监控能力
- ✅ **配置灵活性增强**: 策略查询接口支持动态算法发现和参数配置
- ✅ **前端集成**: 为管理界面提供必要的数据支撑
- ✅ **运维友好**: 支持聚合引擎的健康检查和性能监控

### 功能影响
- ✅ 联邦学习核心功能不受影响
- ✅ 聚合算法实现保持不变
- ✅ 模型训练流程正常运行
- 🆕 增强了系统的监控和配置能力

### 性能影响
- ✅ 无性能影响，聚合仍由后端统一处理
- ✅ 新增接口为轻量级查询，不影响核心性能

### 维护影响
- ✅ 代码复杂度降低
- ✅ 减少角色相关的条件判断
- ✅ 提高系统可维护性
- 🆕 增强了系统的可观测性，便于问题诊断

## 测试建议

### 新增接口测试
1. **引擎状态接口:** 验证聚合引擎状态查询的准确性和实时性
2. **策略查询接口:** 测试各种过滤条件下的策略列表返回
3. **性能监控:** 验证系统指标的准确性和更新频率
4. **集成测试:** 确保新增接口与现有系统的兼容性

### 角色相关测试
5. **API测试:** 验证角色配置接口返回正确
6. **任务创建:** 确保只有PARTICIPANT角色的任务可以创建
7. **聚合流程:** 验证后端聚合功能正常
8. **端到端:** 完整联邦学习流程测试

## 总结

本次v1.4版本包含两个重要变更：

### 🆕 新增聚合引擎监控接口
- 新增了聚合引擎状态查询和策略配置接口
- 提升了系统的可观测性和运维友好性
- 为前端管理界面提供了必要的数据支撑
- 支持动态算法发现和参数配置

### 🔄 架构简化优化
本次修改简化了系统架构，明确了后端和虚拟机的职责分工：
- **后端:** 负责模型聚合、任务协调、状态管理、系统监控
- **虚拟机:** 负责本地训练、数据处理、模型上传

这种设计提高了系统的可扩展性和可维护性，所有虚拟机地位平等，便于动态加入和退出联邦学习任务。同时新增的监控接口增强了系统的可观测性，便于问题诊断和性能优化。