"""
联邦学习协调器实现
"""

import time
import logging
import joblib
from pathlib import Path
from typing import Dict, Any, TYPE_CHECKING
from dataclasses import asdict
import numpy as np

from .config import MLConfig, MLAlgorithm
from .aggregator import FederatedAggregator

if TYPE_CHECKING:
    from .client import FederatedLearningClient


class FederatedLearningCoordinator:
    """联邦学习协调器（服务器端逻辑）"""
    
    def __init__(self, config: MLConfig = None):
        """初始化联邦学习协调器
        
        Args:
            config: 联邦学习配置
        """
        self.config = config or MLConfig()
        self.global_model_params = None
        self.participating_clients = {}
        self.round_history = []
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger("FedCoordinator")
        
        # 早停机制
        self.best_global_loss = float('inf')
        self.patience_counter = 0
    
    def register_client(self, client: 'FederatedLearningClient') -> bool:
        """注册客户端
        
        Args:
            client: 联邦学习客户端
            
        Returns:
            bool: 注册是否成功
        """
        try:
            self.participating_clients[client.client_id] = client
            self.logger.info(f"客户端已注册: {client.client_id}")
            return True
        except Exception as e:
            self.logger.error(f"注册客户端失败: {e}")
            return False
    
    def start_federated_round(self, round_num: int) -> Dict[str, Any]:
        """开始联邦学习轮次
        
        Args:
            round_num: 轮次编号
            
        Returns:
            dict: 轮次结果
        """
        self.logger.info(f"开始第 {round_num} 轮联邦学习")
        
        if not self.participating_clients:
            raise ValueError("没有注册的客户端")
        
        # 选择参与的客户端
        num_clients = max(1, int(len(self.participating_clients) * self.config.client_fraction))
        selected_clients = list(self.participating_clients.values())[:num_clients]
        
        client_results = []
        client_weights = []
        
        # 并行执行客户端训练（简化版，实际应用中可以使用多线程）
        for client in selected_clients:
            try:
                result = client.local_train(
                    global_params=self.global_model_params,
                    validation_data=None  # 可以传入验证数据
                )
                client_results.append(result)
                client_weights.append(result['num_samples'])
                
            except Exception as e:
                self.logger.error(f"客户端 {client.client_id} 训练失败: {e}")
                continue
        
        if not client_results:
            raise RuntimeError("所有客户端训练都失败了")
        
        # 聚合模型参数
        client_params = [result['parameters'] for result in client_results]
        
        if self.config.algorithm == MLAlgorithm.RANDOM_FOREST:
            self.global_model_params = FederatedAggregator.fedavg_aggregate(
                client_params, client_weights
            )
        elif self.config.algorithm == MLAlgorithm.SVM:
            self.global_model_params = FederatedAggregator.fedprox_aggregate(
                client_params, self.global_model_params, client_weights, self.config.mu
            )
        else:
            raise ValueError(f"不支持的联邦学习算法: {self.config.algorithm}")
        
        # 计算全局指标
        avg_loss = np.mean([result['training_loss'] for result in client_results])
        avg_accuracy = np.mean([result['training_accuracy'] for result in client_results])
        total_samples = sum([result['num_samples'] for result in client_results])
        
        round_result = {
            'round_num': round_num,
            'participating_clients': len(client_results),
            'total_samples': total_samples,
            'avg_training_loss': avg_loss,
            'avg_training_accuracy': avg_accuracy,
            'algorithm': self.config.algorithm.value,
            'convergence_status': self._check_convergence(avg_loss)
        }
        
        self.round_history.append(round_result)
        
        self.logger.info(f"第 {round_num} 轮完成 - 平均损失: {avg_loss:.4f}, "
                        f"平均准确率: {avg_accuracy:.4f}")
        
        return round_result
    
    def _check_convergence(self, current_loss: float) -> str:
        """检查收敛状态
        
        Args:
            current_loss: 当前损失
            
        Returns:
            str: 收敛状态
        """
        if current_loss < self.best_global_loss - self.config.min_delta:
            self.best_global_loss = current_loss
            self.patience_counter = 0
            return "improving"
        else:
            self.patience_counter += 1
            if self.patience_counter >= self.config.patience:
                return "converged"
            else:
                return "stable"
    
    def run_federated_training(self) -> Dict[str, Any]:
        """运行完整的联邦学习训练
        
        Returns:
            dict: 训练结果
        """
        self.logger.info("开始联邦学习训练")
        
        training_start_time = time.time()
        
        for round_num in range(1, self.config.max_rounds + 1):
            try:
                round_result = self.start_federated_round(round_num)
                
                # 检查早停条件
                if round_result['convergence_status'] == 'converged':
                    self.logger.info(f"训练在第 {round_num} 轮收敛")
                    break
                    
            except Exception as e:
                self.logger.error(f"第 {round_num} 轮训练失败: {e}")
                break
        
        training_time = time.time() - training_start_time
        
        final_result = {
            'total_rounds': len(self.round_history),
            'training_time': training_time,
            'final_loss': self.round_history[-1]['avg_training_loss'] if self.round_history else 0,
            'final_accuracy': self.round_history[-1]['avg_training_accuracy'] if self.round_history else 0,
            'convergence_achieved': self.round_history[-1]['convergence_status'] == 'converged' if self.round_history else False,
            'round_history': self.round_history
        }
        
        self.logger.info(f"联邦学习训练完成 - 总轮次: {final_result['total_rounds']}, "
                        f"最终损失: {final_result['final_loss']:.4f}")
        
        return final_result
    
    def get_global_model_params(self) -> Dict[str, Any]:
        """获取全局模型参数
        
        Returns:
            dict: 全局模型参数
        """
        return self.global_model_params
    
    def save_global_model(self, filepath: str) -> bool:
        """保存全局模型
        
        Args:
            filepath: 保存路径
            
        Returns:
            bool: 保存是否成功
        """
        try:
            model_data = {
                'global_parameters': self.global_model_params,
                'config': asdict(self.config),
                'round_history': self.round_history,
                'training_completed': True
            }
            
            Path(filepath).parent.mkdir(parents=True, exist_ok=True)
            joblib.dump(model_data, filepath)
            
            self.logger.info(f"全局模型已保存: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"保存全局模型失败: {e}")
            return False

