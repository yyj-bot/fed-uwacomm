# 系统架构设计文档

## 📋 概述

FedUWAComm系统采用模块化、分层架构设计，支持联邦学习的水声通信优化与隐私保护。系统具有良好的可扩展性、可维护性和安全性。

## 🏗️ 整体架构

### 架构层次

```
┌─────────────────────────────────────────────────────────────┐
│                    用户界面层 (UI Layer)                      │
├─────────────────────────────────────────────────────────────┤
│                  业务逻辑层 (Business Layer)                  │
├─────────────────────────────────────────────────────────────┤
│                  数据处理层 (Data Layer)                     │
├─────────────────────────────────────────────────────────────┤
│                  基础设施层 (Infrastructure Layer)            │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端界面      │    │   联邦学习      │    │   数据存储      │
│   (Vue.js)      │    │   (SpringBoot)  │    │   (MySQL)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   核心引擎      │
                    │   (Python)      │
                    └─────────────────┘
                                 │
                    ┌─────────────────┐
                    │   仿真引擎      │
                    │   (BELLHOP)     │
                    └─────────────────┘
```

## 🔧 模块设计

### 1. 数据预处理模块

#### 功能职责
- BELLHOP环境文件生成和修复
- 水声仿真数据生成
- 特征提取和数据标准化
- 数据质量检查和验证

#### 核心类
```python
class OceanEnvironmentGenerator:
    """海洋环境数据生成器"""
    
class BellhopFileManager:
    """BELLHOP文件管理器"""
    
class BellhopFeatureExtractor:
    """特征提取器"""
    
class BellhopManager:
    """BELLHOP管理器"""
```

#### 数据流
```
环境参数 → 环境文件生成 → BELLHOP仿真 → 输出文件 → 特征提取 → 数据库存储
```

### 2. 机器学习模块

#### 功能职责
- 随机森林模型训练
- 特征工程和选择
- 模型评估和优化
- 预测结果输出

#### 核心类
```python
class RandomForestModel:
    """随机森林模型"""
    
class FeatureEngineer:
    """特征工程"""
    
class ModelEvaluator:
    """模型评估器"""
    
class PredictionEngine:
    """预测引擎"""
```

#### 训练流程
```
数据加载 → 特征工程 → 模型训练 → 交叉验证 → 模型评估 → 模型保存
```

### 3. 联邦学习模块

#### 功能职责
- 分布式模型训练
- 参数聚合和分发
- 隐私保护机制
- 通信协议管理

#### 核心类
```java
@Component
public class FederatedLearningServer {
    // 联邦学习服务器
}

@Component
public class ModelAggregator {
    // 模型聚合器
}

@Component
public class PrivacyProtector {
    // 隐私保护器
}
```

#### 联邦学习流程
```
初始化模型 → 分发到节点 → 本地训练 → 参数上传 → 聚合更新 → 分发新模型
```

### 4. 通信优化模块

#### 功能职责
- 通信参数优化
- 自适应均衡
- OFDM技术应用
- 性能监控

#### 核心类
```python
class CommunicationOptimizer:
    """通信优化器"""
    
class AdaptiveEqualizer:
    """自适应均衡器"""
    
class OFDMProcessor:
    """OFDM处理器"""
    
class PerformanceMonitor:
    """性能监控器"""
```

### 5. 可视化模块

#### 功能职责
- 数据可视化展示
- 实时监控界面
- 结果分析和报告
- 用户交互

#### 技术栈
- **前端框架**: Vue.js 3.x
- **图表库**: ECharts 5.x
- **UI组件**: Element Plus
- **状态管理**: Pinia

## 🔄 数据流设计

### 主要数据流

#### 1. 仿真数据流
```
用户配置 → 环境生成 → BELLHOP仿真 → 结果解析 → 特征提取 → 数据库存储
```

#### 2. 训练数据流
```
数据库查询 → 数据预处理 → 特征工程 → 模型训练 → 模型评估 → 模型存储
```

#### 3. 联邦学习数据流
```
中心服务器 → 模型分发 → 本地训练 → 参数上传 → 聚合更新 → 模型分发
```

#### 4. 预测数据流
```
输入数据 → 特征提取 → 模型预测 → 结果优化 → 可视化展示
```

### 数据接口设计

#### RESTful API
```python
# 仿真管理
POST /api/simulation/start
GET  /api/simulation/status/{id}
GET  /api/simulation/results/{id}

# 模型管理
POST /api/model/train
GET  /api/model/status/{id}
GET  /api/model/predict

# 联邦学习
POST /api/federated/join
POST /api/federated/upload
GET  /api/federated/status

# 数据查询
GET  /api/data/features
GET  /api/data/statistics
GET  /api/data/export
```

## 🔒 安全设计

### 隐私保护机制

#### 1. 差分隐私
```python
class DifferentialPrivacy:
    """差分隐私保护"""
    
    def add_noise(self, data, epsilon):
        """添加噪声"""
        pass
    
    def clip_gradients(self, gradients, norm_bound):
        """梯度裁剪"""
        pass
```

#### 2. 局部敏感哈希
```python
class LocalitySensitiveHashing:
    """局部敏感哈希"""
    
    def hash_features(self, features):
        """特征哈希"""
        pass
    
    def find_similar(self, query, threshold):
        """查找相似项"""
        pass
```

#### 3. 参数加密
```python
class ParameterEncryption:
    """参数加密"""
    
    def encrypt_parameters(self, params, public_key):
        """加密参数"""
        pass
    
    def decrypt_parameters(self, encrypted_params, private_key):
        """解密参数"""
        pass
```

### 访问控制

#### 1. 身份认证
- JWT令牌认证
- 多因素认证
- 会话管理

#### 2. 权限管理
- 基于角色的访问控制(RBAC)
- 细粒度权限控制
- 审计日志

## 📊 性能设计

### 性能指标

#### 1. 响应时间
- API响应时间 < 1秒
- 页面加载时间 < 3秒
- 模型预测时间 < 5秒

#### 2. 吞吐量
- 并发用户数 > 100
- 数据处理速度 > 1000条/秒
- 模型训练速度 > 100样本/秒

#### 3. 可用性
- 系统可用性 > 99.9%
- 数据备份恢复时间 < 1小时
- 故障恢复时间 < 30分钟

### 优化策略

#### 1. 缓存策略
```python
class CacheManager:
    """缓存管理器"""
    
    def cache_simulation_results(self, key, data):
        """缓存仿真结果"""
        pass
    
    def cache_model_predictions(self, key, predictions):
        """缓存模型预测"""
        pass
```

#### 2. 异步处理
```python
class AsyncProcessor:
    """异步处理器"""
    
    async def process_simulation(self, config):
        """异步处理仿真"""
        pass
    
    async def train_model(self, data):
        """异步训练模型"""
        pass
```

#### 3. 负载均衡
- 水平扩展
- 负载分发
- 故障转移

## 🧪 测试设计

### 测试策略

#### 1. 单元测试
```python
class TestBellhopTools:
    """BELLHOP工具测试"""
    
    def test_env_file_generation(self):
        """测试环境文件生成"""
        pass
    
    def test_feature_extraction(self):
        """测试特征提取"""
        pass
```

#### 2. 集成测试
```python
class TestWorkflow:
    """工作流程测试"""
    
    def test_complete_workflow(self):
        """测试完整工作流程"""
        pass
    
    def test_federated_learning(self):
        """测试联邦学习"""
        pass
```

#### 3. 性能测试
```python
class TestPerformance:
    """性能测试"""
    
    def test_simulation_performance(self):
        """测试仿真性能"""
        pass
    
    def test_model_training_performance(self):
        """测试模型训练性能"""
        pass
```

## 🔧 部署设计

### 部署架构

#### 1. 开发环境
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端开发      │    │   后端开发      │    │   数据库        │
│   (localhost)   │    │   (localhost)   │    │   (localhost)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### 2. 测试环境
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   负载均衡器    │    │   应用服务器    │    │   数据库集群    │
│   (Nginx)       │    │   (Docker)      │    │   (MySQL)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

#### 3. 生产环境
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CDN/负载均衡  │    │   微服务集群    │    │   分布式存储    │
│   (Cloud)       │    │   (K8s)         │    │   (Cloud DB)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 容器化部署

#### Docker配置
```dockerfile
# 后端服务
FROM python:3.9-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["python", "run.py"]

# 前端服务
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
CMD ["npm", "start"]
```

#### Kubernetes配置
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: feduwacomm-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: feduwacomm-backend
  template:
    metadata:
      labels:
        app: feduwacomm-backend
    spec:
      containers:
      - name: backend
        image: feduwacomm/backend:latest
        ports:
        - containerPort: 8000
```

## 📈 监控设计

### 监控指标

#### 1. 系统监控
- CPU使用率
- 内存使用率
- 磁盘使用率
- 网络流量

#### 2. 应用监控
- API响应时间
- 错误率
- 吞吐量
- 用户活跃度

#### 3. 业务监控
- 仿真成功率
- 模型准确率
- 联邦学习效果
- 通信优化效果

### 日志管理

#### 日志级别
- ERROR: 错误信息
- WARN: 警告信息
- INFO: 一般信息
- DEBUG: 调试信息

#### 日志格式
```python
import logging

logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
```

## 🔮 扩展设计

### 水平扩展

#### 1. 微服务拆分
- 仿真服务
- 模型服务
- 联邦学习服务
- 通信优化服务

#### 2. 数据库分片
- 按时间分片
- 按地域分片
- 按功能分片

### 垂直扩展

#### 1. 功能扩展
- 支持更多机器学习算法
- 增加更多通信优化技术
- 扩展可视化功能

#### 2. 性能扩展
- 优化算法性能
- 提升并发处理能力
- 增强数据存储能力

## 📚 文档规范

### 代码文档
- 使用docstring记录函数和类
- 添加类型注解
- 编写README文件

### API文档
- 使用Swagger/OpenAPI
- 提供示例代码
- 记录错误码

### 架构文档
- 系统架构图
- 数据流图
- 部署图 