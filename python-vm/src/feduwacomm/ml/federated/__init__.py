"""
联邦学习模块

提供联邦学习客户端、协调器、聚合器等功能
"""

from .config import (
    MLAlgorithm,
    MLConfig, 
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
# 注意：client.py和coordinator.py已被删除，功能已集成到WebSocket v1.4架构中
from .task_executor import TaskExecutor

__all__ = [
    # 配置和数据结构
    'MLAlgorithm',
    'MLConfig',
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
    'TaskExecutor'
]

