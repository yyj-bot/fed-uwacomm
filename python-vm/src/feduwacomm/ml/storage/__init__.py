"""
存储模块

提供SQLite本地存储功能和数据集管理功能
v1.5.1: 添加数据集管理器
"""

from .sqlite_storage import VMStorage
from .dataset_manager import DatasetManager, DatasetInfo, DatasetStatus

__all__ = [
    'VMStorage',
    'DatasetManager',
    'DatasetInfo',
    'DatasetStatus'
]

