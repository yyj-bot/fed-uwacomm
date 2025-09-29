"""
联邦学习虚拟机模块

提供完整的联邦学习虚拟机功能，包括：
- HTTP API客户端
- WebSocket通信（待实现）
- 联邦学习算法
- 本地存储管理
- 统一的虚拟机客户端入口
"""

# 保留原有模块
from .feature_extractor import *
from .model_evaluator import *
from .random_forest_trainer import *

# 新增联邦学习虚拟机模块
from .vm_client import VMClient
from .api import (
    VMApiClient, VMApiError, VMApiErrorCode,
    WebSocketClient, ConnectionStatus
)
from .federated import (
    FederatedLearningClient,
    FederatedLearningCoordinator,
    FederatedAggregator,
    ModelWrapper,
    MLConfig,
    MLAlgorithm,
    VMRegisterRequest
)
from .storage import VMStorage
from .utils import SystemMonitor, CryptoUtils

__all__ = [
    # 原有模块
    "feature_extractor",
    "model_evaluator", 
    "random_forest_trainer",
    
    # 联邦学习虚拟机模块
    'VMClient',
    'VMApiClient',
    'VMApiError', 
    'VMApiErrorCode',
    'WebSocketClient',
    'ConnectionStatus',
    'FederatedLearningClient',
    'FederatedLearningCoordinator', 
    'FederatedAggregator',
    'ModelWrapper',
    'MLConfig',
    'MLAlgorithm',
    'VMRegisterRequest',
    'VMStorage',
    'SystemMonitor',
    'CryptoUtils'
] 