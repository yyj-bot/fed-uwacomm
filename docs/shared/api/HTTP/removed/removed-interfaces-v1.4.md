# API接口移除文档 v1.4

## 移除概述
**版本:** v1.4
**日期:** 2025-09-26
**移除类型:** 无接口移除

## 说明

v1.4版本主要进行了以下操作：
- ✅ 新增聚合引擎监控接口
- ✅ 架构简化优化（移除AGGREGATOR角色支持）

**重要提示:** v1.4版本没有移除任何现有的API接口，所有变更都是向后兼容的新增功能。

## 架构变更说明

虽然没有移除具体的API接口，但v1.4版本在架构层面进行了以下简化：

### 移除的概念支持
- **AGGREGATOR角色支持:** 不再接受role为"AGGREGATOR"的参与者配置
- **角色选择选项:** 前端角色选择界面移除"聚合器"选项

### 保留的功能
- ✅ 所有现有API接口保持可用
- ✅ PARTICIPANT角色配置完全兼容
- ✅ 联邦学习核心功能不受影响
- ✅ WebSocket通信协议保持不变

## 迁移指南

### 前端代码调整
如果前端代码中有以下内容需要调整：

#### 角色选择界面
```javascript
// 需要移除的选项
const roles = [
  { value: 'PARTICIPANT', label: '参与者' },
  { value: 'AGGREGATOR', label: '聚合器' }  // ❌ 移除此选项
];

// 更新为
const roles = [
  {
    value: 'PARTICIPANT',
    label: '参与者',
    description: '参与联邦学习训练的客户端节点'
  }
];
```

#### 类型定义调整
```typescript
// 旧的类型定义
type ParticipantRole = 'PARTICIPANT' | 'AGGREGATOR';  // ❌

// 新的类型定义
type ParticipantRole = 'PARTICIPANT';  // ✅
```

### 后端代码调整
- 更新验证逻辑，拒绝role为"AGGREGATOR"的请求
- 更新测试用例，将AGGREGATOR角色改为PARTICIPANT

## 兼容性保证

### ✅ 完全兼容
- 所有现有的API端点继续工作
- PARTICIPANT角色的所有功能保持不变
- 数据库架构保持兼容
- WebSocket协议无变化

### ⚠️ 需要注意
- 新创建的任务不能使用AGGREGATOR角色
- 前端界面需要更新角色选择选项
- 测试代码需要调整角色配置

## 总结

v1.4版本是一个主要以新增功能为主的版本，没有移除任何现有的API接口。主要的"移除"是概念层面的架构简化，通过不再支持AGGREGATOR角色来简化系统设计。

所有现有的联邦学习功能继续正常工作，客户端代码只需要进行少量的界面调整即可完成迁移。