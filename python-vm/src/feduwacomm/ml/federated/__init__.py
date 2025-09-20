"""
联邦学习模块

提供联邦学习客户端、协调器、聚合器等功能
"""

from .config import (
    FederatedAlgorithm,
    FederatedConfig, 
    TrainingState,
    VMRegisterRequest,
    SystemInfo,
    Capabilities,
    NetworkConfig,
    SecurityConfig,
    Metadata
)
from .model_wrapper import ModelWrapper
from .aggregator import FederatedAggregator
from .client import FederatedLearningClient
from .coordinator import FederatedLearningCoordinator

__all__ = [
    # 配置和数据结构
    'FederatedAlgorithm',
    'FederatedConfig',
    'TrainingState', 
    'VMRegisterRequest',
    'SystemInfo',
    'Capabilities',
    'NetworkConfig',
    'SecurityConfig',
    'Metadata',
    
    # 核心组件
    'ModelWrapper',
    'FederatedAggregator',
    'FederatedLearningClient',
    'FederatedLearningCoordinator'
]

