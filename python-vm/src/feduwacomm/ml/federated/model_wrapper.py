"""
统一的模型包装器，专为Scikit-learn模型设计
"""

import copy
import logging
from typing import Dict, Any
import numpy as np

# Scikit-learn依赖
from sklearn.base import BaseEstimator


class ModelWrapper:
    """模型包装器，专为Scikit-learn模型设计"""
    
    def __init__(self, model: BaseEstimator):
        """初始化模型包装器
        
        Args:
            model: Scikit-learn模型对象
        """
        if not isinstance(model, BaseEstimator):
            raise TypeError("模型必须是Scikit-learn的BaseEstimator实例")
            
        self.model = model
        self.model_type = "sklearn"
        self.original_state = copy.deepcopy(model)
    
    def get_parameters(self) -> Dict[str, Any]:
        """获取模型参数
        
        Returns:
            dict: 模型参数字典
        """
        # 对于sklearn模型，获取相关参数
        params = {}
        if hasattr(self.model, 'coef_'):
            params['coef_'] = self.model.coef_
        if hasattr(self.model, 'intercept_'):
            params['intercept_'] = self.model.intercept_
        if hasattr(self.model, 'feature_importances_'):
            params['feature_importances_'] = self.model.feature_importances_
        # 对于RandomForest，获取树的参数
        if hasattr(self.model, 'estimators_'):
            params['n_estimators'] = len(self.model.estimators_)
            # 简化：只存储特征重要性，完整的树结构太大
        return params
    
    def set_parameters(self, parameters: Dict[str, Any]) -> bool:
        """设置模型参数
        
        Args:
            parameters: 要设置的参数字典
            
        Returns:
            bool: 设置是否成功
        """
        try:
            # 对于sklearn模型，设置相关参数
            for key, value in parameters.items():
                if hasattr(self.model, key):
                    setattr(self.model, key, value)
            return True
        except Exception as e:
            logging.error(f"设置模型参数失败: {e}")
            return False
    
    def get_parameter_count(self) -> int:
        """获取参数数量
        
        Returns:
            int: 模型参数总数
        """
        count = 0
        params = self.get_parameters()
        for key, value in params.items():
            if isinstance(value, np.ndarray):
                count += value.size
            elif isinstance(value, (int, float)):
                count += 1
        return count

