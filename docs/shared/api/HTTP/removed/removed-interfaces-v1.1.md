# 水声联邦学习系统 已移除接口文档 v1.1

## 概述

本文档记录了在系统版本 v1.1 中移除的 API 接口，包括移除原因和建议的替代方案。

**移除日期**: 2025-09-16
**移除版本**: v1.1
**文档维护**: FedUWAComm Team

---

## 移除的接口列表

### 1. 虚拟机部署管理 API

**移除的接口模块**: 虚拟机部署管理 (VM Deployment Management)

#### 1.1 基本信息
- **原始路径前缀**: `/api/vm/deploy`
- **原始文档**: `docs/shared/api/HTTP/vm/vm-deployment-api-reference.md`
- **移除日期**: 2025-09-16
- **移除原因**: 功能需求变更，当前系统不需要自动化虚拟机部署功能

#### 1.2 移除的具体接口

| 接口路径 | 方法 | 功能描述 | 状态 |
|---------|------|----------|------|
| `/api/vm/deploy` | POST | 创建部署任务 | 已移除 |
| `/api/vm/deploy/{deploymentId}/start` | POST | 启动部署任务 | 已移除 |
| `/api/vm/deploy/{deploymentId}/status` | GET | 查询部署状态 | 已移除 |
| `/api/vm/deploy/{deploymentId}/configure` | POST | 配置已部署虚拟机 | 已移除 |
| `/api/vm/deploy/{deploymentId}/scale` | POST | 扩容部署 | 已移除 |
| `/api/vm/deploy/{deploymentId}` | DELETE | 销毁部署 | 已移除 |
| `/api/vm/deploy` | GET | 获取部署列表 | 已移除 |
| `/api/vm/deploy/templates` | GET | 获取部署模板 | 已移除 |

#### 1.3 移除的功能特性

**自动化部署功能**:
- 多平台支持(Docker, VMware, KVM等)
- 部署模板管理
- 实时部署状态监控
- 批量部署操作
- 资源配额管理
- 弹性扩缩容

**批量管理功能**:
- 批量虚拟机创建
- 统一配置管理
- 集群网络配置
- 自动化健康检查

#### 1.4 替代方案

对于需要虚拟机管理的场景，请使用以下现有接口：

**单个虚拟机管理** (推荐):
- **虚拟机注册**: `POST /api/v1/vm/register`
- **虚拟机列表**: `GET /api/v1/vm/list`
- **虚拟机详情**: `GET /api/v1/vm/{vmId}`
- **虚拟机更新**: `PUT /api/v1/vm/{vmId}`
- **虚拟机删除**: `DELETE /api/v1/vm/{vmId}`
- **虚拟机控制**: `POST /api/v1/vm/{vmId}/start|stop|restart`

**管理员虚拟机操作**:
- **管理员VM接口**: `/api/admin/vm/**`

#### 1.5 迁移指南

如果您的代码中使用了已移除的部署接口，请按以下方式迁移：

**原部署创建流程**:
```javascript
// 已移除 - 不再支持
const deployment = await fetch('/api/vm/deploy', { ... });
const start = await fetch(`/api/vm/deploy/${deploymentId}/start`, { ... });
```

**新的虚拟机管理流程**:
```javascript
// 推荐方式 - 单个虚拟机注册和管理
const vmRegistration = await fetch('/api/v1/vm/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    vmId: "a1b2c3d4e5f678901234567890123456",
    name: "水声联邦学习节点-001",
    ipAddress: "192.168.1.100",
    // ... 其他配置
  })
});

// 查询虚拟机列表
const vmList = await fetch('/api/v1/vm/list');

// 控制虚拟机
const startVm = await fetch(`/api/v1/vm/${vmId}/start`, { method: 'POST' });
```

#### 1.6 配置清理

由于移除了部署管理功能，以下配置项也不再需要：

**WebConfig 清理**:
- 移除了 `/api/vm/deploy/**` 相关的拦截器配置
- 部署管理相关的权限配置已清理

**数据库表**:
- 如果存在部署相关的数据表，请根据实际情况决定是否保留

---

## 注意事项

### 1. 向后兼容性
- 移除的接口**不提供向后兼容**
- 使用这些接口的客户端代码需要更新

### 2. 数据迁移
- 如果系统中存在部署相关的历史数据，请在移除前进行备份
- 考虑是否需要将部署数据转换为单个虚拟机记录

### 3. 功能影响
- 不再支持批量虚拟机部署
- 不再支持部署模板功能
- 不再支持自动化扩缩容

### 4. 未来规划
- 如果未来需要恢复部署功能，将在新版本中重新设计
- 新的部署功能将基于当前的虚拟机管理接口进行扩展

---

## 技术支持

如果您在迁移过程中遇到问题，请：

1. **参考现有文档**: `docs/shared/api/HTTP/vm/vm-api-reference.md`
2. **查看代码实现**: `VmInstanceController.java`
3. **联系开发团队**: FedUWAComm Team

---

## 变更记录

| 日期 | 版本 | 变更内容 | 负责人 |
|------|------|----------|--------|
| 2025-09-16 | v1.1 | 移除虚拟机部署管理API | FedUWAComm Team |

---

*本文档将随着系统演进持续更新，请定期查看最新版本。*