"""
WebSocket 模块 - v1.4协议，被动响应式架构，多任务并发
"""

from .client import WebSocketClient
from .message_router import MessageRouter
from .task_context import TaskContext
from .task_manager import TaskManager

__all__ = [
    'WebSocketClient',
    'MessageRouter', 
    'TaskContext',
    'TaskManager'
]

__version__ = '1.4.0'
