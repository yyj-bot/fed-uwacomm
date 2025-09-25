"""
联邦学习客户端实现
"""

import time
import logging
import joblib
from pathlib import Path
from typing import Dict, Any, Tuple, TYPE_CHECKING
from dataclasses import asdict
import pandas as pd
import numpy as np

from .config import MLConfig, TrainingState, MLAlgorithm
from .model_wrapper import ModelWrapper

# Scikit-learn是必需依赖
from sklearn.base import BaseEstimator
from sklearn.metrics import mean_squared_error, accuracy_score


class FederatedLearningClient:
    """联邦学习客户端"""
    
    def __init__(self, client_id: str, model: BaseEstimator, 
                 config: MLConfig = None):
        """初始化联邦学习客户端
        
        Args:
            client_id: 客户端唯一标识
            model: Scikit-learn模型对象
            config: 联邦学习配置
        """
        self.client_id = client_id
        self.model_wrapper = ModelWrapper(model)
        self.config = config or MLConfig()
        self.training_state = TrainingState()
        self.local_data = None
        self.local_labels = None
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(f"FedClient-{client_id}")
        
        # 训练历史
        self.training_history = {
            'rounds': [],
            'local_losses': [],
            'local_accuracies': [],
            'validation_scores': []
        }
    
    def load_local_data(self, X: pd.DataFrame, y: pd.Series) -> bool:
        """加载本地训练数据
        
        Args:
            X: 特征数据
            y: 标签数据
            
        Returns:
            bool: 加载是否成功
        """
        try:
            self.local_data = X
            self.local_labels = y
            self.training_state.total_samples = len(X)
            self.logger.info(f"加载本地数据: {len(X)} 样本, {len(X.columns)} 特征")
            return True
        except Exception as e:
            self.logger.error(f"加载本地数据失败: {e}")
            return False
    
    def local_train(self, global_params: Dict[str, Any] = None, 
                   validation_data: Tuple = None) -> Dict[str, Any]:
        """执行本地训练
        
        Args:
            global_params: 全局模型参数
            validation_data: 验证数据元组 (X_val, y_val)
            
        Returns:
            dict: 训练结果，包含参数和指标
        """
        if self.local_data is None or self.local_labels is None:
            raise ValueError("本地数据未加载")
        
        self.training_state.is_training = True
        self.training_state.error_message = ""
        
        try:
            # 设置全局参数
            if global_params:
                self.model_wrapper.set_parameters(global_params)
            
            # 执行本地训练
            # 训练sklearn模型
            training_result = self._train_sklearn_model(validation_data)
            
            # 更新训练状态
            self.training_state.round_num += 1
            self.training_state.local_epochs_completed += self.config.local_epochs
            self.training_state.last_update_time = time.time()
            
            # 记录训练历史
            self.training_history['rounds'].append(self.training_state.round_num)
            self.training_history['local_losses'].append(training_result.get('final_loss', 0))
            self.training_history['local_accuracies'].append(training_result.get('final_accuracy', 0))
            
            if validation_data:
                val_score = self._evaluate_model(validation_data[0], validation_data[1])
                self.training_history['validation_scores'].append(val_score)
            
            self.logger.info(f"本地训练完成 - 轮次: {self.training_state.round_num}, "
                           f"损失: {training_result.get('final_loss', 0):.4f}")
            
            return {
                'client_id': self.client_id,
                'parameters': self.model_wrapper.get_parameters(),
                'num_samples': self.training_state.total_samples,
                'training_loss': training_result.get('final_loss', 0),
                'training_accuracy': training_result.get('final_accuracy', 0),
                'round_num': self.training_state.round_num
            }
            
        except Exception as e:
            self.training_state.error_message = str(e)
            self.logger.error(f"本地训练失败: {e}")
            raise
        finally:
            self.training_state.is_training = False
    
    def _train_sklearn_model(self, validation_data: Tuple = None) -> Dict[str, Any]:
        """训练Scikit-learn模型
        
        Args:
            validation_data: 验证数据
            
        Returns:
            dict: 训练结果
        """
        model = self.model_wrapper.model
        
        # 对于sklearn模型，执行多次拟合来模拟多个epoch
        losses = []
        accuracies = []
        
        for epoch in range(self.config.local_epochs):
            # 重新拟合模型
            if hasattr(model, 'partial_fit'):
                # 支持增量学习的模型
                model.partial_fit(self.local_data.values, self.local_labels.values)
            else:
                # 重新训练模型
                model.fit(self.local_data.values, self.local_labels.values)
            
            # 计算训练指标
            try:
                predictions = model.predict(self.local_data.values)
                
                if hasattr(model, 'predict_proba'):
                    # 分类任务
                    from sklearn.metrics import accuracy_score, log_loss
                    accuracy = accuracy_score(self.local_labels.values, predictions)
                    # 简化的损失计算
                    loss = 1 - accuracy
                else:
                    # 回归任务
                    from sklearn.metrics import mean_squared_error, r2_score
                    loss = mean_squared_error(self.local_labels.values, predictions)
                    accuracy = r2_score(self.local_labels.values, predictions)
                
                losses.append(loss)
                accuracies.append(accuracy)
                
            except Exception as e:
                self.logger.warning(f"计算训练指标失败: {e}")
                losses.append(0)
                accuracies.append(0)
        
        return {
            'epoch_losses': losses,
            'final_loss': losses[-1] if losses else 0,
            'final_accuracy': accuracies[-1] if accuracies else 0
        }
    
    def _evaluate_model(self, X_val: pd.DataFrame, y_val: pd.Series) -> float:
        """评估模型
        
        Args:
            X_val: 验证特征数据
            y_val: 验证标签数据
            
        Returns:
            float: 评估分数
        """
        try:
            if self.model_wrapper.model_type == "pytorch":
                model = self.model_wrapper.model
                model.eval()
                
                # 确定设备
                device = next(model.parameters()).device
                
                with torch.no_grad():
                    X_tensor = torch.FloatTensor(X_val.values).to(device)
                    y_tensor = torch.FloatTensor(y_val.values).to(device)
                    outputs = model(X_tensor)
                    
                    # 计算R²分数
                    y_mean = torch.mean(y_tensor)
                    ss_tot = torch.sum((y_tensor - y_mean) ** 2)
                    ss_res = torch.sum((y_tensor - outputs.squeeze()) ** 2)
                    r2_score = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0
                    return r2_score.item()
            
            elif self.model_wrapper.model_type == "sklearn":
                model = self.model_wrapper.model
                return model.score(X_val.values, y_val.values)
                
        except Exception as e:
            self.logger.warning(f"模型评估失败: {e}")
            return 0.0
    
    def get_training_status(self) -> Dict[str, Any]:
        """获取训练状态
        
        Returns:
            dict: 训练状态信息
        """
        return {
            'client_id': self.client_id,
            'is_training': self.training_state.is_training,
            'round_num': self.training_state.round_num,
            'local_epochs_completed': self.training_state.local_epochs_completed,
            'total_samples': self.training_state.total_samples,
            'last_update_time': self.training_state.last_update_time,
            'error_message': self.training_state.error_message,
            'parameter_count': self.model_wrapper.get_parameter_count()
        }
    
    def save_checkpoint(self, filepath: str) -> bool:
        """保存检查点
        
        Args:
            filepath: 保存路径
            
        Returns:
            bool: 保存是否成功
        """
        try:
            checkpoint = {
                'client_id': self.client_id,
                'model_parameters': self.model_wrapper.get_parameters(),
                'model_type': self.model_wrapper.model_type,
                'training_state': asdict(self.training_state),
                'config': asdict(self.config),
                'training_history': self.training_history
            }
            
            Path(filepath).parent.mkdir(parents=True, exist_ok=True)
            
            if self.model_wrapper.model_type == "pytorch":
                torch.save(checkpoint, filepath)
            else:
                joblib.dump(checkpoint, filepath)
            
            self.logger.info(f"检查点已保存: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"保存检查点失败: {e}")
            return False
    
    def load_checkpoint(self, filepath: str) -> bool:
        """加载检查点
        
        Args:
            filepath: 检查点文件路径
            
        Returns:
            bool: 加载是否成功
        """
        try:
            if self.model_wrapper.model_type == "pytorch":
                checkpoint = torch.load(filepath)
            else:
                checkpoint = joblib.load(filepath)
            
            # 恢复训练状态
            self.training_state = TrainingState(**checkpoint['training_state'])
            self.training_history = checkpoint['training_history']
            
            # 恢复模型参数
            self.model_wrapper.set_parameters(checkpoint['model_parameters'])
            
            self.logger.info(f"检查点已加载: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"加载检查点失败: {e}")
            return False
