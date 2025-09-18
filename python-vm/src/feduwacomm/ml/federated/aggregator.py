"""
联邦学习参数聚合器
"""

from typing import List, Dict, Any
import numpy as np


class FederatedAggregator:
    """联邦学习聚合器"""
    
    @staticmethod
    def fedavg_aggregate(client_params: List[Dict[str, Any]], 
                        client_weights: List[float] = None) -> Dict[str, Any]:
        """FedAvg 聚合算法
        
        Args:
            client_params: 客户端参数列表
            client_weights: 客户端权重列表
            
        Returns:
            dict: 聚合后的参数
        """
        if not client_params:
            return {}
        
        if client_weights is None:
            client_weights = [1.0 / len(client_params)] * len(client_params)
        
        # 归一化权重
        total_weight = sum(client_weights)
        client_weights = [w / total_weight for w in client_weights]
        
        aggregated_params = {}
        
        # 聚合每个参数
        for param_name in client_params[0].keys():
            weighted_sum = None
            
            for client_param, weight in zip(client_params, client_weights):
                param_value = client_param[param_name]
                
                if isinstance(param_value, np.ndarray):
                    if weighted_sum is None:
                        weighted_sum = weight * param_value
                    else:
                        weighted_sum += weight * param_value
                elif isinstance(param_value, (int, float)):
                    if weighted_sum is None:
                        weighted_sum = weight * param_value
                    else:
                        weighted_sum += weight * param_value
            
            aggregated_params[param_name] = weighted_sum
        
        return aggregated_params
    
    @staticmethod
    def fedprox_aggregate(client_params: List[Dict[str, Any]], 
                         server_params: Dict[str, Any],
                         client_weights: List[float] = None,
                         mu: float = 0.01) -> Dict[str, Any]:
        """FedProx 聚合算法（添加正则化项）
        
        Args:
            client_params: 客户端参数列表
            server_params: 服务器参数
            client_weights: 客户端权重列表
            mu: 正则化参数
            
        Returns:
            dict: 聚合后的参数
        """
        # 基础FedAvg聚合
        aggregated = FederatedAggregator.fedavg_aggregate(client_params, client_weights)
        
        # 添加正则化项
        if server_params:
            for param_name in aggregated.keys():
                if param_name in server_params:
                    server_param = server_params[param_name]
                    aggregated_param = aggregated[param_name]
                    
                    if isinstance(aggregated_param, np.ndarray):
                        # 添加向服务器参数的正则化
                        regularization = mu * (aggregated_param - server_param)
                        aggregated[param_name] = aggregated_param - regularization
        
        return aggregated

