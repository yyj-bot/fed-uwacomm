"""
联邦学习协调器实现 - 重构为纯执行器模式
职责：被动响应服务器指令，执行本地训练和模型更新
"""

import time
import logging
import joblib
from pathlib import Path
from typing import Dict, Any, Optional
from dataclasses import asdict
import numpy as np

from .config import MLConfig, MLAlgorithm
from .aggregator import FederatedAggregator


class FederatedLearningCoordinator:
    """联邦学习协调器 - 简化为任务执行适配器"""
    
    def __init__(self, task_id: str, config: MLConfig = None):
        """初始化联邦学习协调器
        
        Args:
            task_id: 任务ID
            config: 联邦学习配置
        """
        self.task_id = task_id
        self.config = config or MLConfig()
        self.current_model_params = None
        self.training_history = []
        
        # 设置日志
        self.logger = logging.getLogger(f"FedCoordinator-{task_id}")
        
        # 执行状态
        self.is_initialized = False
        self.current_round = 0
        self.last_training_result = None
    
    def initialize(self, initial_model_params: Optional[Dict[str, Any]] = None) -> bool:
        """初始化协调器
        
        Args:
            initial_model_params: 初始模型参数
            
        Returns:
            bool: 初始化是否成功
        """
        try:
            if initial_model_params:
                self.current_model_params = initial_model_params
            
            self.is_initialized = True
            self.logger.info(f"任务 {self.task_id} 协调器初始化成功")
            return True
            
        except Exception as e:
            self.logger.error(f"协调器初始化失败: {e}")
            return False
    
    def execute_local_training(self, round_num: int, round_config: Dict[str, Any], 
                              client: 'FederatedLearningClient') -> Dict[str, Any]:
        """执行本地训练 - 被动响应模式
        
        Args:
            round_num: 轮次编号
            round_config: 轮次配置
            client: 联邦学习客户端
            
        Returns:
            dict: 训练结果
        """
        if not self.is_initialized:
            raise RuntimeError("协调器未初始化")
        
        self.current_round = round_num
        self.logger.info(f"任务 {self.task_id} 执行第 {round_num} 轮本地训练")
        
        try:
            # 执行本地训练
            training_result = client.local_train(
                global_params=self.current_model_params,
                validation_data=None
            )
            
            # 记录训练结果
            self.last_training_result = training_result
            self.training_history.append({
                'round_num': round_num,
                'training_loss': training_result.get('training_loss', 0),
                'training_accuracy': training_result.get('training_accuracy', 0),
                'num_samples': training_result.get('num_samples', 0),
                'timestamp': time.time()
            })
            
            self.logger.info(f"任务 {self.task_id} 第 {round_num} 轮训练完成")
            return training_result
            
        except Exception as e:
            self.logger.error(f"任务 {self.task_id} 第 {round_num} 轮训练失败: {e}")
            raise
    
    def update_global_model(self, model_params: Dict[str, Any]) -> bool:
        """更新全局模型参数 - 被动接收服务器发送的模型
        
        Args:
            model_params: 新的全局模型参数
            
        Returns:
            bool: 更新是否成功
        """
        try:
            self.current_model_params = model_params
            self.logger.info(f"任务 {self.task_id} 全局模型参数已更新")
            return True
            
        except Exception as e:
            self.logger.error(f"更新全局模型参数失败: {e}")
            return False
    
    def get_current_model_params(self) -> Optional[Dict[str, Any]]:
        """获取当前模型参数
        
        Returns:
            dict: 当前模型参数
        """
        return self.current_model_params
    
    def get_training_statistics(self) -> Dict[str, Any]:
        """获取训练统计信息
        
        Returns:
            dict: 训练统计信息
        """
        return {
            'task_id': self.task_id,
            'current_round': self.current_round,
            'total_rounds_completed': len(self.training_history),
            'is_initialized': self.is_initialized,
            'last_training_result': self.last_training_result,
            'training_history': self.training_history
        }
    
    def cleanup(self):
        """清理协调器资源"""
        self.logger.info(f"清理任务 {self.task_id} 协调器资源")
        self.current_model_params = None
        self.training_history.clear()
        self.last_training_result = None
        self.is_initialized = False

