# v1.5.1 快速参考指南

## 核心概念速查

### 1. 数据集标识符变更

| 版本 | 字段名 | 来源 | 说明 |
|------|--------|------|------|
| v1.4 | `datasetId` | VM生成 | ❌ 已移除 |
| v1.5+ | `assignedDatasetId` | 后端生成 | ✅ 唯一标识符 |

### 2. 索引系统

| 索引类型 | 范围 | 用途 |
|----------|------|------|
| `localIndex` | [0, sliceSamples-1] | VM本地访问 |
| `globalIndex` | [startIndex, endIndex] | 全局追溯 |

**验证公式**: `globalIndex = localIndex + startIndex`

### 3. 协议枚举（新增）

```python
# v1.5新增
DATASET_LIST_QUERY
DATASET_LIST_RESPONSE
DATASET_STATUS_QUERY
DATASET_STATUS_RESPONSE
MESSAGE_ERROR
CONNECTION_ERROR
```

### 4. 数据集状态流转

```
PENDING → CREATED → UPLOADING → COMPLETED
                              ↓
                            FAILED
```

## 关键API速查

### DatasetManager

```python
from feduwacomm.ml.storage import DatasetManager

# 初始化
manager = DatasetManager()

# 创建数据集
manager.create_dataset(
    task_id="task-123",
    assigned_dataset_id="dataset-uuid",
    slice_info={
        "startIndex": 0,
        "endIndex": 1999,
        "sliceSamples": 2000,
        "allocationStrategy": "IID"
    }
)

# 追加数据
manager.append_rows(
    assigned_dataset_id="dataset-uuid",
    batch_id="batch-001",
    rows=[{
        "localIndex": 0,
        "globalIndex": 0,
        "data": {...}
    }],
    batch_range={
        "localStartIndex": 0,
        "localEndIndex": 199,
        "globalStartIndex": 0,
        "globalEndIndex": 199
    }
)

# 完成并验证
result = manager.complete_dataset(
    task_id="task-123",
    assigned_dataset_id="dataset-uuid"
)

# 检查结果
verification = result["slice_verification"]
is_complete = verification["isComplete"]
missing = verification["missingIndices"]
```

### SQLite存储

```python
from feduwacomm.ml.storage import VMStorage

storage = VMStorage("vm_storage.db")

# 保存数据集
storage.save_dataset(
    assigned_dataset_id="dataset-uuid",
    task_id="task-123",
    slice_info={...}
)

# 保存数据行
storage.save_dataset_rows(
    assigned_dataset_id="dataset-uuid",
    batch_id="batch-001",
    rows=[...]
)

# 验证完整性
verification = storage.verify_dataset_completeness(
    assigned_dataset_id="dataset-uuid"
)
```

## 协议消息示例

### DATASET_CREATE (v1.5.1)

```json
{
  "type": "DATASET_CREATE",
  "data": {
    "taskId": "task-123",
    "assignedDatasetId": "dataset-uuid",
    "expectedRows": 2000,
    "sliceInfo": {
      "startIndex": 0,
      "endIndex": 1999,
      "sliceSamples": 2000,
      "totalSamples": 10000,
      "sliceIndex": 1,
      "totalSlices": 5,
      "allocationStrategy": "IID"
    }
  }
}
```

### DATASET_APPEND_ROWS (v1.5.1)

```json
{
  "type": "DATASET_APPEND_ROWS",
  "data": {
    "taskId": "task-123",
    "assignedDatasetId": "dataset-uuid",
    "batchId": "batch-001",
    "batchRange": {
      "localStartIndex": 0,
      "localEndIndex": 199,
      "globalStartIndex": 0,
      "globalEndIndex": 199
    },
    "rows": [
      {
        "rowId": "row-001",
        "localIndex": 0,
        "globalIndex": 0,
        "data": {...}
      }
    ]
  }
}
```

### DATASET_COMPLETE (v1.5.1响应)

```json
{
  "type": "DATASET_COMPLETE",
  "data": {
    "taskId": "task-123",
    "assignedDatasetId": "dataset-uuid",
    "finalRowCount": 2000,
    "sliceVerification": {
      "expectedSamples": 2000,
      "actualSamples": 2000,
      "isComplete": true,
      "missingIndices": [],
      "continuityCheck": {
        "hasGaps": false,
        "gapRanges": []
      }
    }
  }
}
```

## 故障排查速查

### 问题：数据集管理器未初始化
```python
# 解决方案
from feduwacomm.ml.storage import DatasetManager
client.dataset_manager = DatasetManager()
```

### 问题：索引验证失败
```python
# 检查公式
expected_global = local_index + start_index
assert global_index == expected_global
```

### 问题：数据不完整
```python
# 查看缺失索引
verification = result["slice_verification"]
missing = verification["missingIndices"]
print(f"缺失索引: {missing}")
```

### 问题：范围超出
```python
# 验证范围
assert global_start >= slice_start_index
assert global_end <= slice_end_index
```

## 日志级别设置

```python
import logging

# 启用详细日志
logging.basicConfig(level=logging.DEBUG)

# 特定模块日志
logging.getLogger("feduwacomm.ml.storage.dataset_manager").setLevel(logging.DEBUG)
logging.getLogger("feduwacomm.ml.websocket.message_router").setLevel(logging.INFO)
```

## 配置迁移速查

### v1.4 → v1.5.1

**移除**:
```python
# ❌ 不再使用
{
    "localTrainingConfig": {
        "datasetId": "dataset-123"
    }
}
```

**新配置**:
```python
# ✅ v1.5.1标准
{
    "dataConfig": {
        "assignedDatasetId": "backend-uuid-generated-id"
    }
}
```

## 数据库查询示例

```sql
-- 查询数据集状态
SELECT assigned_dataset_id, status, actual_rows, expected_rows
FROM datasets
WHERE task_id = 'task-123';

-- 查询缺失的全局索引
WITH expected_indices AS (
  SELECT assigned_dataset_id,
         start_index + LEVEL - 1 AS expected_index
  FROM datasets
  CONNECT BY LEVEL <= (end_index - start_index + 1)
)
SELECT e.expected_index
FROM expected_indices e
LEFT JOIN dataset_rows r
  ON e.assigned_dataset_id = r.assigned_dataset_id
  AND e.expected_index = r.global_index
WHERE r.global_index IS NULL;

-- 检查索引连续性
SELECT assigned_dataset_id,
       global_index,
       LAG(global_index) OVER (ORDER BY global_index) AS prev_index,
       global_index - LAG(global_index) OVER (ORDER BY global_index) AS gap
FROM dataset_rows
WHERE global_index - LAG(global_index) OVER (ORDER BY global_index) > 1;
```

## 性能优化提示

### 批次大小建议
- **小数据集** (<1000行): 100-200行/批次
- **中等数据集** (1000-10000行): 200-500行/批次
- **大数据集** (>10000行): 500-1000行/批次

### 索引优化
```sql
-- 已自动创建的索引
idx_datasets_task_id
idx_datasets_status
idx_dataset_rows_dataset_id
idx_dataset_rows_global_index

-- 如需额外查询优化，可添加
CREATE INDEX idx_dataset_rows_local_index 
ON dataset_rows(assigned_dataset_id, local_index);
```

### 内存优化
```python
# 使用生成器处理大批次
def process_large_dataset(rows):
    for batch in chunk_rows(rows, batch_size=500):
        manager.append_rows(batch_id=..., rows=batch)
```

## 测试命令

```bash
# 运行单元测试
python -m pytest python-vm/src/feduwacomm/ml/storage/test_dataset_manager.py

# 运行集成测试
python -m pytest python-vm/tests/test_dataset_integration.py

# 测试覆盖率
python -m pytest --cov=feduwacomm.ml.storage --cov-report=html
```

## 常用命令

```bash
# 查看数据库
sqlite3 vm_storage.db "SELECT * FROM datasets LIMIT 10;"

# 统计数据集
sqlite3 vm_storage.db "SELECT status, COUNT(*) FROM datasets GROUP BY status;"

# 查看最近数据集
sqlite3 vm_storage.db "SELECT * FROM datasets ORDER BY created_at DESC LIMIT 5;"

# 清理失败数据集
sqlite3 vm_storage.db "DELETE FROM datasets WHERE status='FAILED';"
```

## 兼容性矩阵

| 后端版本 | VM版本 | 兼容性 | 说明 |
|----------|--------|--------|------|
| v1.4 | v1.4 | ✅ | 完全兼容 |
| v1.5 | v1.5 | ✅ | 完全兼容 |
| v1.5.1 | v1.5.1 | ✅ | 完全兼容 |
| v1.5.1 | v1.5 | ✅ | 向后兼容 |
| v1.5 | v1.5.1 | ⚠️ | 部分功能不可用 |
| v1.4 | v1.5+ | ❌ | 不兼容 |
| v1.5+ | v1.4 | ❌ | 不兼容 |

## 联系支持

- 📧 Email: dev@feduwacomm.com
- 📖 完整文档: [V1.5.1-UPGRADE-README.md](./V1.5.1-UPGRADE-README.md)
- 📝 更新日志: [CHANGELOG-v1.5.1.md](./CHANGELOG-v1.5.1.md)

---

**版本**: v1.5.1  
**更新**: 2025-01-30

