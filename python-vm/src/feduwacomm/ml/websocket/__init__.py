"""
WebSocket 模块 - v1.4 协议实现

基于中心化架构的 WebSocket 通信模块，支持：
- v1.4 简化协议
- 被动响应式架构
- 多任务并发管理
- 任务级别的消息路由
"""

from .client import FederatedLearningClient
from .message_router import MessageRouter
from .task_context import TaskContext
from .task_manager import TaskManager

__all__ = [
    'FederatedLearningClient',
    'MessageRouter', 
    'TaskContext',
    'TaskManager'
]

__version__ = '1.4.0'
