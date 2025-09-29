# Scikit-learn 初始模型 JSON 格式指导文档

## 概述

本文档详细说明基于 ModelWrapper 的 Scikit-learn 初始模型 JSON 格式规范。该格式专门设计用于联邦学习系统中，通过 WebSocket 协议在后端与虚拟机之间传输初始模型参数。

## ModelWrapper 兼容性

初始模型 JSON 格式必须与 `python-vm/src/feduwacomm/ml/federated/model_wrapper.py` 中的 ModelWrapper 类完全兼容：

- **参数提取**：JSON 中的 `parameters` 字段对应 `get_parameters()` 方法的返回格式
- **参数设置**：虚拟机通过 `set_parameters()` 方法加载 JSON 参数
- **框架标识**：`model_type` 字段必须设置为 `"sklearn"`

## JSON 格式结构

### 基本结构

```json
{
  "model_metadata": {
    "model_id": "string",
    "model_type": "sklearn",
    "algorithm": "RandomForest",
    "task_type": "regression|classification",
    "created_at": "ISO8601_timestamp",
    "version": "1.0.0"
  },
  "parameters": {
    // ModelWrapper.get_parameters() 兼容的参数格式
  },
  "training_config": {
    // 训练相关配置参数
  },
  "metadata": {
    // 额外的元数据信息
  }
}
```

### 字段说明

#### model_metadata（必需）
- `model_id`：模型唯一标识符
- `model_type`：固定为 "sklearn"，用于 ModelWrapper 识别
- `algorithm`：算法类型（如 "RandomForest"）
- `task_type`：任务类型（"regression" 或 "classification"）
- `created_at`：模型创建时间（ISO8601 格式）
- `version`：模型版本号

#### parameters（必需）
包含 Scikit-learn 模型的实际参数，格式必须与 ModelWrapper 的参数提取逻辑完全一致。

#### training_config（可选）
包含训练配置信息，如特征名称、目标变量等。

#### metadata（可选）
包含其他元数据信息，如性能指标、描述等。

## RandomForest 参数映射

### 回归模型参数（RandomForestRegressor）

基于 ModelWrapper 的参数提取逻辑（`model_wrapper.py:36-48`），RandomForest 回归模型的参数包括：

```json
{
  "parameters": {
    "feature_importances_": [0.15, 0.23, 0.08, 0.19, 0.35],
    "n_estimators": 100
  }
}
```

**注意**：新的 ModelWrapper 实现对 RandomForest 模型进行了优化处理：
- 只存储特征重要性（`feature_importances_`）和树的数量（`n_estimators`）
- 完整的树结构（`estimators_`）因数据量过大不进行传输
- 如果模型具有其他支持的属性（如 `coef_`, `intercept_`），也会被包含在参数中

### 分类模型参数（RandomForestClassifier）

分类模型与回归模型使用相同的简化参数格式：

```json
{
  "parameters": {
    "feature_importances_": [0.15, 0.23, 0.08, 0.19, 0.35],
    "n_estimators": 100
  }
}
```

**注意**：分类模型的类别信息（`n_classes_`, `classes_`）通常在训练过程中自动推断，不需要在初始模型参数中指定。如果模型在训练后具有这些属性，ModelWrapper会自动提取。

### 关键参数说明

**核心参数（必需）**：
- `feature_importances_`：特征重要性数组，长度必须等于特征数量，所有值非负且和接近1.0
- `n_estimators`：决策树的数量，必须为正整数

**可选参数（根据模型类型自动提取）**：
- `coef_`：模型系数（如果模型具有此属性，如线性模型）
- `intercept_`：截距项（如果模型具有此属性）
- `n_classes_`：类别数量（分类模型训练后自动获得）
- `classes_`：类别标签列表（分类模型训练后自动获得）

**注意**：新的ModelWrapper实现采用简化的参数格式，只传输核心参数以减少数据传输量。其他模型属性在训练过程中自动推断和设置。

## 数据类型要求

### 数值类型
- **整数**：使用 JSON number 类型
- **浮点数**：使用 JSON number 类型，保持足够精度
- **数组**：使用 JSON array，支持多维嵌套

### 特殊处理
- **NumPy 数组**：转换为嵌套的 JSON 数组
- **时间戳**：使用 ISO8601 格式字符串
- **空值**：使用 JSON null

## 验证规则

### 必需字段验证
- `model_metadata.model_type` 必须为 "sklearn"
- `model_metadata.model_id` 不能为空
- `parameters` 字段必须存在且非空

### 参数一致性验证
- `feature_importances_` 数组必须存在且包含有效的特征重要性值
- `n_estimators` 必须为正整数
- 如果存在 `classes_` 和 `n_classes_`，则数组长度必须相等（通常在训练后自动生成）

### 数据范围约束
- `feature_importances_` 中的值必须为非负数，且总和应接近 1.0
- `n_estimators` 建议范围：1-1000
- `n_features_` 必须为正整数

## 最佳实践

### 1. 参数初始化
```json
{
  "parameters": {
    "feature_importances_": [0.2, 0.2, 0.2, 0.2, 0.2],
    "n_estimators": 100,
    "n_features_": 5,
    "n_outputs_": 1
  }
}
```

### 2. 特征重要性设置
- 如果没有预训练模型，可以设置均匀分布的特征重要性
- 确保所有值为非负数且总和为 1.0

### 3. 版本兼容性
- 始终包含 `version` 字段以便后续兼容性管理
- 使用语义化版本号（如 "1.0.0"）

## 传输优化建议

### 1. JSON 压缩
- 移除不必要的空白字符
- 对于大型模型，考虑使用压缩算法

### 2. 参数精度
- 浮点数保持适当精度（通常 6-8 位小数）
- 避免过高精度导致传输负担

### 3. 分块传输
- 对于超大模型（>10MB），考虑参数分块传输
- 使用模型检验和确保传输完整性

## 错误处理

### 常见错误
1. **参数类型错误**：确保数值参数为 JSON number 类型
2. **数组维度不匹配**：检查特征重要性数组长度
3. **必需字段缺失**：验证所有必需字段是否存在

### 错误恢复
- 提供默认参数值用于参数缺失的情况
- 实施参数验证并返回具体错误信息
- 支持参数自动修正（如归一化特征重要性）

## 示例文件

参考以下示例文件了解完整格式：
- `random-forest-regressor-example.json` - 回归模型示例
- `random-forest-classifier-example.json` - 分类模型示例
- `model-parameter-schema.json` - 参数验证模式
- `model-validation-template.py` - Python 验证脚本

## 版本历史

- **v1.0.0** (2025-01-15): 初始版本，支持 RandomForest 模型