"""
FedUWAComm - 联邦学习水声通信优化系统

主要模块：
- core: 核心功能模块
- ml: 机器学习模块  
- database: 数据库管理模块
- acoustic: 声学模拟模块
- utils: 工具函数模块
"""

from .core import *
from .ml import *
from .database import *
from .acoustic import *
from .utils import *

__version__ = "1.0.0"
__all__ = [
    "core",
    "ml", 
    "database",
    "acoustic",
    "utils"
] 