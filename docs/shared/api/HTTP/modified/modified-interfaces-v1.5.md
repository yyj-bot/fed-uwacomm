# API接口修改文档 v1.5

## 修改概述
- **版本:** v1.5  
- **日期:** 2025-10-04  
- **变更主题:** 联邦学习任务创建流程前置模型/数据集资源，完善初始模型与数据集文档协同说明

本次修改聚焦于梳理“先准备资源，再创建任务”的业务路径，并将初始模型管理、训练数据管理与联邦任务管理三份文档对齐。同时保留此前 v1.5 已完成的文档规范化成果（例如模型类型枚举与示例统一为随机森林），确保读者一次即可了解所有与初始模型和任务创建相关的改动。

## 主要变更

### 1. 联邦任务创建流程调整
- `POST /api/federated/tasks` 新增 `initialModelConfig` 结构体，支持 `CUSTOM` / `AUTO` 双模式
- 废弃旧的 `modelConfig` 字段；自动生成所需的模型参数转移至 `initialModelConfig.autoGenerateConfig`
- 任务创建示例强调必须传入已上传的 `datasetId`，响应中新增 `configSummary.initialModel`
- `PUT /api/federated/tasks/{taskId}/config` 同步改用 `initialModelConfig`
- `POST /api/federated/tasks/preview-distribution` 与 `POST /api/federated/tasks/validate-participants` 保持可用，用于任务提交前的数据与参与者校验，并在文档中补充示例

### 2. 初始模型 API 解耦任务绑定
- `POST /api/model/initial/generate` 与 `upload` 移除 `taskId`，返回 `boundTaskId: null`、`bindingStatus` 字段
- 新增 `GET /api/model/initial/{modelId}` 与 `GET /api/model/initial/task/{taskId}`，区分模型视角和任务视角的查询
- 下载、删除等操作改为以 `modelId` 为主键；分发接口路径变更为 `POST /api/model/initial/task/{taskId}/distribute`
- 数据模型说明新增 `labels`、`bindings[]` 等字段，使用示例改写为“生成模型 → 上传数据 → 创建任务 → 分发模型”的完整链路

### 3. 训练数据文档补充
- 新增 `### 3.13 与联邦学习任务集成` 小节，明确 `datasetId` 在任务创建时的使用方式
- 强调推荐流程：上传训练数据 → 生成/上传初始模型 → 创建联邦学习任务绑定 `datasetId` 与 `initialModelId`

### 4. 既有文档规范化内容延续
- 仍保留先前 v1.5 中对初始模型文档的标准化（模型类型枚举、示例统一为随机森林等），本文档作为统一记录

## 具体修改内容

### 1. 联邦任务创建接口 (`docs/shared/api/HTTP/federated-task/federated-task-api-reference.md`)
1. **请求体升级**  
   - 引入 `initialModelConfig.mode`、`initialModelId`、`autoGenerateConfig` 字段  
   - `datasetConfig` 明确要求传入已上传数据集的 `datasetId`
2. **响应体增强**  
   - `configSummary` 新增 `initialModel` 摘要，展示绑定模式、模型ID、是否自动生成等信息
3. **配置接口同步**  
   - `PUT /api/federated/tasks/{taskId}/config` 用于切换初始模型模式或调整自动生成参数，并在文档中加入迁移提示

### 2. 初始模型管理接口 (`docs/shared/api/HTTP/model/initial-model-api-reference.md`)
1. **生成/上传接口去掉 `taskId`**，新增 `labels` 可选字段，并在响应中返回 `bindingStatus`
2. **查询接口拆分**  
   - `GET /api/model/initial/{modelId}`：查看模型详情及绑定列表  
   - `GET /api/model/initial/task/{taskId}`：获取特定任务当前使用的初始模型
3. **下载/删除/分发接口调整**  
   - 下载、删除以 `modelId` 为主，分发通过 `/task/{taskId}` 显式指向任务
4. **数据模型与示例更新**  
   - `InitialModelInfo` 增加 `labels`、`bindingStatus`、`bindings[]`、`updatedAt`  
   - 使用示例演示在创建任务前准备初始模型与数据集

### 3. 训练数据管理接口 (`docs/shared/api/HTTP/train-data/training-data-api-reference.md`)
- 增补 `### 3.13 与联邦学习任务集成` 小节，描述 `datasetId` 与 `initialModelId` 在任务创建中的组合使用
- 在流程建议中强调“数据集与初始模型需提前准备”

### 4. 初始模型文档标准化（沿用并与新流程对齐）
- 保留 `1.3 支持的模型类型` 章节，明确 `NEURAL_NETWORK` 与 `RANDOM_FOREST` 的参数结构及推荐场景
- 所有接口示例统一使用 `RANDOM_FOREST` 架构参数；生成/上传示例已调整为不再包含 `taskId`
- `GET /api/model/initial/{modelId}`、`GET /api/model/initial/task/{taskId}`、分发/下载等示例同步展示随机森林配置，确保与最新路径一致
- JavaScript 使用示例更新为“先生成模型（无 taskId）→ 上传数据 → 创建任务绑定模型 ID”，保持与现行流程一致
- 保留数据模型定义章节中对 `RANDOM_FOREST` / `NEURAL_NETWORK` 架构字段的详细说明，并在 `InitialModelInfo` 结构中补充 `labels`、`bindingStatus`
- 兼容性说明继续强调：功能与认证方式未变，属于文档规范化；影响评估结论（准确性提升、开发体验优化）保持有效

## 兼容性说明

### ⚠️ 需重点关注
- 创建/上传初始模型时继续提交 `taskId` 会导致 400 参数错误
- 未按新格式提交 `initialModelConfig` 将触发联邦任务创建失败
- 旧的下载、删除地址若仍使用 `taskId` 将返回 404

### ✅ 未受影响部分
- 基础URL、认证方式（JWT）、任务状态机、参与者配置等无改动
- 现有模型类型（RANDOM_FOREST / NEURAL_NETWORK）与架构参数说明保持不变
- v1.5 早期的文档规范化（模型类型枚举、示例修复）仍然适用

## 影响评估

### 积极影响
- 文档流程与后端实现一致，有利于前后端协作与自动化测试编排
- 清晰区分“资源管理”与“任务管理”，支持初始模型复用与任务追踪
- 增强的配置示例覆盖自动生成与自定义两种模式，降低理解成本

### 风险与缓解
- **前端/脚本适配风险**：需尽快迁移到 `initialModelConfig`，并在创建任务前调用模型与数据集接口  
  _缓解：提供完整示例与迁移指南_
- **历史文档缓存**：若仍引用旧文档可能导致参数错误  
  _缓解：在 README/开发者门户中同步链接_

## 验收标准
1. `POST /api/model/initial/generate` 与 `upload` 请求示例均不再出现 `taskId`
2. `POST /api/federated/tasks` 文档展示 `initialModelConfig`，并说明 `datasetId` 必须预先存在
3. `configSummary` 示例包含 `initialModel` 段落
4. 训练数据文档出现新的 `3.13` 小节，描述任务绑定流程
5. `modified-interfaces-v1.5.md` 与 `removed-interfaces-v1.5.md` 同步记录字段移除与路径调整

## 总结
- v1.5 将初始模型与数据集从任务创建流程中解耦，实现“资源先行、任务绑定”的清晰路径  
- 三份核心文档（初始模型、训练数据、联邦任务）实现跨引用与示例统一，开发者可按文档快速完成完整流程  
- 建议后续在 OpenAPI/Swagger 合同中同步这些字段调整，并考虑提供解绑或多任务复用的补充接口说明
