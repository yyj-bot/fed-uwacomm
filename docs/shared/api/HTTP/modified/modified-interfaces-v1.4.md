# API接口修改文档 v1.4

## 修改概述
**版本:** v1.4
**日期:** 2025-01-23
**修改类型:** 架构简化 - 移除AGGREGATOR角色

## 主要变更

### 1. 联邦学习架构简化
- **变更原因:** 明确后端负责聚合，虚拟机只负责训练的架构设计
- **影响范围:** 所有涉及参与者角色配置的API接口

### 2. 移除AGGREGATOR角色
- **实体类更新:** TaskParticipant.role 注释更新，只支持 PARTICIPANT
- **DTO类更新:** 所有DTO中role字段只支持 PARTICIPANT
- **服务层更新:** 角色配置API只返回 PARTICIPANT 选项
- **文档更新:** 所有API示例移除AGGREGATOR角色引用

## 具体接口变更

### 联邦任务创建接口
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

### 功能影响
- ✅ 联邦学习核心功能不受影响
- ✅ 聚合算法实现保持不变
- ✅ 模型训练流程正常运行

### 性能影响
- ✅ 无性能影响，聚合仍由后端统一处理

### 维护影响
- ✅ 代码复杂度降低
- ✅ 减少角色相关的条件判断
- ✅ 提高系统可维护性

## 测试建议

1. **API测试:** 验证角色配置接口返回正确
2. **任务创建:** 确保只有PARTICIPANT角色的任务可以创建
3. **聚合流程:** 验证后端聚合功能正常
4. **端到端:** 完整联邦学习流程测试

## 总结

本次修改简化了系统架构，明确了后端和虚拟机的职责分工：
- **后端:** 负责模型聚合、任务协调、状态管理
- **虚拟机:** 负责本地训练、数据处理、模型上传

这种设计提高了系统的可扩展性和可维护性，所有虚拟机地位平等，便于动态加入和退出联邦学习任务。