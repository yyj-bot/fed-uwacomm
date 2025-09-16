# 已修改的接口总览 (v1.0)

## 概述
本文档记录了在v1.0系统架构升级过程中被修改的所有接口。这些接口因为功能优化、架构简化或用户体验改进等原因进行了修改。

## 修改时间
- **修改版本**: v1.0
- **修改日期**: 2024年系统架构升级
- **影响范围**: 日志管理功能

---

## 一、日志导出接口修改

### 修改原因
- **简化用户操作**: 从多步异步操作简化为单步同步下载
- **提升用户体验**: 去除等待时间，即点即下载
- **减少系统复杂度**: 移除异步任务管理，降低维护成本
- **提高性能**: 直接生成和返回文件，减少IO操作

### system-log-api-reference.md-4.3 日志下载接口 (已修改)

#### 修改前
**接口地址**: `GET /api/log/export/download/{exportId}`

**特点**:
- 需要先通过 `POST /api/log/export` 创建导出任务
- 通过 `GET /api/log/export/status/{exportId}` 轮询任务状态
- 任务完成后通过此接口下载文件
- 支持异步处理，但操作复杂

**路径参数**:
- exportId: 导出任务ID (string, 必填)

**响应**: 文件流

#### 修改后
**接口地址**: `POST /api/log/download`

**特点**:
- 直接提交下载请求，同步返回文件
- 一步完成，无需任务管理
- 支持实时过滤和格式化
- 操作简单，响应迅速

**请求头**:
```
Authorization: Bearer {token}
Content-Type: application/json
```

**请求参数**:
```json
{
  "level": "INFO",                    // 日志级别过滤，可选 (DEBUG/INFO/WARN/ERROR)
  "category": "SYSTEM",               // 日志类别过滤，可选 (SYSTEM/USER/VM/TASK/DATA/MODEL/SECURITY/PERFORMANCE)
  "vmId": "a1b2c3d4e5f678901234567890123456",           // 虚拟机ID过滤，可选，32位UUID格式
  "taskId": "b2c3d4e5f67890123456789012345678",       // 任务ID过滤，可选，32位UUID格式
  "startTime": "2024-01-01T00:00:00", // 开始时间，可选，ISO 8601格式
  "endTime": "2024-01-02T00:00:00",   // 结束时间，可选，ISO 8601格式
  "keyword": "string",                // 关键词搜索，可选（消息内容）
  "format": "CSV",                    // 导出格式，可选，CSV/JSON/EXCEL，默认CSV
  "includeDetails": true              // 是否包含详细信息，可选，默认true
}
```

**响应**:
- **Content-Type**: `application/octet-stream`
- **Content-Disposition**: `attachment; filename="logs_[filter]_[timestamp].[extension]"`
- **Body**: 文件二进制内容

**文件名规则**:
- 基础格式: `logs_[timestamp].[extension]`
- 包含级别: `logs_info_[timestamp].[extension]`
- 包含类别: `logs_info_system_[timestamp].[extension]`
- 时间戳格式: `yyyyMMdd_HHmmss`

**导出格式说明**:

1. **CSV格式** (默认)
   - 包含详细信息: `日志ID,时间,级别,类别,虚拟机ID,任务ID,消息,详细信息`
   - 简化信息: `时间,级别,类别,消息`
   - 编码: UTF-8
   - 分隔符: 逗号

2. **JSON格式**
   - 包含详细信息: 完整的日志对象数组
   - 简化信息: 仅包含 timestamp, level, category, message 字段
   - 编码: UTF-8

3. **EXCEL格式**
   - 当前实现为CSV格式，客户端可转换为Excel

**错误响应**:
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null
}
```

**使用限制**:
- 最大导出记录数: 10,000条
- 默认时间范围: 最近24小时（如未指定时间范围）
- 权限要求: 仅管理员可访问

#### 修改对比

| 特性 | 修改前 (异步模式) | 修改后 (同步模式) |
|------|-----------------|-----------------|
| **操作步骤** | 3步: 创建任务 → 查询状态 → 下载文件 | 1步: 直接下载 |
| **响应时间** | 需要等待任务处理完成 | 即时响应 |
| **请求方式** | GET (带路径参数) | POST (带请求体) |
| **参数传递** | 通过之前的创建任务接口 | 直接在下载请求中传递 |
| **任务管理** | 需要维护任务状态和文件存储 | 无需任务管理 |
| **文件存储** | 服务器临时存储文件 | 即时生成，无存储 |
| **错误处理** | 任务级别错误追踪 | 请求级别错误处理 |
| **并发处理** | 任务队列管理 | 直接并发处理 |

#### 迁移指南

##### 客户端代码迁移

**旧的异步下载代码**:
```javascript
// 1. 创建导出任务
const exportResponse = await fetch('/api/log/export', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ level: 'INFO', format: 'CSV' })
});
const { exportId } = await exportResponse.json();

// 2. 轮询任务状态
let status = 'PROCESSING';
while (status === 'PROCESSING') {
  await new Promise(resolve => setTimeout(resolve, 1000));
  const statusResponse = await fetch(`/api/log/export/status/${exportId}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const statusData = await statusResponse.json();
  status = statusData.data.status;
}

// 3. 下载文件
if (status === 'COMPLETED') {
  window.open(`/api/log/export/download/${exportId}`);
}
```

**新的同步下载代码**:
```javascript
// 直接下载
const response = await fetch('/api/log/download', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify({ level: 'INFO', format: 'CSV' })
});

if (response.ok) {
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = response.headers.get('Content-Disposition').split('filename=')[1].replace(/"/g, '');
  a.click();
  window.URL.revokeObjectURL(url);
}
```

##### 优势总结

1. **用户体验优化**
   - 减少等待时间
   - 简化操作流程
   - 降低操作复杂度

2. **系统性能提升**
   - 减少数据库查询
   - 降低磁盘IO
   - 减少内存占用

3. **开发维护成本**
   - 简化错误处理逻辑
   - 减少状态管理
   - 降低系统复杂度

---

## 二、权限控制增强

### PermissionInterceptor 路径配置修改

#### 修改内容
更新了日志相关接口的权限控制路径：

**修改前**:
```java
"/api/log/export",  // 旧的异步导出接口
```

**修改后**:
```java
"/api/log/download",  // 新的同步下载接口
```

#### 影响
- 确保新的下载接口受到管理员权限保护
- 维持了与其他日志接口一致的权限控制策略

---

## 三、后端实现改进

### LogService 接口简化

#### 修改前
```java
// 异步导出相关方法
LogExportTaskVO createExportTask(LogExportDTO exportDTO);
LogExportTaskVO getExportStatus(String exportId);
byte[] downloadExport(String exportId);
PageResult<LogExportTaskVO> getExportHistory(Integer page, Integer size, String status);
```

#### 修改后
```java
// 同步导出方法
byte[] generateLogFile(LogExportDTO exportDTO);
```

#### 优势
- 接口更加简洁明确
- 减少方法数量和复杂度
- 提高代码可维护性

### 新增功能特性

1. **智能文件命名**
   - 根据过滤条件自动生成文件名
   - 包含时间戳确保文件唯一性
   - 支持多种格式扩展名

2. **增强的错误处理**
   - 统一的异常处理机制
   - 详细的错误信息返回
   - 优雅的降级处理

3. **性能优化**
   - 限制导出记录数量防止内存溢出
   - 高效的CSV和JSON生成算法
   - 优化的字符串处理和转义

---

## 四、兼容性说明

### 不兼容变更
- 日志导出接口从异步模式改为同步模式
- 接口路径从 `/api/log/export/*` 改为 `/api/log/download`
- 请求方式从GET改为POST
- 参数传递方式发生变化

### 建议措施
1. **及时更新客户端代码**: 按照迁移指南更新相关代码
2. **测试验证**: 确保新接口功能正常
3. **用户通知**: 告知用户操作流程的简化改进

---

## 五、相关文档

- [系统日志API参考文档](../system-log/system-log-api-reference.md) - 完整的日志管理接口文档
- [已移除接口文档](../removed/removed-interfaces-v1.0.md) - 被移除的接口列表
- [HTTP接口导览](../HTTP接口导览.md) - 系统API接口总览

---

## 六、版本信息

- **修改版本**: v1.0
- **修改日期**: 2024年系统架构升级
- **影响范围**: 日志导出功能
- **向下兼容**: 不兼容，需要按照迁移指南更新客户端代码
- **推荐升级**: 强烈建议升级以获得更好的用户体验