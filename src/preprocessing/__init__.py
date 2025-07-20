"""
预处理模块，包含Bellhop文件处理和特征提取功能
"""

import importlib.util

# 导入模块
from .bellhop_tools import BellhopFileManager, BellhopFeatureExtractor, BellhopManager, OceanEnvironmentGenerator
try:
    from .feature_extractor import FeatureExtractor
except (ImportError, ModuleNotFoundError):
    pass 