# 水声联邦学习系统 接口变更记录 v1.1

## 概述

本文档记录了在系统版本 v1.1 中对API接口进行的重大重构，主要解决了虚拟机接口认证混乱的问题。

**变更日期**: 2025-09-16
**变更版本**: v1.1
**变更类型**: 重大重构 (Breaking Changes)
**文档维护**: FedUWAComm Team

---

## 变更背景

### 问题描述
在v1.0版本中，虚拟机相关接口存在认证逻辑混乱的问题：
- VM自身操作（注册、Token刷新）和用户管理操作（列表、控制）混合在同一个路径前缀 `/api/v1/vm` 下
- 拦截器无法正确区分不同性质的操作，导致JWT认证错误
- 用户在测试页面访问VM列表等接口时频繁出现认证失败

### 解决方案
将VM接口按操作性质重新分类，使用不同的路径前缀和认证策略：
1. **VM自身操作** - 保留 `/api/v1/vm` 路径，无需用户JWT认证
2. **用户管理操作** - 迁移到 `/api/vm` 路径，使用用户JWT认证

---

## 详细变更内容

### 1. 控制器重构

#### 1.1 VmInstanceController (`/api/v1/vm`)
**变更内容**: 保留VM自身操作，移除用户管理操作

**保留的接口**:
- `POST /api/v1/vm/register` - 虚拟机注册
- `POST /api/v1/vm/token/refresh` - Token刷新
- `POST /api/v1/vm/{vmId}/heartbeat` - 心跳上报
- `GET /api/v1/vm/health` - 健康检查

**移除的接口**:
- ❌ `GET /api/v1/vm/list` → 迁移到 VmManagementController
- ❌ `GET /api/v1/vm/{vmId}` → 迁移到 VmManagementController
- ❌ `PUT /api/v1/vm/{vmId}` → 迁移到 VmManagementController
- ❌ `DELETE /api/v1/vm/{vmId}` → 迁移到 VmManagementController
- ❌ `POST /api/v1/vm/{vmId}/start` → 迁移到 VmManagementController
- ❌ `POST /api/v1/vm/{vmId}/stop` → 迁移到 VmManagementController
- ❌ `POST /api/v1/vm/{vmId}/restart` → 迁移到 VmManagementController
- ❌ `GET /api/v1/vm/{vmId}/status` → 迁移到 VmManagementController

#### 1.2 VmManagementController (`/api/vm`)
**变更内容**: 新建控制器，承接用户管理VM的操作

**新增的接口**:
- ✅ `GET /api/vm/list` - 虚拟机列表查询
- ✅ `GET /api/vm/{vmId}` - 虚拟机详情查询
- ✅ `PUT /api/vm/{vmId}` - 虚拟机配置更新
- ✅ `DELETE /api/vm/{vmId}` - 虚拟机删除
- ✅ `POST /api/vm/{vmId}/start` - 虚拟机启动
- ✅ `POST /api/vm/{vmId}/stop` - 虚拟机停止
- ✅ `POST /api/vm/{vmId}/restart` - 虚拟机重启
- ✅ `GET /api/vm/{vmId}/status` - 虚拟机状态查询

### 2. 认证拦截器配置更新

#### 2.1 用户JWT认证拦截器
**变更前**:
```java
.addPathPatterns("/api/user/**", "/api/admin/**", "/api/log/**", "/api/v1/vm/**")
.excludePathPatterns(
    "/api/user/register", "/api/user/login",
    "/api/v1/vm/register", "/api/v1/vm/token/refresh",
    "/api/health", "/pages/**", "/assets/**", "/error"
);
```

**变更后**:
```java
.addPathPatterns("/api/user/**", "/api/admin/**", "/api/log/**", "/api/vm/**")
.excludePathPatterns(
    "/api/user/register", "/api/user/login",
    "/api/health", "/pages/**", "/assets/**", "/error"
);
```

#### 2.2 权限拦截器配置
**变更前**:
```java
.addPathPatterns("/api/user/**", "/api/admin/**", "/api/log/**")
```

**变更后**:
```java
.addPathPatterns("/api/user/**", "/api/admin/**", "/api/log/**", "/api/vm/**")
```

### 3. 测试页面更新

#### 3.1 vm-api-test.html
**变更内容**: 更新API调用路径，保持VM自身操作使用v1路径，用户管理操作使用新路径

**路径变更对照表**:

| 操作类型 | 变更前路径 | 变更后路径 | 认证要求 |
|---------|------------|------------|----------|
| VM注册 | `/api/v1/vm/register` | `/api/v1/vm/register` ✅ 保持不变 | 无需认证 |
| Token刷新 | `/api/v1/vm/token/refresh` | `/api/v1/vm/token/refresh` ✅ 保持不变 | 特殊认证 |
| VM列表 | `/api/v1/vm/list` | `/api/vm/list` 🔄 已更改 | 用户JWT |
| VM详情 | `/api/v1/vm/{vmId}` | `/api/vm/{vmId}` 🔄 已更改 | 用户JWT |
| VM更新 | `/api/v1/vm/{vmId}` | `/api/vm/{vmId}` 🔄 已更改 | 用户JWT |
| VM删除 | `/api/v1/vm/{vmId}` | `/api/vm/{vmId}` 🔄 已更改 | 用户JWT |
| VM启动 | `/api/v1/vm/{vmId}/start` | `/api/vm/{vmId}/start` 🔄 已更改 | 用户JWT |
| VM停止 | `/api/v1/vm/{vmId}/stop` | `/api/vm/{vmId}/stop` 🔄 已更改 | 用户JWT |
| VM重启 | `/api/v1/vm/{vmId}/restart` | `/api/vm/{vmId}/restart` 🔄 已更改 | 用户JWT |
| VM状态 | `/api/v1/vm/{vmId}/status` | `/api/vm/{vmId}/status` 🔄 已更改 | 用户JWT |

### 4. 文档更新

#### 4.1 vm-api-reference.md
**变更内容**: 更新API版本为v1.1，重新组织接口分类

**主要变更**:
- API版本从v1.0升级到v1.1
- 重新定义接口分类：VM自身操作 vs 用户管理操作
- 更新认证方式说明
- 更新所有用户管理操作的路径示例
- 明确区分不同接口的认证要求

---

## 迁移指南

### 1. 客户端代码迁移

#### 1.1 JavaScript/前端代码
**需要更新的API调用**:

```javascript
// ❌ 旧的调用方式
const vmList = await fetch('/api/v1/vm/list', { headers: { Authorization: 'Bearer ' + token } });
const vmDetail = await fetch(`/api/v1/vm/${vmId}`, { headers: { Authorization: 'Bearer ' + token } });
const startVm = await fetch(`/api/v1/vm/${vmId}/start`, { method: 'POST', headers: { Authorization: 'Bearer ' + token } });

// ✅ 新的调用方式
const vmList = await fetch('/api/vm/list', { headers: { Authorization: 'Bearer ' + token } });
const vmDetail = await fetch(`/api/vm/${vmId}`, { headers: { Authorization: 'Bearer ' + token } });
const startVm = await fetch(`/api/vm/${vmId}/start`, { method: 'POST', headers: { Authorization: 'Bearer ' + token } });

// ✅ VM自身操作保持不变
const register = await fetch('/api/v1/vm/register', { method: 'POST', body: vmData });
const refreshToken = await fetch('/api/v1/vm/token/refresh', { method: 'POST', body: refreshData });
```

#### 1.2 Python/VM客户端代码
**VM自身操作无需变更**:

```python
# ✅ VM注册和Token刷新保持原有路径
response = requests.post('http://server/api/v1/vm/register', json=vm_data)
response = requests.post('http://server/api/v1/vm/token/refresh', json=refresh_data)
```

### 2. 测试用例迁移

**需要更新测试用例中的URL**:
```python
# ❌ 旧的测试URL
def test_vm_list():
    response = client.get('/api/v1/vm/list', headers=auth_headers)

# ✅ 新的测试URL
def test_vm_list():
    response = client.get('/api/vm/list', headers=auth_headers)
```

### 3. 配置文件迁移

**API网关/反向代理配置**:
```nginx
# 需要更新Nginx配置
location /api/vm/ {
    # 用户管理VM操作，需要认证
    proxy_pass http://backend;
}

location /api/v1/vm/ {
    # VM自身操作，特殊处理
    proxy_pass http://backend;
}
```

---

## 兼容性说明

### 1. 向后兼容性
⚠️ **此次变更为破坏性变更（Breaking Changes）**：
- 用户管理VM操作的API路径发生变化
- 使用旧路径的客户端代码将无法正常工作
- 需要强制升级客户端代码

### 2. VM自身操作兼容性
✅ **VM自身操作保持完全兼容**：
- VM注册和Token刷新接口路径不变
- 已部署的VM客户端无需更新

### 3. 数据兼容性
✅ **数据库结构无变化**：
- 不涉及数据迁移
- 现有VM数据完全保留

---

## 验证清单

### 1. 功能验证
- [ ] VM注册功能正常工作
- [ ] VM Token刷新功能正常工作
- [ ] 用户登录后可以正常查看VM列表
- [ ] 用户可以正常控制VM（启动、停止、重启）
- [ ] 用户可以正常查看VM详情和状态
- [ ] VM删除功能正常工作

### 2. 认证验证
- [ ] VM自身操作无需用户JWT认证
- [ ] 用户管理操作需要有效的用户JWT认证
- [ ] 无效Token会被正确拒绝
- [ ] 权限拦截器正常工作

### 3. 错误处理验证
- [ ] 认证失败返回正确的错误信息
- [ ] 路径不存在返回404错误
- [ ] 服务器错误有适当的错误处理

---

## 性能影响

### 1. 正面影响
- **认证逻辑清晰**：减少了拦截器配置的复杂性
- **路径分离明确**：提高了路由效率
- **减少认证错误**：降低了不必要的认证检查

### 2. 资源影响
- **新增控制器**：增加了一个VmManagementController，内存占用略有增加
- **代码维护性**：提高了代码的可维护性和可读性

---

## 未来规划

### 1. 短期计划
- 监控新接口的使用情况和性能表现
- 收集用户反馈，优化接口设计
- 完善API文档和示例代码

### 2. 长期计划
- 考虑为其他模块应用类似的接口分离策略
- 建立标准化的API设计规范
- 实施更细粒度的权限控制

---

## 技术支持

### 1. 迁移支持
如果在迁移过程中遇到问题，请：
1. 参考本文档的迁移指南
2. 查看更新后的API文档：`vm-api-reference.md`
3. 参考测试页面的实现：`vm-api-test.html`
4. 联系开发团队获取技术支持

### 2. 问题报告
如发现问题，请提供：
- 详细的错误信息
- 请求的URL和参数
- 预期行为和实际行为
- 客户端和服务器版本信息

---

## 变更记录

| 日期 | 版本 | 变更内容 | 负责人 |
|------|------|----------|--------|
| 2025-09-16 | v1.1 | VM接口重构，分离VM自身操作和用户管理操作 | FedUWAComm Team |

---

*本文档是重要的API变更记录，请妥善保存并及时更新相关系统。*