# 后端重构文档套件 - WebSocket v1.5协议升级

## 📋 文档套件概览

**版本**: v1.5.0
**创建时间**: 2025-01-29
**适用范围**: FedUWAComm后端系统完整重构到v1.5协议

本文档套件提供了将现有后端系统从v1.4协议升级到v1.5协议的完整指南，实现assignedDatasetId统一管理和13步标准化联邦学习流程。

## 📚 文档结构

### 🎯 主重构指南
**[federated-learning-v1.5-refactoring-guide.md](./federated-learning-v1.5-refactoring-guide.md)**
- **作用**: 总体重构战略和13步标准化流程设计
- **关键内容**:
  - v1.5协议核心变更：assignedDatasetId完全替代datasetId
  - 13步标准联邦学习链路详细设计
  - 数据库表结构更新和迁移计划
  - 实施时间表和风险评估

### 🔧 后端服务层重构
**[backend-service-refactoring.md](./backend-service-refactoring.md)**
- **作用**: 核心业务逻辑层面的v1.5协议实现
- **关键内容**:
  - FederatedTaskService v1.5任务创建和数据集分配逻辑
  - WebSocketProtocolService v1.5新协议支持
  - DataDistributionService assignedDatasetId管理
  - TrainingDataService数据集解析和分配逻辑
  - **进度跟踪**: 包含详细的实施进度标注

### 🧪 单元测试重构
**[unit-test-refactoring.md](./unit-test-refactoring.md)**
- **作用**: 测试套件升级到v1.5协议验证
- **关键内容**:
  - CompleteFederatedLearningFlowTest完整13步流程测试
  - MockVirtualMachineV15协议v1.5适配
  - assignedDatasetId验证测试用例
  - 破坏性变更测试覆盖

### 🤖 Mock虚拟机重构
**[mock-vm-refactoring.md](./mock-vm-refactoring.md)**
- **作用**: 测试环境中虚拟机模拟器的v1.5协议升级
- **关键内容**:
  - MockVirtualMachine v1.4到v1.5完整迁移
  - assignedDatasetId完全依赖实现
  - DATASET_LIST_QUERY/RESPONSE协议支持
  - v1.5错误模拟和测试适配

## 🔄 协同关系图

```
主重构指南 (总体战略)
    ├── 后端服务层重构 (业务逻辑实现)
    │   ├── FederatedTaskService
    │   ├── WebSocketProtocolService
    │   ├── DataDistributionService
    │   └── TrainingDataService
    │
    ├── 单元测试重构 (测试验证)
    │   ├── CompleteFederatedLearningFlowTestV15
    │   ├── 13步流程测试覆盖
    │   └── assignedDatasetId验证测试
    │
    └── Mock虚拟机重构 (测试环境)
        ├── MockVirtualMachineV15
        ├── v1.5协议模拟
        └── 数据集依赖模拟
```

## 🎯 v1.5核心变更摘要

### 💔 破坏性变更
1. **assignedDatasetId替代datasetId**: 完全移除对v1.4 datasetId的兼容
2. **dataConfig结构变更**: assignedDatasetId现在位于FEDERATED_TASK_START的dataConfig内部
3. **数据集完全依赖**: 虚拟机不再自主生成数据集ID，完全依赖后端分配

### 🆕 新增功能
1. **13步标准化流程**: 完整的联邦学习生命周期管理
2. **DATASET_LIST协议**: 新增DATASET_LIST_QUERY/RESPONSE协议支持
3. **统一ID管理**: 后端UuidUtil作为唯一数据集ID生成源

### 📋 标准联邦学习13步流程
```
阶段A: 任务准备 (步骤1-5)
前端操作 → 查询VM → 数据集上传 → 数据集解析 → 任务创建

阶段B: 数据集分配 (步骤6-9)
任务创建 → 数据集分配 → WebSocket数据集创建 → 数据集查询验证

阶段C: 联邦学习执行 (步骤10-13)
数据集确认 → 数据集存储 → 任务启动 → 联邦学习流程
```

## 📊 实施进度总览

| 模块 | 文档状态 | 实施状态 | 预计耗时 |
|------|---------|----------|----------|
| 🎯 主重构指南 | ✅ 完成 | ⏳ 待开始 | 总计: 5-7小时 |
| 🔧 后端服务层 | ✅ 完成 | ⚠️ 进度: 0% | 2-3小时 |
| 🧪 单元测试 | ✅ 完成 | ⚠️ 进度: 0% | 1-2小时 |
| 🤖 Mock虚拟机 | ✅ 完成 | ⚠️ 进度: 0% | 1小时 |

## 🛠️ 使用指南

### 对于后端开发者
1. **第一步**: 阅读主重构指南了解整体架构变更
2. **第二步**: 按照后端服务层重构文档实施业务逻辑
3. **第三步**: 参考单元测试重构文档验证实现正确性
4. **第四步**: 使用Mock虚拟机重构指南更新测试环境

### 对于测试工程师
1. **第一步**: 重点关注单元测试重构文档
2. **第二步**: 使用Mock虚拟机重构文档理解测试环境变更
3. **第三步**: 参考主重构指南了解测试覆盖要求

### 对于项目经理
1. **第一步**: 通过主重构指南评估项目影响和时间计划
2. **第二步**: 使用各模块文档的进度跟踪监控实施进展
3. **第三步**: 根据实施复杂度分配开发资源

## ⚠️ 重要提醒

### 破坏性变更检查清单
- [ ] 确保所有datasetId引用已替换为assignedDatasetId
- [ ] 验证assignedDatasetId在dataConfig内部而非顶层
- [ ] 确认虚拟机完全依赖后端分配的数据集ID
- [ ] 验证DATASET_LIST_QUERY/RESPONSE协议实现

### 迁移风险点
1. **数据一致性**: 确保数据库迁移过程中数据完整性
2. **协议兼容**: v1.5与现有虚拟机的兼容性测试
3. **性能影响**: 13步流程对系统性能的影响评估
4. **回滚计划**: 如需回滚到v1.4的应急预案

## 📚 相关文档

### v1.5协议文档
- [WebSocket协议文档v1.5](../shared/api/WebSocket/WebSocket协议文档-中心化-简化.md)
- [v1.5修改接口文档](../shared/api/WebSocket/modified/modified-interfaces-v1.5.md)
- [v1.5移除接口文档](../shared/api/WebSocket/removed/removed-interfaces-v1.5.md)

### 联邦学习流程
- [联邦学习完整流程](../shared/api/WebSocket/example/联邦学习完整流程.md)
- [后端职责与实现要求](../shared/api/WebSocket/example/后端职责与实现要求.md)
- [虚拟机职责与实现要求](../shared/api/WebSocket/example/虚拟机职责与实现要求.md)

### 数据库设计
- [数据库架构文档](../shared/database/database_schema.md)
- [MySQL初始化脚本](../shared/database/mysql/init/init_mysql.sql)

## 🔧 技术支持

如在重构过程中遇到技术问题，请：
1. 首先查阅对应的重构文档
2. 检查v1.5协议文档中的相关说明
3. 参考已有的代码示例和测试用例
4. 必要时更新相关文档以反映实际实现

---

这套文档确保了FedUWAComm后端系统能够平滑升级到WebSocket v1.5协议，实现assignedDatasetId统一管理和13步标准化联邦学习流程。