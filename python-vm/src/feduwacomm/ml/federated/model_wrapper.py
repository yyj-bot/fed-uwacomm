"""
统一的模型包装器，支持PyTorch和Scikit-learn模型
"""

import copy
import logging
from typing import Dict, Any, Union
import numpy as np

# 可选依赖：PyTorch（如果可用）
try:
    import torch
    TORCH_AVAILABLE = True
except ImportError:
    TORCH_AVAILABLE = False

# 可选依赖：Scikit-learn（如果可用）
try:
    from sklearn.base import BaseEstimator
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False


class ModelWrapper:
    """模型包装器，支持PyTorch和Scikit-learn模型"""
    
    def __init__(self, model: Union[Any, Any], model_type: str = "pytorch"):
        """初始化模型包装器
        
        Args:
            model: 要包装的模型对象
            model_type: 模型类型，"pytorch" 或 "sklearn"
        """
        self.model = model
        self.model_type = model_type.lower()
        self.original_state = None
        
        # 检查依赖可用性
        if self.model_type == "pytorch" and not TORCH_AVAILABLE:
            raise ImportError("PyTorch模型需要安装PyTorch库")
        elif self.model_type == "sklearn" and not SKLEARN_AVAILABLE:
            raise ImportError("Scikit-learn模型需要安装scikit-learn库")
        
        if self.model_type == "pytorch":
            self.original_state = copy.deepcopy(model.state_dict())
        elif self.model_type == "sklearn":
            self.original_state = copy.deepcopy(model)
    
    def get_parameters(self) -> Dict[str, Any]:
        """获取模型参数
        
        Returns:
            dict: 模型参数字典
        """
        if self.model_type == "pytorch":
            return {name: param.data.cpu().numpy() for name, param in self.model.named_parameters()}
        elif self.model_type == "sklearn":
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
        else:
            raise ValueError(f"不支持的模型类型: {self.model_type}")
    
    def set_parameters(self, parameters: Dict[str, Any]) -> bool:
        """设置模型参数
        
        Args:
            parameters: 要设置的参数字典
            
        Returns:
            bool: 设置是否成功
        """
        try:
            if self.model_type == "pytorch":
                state_dict = {}
                for name, param in parameters.items():
                    if isinstance(param, np.ndarray):
                        state_dict[name] = torch.tensor(param)
                    else:
                        state_dict[name] = param
                self.model.load_state_dict(state_dict)
                return True
            elif self.model_type == "sklearn":
                # 对于sklearn模型，设置相关参数
                for key, value in parameters.items():
                    if hasattr(self.model, key):
                        setattr(self.model, key, value)
                return True
            return False
        except Exception as e:
            logging.error(f"设置模型参数失败: {e}")
            return False
    
    def get_parameter_count(self) -> int:
        """获取参数数量
        
        Returns:
            int: 模型参数总数
        """
        if self.model_type == "pytorch":
            return sum(p.numel() for p in self.model.parameters())
        elif self.model_type == "sklearn":
            count = 0
            params = self.get_parameters()
            for key, value in params.items():
                if isinstance(value, np.ndarray):
                    count += value.size
                elif isinstance(value, (int, float)):
                    count += 1
            return count
        return 0

