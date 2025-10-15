# API接口移除文档 v1.5

## 移除概述
**版本:** v1.5  
**日期:** 2025-10-04  
**移除类型:** 请求参数调整 / 字段废弃 / 接口删除

v1.5 版本以“先创建初始模型和数据集，再创建联邦学习任务”为核心，完成请求参数精简，并正式下线前端不需调用的模型分发与数据分发接口。请前端与 SDK 及时完成适配。

## 接口移除列表
- `POST /api/model/initial/task/{taskId}/distribute`
- `GET /api/model/initial/distribution/{distributionId}`
- `POST /api/training-data/distribution`
- `POST /api/training-data/distribution/{distributionId}/start`
- `GET /api/training-data/distribution/{distributionId}`

> 说明：上述接口现由后端运维入口或自动化流程封装调用，前端与开放 API 不再暴露。

## 参数移除与替换列表

### 1. 初始模型管理 API
- `POST /api/model/initial/generate`
  - ❌ 移除 `taskId` 请求字段
  - ✅ 新增可选字段 `labels`，返回体增加 `boundTaskId`（默认为 `null`）
- `POST /api/model/initial/upload`
  - ❌ 移除 `taskId` 请求字段
  - ✅ 新增 `labels`、`boundTaskId`
- `GET /api/model/initial/{taskId}`
  - ❌ 废弃原有按任务ID查询的路径
  - ✅ 使用 `GET /api/model/initial/task/{taskId}` 获取任务绑定模型
- `GET /api/model/initial/{taskId}/download`
  - ❌ 路径参数由 `taskId` 调整为 `modelId`：`GET /api/model/initial/{modelId}/download`
- `DELETE /api/model/initial/{taskId}`
  - ❌ 路径参数由 `taskId` 调整为 `modelId`：`DELETE /api/model/initial/{modelId}`

### 2. 联邦学习任务管理 API
- `POST /api/federated/tasks`
  - ❌ 废弃 `modelConfig` 字段
  - ✅ 使用新的 `initialModelConfig`（包含 `mode`、`initialModelId`、`autoGenerateConfig`）
  - ✅ 强制要求 `datasetConfig.datasetId` 指向已上传的数据集
- `PUT /api/federated/tasks/{taskId}/config`
  - ❌ 废弃 `modelConfig` 字段
  - ✅ 使用新的 `initialModelConfig` 配置初始模型

## 迁移建议
1. **前端 / SDK**  
   - 移除创建初始模型时传入的 `taskId` 字段，改为记录返回的 `modelId`
   - 更新任务创建表单，新增 `initialModelConfig` 配置项（支持 AUTO / CUSTOM）
2. **自动化测试**  
   - 调整测试数据构造顺序：先调用模型与数据集接口，再调用任务创建接口
   - 更新断言逻辑，验证响应体中的 `configSummary.initialModel` 与 `bindingStatus`
3. **文档与示例代码**  
   - 引导开发者通过新的 `GET /api/model/initial/task/{taskId}` 查询绑定关系
   - 在项目 README 或 Quick Start 中同步新的三步流程

## 兼容性提示
- 若继续使用旧的请求结构（包含 `taskId` 或 `modelConfig`），后端将返回 400 参数校验错误
- 已存在的任务数据不受影响；只有新建或调整任务时需要遵循 v1.5 流程
- 下载、删除模型时请确认获取的是 `modelId`，避免误用任务ID

## 总结
v1.5 通过移除冗余字段与调整接口路径，明确了“资源先创建、任务后绑定”的使用模式。请尽快更新客户端实现，以免在创建任务或管理初始模型时遇到参数校验错误。
